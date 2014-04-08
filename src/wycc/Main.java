package wycc;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.jar.*;

import wycc.lang.Plugin;
import wycc.lang.PluginActivator;
import wycc.lang.PluginContext;
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

	/**
	 * Scan a given directory for plugins. A plugin is a jar file which contains
	 * an appropriate plugin.xml file. This method does not start any plugins,
	 * it simply extracts the appropriate meta-data from their plugin.xml file.
	 * 
	 * @param directory
	 *            Directory to scan for plugins.
	 * @param plugins
	 *            List of plugins to which any plugins found will be added.
	 */
	private static void scanForPlugins(String directory,
			ArrayList<Plugin> plugins) {
		File pluginDir = new File(directory);
		for (String n : pluginDir.list()) {
			if (n.endsWith(".jar")) {
				try {
					URL url = new File(directory + File.separator + n).toURI()
							.toURL();
					Plugin plugin = parsePluginManifest(url);
					if (plugin != null) {
						plugins.add(plugin);
					}
				} catch (MalformedURLException e) {
					// This basically shouldn't happen, since we're constructing
					// the URL directly from the directory name and the name of
					// a located file.
				}
			}
		}
	}

	/**
	 * Open a given plugin Jar and attempt to extract the plugin meta-data from
	 * the manifest. If the manifest doesn't contain the appropriate
	 * information, then it's ignored an null is returned.
	 * 
	 * @param bundleURL
	 * @return
	 */
	private static Plugin parsePluginManifest(URL bundleURL) {
		try {
			JarFile jarFile = new JarFile(bundleURL.getFile());
			Manifest manifest = jarFile.getManifest();
			Attributes attributes = manifest.getMainAttributes();
			String bundleName = attributes.getValue("Bundle-Name");
			String bundleId = attributes.getValue("Bundle-SymbolicName");
			Plugin.Version bundleVersion = new Plugin.Version(
					attributes.getValue("Bundle-Version"));
			String bundleActivator = attributes.getValue("Bundle-Activator");
			List<Plugin.Dependency> bundleDependencies = Collections.EMPTY_LIST;
			return new Plugin(bundleName, bundleId, bundleVersion, bundleURL,
					bundleActivator, bundleDependencies);
		} catch (IOException e) {
			// Just ignore this jar file ... something is wrong.
		}
		return null;
	}

	// ==================================================================
	// Main Method
	// ==================================================================

	public static void main(String[] args) {
		ArrayList<Plugin> plugins = new ArrayList<Plugin>();

		// First, scan for any plugins in the given directory.
		scanForPlugins(PLUGINS_DIR, plugins);

		// Second, construct the URLClassLoader which will be used to load
		// classes within the plugins.
		URL[] urls = new URL[plugins.size()];
		for(int i=0;i!=plugins.size();++i) {
			urls[i] = plugins.get(i).getLocation();
		}
		URLClassLoader loader = new URLClassLoader(urls);
		PluginContext context = new DefaultPluginContext();

		// Third, active the plugins. This will give them the opportunity to
		// register whatever extensions they like.
		for (Plugin plugin : plugins) {
			try {
				System.out.println(Arrays.toString(urls));
				Class c = loader.loadClass(plugin.getActivator());				
				PluginActivator self = (PluginActivator) c.newInstance();
				self.start(context);				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		// Fourth, do something ??
		
		// We need to build up and instantiate instances of Content.Type
		// We need to instantiate and configure the builders / routes
		// We need to construct the content directories (?)
		// We need to construct various build rules and create a project!
	}
}
