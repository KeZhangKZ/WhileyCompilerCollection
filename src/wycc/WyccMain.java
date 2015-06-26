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

package wycc;

import java.io.File;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import wycc.util.FunctionExtension;
import wycc.util.OptArg;
import jplug.lang.Feature;
import jplug.lang.Plugin;
import jplug.util.*;

/**
 * Provides a command-line interface to the Whiley Compiler Collection. This
 * supports loading and configuring plugins, as well as compiling files.
 *
 * @author David J. Pearce
 *
 */
public class WyccMain {

	/**
	 * A default error output stream. This is configured separately from
	 * System.err in order to enable Unicode to be displayed.
	 */
	private static PrintStream errout;

	/**
	 * The major version for this plugin application
	 */
	public static final int MAJOR_VERSION;

	/**
	 * The minor version for this plugin application
	 */
	public static final int MINOR_VERSION;

	/**
	 * The minor revision for this plugin application
	 */
	public static final int MINOR_REVISION;

	/**
	 * The base list of recognised command-line options. These will be appended
	 * with additional arguments, as determined by the available activated
	 * plugins.
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
	 * Identifies the location where plugins are stored.
	 */
	public static final String PLUGINS_DIR = "lib/plugins/";

	/**
	 * Identifies the location where local plugins are stored.
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
		String versionStr = WyccMain.class.getPackage()
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

		DefaultPluginManager manager = activatePluginSystem(verbose);

		// --------------------------------------------------------------
		// Fourth, parse command-line options
		// --------------------------------------------------------------
		ArrayList<String> args = new ArrayList<String>(Arrays.asList(_args));
		Map<String, Object> values = OptArg.parseOptions(args,
				COMMANDLINE_OPTIONS);

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
		// Fifth, configure plugin system
		// --------------------------------------------------------------
		Map<String,Map<String,Object>> attributes = (Map<String,Map<String,Object>>) values.get("X");

		if(attributes != null) {
			System.out.println("LOOKING TO CONFIGURE: " + attributes);
		}

		System.out.println("INVOKING BUILDER MAIN");

		FunctionExtension.invoke("builderMain", outputDirectory, libraries, args);

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
	public static DefaultPluginManager activatePluginSystem(boolean verbose) {

		// Determine plugin locations
		List<String> locations = getPluginLocations();

		// create the context and manager
		DefaultPluginContext context = new DefaultPluginContext();
		DefaultPluginManager manager = new DefaultPluginManager(context,
				locations);

		if (verbose) {
			manager.setLogger(new Logger.Default(System.err));
			context.setLogger(new Logger.Default(System.err));
		}

		// Create the global functions list, which allows plugins to provide
		// functionality to be called directly from here.
		context.create("wycc.functions", new Plugin.ExtensionPoint() {
			@Override
			public void register(Feature extension) {
				FunctionExtension.register((FunctionExtension)extension);
			}
		});

		manager.start();

		return manager;
	}

	/**
	 * Create the list of plugin locations. That is, locations on the filesystem
	 * where the plugin system will look for plugins.
	 *
	 * @return
	 */
	public static List<String> getPluginLocations() {
		ArrayList<String> locations = new ArrayList<String>();
		locations.add(PLUGINS_DIR);
		String HOME = System.getenv("HOME");
		if(HOME != null) {
			locations.add(HOME + LOCAL_PLUGINS_DIR);
		}
		return locations;
	}
}
