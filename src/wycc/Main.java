package wycc;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.jar.*;

import wycc.lang.Logger;
import wycc.lang.Plugin;
import wycc.lang.PluginActivator;
import wycc.lang.PluginContext;
import wycc.util.DefaultPluginContext;
import wycc.util.DefaultPluginManager;
import wyfs.lang.Content;
import wyfs.lang.Path;

public class Main {
	
	public static PrintStream errout;
	
	public static final int MAJOR_VERSION;
	public static final int MINOR_VERSION;
	public static final int MINOR_REVISION;
	
	/**
	 * Identifies the location where plugins are stored.
	 */
	public static final String PLUGINS_DIR = "lib/plugins/";

	/**
	 * Identifies the location where local plugins are stored.
	 */
	public static final String LOCAL_PLUGINS_DIR = ".wycc/plugins/";

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
		String versionStr = Main.class.getPackage()
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

	public static void main(String[] args) {
		
		// First, determine the set of plugin locations 
		ArrayList<String> locations = new ArrayList<String>();
		locations.add(PLUGINS_DIR);
		String HOME = System.getenv("HOME");
		if(HOME != null) {
			locations.add(HOME + LOCAL_PLUGINS_DIR);
		}
		
		// Second, create the plugin manager
		PluginContext context = new DefaultPluginContext();
		DefaultPluginManager manager = new DefaultPluginManager(context,
				locations);
		manager.setLogger(new Logger.Default(System.err));
		
		// Third, activate all plugins
		manager.start();

		// Finally, deactivate all plugins
		manager.stop();
	}
	
	
}
