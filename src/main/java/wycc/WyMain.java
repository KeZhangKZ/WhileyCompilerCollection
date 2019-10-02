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
import wycc.lang.Package;
import wycc.lang.Command.Option;
import wycc.lang.Command.Template;
import wycc.util.AbstractCommandEnvironment;
import wycc.util.AbstractPluginEnvironment;
import wycc.util.AbstractWorkspace;
import wycc.util.ArrayUtils;
import wycc.util.CommandParser;
import wycc.util.LocalPackageRepository;
import wycc.util.Logger;
import wycc.util.Pair;
import wycc.util.StdModuleContext;
import wycc.util.StdPackageResolver;
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
public class WyMain extends AbstractWorkspace {

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
	 * Schema for local configuration (i.e. which applies to a given workspace).
	 */
	public static Configuration.Schema LOCAL_CONFIG_SCHEMA = Configuration.fromArray(
			Configuration.UNBOUND_STRING_ARRAY(Trie.fromString("workspace/projects"), "list of projects", false));

	/**
	 * Path to the dependency repository within the global root.
	 */
	private static Path.ID DEFAULT_REPOSITORY_PATH = Trie.fromString("repository");

	// ========================================================================
	// Instance Fields
	// ========================================================================

	/**
	 * The root of the build environment itself. From this, all relative paths
	 * within the build environment are determined. For example, the location of
	 * source files or the build configuration files, etc.
	 */
	protected Path.Root localRoot;
	/**
	 * The package resolver used in this workspace.
	 */
	protected Package.Resolver resolver;

	public WyMain(Configuration configuration, String dir, Path.Root repository) throws IOException {
		super(configuration);
		// Setup workspace root
		this.localRoot = new DirectoryRoot(dir, registry);
		// Setup package resolver
		this.resolver = new StdPackageResolver(new LocalPackageRepository(registry, repository));
	}

	@Override
	public Root getRoot() {
		return localRoot;
	}

	@Override
	public Package.Resolver getPackageResolver() {
		return resolver;
	}

	// ==================================================================
	// Main Method
	// ==================================================================

	public static void main(String[] args) throws Exception {
		// Determine system-wide directory
		Path.Root systemRoot = determineSystemRoot();
		// Determine user-wide directory
		Path.Root globalRoot = determineGlobalRoot();
		// Determine workspace directory
		Pair<Path.Root,Path.ID> wrp = determineLocalRootAndProject();
		Path.Root localRoot = wrp.first();
		Path.ID pid = wrp.second();
		// Construct local repository root
		Path.Root repository = globalRoot.createRelativeRoot(DEFAULT_REPOSITORY_PATH);
		// Read the system configuration file
		Configuration system = readConfigFile("wy", systemRoot, SYSTEM_CONFIG_SCHEMA);
		// Read the global configuration file
		Configuration global = readConfigFile("wy", globalRoot, GLOBAL_CONFIG_SCHEMA);
		// Read the global configuration file
		Configuration local = readConfigFile("wy", localRoot, LOCAL_CONFIG_SCHEMA);
		// Construct the merged configuration
		Configuration config = new ConfigurationCombinator(local, global, system);
		// Construct the workspace
		WyMain workspace = new WyMain(config, localRoot.toString(), repository);
		// Select project (if applicable)
		Command.Project project = workspace.open(pid);
		// Construct environment and execute arguments
		Command.Descriptor descriptor = ROOT_DESCRIPTOR(workspace);
		// Parse the given command-line
		Command.Template template = new CommandParser(descriptor).parse(args);
		// Create command instance
		Command instance = descriptor.initialise(workspace);
		// Execute command
		instance.execute(project,template);
		// Flush all modified files to disk
		localRoot.flush();
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
	private static Path.Root determineSystemRoot() throws IOException {
		String whileyhome = System.getenv("WHILEYHOME");
		if (whileyhome == null) {
			System.err.println("error: WHILEYHOME environment variable not set");
			System.exit(-1);
		}
		return new DirectoryRoot(whileyhome,BOOT_REGISTRY);
	}

	/**
	 * Determine the global root. That is, the hidden whiley directory in the user's
	 * home directory (e.g. ~/.whiley).
	 *
	 * @param tool
	 * @return
	 * @throws IOException
	 */
	private static Path.Root determineGlobalRoot() throws IOException {
		String userhome = System.getProperty("user.home");
		String whileydir = userhome + File.separator + ".whiley";
		return new DirectoryRoot(whileydir, BOOT_REGISTRY);
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
	private static Pair<Path.Root,Path.ID> determineLocalRootAndProject() throws IOException {
		// Search for inner configuration.
		File inner = findConfigFile(new File("."));
		if(inner == null) {
			throw new IllegalArgumentException("unable to find build configuration (\"wy.toml\")");
		}
		// Search for enclosing configuration (if applicable).
		File outer = findConfigFile(inner.getParentFile());
		if(outer == null) {
			// No enclosing configuration found.
			return new Pair<>(new DirectoryRoot(inner,BOOT_REGISTRY),Trie.ROOT);
		} else {
			// Calculate relative path
			String path = inner.getPath().replace(outer.getPath(), "").replace(File.separatorChar,'/');
			// Done
			return new Pair<>(new DirectoryRoot(outer,BOOT_REGISTRY),Trie.fromString(path));
		}
	}

	private static File findConfigFile(File dir) {
		// Traverse up the directory hierarchy
		while (dir != null && dir.exists() && dir.isDirectory()) {
			File wyf = new File(dir + File.separator + "wy.toml");
			if (wyf.exists()) {
				return dir;
			}
			// Traverse back up the directory hierarchy looking for a suitable directory.
			dir = dir.getParentFile();
		}
		// If we get here then it means we didn't find a root, therefore just use
		// current directory.
		return null;
	}

	/**
	 * Used for reading the various configuration files prior to instantiating the
	 * main tool itself.
	 */
	private static Content.Registry BOOT_REGISTRY = new DefaultContentRegistry()
			.register(ConfigFile.ContentType, "toml").register(ZipFile.ContentType, "zip");

	/**
	 * Attempt to read a configuration file from a given root.
	 *
	 * @param name
	 * @param root
	 * @return
	 * @throws IOException
	 */
	private static Configuration readConfigFile(String name, Path.Root root, Configuration.Schema... schemas) throws IOException {
		Configuration.Schema schema = Configuration.toCombinedSchema(schemas);
		Path.Entry<ConfigFile> config = root.get(Trie.fromString(name), ConfigFile.ContentType);
		if (config == null) {
			return Configuration.EMPTY(schema);
		}
		try {
			// Read the configuration file
			ConfigFile cf = config.read();
			// Construct configuration according to given schema
			return cf.toConfiguration(schema, false);
		} catch (SyntacticException e) {
			e.outputSourceError(System.out, false);
			System.exit(-1);
			return null;
		}
	}
}
