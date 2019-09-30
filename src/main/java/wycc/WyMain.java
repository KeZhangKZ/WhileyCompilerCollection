// Copyright 2011 The Whiley Project Developers
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package wycc;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.regex.Pattern;

import wybs.lang.Build.Project;
import wybs.lang.SyntacticException;
import wybs.lang.SyntacticItem;
import wybs.util.SequentialBuildProject;
import wybs.util.AbstractCompilationUnit.Value;
import wybs.util.AbstractCompilationUnit.Value.UTF8;
import wycc.cfg.ConfigFile;
import wycc.cfg.Configuration;
import wycc.cfg.ConfigurationCombinator;
import wycc.cfg.HashMapConfiguration;
import wycc.commands.Build;
import wycc.commands.Clean;
import wycc.commands.Config;
import wycc.commands.Help;
import wycc.commands.Inspect;
import wycc.commands.Install;
import wycc.commands.Run;
import wycc.lang.Command;
import wycc.lang.Module;
import wycc.lang.Command.Option;
import wycc.lang.Command.Template;
import wycc.util.AbstractCommandEnvironment;
import wycc.util.ArrayUtils;
import wycc.util.CommandParser;
import wycc.util.Logger;
import wycc.util.StdModuleContext;
import wyfs.lang.Content;
import wyfs.lang.Path;
import wyfs.lang.Content.Registry;
import wyfs.lang.Content.Type;
import wyfs.lang.Path.Entry;
import wyfs.lang.Path.Filter;
import wyfs.lang.Path.ID;
import wyfs.lang.Path.Root;
import wyfs.util.DefaultContentRegistry;
import wyfs.util.DirectoryRoot;
import wyfs.util.Trie;
import wyfs.util.ZipFile;
import wyfs.util.ZipFileRoot;

/**
 * Provides a command-line interface to the Whiley Compiler Collection. This is
 * responsible for various tasks, such as loading various configuration files
 * from disk, activating plugins, parsing command-line arguments and actually
 * activating the tool itself.
 *
 * @author David J. Pearce
 *
 */
public class WyMain extends AbstractCommandEnvironment {
	private static final Trie BUILD_PLATFORMS = Trie.fromString("build/platforms");

	/**
	 * This determines what files are included in a package be default (i.e. when
	 * the build/includes attribute is not specified).
	 */
	public static final Value.Array DEFAULT_BUILD_INCLUDES = new Value.Array(
			// Include package description by default
			new Value.UTF8("wy.toml"),
			// Include all wyil files by default
			new Value.UTF8("**/*.wyil"),
			// Include all whiley files by default
			new Value.UTF8("**/*.whiley")
		);

	/**
	 * Schema for system configuration (i.e. which applies to all users).
	 */
	public static Configuration.Schema SYSTEM_RUNTIME_SCHEMA = Configuration.fromArray(
			Configuration.UNBOUND_STRING(Trie.fromString("system/commands"), "list of available commands", false),
			Configuration.UNBOUND_STRING(Trie.fromString("system/platforms"), "list of available platforms", false),
			Configuration.UNBOUND_STRING(Trie.fromString("system/content_types"), "list of available content types",
					false));

	/**
	 * Schema for system configuration (i.e. which applies to all users).
	 */
	public static Configuration.Schema SYSTEM_CONFIG_SCHEMA = Configuration.fromArray(
			Configuration.UNBOUND_STRING(Trie.fromString("plugins/*"), "list of globally installed plugins", true));

	/**
	 * Schema for global configuration (i.e. which applies to all projects for a given user).
	 */
	public static Configuration.Schema GLOBAL_CONFIG_SCHEMA = Configuration.fromArray(
			Configuration.UNBOUND_STRING(Trie.fromString("user/name"), "username", false),
			Configuration.UNBOUND_STRING(Trie.fromString("user/email"), "email", false));

	/**
	 * Schema for local configuration (i.e. which applies to a single project for a given user).
	 */
	public static Configuration.Schema LOCAL_CONFIG_SCHEMA = Configuration.fromArray(
			// Required items
			Configuration.UNBOUND_STRING(Trie.fromString("package/name"), "Name of this package", new Value.UTF8("main")),
			Configuration.UNBOUND_STRING_ARRAY(Trie.fromString("package/authors"), "Author(s) of this package", false),
			Configuration.UNBOUND_STRING(Trie.fromString("package/version"), "Semantic version of this package", false),
			// Build items
			Configuration.UNBOUND_STRING_ARRAY(Trie.fromString("build/platforms"),
					"Target platforms for this package (default just \"whiley\")",
					new Value.Array(new Value.UTF8("whiley"))),
			Configuration.UNBOUND_STRING_ARRAY(Trie.fromString("build/includes"), "Files to include in package",
					DEFAULT_BUILD_INCLUDES),
			Configuration.UNBOUND_STRING(Trie.fromString("build/main"), "Identify main method", false),
			// Optional items
			Configuration.REGEX_STRING(Trie.fromString("dependencies/*"), "Packages this package depends on", false,
					Pattern.compile("\\d+.\\d+.\\d+"))
	);

	/**
	 * The descriptor for the outermost command.
	 *
	 * @author David J. Pearce
	 *
	 */
	public static Command.Descriptor DESCRIPTOR(List<Command.Descriptor> descriptors) {
		return new Command.Descriptor() {

			@Override
			public Schema getConfigurationSchema() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public List<Option.Descriptor> getOptionDescriptors() {
				return Arrays.asList(
						Command.OPTION_FLAG("verbose", "generate verbose information about the build", false),
						Command.OPTION_FLAG("brief", "generate brief output for syntax errors", false));
			}

			@Override
			public Command initialise(Command.Environment environment) {
				return new WyProject(environment, System.out, System.err);
			}

			@Override
			public String getName() {
				return "wy";
			}

			@Override
			public String getDescription() {
				return "Command-line interface for the Whiley Compiler Collection";
			}

			@Override
			public List<Command.Descriptor> getCommands() {
				return descriptors;
			}
		};
	}

	/**
	 * Set of default command descriptors.
	 */
	public static final Command.Descriptor[] DESCRIPTORS = {
			Build.DESCRIPTOR, Clean.DESCRIPTOR, Config.DESCRIPTOR, Help.DESCRIPTOR, Install.DESCRIPTOR,
			Inspect.DESCRIPTOR, Run.DESCRIPTOR
	};

	/**
	 * Set of default content types.
	 */
	public static final Content.Type[] CONTENT_TYPES = {
			ConfigFile.ContentType,
			ZipFile.ContentType
	};

	/**
	 * Path to the dependency repository within the global root.
	 */
	private static Path.ID DEFAULT_REPOSITORY_PATH = Trie.fromString("repository");

	// ========================================================================
	// Instance Fields
	// ========================================================================

	/**
	 * The system root identifies the location of all files and configuration data
	 * that are global to all users.
	 */
	protected Path.Root systemRoot;
	/**
	 * The global root identifies the location of all user-specific but project
	 * non-specific files and other configuration data. For example, this is where
	 * the cache of installed packages lives.
	 */
	protected Path.Root globalRoot;
	/**
	 * The root of the project itself. From this, all relative paths within the
	 * project are determined. For example, the location of source files or the the
	 * build configuration file, etc.
	 */
	protected Path.Root localRoot;

	/**
	 * The root of the package repository. This is used to resolve all external
	 * dependencies.
	 */
	protected Path.Root repositoryRoot;

	public WyMain(String systemDir, String globalDir, String localDir) throws IOException {
		super(null, new Logger.Default(System.err), ForkJoinPool.commonPool(), DESCRIPTORS,CONTENT_TYPES);
		// Add default content types
		// Setup project roots
		this.systemRoot = new DirectoryRoot(systemDir, registry);
		this.globalRoot = new DirectoryRoot(globalDir, registry);
		this.localRoot = new DirectoryRoot(localDir, registry);
		this.repositoryRoot = globalRoot.createRelativeRoot(DEFAULT_REPOSITORY_PATH);
		// Read the system configuration file
		Configuration system = readConfigFile("wy", systemDir, SYSTEM_CONFIG_SCHEMA);
		// Read the global configuration file
		Configuration global = readConfigFile("wy", globalDir, GLOBAL_CONFIG_SCHEMA);
		// Read the local configuration file
		Configuration local = readConfigFile("wy", localDir, getBuildSchema());
		// Create the dynamic configuration
		Configuration runtime = new HashMapConfiguration(SYSTEM_RUNTIME_SCHEMA);
		// Construct the merged configuration
		this.configuration = new ConfigurationCombinator(runtime, local, global, system);
	}

	public Path.Root getSystemRoot() {
		return systemRoot;
	}

	public Path.Root getGlobalRoot() {
		return globalRoot;
	}

	/**
	 * Get the appropriate configuration schema for a project. This defines what
	 * keys are permitted.
	 *
	 * @return
	 */
	public Configuration.Schema getBuildSchema() {
		Configuration.Schema[] schemas = new Configuration.Schema[buildPlatforms.size() + commandDescriptors.size() + 1];
		int index = 0;
		schemas[index++] = LOCAL_CONFIG_SCHEMA;
		for (int i = 0; i != buildPlatforms.size(); ++i) {
			wybs.lang.Build.Platform platform = buildPlatforms.get(i);
			schemas[index++] = platform.getConfigurationSchema();
		}
		for (int i = 0; i != commandDescriptors.size(); ++i) {
			Command.Descriptor cmd = commandDescriptors.get(i);
			schemas[index++] = cmd.getConfigurationSchema();
		}
		return Configuration.toCombinedSchema(schemas);
	}

	public void execute(String[] args) throws Exception {
		try {
			// Construct new project
			Project project = new SequentialBuildProject(this, getRoot());
			// Find and resolve package dependencies
			resolvePackageDependencies(project);
			// Configure package directory structure
			initialisePlatforms(project);
			// Construct the root descriptor
			Command.Descriptor descriptor = DESCRIPTOR(commandDescriptors);
			// Parse the given comand-line
			Command.Template template = new CommandParser(descriptor).parse(args);
			// Create command template
			execute(template);
		} catch(IllegalArgumentException e) {
			System.out.println(e.getMessage());
		}
	}

	public boolean execute(Template template) {
		// Extract options
		boolean verbose = template.getOptions().get("verbose", Boolean.class);
		try {
			//
			if(template.getChild() != null) {
				// Execute a subcommand
				template = template.getChild();
				// Access the descriptor
				Command.Descriptor descriptor = template.getCommandDescriptor();
				// Construct an instance of the command
				Command command = descriptor.initialise(this);
				//
				return command.execute(template);
			} else {
				// Initialise command
				Command cmd = Help.DESCRIPTOR.initialise(this);
				// Execute command
				return cmd.execute(template);
			}
		} catch (SyntacticException e) {
			SyntacticItem element = e.getElement();
			e.outputSourceError(System.err, false);
			if (verbose) {
				printStackTrace(System.err, e);
			}
			return false;
		} catch (Exception e) {
			// FIXME: do something here??
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * Add any declared dependencies to the set of project roots. The challenge here
	 * is that we may need to download, install and compile these dependencies if
	 * they are not currently installed.
	 *
	 * @throws IOException
	 */
	private void resolvePackageDependencies(Project project) throws IOException {
		// FIXME: should produce a wy.lock file?
		Configuration.Schema buildSchema = getBuildSchema();
		// Dig out all the defined dependencies
		List<Path.ID> deps = matchAll(Trie.fromString("dependencies/**"));
		// Resolve each dependencies and add to project roots
		for (int i = 0; i != deps.size(); ++i) {
			Path.ID dep = deps.get(i);
			// Get dependency name
			String name = dep.get(1);
			// Get version string
			UTF8 version = get(UTF8.class, dep);
			// Construct path to the config file
			Trie root = Trie.fromString(name + "-v" + version);
			// Attempt to resolve it.
			if (!repositoryRoot.exists(root, ZipFile.ContentType)) {
				// TODO: employ a package resolver here
				// FIXME: handle better error handling.
				throw new RuntimeException("missing dependency \"" + name + "-" + version + "\"");
			} else {
				// Extract entry for ZipFile
				Path.Entry<ZipFile> zipfile = repositoryRoot.get(root, ZipFile.ContentType);
				// Construct root repesenting this ZipFile
				Path.Root pkgRoot = new ZipFileRoot(zipfile, getContentRegistry());
				// Extract configuration from package
				Path.Entry<ConfigFile> entry = pkgRoot.get(Trie.fromString("wy"),ConfigFile.ContentType);
				if(entry == null) {
					throw new RuntimeException("corrupt package (missing wy.toml) \"" + name + "-" + version + "\"");
				} else {
					ConfigFile pkgcfg = pkgRoot.get(Trie.fromString("wy"),ConfigFile.ContentType).read();
					// Construct a package representation of this root.
					wybs.lang.Build.Package pkg = new Package(pkgRoot,pkgcfg.toConfiguration(buildSchema));
					// Add a relative ZipFile root
					project.getPackages().add(pkg);
				}
			}
		}
	}

	private static class Package implements wybs.lang.Build.Package {
		private final Path.Root root;
		private final Configuration configuration;

		public Package(Path.Root root, Configuration configuration) {
			this.root = root;
			this.configuration = configuration;
		}

		@Override
		public Configuration getConfiguration() {
			return configuration;
		}

		@Override
		public Root getRoot() {
			return root;
		}
	}

	/**
	 * Setup the various roots based on the target platform(s). This requires going
	 * through and adding roots for all source and intermediate files.
	 *
	 * @throws IOException
	 */
	private void initialisePlatforms(Project project) throws IOException {
		List<wybs.lang.Build.Platform> platforms = getTargetPlatforms();
		//
		for (int i = 0; i != platforms.size(); ++i) {
			wybs.lang.Build.Platform platform = platforms.get(i);
			// Apply current configuration
			platform.initialise(this, project);
		}
	}


	/**
	 * Get the list of declared target platforms for this project. This is
	 * determined by the attribute "build.platforms" in the project (wy.toml) build
	 * file.
	 *
	 * @return
	 */
	public List<wybs.lang.Build.Platform> getTargetPlatforms() {
		ArrayList<wybs.lang.Build.Platform> targetPlatforms = new ArrayList<>();
		// Ensure target platforms are specified
		if(hasKey(BUILD_PLATFORMS)) {
			Value.UTF8[] targetPlatformNames = get(Value.Array.class, BUILD_PLATFORMS).toArray(Value.UTF8.class);
			// Get list of all build platforms.
			List<wybs.lang.Build.Platform> platforms = getBuildPlatforms();
			// Check each platform for inclusion
			for (int i = 0; i != platforms.size(); ++i) {
				wybs.lang.Build.Platform platform = platforms.get(i);
				// Convert name to UTF8 value (ugh)
				Value.UTF8 name = new Value.UTF8(platform.getName().getBytes());
				// Determine whether is a target platform or not
				if (ArrayUtils.firstIndexOf(targetPlatformNames, name) >= 0) {
					targetPlatforms.add(platform);
				}
			}
		}
		// Done
		return targetPlatforms;
	}


	// ==================================================================
	// Main Method
	// ==================================================================

	public static void main(String[] args) throws Exception {
		// Determine system-wide directory
		String systemDir = determineSystemRoot();
		// Determine user-wide directory
		String globalDir = determineGlobalRoot();
		// Determine project directory
		String localDir = determineLocalRoot();
		// Construct environment and execute arguments
		new WyMain(systemDir,globalDir,localDir).execute(args);
		// Done
		System.exit(0);
	}

	// ==================================================================
	// Helpers
	// ==================================================================

	/**
	 * Determine the system root. That is, the installation directory for the
	 * compiler itself.
	 *
	 * @param tool
	 * @return
	 * @throws IOException
	 */
	private static String determineSystemRoot() throws IOException {
		String whileyhome = System.getenv("WHILEYHOME");
		if (whileyhome == null) {
			System.err.println("error: WHILEYHOME environment variable not set");
			System.exit(-1);
		}
		return whileyhome;
	}

	/**
	 * Determine the global root. That is, the hidden whiley directory in the user's
	 * home directory (e.g. ~/.whiley).
	 *
	 * @param tool
	 * @return
	 * @throws IOException
	 */
	private static String determineGlobalRoot() throws IOException {
		String userhome = System.getProperty("user.home");
		String whileydir = userhome + File.separator + ".whiley";
		return whileydir;
	}

	/**
	 * Determine where the root of this project is. This is the nearest enclosing
	 * directory containing a "wy.toml" file. The point is that we may be operating
	 * in some subdirectory of the project and want the tool to automatically search
	 * out the real root for us.
	 *
	 * @return
	 * @throws IOException
	 */
	private static String determineLocalRoot() throws IOException {
		// Determine current working directory
		File dir = new File(".");
		// Traverse up the directory hierarchy
		while (dir != null && dir.exists() && dir.isDirectory()) {
			File wyf = new File(dir + File.separator + "wy.toml");
			if (wyf.exists()) {
				return dir.getPath();
			}
			// Traverse back up the directory hierarchy looking for a suitable directory.
			dir = dir.getParentFile();
		}
		// If we get here then it means we didn't find a root, therefore just use
		// current directory.
		return ".";
	}

	/**
	 * Used for reading the various configuration files prior to instantiating the
	 * main tool itself.
	 */
	private static Content.Registry BOOT_REGISTRY = new DefaultContentRegistry().register(ConfigFile.ContentType,
			"toml");

	/**
	 * Attempt to read a configuration file from a given root.
	 *
	 * @param name
	 * @param root
	 * @return
	 * @throws IOException
	 */
	private static Configuration readConfigFile(String name, String dir, Configuration.Schema... schemas) throws IOException {
		Configuration.Schema schema = Configuration.toCombinedSchema(schemas);
		DirectoryRoot root = new DirectoryRoot(dir, BOOT_REGISTRY);
		Path.Entry<ConfigFile> config = root.get(Trie.fromString(name), ConfigFile.ContentType);
		if (config == null) {
			return Configuration.EMPTY(schema);
		}
		try {
			// Read the configuration file
			ConfigFile cf = config.read();
			// Construct configuration according to given schema
			return cf.toConfiguration(schema);
		} catch (SyntacticException e) {
			e.outputSourceError(System.out, false);
			System.exit(-1);
			return null;
		}
	}

	/**
	 * Print a complete stack trace. This differs from Throwable.printStackTrace()
	 * in that it always prints all of the trace.
	 *
	 * @param out
	 * @param err
	 */
	private static void printStackTrace(PrintStream out, Throwable err) {
		out.println(err.getClass().getName() + ": " + err.getMessage());
		for (StackTraceElement ste : err.getStackTrace()) {
			out.println("\tat " + ste.toString());
		}
		if (err.getCause() != null) {
			out.print("Caused by: ");
			printStackTrace(out, err.getCause());
		}
	}
}
