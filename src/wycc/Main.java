package wycc;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.jar.*;

import wycc.lang.Logger;
import wycc.lang.Plugin;

public class Main {

	/**
	 * Identifies the location where plugins are stored.
	 */
	public static final String PLUGINS_DIR = "lib/plugins/";

	public static void main(String[] args) {
		ArrayList<Plugin> plugins = new ArrayList<Plugin>();

		// First, scan for any plugins
		scanForPlugins(PLUGINS_DIR, plugins);

		// Second, construct the URLClassLoader
		URL[] urls = new URL[plugins.size()];
		for(int i=0;i!=plugins.size();++i) {
			urls[i] = plugins.get(i).getLocation();
		}
		URLClassLoader loader = new URLClassLoader(urls);
		
		// Third, active the plugins!!
		for (Plugin plugin : plugins) {
			try {
				System.out.println(Arrays.toString(urls));
				Class c = loader.loadClass(plugin.getActivator());
				Method m = c.getMethod("activate");
				Object self = c.newInstance();
				m.invoke(self);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		// Fourth, do something ??
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
}
