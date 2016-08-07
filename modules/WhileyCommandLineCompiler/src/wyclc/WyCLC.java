// Copyright (c) 2014, David J. Pearce (djp@ecs.vuw.ac.nz)
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
//    * Redistributions of source code must retain the above copyright
//      notice, this list of conditions and the following disclaimer.
//    * Redistributions in binary form must reproduce the above copyright
//      notice, this list of conditions and the following disclaimer in the
//      documentation and/or other materials provided with the distribution.
//    * Neither the name of the <organization> nor the
//      names of its contributors may be used to endorse or promote products
//      derived from this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
// ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
// WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
// DISCLAIMED. IN NO EVENT SHALL DAVID J. PEARCE BE LIABLE FOR ANY
// DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
// LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
// ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

package wyclc;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;

import wycommon.util.Logger;
import wycommon.util.OptArg;
import wybs.WyBS;
import wybs.lang.Build;
import wybs.util.StdBuildRule;
import wybs.util.StdProject;
import wyfs.WyFS;
import wyfs.lang.Content;
import wyfs.lang.Path;
import wyfs.util.DirectoryRoot;
import wyfs.util.JarFileRoot;
import wyms.util.*;

/**
 * Provides a command-line interface to the Whiley Compiler Collection. This
 * supports loading and configuring modules, as well as compiling files.
 *
 * @author David J. Pearce
 *
 */
public class WyCLC {

	/**
	 * A default error output stream. This is configured separately from
	 * System.err in order to enable Unicode to be displayed.
	 */
	private static PrintStream errout;

	/**
	 * The major version for this module application
	 */
	public static final int MAJOR_VERSION;

	/**
	 * The minor version for this module application
	 */
	public static final int MINOR_VERSION;

	/**
	 * The minor revision for this module application
	 */
	public static final int MINOR_REVISION;

	/**
	 * The base list of recognised command-line options. These will be appended
	 * with additional arguments, as determined by the available activated
	 * modules.
	 */
	private static OptArg[] COMMANDLINE_OPTIONS = new OptArg[] {
			new OptArg("help", "Print this help information"),
			new OptArg("version", "Print version information"),
			new OptArg("verbose",
					"Print detailed information on what the system is doing"),
			new OptArg("L", OptArg.FILELIST,
					"Specify external libraries to include"),
			new OptArg("D", OptArg.FILEDIR,
					"Specify output directory for generated files"),
			new OptArg("T", OptArg.STRING, "Specify target platform"),
			new OptArg("X", OptArg.OPTIONSMAP, "Configure system component") };

	/**
	 * Identifies the location where modules are stored.
	 */
	public static final String PLUGINS_DIR = "lib/plugins/";

	/**
	 * Identifies the location where local modules are stored.
	 */
	public static final String LOCAL_PLUGINS_DIR = "/.wycc/plugins/";

	// ==================================================================
	// Helpers
	// ==================================================================

	/**
	 * Print versioning information to the console.
	 */
	protected static void version() {
		System.out.println("Whiley Compiler Collection version "
				+ MAJOR_VERSION + "." + MINOR_VERSION + "."
				+ MINOR_REVISION);
	}

	/**
	 * Print usage information to the console.
	 */
	protected static void usage() {
		System.out.println("usage: wycc <options> <files>");
		OptArg.usage(System.out, COMMANDLINE_OPTIONS);
	}

	/**
	 * Initialise the error output stream so as to ensure it will display
	 * unicode characters (when possible). Additionally, extract version
	 * information from the enclosing jar file.
	 */
	static {
		try {
			errout = new PrintStream(System.err, true, "UTF-8");
		} catch (Exception e) {
			errout = System.err;
			System.err.println("Warning: terminal does not support unicode");
		}

		// determine version numbering from the MANIFEST attributes
		String versionStr = WyCLC.class.getPackage()
				.getImplementationVersion();
		if (versionStr != null) {
			String[] pts = versionStr.split("\\.");
			MAJOR_VERSION = Integer.parseInt(pts[0]);
			MINOR_VERSION = Integer.parseInt(pts[1]);
			MINOR_REVISION = Integer.parseInt(pts[2]);
		} else {
			System.err.println("WARNING: version numbering unavailable");
			MAJOR_VERSION = 0;
			MINOR_VERSION = 0;
			MINOR_REVISION = 0;
		}
	}

	// ==================================================================
	// Main Method
	// ==================================================================

	public static void main(String[] _args) {
		// --------------------------------------------------------------
		// First, preprocess command-line arguments
		// --------------------------------------------------------------
		boolean verbose = false;
		for(String arg : _args) {
			verbose |= arg.equals("-verbose");
		}

		StdModuleManager manager = activateModuleSystem(verbose);

		// --------------------------------------------------------------
		// Fourth, parse command-line options
		// --------------------------------------------------------------
		ArrayList<String> args = new ArrayList<String>(Arrays.asList(_args));
		Map<String, Object> values = OptArg.parseOptions(args, COMMANDLINE_OPTIONS);

		// Check if we're printing version
		if (values.containsKey("version")) {
			version();
			System.exit(0);
		}
		// Otherwise, if no files to compile specified, then print usage
		if (args.isEmpty() || values.containsKey("help")) {
			usage();
			System.exit(0);
		}

		String target = (String) values.get("T");
		File outputDirectory = (File) values.get("D");
		List<File> libraries = (ArrayList<File>) values.get("L");
		// Apply some defaults
		if(target == null) { target = "wyil"; }
		if(outputDirectory == null) { outputDirectory = new File("."); }
		if(libraries == null) { libraries = Collections.EMPTY_LIST; }

		// --------------------------------------------------------------
		// Fifth, configure module system
		// --------------------------------------------------------------
		Map<String,Map<String,Object>> attributes = (Map<String,Map<String,Object>>) values.get("X");

		if(attributes != null) {
			System.out.println("LOOKING TO CONFIGURE: " + attributes);
		}

		try {
		
		Build.Project project = createBuildProject(target,outputDirectory,libraries,manager);
		
		} catch(IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		
		// --------------------------------------------------------------
		// Finally, deactivate all plugins
		// --------------------------------------------------------------
		manager.stop();
	}

	/**
	 * Activate the plugin system and create the plugin manager. A default
	 * extension point is created for registering functions to be called from
	 * here.
	 *
	 * @param verbose
	 * @param locations
	 * @return
	 */
	public static StdModuleManager activateModuleSystem(boolean verbose) {
		// Determine plugin locations
		List<String> locations = getModuleLocations();

		// create the context and manager
		StdModuleContext context = new StdModuleContext();
		StdModuleManager manager = new StdModuleManager(context, locations);

		if (verbose) {
			manager.setLogger(new Logger.Default(System.err));
			context.setLogger(new Logger.Default(System.err));
		}

		manager.start();

		return manager;
	}

	/**
	 * Create the list of module locations. That is, locations on the filesystem
	 * where the module system will look for plugins.
	 *
	 * @return
	 */
	public static List<String> getModuleLocations() {
		ArrayList<String> locations = new ArrayList<String>();
		locations.add(PLUGINS_DIR);
		String HOME = System.getenv("HOME");
		if(HOME != null) {
			locations.add(HOME + LOCAL_PLUGINS_DIR);
		}
		return locations;
	}
	

	// ========================================================================
	// Function Features
	// ========================================================================
	
	/**
	 * The job of this function is to construct an appropriate project, and
	 * entirely manage the compilation of that project.
	 *
	 * @param targetPlatform
	 *            --- The name of the target platform to generate code for.
	 * @param outputDirectory
	 *            --- The output directory into which to place generated files.
	 *            Note, in the case of files in packages, this directory is the
	 *            root of the package directory structure.
	 * @param libraries
	 *            --- Any additional libraries to include on the WhileyPath.
	 */
	public static Build.Project createBuildProject(String targetPlatform, File outputDirectory, List<File> libraries,
			StdModuleManager manager) throws IOException {
		WyFS wyfs = manager.getInstance(wyfs.WyFS.class);
		WyBS wybs = manager.getInstance(wybs.WyBS.class);		
		Content.Registry registry = wyfs.getContentRegistry();
		System.out.println("Found registry : " + registry);		
		Build.Platform platform = wybs.getBuildPlatform(targetPlatform);
		System.out.println("Searching for build platform...");
		// The output root is the destination for all compiled files.		
		DirectoryRoot outputRoot = new DirectoryRoot(outputDirectory,registry);
		// Construct the roots for every library supplied.
		ArrayList<Path.Root> libraryRoots = new ArrayList<Path.Root>();
		for(File lib : libraries) {
			libraryRoots.add(new JarFileRoot(lib,registry));
		}
		// Create the build project
		return createBuildProject(wybs, platform, outputRoot, outputRoot, libraryRoots);
	}

	public static Build.Project createBuildProject(WyBS wybs, Build.Platform platform, Path.Root srcRoot,
			Path.Root binRoot, List<Path.Root> roots) {
		roots.add(srcRoot);
		roots.add(binRoot);
		// TODO: add virtual roots here for intermediate file formats
		// Construct the project
		StdProject project = new StdProject(roots);
		// Include all files
		Content.Filter<?> includes = Content.filter("**", platform.sourceType());
		Content.Filter<?> excludes = null;
		// Add all necessary build rules
		for (Class<? extends Build.Task> taskClass : platform.builders()) {
			Build.Task task = wybs.getBuildTask(taskClass);
			Build.Task.Instance buildInstance = task.instantiate();
			StdBuildRule rule = new StdBuildRule(buildInstance, srcRoot, includes, excludes, binRoot);
			project.add(rule);
		}
		// Done
		return project;
	}
	
}
