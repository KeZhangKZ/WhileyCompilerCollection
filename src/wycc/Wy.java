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
import java.util.*;

import wybs.lang.Build;
import wycc.lang.Feature;
import wycc.lang.Module;
import wycc.util.Logger;
import wycc.util.OptArg;
import wycc.util.StdModuleContext;
import wycc.util.StdModuleManager;
import wyfs.lang.Content;

/**
 * Provides a command-line interface to the Whiley Compiler Collection. This
 * supports loading and configuring modules, as well as compiling files.
 *
 * @author David J. Pearce
 *
 */
public class Wy {

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
			new OptArg("verbose", "Print detailed information on what the system is doing"),
			new OptArg("X", OptArg.OPTIONSMAP, "Configure system component") };

	/**
	 * Identifies the location where modules are stored.
	 */
	public static final String PLUGINS_DIR = "lib/plugins/";

	/**
	 * Identifies the location where local modules are stored.
	 */
	public static final String LOCAL_PLUGINS_DIR = "/.wy/plugins/";

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
		System.out.println("usage: wy <options> <files>");
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
		String versionStr = Wy.class.getPackage()
				.getImplementationVersion();
		if (versionStr != null) {
			String[] bits = versionStr.split("-");
			String[] pts = bits[0].split("\\.");
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
	// Extension Points
	// ==================================================================

	/**
	 * Create the Build.Template extension point. This is where plugins register
	 * their primary functionality for constructing a specific build project.
	 * 
	 * @param context
	 * @param templates
	 */
	private static void createTemplateExtensionPoint(Module.Context context, final List<Build.Template> templates) {
		context.create(Build.Template.class, new Module.ExtensionPoint<Build.Template>() {
			@Override
			public void register(Build.Template template) {
				System.out.println("Registering build template: " + template);
				templates.add(template);
			}
		});
	}
	
	/**
	 * Create the Content.Type extension point. 
	 * 
	 * @param context
	 * @param templates
	 */
	private static void createContentTypeExtensionPoint(Module.Context context, final List<Content.Type> contentTypes) {
		context.create(Content.Type.class, new Module.ExtensionPoint<Content.Type>() {
			@Override
			public void register(Content.Type contentType) {
				contentTypes.add(contentType);
			}
		});
	}
	
	// ==================================================================
	// Main Method
	// ==================================================================

	public static void main(String[] _args) {
		ArrayList<Build.Template> templates = new ArrayList<Build.Template>();
		ArrayList<Content.Type> contentTypes = new ArrayList<Content.Type>(); 
		// --------------------------------------------------------------
		// First, preprocess command-line arguments
		// --------------------------------------------------------------
		boolean verbose = false;
		for(String arg : _args) {
			verbose |= arg.equals("-verbose");
		}
		
		StdModuleManager manager = activateModuleSystem(verbose, templates, contentTypes);

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

		// --------------------------------------------------------------
		// Fifth, configure module system
		// --------------------------------------------------------------
		
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
	public static StdModuleManager activateModuleSystem(boolean verbose, List<Build.Template> templates,
			List<Content.Type> contentTypes) {
		// Determine plugin locations
		List<String> locations = getPluginLocations();

		// create the context and manager
		StdModuleContext context = new StdModuleContext();
		StdModuleManager manager = new StdModuleManager(context, locations);

		if (verbose) {
			manager.setLogger(new Logger.Default(System.err));
			context.setLogger(new Logger.Default(System.err));
		}


		// create extension points		
		createTemplateExtensionPoint(context,templates);
		createContentTypeExtensionPoint(context,contentTypes);
		
		// start modules		
		manager.start();

		return manager;
	}

	/**
	 * Create the list of module locations. That is, locations on the filesystem
	 * where the module system will look for plugins.
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
