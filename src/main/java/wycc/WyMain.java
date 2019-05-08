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

import wybs.lang.SyntacticException;
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
import wycc.util.CommandParser;
import wycc.util.Logger;
import wycc.util.StdModuleContext;
import wyfs.lang.Content;
import wyfs.lang.Path;
import wyfs.lang.Content.Registry;
import wyfs.lang.Content.Type;
import wyfs.lang.Path.Entry;
import wyfs.util.DirectoryRoot;
import wyfs.util.Trie;
import wyfs.util.ZipFile;

/**
 * Provides a command-line interface to the Whiley Compiler Collection. This is
 * responsible for various tasks, such as loading various configuration files
 * from disk, activating plugins, parsing command-line arguments and actually
 * activating the tool itself.
 *
 * @author David J. Pearce
 *
 */
public class WyMain implements Command {
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

	// ========================================================================
	// Instance Fields
	// ========================================================================

	/**
	 * List of all known content types to the system.
	 */
	private ArrayList<Content.Type<?>> contentTypes = new ArrayList<>();

	/**
	 * List of all known commands registered by plugins.
	 */
	private ArrayList<Command.Descriptor> commandDescriptors = new ArrayList<>();

	/**
	 * List of all known build platforms registered by plugins.
	 */
	private ArrayList<wybs.lang.Build.Platform> buildPlatforms = new ArrayList<>();

	/**
	 * The master registry which provides knowledge of all file types used within
	 * the system.
	 */
	private Content.Registry registry = new Content.Registry() {

		@Override
		public String suffix(Type<?> t) {
			return t.getSuffix();
		}

		@Override
		public void associate(Entry<?> e) {
			for (Content.Type<?> ct : contentTypes) {
				if (ct.getSuffix().equals(e.suffix())) {
					e.associate((Content.Type) ct, null);
					return;
				}
			}
			e.associate((Content.Type) Content.BinaryFile, null);
		}
	};

	/**
	 * Provides the default plugin context.
	 */
	private final StdModuleContext context = new StdModuleContext();

	/**
	 * The complete configuration for this execution. This basically contains
	 * everything configurable about the system.
	 */
	private final Configuration configuration;

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
	 * Standard log output
	 */
	protected Logger logger;

	/**
	 * Top-level executor used for compiling all projects within this environment.
	 */
	protected ExecutorService executor;

	public WyMain(String systemDir, String globalDir, String localDir) throws IOException {
		// Add default content types
		this.contentTypes.add(ConfigFile.ContentType);
		this.contentTypes.add(ZipFile.ContentType);
		// Add default commands
		this.commandDescriptors.add(Build.DESCRIPTOR);
		this.commandDescriptors.add(Clean.DESCRIPTOR);
		this.commandDescriptors.add(Config.DESCRIPTOR);
		this.commandDescriptors.add(Help.DESCRIPTOR);
		this.commandDescriptors.add(Install.DESCRIPTOR);
		this.commandDescriptors.add(Inspect.DESCRIPTOR);
		this.commandDescriptors.add(Run.DESCRIPTOR);
		// Setup project roots
		this.systemRoot = new DirectoryRoot(systemDir, registry);
		this.globalRoot = new DirectoryRoot(globalDir, registry);
		this.localRoot = new DirectoryRoot(localDir, registry);
		// Read the system configuration file
		Configuration system = readConfigFile("wy", systemDir, SYSTEM_CONFIG_SCHEMA);
		// Activate plugins
		createTemplateExtensionPoint();
		createContentTypeExtensionPoint();
		createBuildPlatformExtensionPoint();
		activateDefaultPlugins(system);
		// Read the global configuration file
		Configuration global = readConfigFile("wy", globalDir, GLOBAL_CONFIG_SCHEMA);
		// Read the local configuration file
		Configuration local = readConfigFile("wy", localDir, getBuildSchema());
		// Create the dynamic configuration
		Configuration runtime = new HashMapConfiguration(SYSTEM_RUNTIME_SCHEMA);
		// Construct the merged configuration
		this.configuration = new ConfigurationCombinator(runtime, local, global, system);
		this.logger = new Logger.Default(System.err);
		// Construct the underlying executor
		this.executor = ForkJoinPool.commonPool();

	}

	public Registry getContentRegistry() {
		return registry;
	}

	public List<Type<?>> getContentTypes() {
		return contentTypes;
	}

	public List<Command.Descriptor> getCommandDescriptors() {
		return commandDescriptors;
	}

	/**
	 * Get the list of available build platforms. These help determine what the
	 * valid build targets are.
	 *
	 * @return
	 */
	public List<wybs.lang.Build.Platform> getBuildPlatforms() {
		return buildPlatforms;
	}

	public Path.Root getSystemRoot() {
		return systemRoot;
	}

	public Path.Root getGlobalRoot() {
		return globalRoot;
	}

	public Path.Root getLocalRoot() {
		return localRoot;
	}

	public Logger getLogger() {
		return logger;
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

	@Override
	public Descriptor getDescriptor() {
		return null;
	}

	@Override
	public void initialise() {
	}

	@Override
	public void finalise() {
	}

	@Override
	public boolean execute(Command.Template template) {
		// TODO Auto-generated method stub
		return false;
	}

	public void execute(String[] args) throws Exception {
		try {
			// Construct the root descriptor
			Command.Descriptor descriptor = WyProject.DESCRIPTOR(commandDescriptors);
			// Parse the given comand-line
			Command.Template pipeline = new CommandParser(descriptor).parse(args);
			// Create command instance
			Command instance = descriptor.initialise(this, configuration);
			// Initialise command
			instance.initialise();
			// Execute the command (if applicable)
			instance.execute(pipeline);
			// Finalise command
			instance.finalise();
		} catch(IllegalArgumentException e) {
			System.out.println(e.getMessage());
		}
	}

	/**
	 * Create the Build.Template extension point. This is where plugins register
	 * their primary functionality for constructing a specific build project.
	 *
	 * @param context
	 * @param templates
	 */
	private void createTemplateExtensionPoint() {
		context.create(Command.Descriptor.class, new Module.ExtensionPoint<Command.Descriptor>() {
			@Override
			public void register(Command.Descriptor command) {
				commandDescriptors.add(command);
			}
		});
	}

	/**
	 * Create the Content.Type extension point.
	 *
	 * @param context
	 * @param templates
	 */
	private void createContentTypeExtensionPoint() {
		context.create(Content.Type.class, new Module.ExtensionPoint<Content.Type>() {
			@Override
			public void register(Content.Type contentType) {
				contentTypes.add(contentType);
			}
		});
	}


	/**
	 * Create the Content.Type extension point.
	 *
	 * @param context
	 * @param templates
	 */
	private void createBuildPlatformExtensionPoint() {
		context.create(wybs.lang.Build.Platform.class, new Module.ExtensionPoint<wybs.lang.Build.Platform>() {
			@Override
			public void register(wybs.lang.Build.Platform platform) {
				buildPlatforms.add(platform);
			}
		});
	}
	/**
	 * Activate the default set of plugins which the tool uses. Currently this list
	 * is statically determined, but eventually it will be possible to dynamically
	 * add plugins to the system.
	 *
	 * @param verbose
	 * @param locations
	 * @return
	 */
	private void activateDefaultPlugins(Configuration global) {
		// Determine the set of install plugins
		List<Path.ID> plugins = global.matchAll(Trie.fromString("plugins/*"));
		// start modules
		for (Path.ID id : plugins) {
			UTF8 activator = global.get(UTF8.class, id);
			try {
				Class<?> c = Class.forName(activator.toString());
				Module.Activator instance = (Module.Activator) c.newInstance();
				instance.start(context);
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		}
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
	private static Content.Registry BOOT_REGISTRY = new Content.Registry() {

		@Override
		public String suffix(Type<?> t) {
			return t.getSuffix();
		}

		@Override
		public void associate(Entry<?> e) {
			if(e.suffix().equals("toml")) {
				e.associate((Content.Type) ConfigFile.ContentType, null);
			}
		}
	};

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
