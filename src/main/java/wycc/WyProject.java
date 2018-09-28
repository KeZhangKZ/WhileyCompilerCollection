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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.*;

import wybs.lang.Build;
import wybs.lang.SyntacticItem;
import wybs.lang.SyntaxError;
import wybs.util.AbstractCompilationUnit.Value;
import wybs.util.AbstractCompilationUnit.Value.UTF8;
import wybs.util.StdBuildRule;
import wybs.util.StdProject;
import wycc.cfg.ConfigFile;
import wycc.cfg.Configuration;
import wycc.cfg.Configuration.Schema;
import wycc.commands.Help;
import wycc.lang.Command;
import wycc.util.ArrayUtils;
import wycc.util.CommandParser;
import wyfs.lang.Content;
import wyfs.lang.Path;
import wyfs.lang.Path.Entry;
import wyfs.lang.Path.Root;
import wyfs.util.ZipFileRoot;
import wyfs.util.Trie;
import wyfs.util.ZipFile;

public class WyProject implements Command {
	private static final Trie BUILD_PLATFORMS = Trie.fromString("build/platforms");

	/**
	 * The descriptor for this command.
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
			public Command initialise(Command environment, Configuration configuration) {
				return new WyProject((WyMain) environment, configuration, System.out, System.err);
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
	 * Configuration options specifically required by the tool.
	 */
	public static Configuration.Schema SCHEMA = Configuration.fromArray();

	/**
	 * Path to the dependency repository within the global root.
	 */
	private static Path.ID REPOSITORY_PATH = Trie.fromString("repository");

	// ==================================================================
	// Instance Fields
	// ==================================================================

	/**
	 * The outermost environment.
	 */
	protected final WyMain environment;

	/**
	 * The combined configuration
	 */
	protected final Configuration configuration;

	/**
	 * Contains project information.
	 */
	protected final StdProject project;

	/**
	 * List of target build platforms.
	 */
	private final Value.UTF8[] platforms;

	/**
	 * Provides a generic place to which normal output should be directed. This
	 * should eventually be replaced.
	 */
	private final PrintStream sysout;

	/**
	 * Provides a generic place to which error output should be directed. This
	 * should eventually be replaced.
	 */
	private final PrintStream syserr;

	// ==================================================================
	// Constructors
	// ==================================================================

	public WyProject(WyMain environment, Configuration configuration, OutputStream sysout, OutputStream syserr) {
		this.configuration = configuration;
		this.environment = environment;
		this.project = new StdProject();
		this.platforms = configuration.get(Value.Array.class, BUILD_PLATFORMS).toArray(Value.UTF8.class);
		this.sysout = new PrintStream(sysout);
		this.syserr = new PrintStream(syserr);
	}

	// ==================================================================
	// Command stuff
	// ==================================================================

	public WyMain getParent() {
		return environment;
	}

	@Override
	public Command.Descriptor getDescriptor() {
		return DESCRIPTOR(Collections.EMPTY_LIST);
	}

	/**
	 * Get the list of declared target platforms for this project. This is
	 * determined by the attribute "build.platforms" in the project (wy.toml) build
	 * file.
	 *
	 * @return
	 */
	public List<wybs.lang.Build.Platform> getTargetPlatforms() {
		// Get list of all build platforms.
		List<wybs.lang.Build.Platform> platforms = environment.getBuildPlatforms();
		ArrayList<Build.Platform> targetPlatforms = new ArrayList<>();
		// Check each platform for inclusion
		for (int i = 0; i != platforms.size(); ++i) {
			Build.Platform platform = platforms.get(i);
			// Convert name to UTF8 value (ugh)
			Value.UTF8 name = new Value.UTF8(platform.getName().getBytes());
			// Determine whether is a target platform or not
			if (ArrayUtils.firstIndexOf(this.platforms, name) >= 0) {
				targetPlatforms.add(platform);
			}
		}
		// Done
		return targetPlatforms;
	}

	public Build.Project getBuildProject() {
		return project;
	}

	/**
	 * Get the root of the package repository. This is the global directory in which
	 * all installed packages are found.
	 *
	 * @return
	 * @throws IOException
	 */
	public Path.Root getRepositoryRoot() throws IOException {
		Path.Root root = environment.getGlobalRoot().createRelativeRoot(REPOSITORY_PATH);
		// TODO: create repository if it doesn't exist.
		return root;
	}

	@Override
	public void initialise() {
		try {
			// Find and resolve package dependencies
			resolvePackageDependencies();
			// Configure package directory structure
			configurePlatforms();
			// Find dependencies
		} catch (IOException e) {
			// FIXME
			throw new RuntimeException(e);
		}
	}

	@Override
	public void finalise() {
		// Flush any roots
		// Deactivate plugins
		// Write back configuration files?
		try {
			project.flush();
		} catch (IOException e) {
			// FIXME
			throw new RuntimeException(e);
		}
	}

	@Override
	public boolean execute(Template template) {
		// Extract options
		boolean verbose = template.getOptions().get("verbose", Boolean.class);
		try {
			if(template.getChild() != null) {
				// Execute a subcommand
				template = template.getChild();
				// Access the descriptor
				Command.Descriptor descriptor = template.getCommandDescriptor();
				// Construct an instance of the command
				Command command = descriptor.initialise(this, configuration);
				//
				return command.execute(template);
			} else {
				// Initialise command
				Command cmd = Help.DESCRIPTOR.initialise(this, configuration);
				// Execute command
				return cmd.execute(template);
			}
		} catch (SyntaxError e) {
			SyntacticItem element = e.getElement();
			e.outputSourceError(syserr, false);
			if (verbose) {
				printStackTrace(syserr, e);
			}
			return false;
		} catch (Exception e) {
			// FIXME: do something here??
			e.printStackTrace();
			return false;
		}
	}

	// ==================================================================
	// Other
	// ==================================================================

	/**
	 * Get the content registry associated with this tool instance.
	 *
	 * @return
	 */
	public Content.Registry getRegistry() {
		return environment.getContentRegistry();
	}

	public void build(Collection<Path.Entry<?>> delta, Build.Graph graph) throws Exception {
		project.build(delta,graph);
	}

	// ==================================================================
	// Helpers
	// ==================================================================

	/**
	 * Add any declared dependencies to the set of project roots. The challenge here
	 * is that we may need to download, install and compile these dependencies if
	 * they are not currently installed.
	 *
	 * @throws IOException
	 */
	private void resolvePackageDependencies() throws IOException {
		// FIXME: should produce a wy.lock file?
		Configuration.Schema buildSchema = environment.getBuildSchema();
		// Global root is where all dependencies will be stored
		Path.Root repository = getRepositoryRoot();
		// Dig out all the defined dependencies
		List<Path.ID> deps = configuration.matchAll(Trie.fromString("dependencies/**"));
		// Resolve each dependencies and add to project roots
		for (int i = 0; i != deps.size(); ++i) {
			Path.ID dep = deps.get(i);
			// Get dependency name
			String name = dep.get(1);
			// Get version string
			UTF8 version = configuration.get(UTF8.class, dep);
			// Construct path to the config file
			Trie root = Trie.fromString(name + "-v" + version);
			// Attempt to resolve it.
			if (!repository.exists(root, ZipFile.ContentType)) {
				// TODO: employ a package resolver here
				// FIXME: handle better error handling.
				throw new RuntimeException("missing dependency \"" + name + "-" + version + "\"");
			} else {
				// Extract entry for ZipFile
				Path.Entry<ZipFile> zipfile = repository.get(root, ZipFile.ContentType);
				// Construct root repesenting this ZipFile
				Path.Root pkgRoot = new ZipFileRoot(zipfile, environment.getContentRegistry());
				// Extract configuration from package
				Path.Entry<ConfigFile> entry = pkgRoot.get(Trie.fromString("wy"),ConfigFile.ContentType);
				if(entry == null) {
					throw new RuntimeException("corrupt package (missing wy.toml) \"" + name + "-" + version + "\"");
				} else {
					ConfigFile pkgcfg = pkgRoot.get(Trie.fromString("wy"),ConfigFile.ContentType).read();
					// Construct a package representation of this root.
					Build.Package pkg = new Package(pkgRoot,pkgcfg.toConfiguration(buildSchema));
					// Add a relative ZipFile root
					project.getPackages().add(pkg);
				}
			}
		}
	}

	/**
	 * Setup the various roots based on the target platform(s). This requires going
	 * through and adding roots for all source and intermediate files.
	 *
	 * @throws IOException
	 */
	private void configurePlatforms() throws IOException {
		Path.Root root = environment.getLocalRoot();
		List<Build.Platform> platforms = getTargetPlatforms();
		//
		for (int i = 0; i != platforms.size(); ++i) {
			Build.Platform platform = platforms.get(i);
			// Apply current configuration
			platform.apply(configuration);
			// Configure Source root
			Path.Root srcRoot = platform.getSourceRoot(root);
			project.getRoots().add(srcRoot);
			// Configure Binary root
			Path.Root binRoot = platform.getTargetRoot(root);
			project.getRoots().add(binRoot);
			// Initialise build task
			Build.Task task = platform.initialise(project);
			// Add the appropriate build rule(s)
			project.add(
					new StdBuildRule(task, srcRoot, platform.getSourceFilter(), platform.getTargetFilter(), binRoot));
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

	private static class Package implements Build.Package {
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
}
