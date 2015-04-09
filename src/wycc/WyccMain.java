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

import wycc.util.OptArg;
import jplug.lang.PluginContext;
import jplug.lang.PluginContext.Extension;
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
		
		// --------------------------------------------------------------
		// Second, determine plugin locations
		// --------------------------------------------------------------
		ArrayList<String> locations = new ArrayList<String>();
		locations.add(PLUGINS_DIR);
		String HOME = System.getenv("HOME");
		if(HOME != null) {
			locations.add(HOME + LOCAL_PLUGINS_DIR);
		}
		
		// --------------------------------------------------------------
		// Third, activate plugin system
		// --------------------------------------------------------------
		DefaultPluginContext context = new DefaultPluginContext();
		DefaultPluginManager manager = new DefaultPluginManager(context,
				locations);

		if (verbose) {
			manager.setLogger(new Logger.Default(System.err));
			context.setLogger(new Logger.Default(System.err));
		}
				
		// Create the global functions list, which allows plugins to provide
		// functionality to be called directly from here.
		final ArrayList<Method> functions = new ArrayList<Method>();
		context.create("wycc.functions", new PluginContext.ExtensionPoint() {
			@Override
			public void register(Extension extension) {				
				functions.add((Method)extension.data());
			}			
		});
		
		manager.start();
		
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
		
		// --------------------------------------------------------------
		// Sixth, find main function and invoke it
		// --------------------------------------------------------------
		Method builderMain = null;
		for(Method m : functions) {
			if(m.getName().equals("builderMain")) {
				builderMain = m;
				break;
			}
		}
		if(builderMain != null) {
			try {
				builderMain.invoke(null, new Object[]{target,outputDirectory,libraries});
			} catch(InvocationTargetException e) {
				e.printStackTrace();
			} catch(IllegalAccessException e) {
				e.printStackTrace();
			} catch(IllegalArgumentException e) {
				e.printStackTrace();
			}
		} else {
			System.out.println("Error: unable to find builder main method");
		}
		
		// --------------------------------------------------------------
		// Finally, deactivate all plugins
		// --------------------------------------------------------------
		manager.stop();			
	}		
}
