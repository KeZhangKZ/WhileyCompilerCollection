package wyms.util;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import wycommon.util.Logger;
import wycommon.util.Pair;
import wyms.lang.Plugin;
import wyms.lang.PluginDescriptor;
import wyms.lang.SemanticDependency;
import wyms.lang.SemanticVersion;


public class DefaultPluginManager {

	/**
	 * Logging stream, which is null by default.
	 */
	private Logger logger = Logger.NULL;

	/**
	 * The list of locations into where we will search for plugin
	 */
	private ArrayList<String> locations = new ArrayList<String>();

	/**
	 * The list of activated plugins
	 */
	private ArrayList<PluginDescriptor> plugins = new ArrayList<PluginDescriptor>();

	/**
	 * The plugin context used to manage extension points for plugins.
	 *
	 * @param locations
	 */
	private Plugin.Context context;

	public DefaultPluginManager(Plugin.Context context,
			Collection<String> locations) {
		this.locations.addAll(locations);
		this.context = context;
	}

	public void setLogger(Logger logger) {
		this.logger = logger;
	}

	/**
	 * Scan and activate all plugins on the search path. As part of this, all
	 * plugin dependencies will be checked.
	 */
	public void start() {
		// First, scan for any plugins in the given directory.
		scan();

		// Second, arrange plugins in a topological order to resolve dependencies
		pluginToplogicalSort();

		// Second, construct the URLClassLoader which will be used to load
		// classes within the plugins.
		URL[] urls = new URL[plugins.size()];
		for(int i=0;i!=plugins.size();++i) {
			urls[i] = plugins.get(i).getLocation();
		}
		URLClassLoader loader = new URLClassLoader(urls);

		// Third, active the plugins. This will give them the opportunity to
		// register whatever extensions they like.
		activatePlugins(loader);
	}

	/**
	 * Deactivate all plugins previously activated.
	 */
	public void stop() {
		deactivatePlugins();
	}

	/**
	 * Activate all plugins in the order of occurrence in the given list. It is
	 * assumed that all dependencies are already resolved prior to this and all
	 * plugins are topologically sorted.
	 */
	private void activatePlugins(URLClassLoader loader) {
		for (int i=0;i!=plugins.size();++i) {
			PluginDescriptor plugin = plugins.get(i);
			try {
				Class c = loader.loadClass(plugin.getActivator());
				Plugin.Activator self = (Plugin.Activator) c.newInstance();
				self.start(context);
				logger.logTimedMessage("Activated plugin " + plugin.getId()
						+ " (v" + plugin.getVersion() + ")" , 0, 0);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Deactivate all plugins in the reverse order of occurrence in the given
	 * list. It is assumed that all dependencies are already resolved prior to
	 * this and all plugins are topologically sorted.
	 */
	private void deactivatePlugins() {

		// TODO!

	}

	/**
	 * Scan a given directory for plugins. A plugin is a jar file which contains
	 * an appropriate plugin.xml file. This method does not start any plugins,
	 * it simply extracts the appropriate meta-data from their plugin.xml file.
	 */
	private void scan() {
		for(String location : locations) {
			File pluginDir = new File(location);
			if (pluginDir.exists() && pluginDir.isDirectory()) {
				for (String n : pluginDir.list()) {
					if (n.endsWith(".jar")) {
						try {
							URL url = new File(location + File.separator + n)
									.toURI().toURL();
							PluginDescriptor plugin = parsePluginManifest(url);
							if (plugin != null) {
								plugins.add(plugin);
							}
						} catch (MalformedURLException e) {
							// This basically shouldn't happen, since we're
							// constructing
							// the URL directly from the directory name and the
							// name of
							// a located file.
						}
					}
				}
			}
		}
		logger.logTimedMessage("Found " + plugins.size() + " plugins", 0,0);
	}

	/**
	 * Open a given plugin Jar and attempt to extract the plugin meta-data from
	 * the manifest. If the manifest doesn't contain the appropriate
	 * information, then it's ignored an null is returned.
	 *
	 * @param bundleURL
	 * @return
	 */
	private static PluginDescriptor parsePluginManifest(URL bundleURL) {
		try {
			JarFile jarFile = new JarFile(bundleURL.getFile());
			Manifest manifest = jarFile.getManifest();
			Attributes attributes = manifest.getMainAttributes();
			String bundleName = attributes.getValue("Bundle-Name");
			String bundleId = attributes.getValue("Bundle-SymbolicName");
			SemanticVersion bundleVersion = new SemanticVersion(
					attributes.getValue("Bundle-Version"));
			String bundleActivator = attributes.getValue("Bundle-Activator");
			String requireBundle = attributes.getValue("Require-Bundle");
			List<SemanticDependency> bundleDependencies = Collections.EMPTY_LIST;
			if(requireBundle != null) {
				bundleDependencies = parsePluginDependencies(requireBundle);
			}
			return new PluginDescriptor(bundleName, bundleId, bundleVersion, bundleURL,
					bundleActivator, bundleDependencies);
		} catch (IOException e) {
			// Just ignore this jar file ... something is wrong.
		}
		return null;
	}

	/**
	 * Parse the Require-Bundle string to generate a list of plugin
	 * dependencies.
	 *
	 * @param requireBundle
	 * @return
	 */
	private static List<SemanticDependency> parsePluginDependencies(String requireBundle) {
		String[] deps = requireBundle.split(",");
		ArrayList<SemanticDependency> dependencies = new ArrayList<SemanticDependency>();
		for(int i=0;i!=deps.length;++i) {
			SemanticDependency dep = parsePluginDependency(deps[i]);
			dependencies.add(parsePluginDependency(deps[i]));
		}
		return dependencies;
	}

	private static SemanticDependency parsePluginDependency(String dependency) {
		String[] components = dependency.split(";");
		SemanticVersion minVersion = null;
		SemanticVersion maxVersion = null;
		for (int i = 1; i != components.length; ++i) {
			String c = components[1];
			if (c.startsWith("bundle-version=\"")) {
				c = c.substring(16,c.length()-1);
				Pair<SemanticVersion, SemanticVersion> p = parsePluginDependencyRange(c);
				minVersion = p.first();
				maxVersion = p.second();
			}
		}
		return new SemanticDependency(components[0], minVersion, maxVersion);
	}

	private static Pair<SemanticVersion, SemanticVersion> parsePluginDependencyRange(
			String range) {
		SemanticVersion minVersion = null;
		SemanticVersion maxVersion = null;
		if (range.startsWith("(")) {
			// Indicates a range
			String inner = range.substring(1, range.length() - 1);
			String[] versions = inner.split(",");
			minVersion = new SemanticVersion(versions[0]);
			maxVersion = new SemanticVersion(versions[1]);
		} else {
			// single version
			minVersion = new SemanticVersion(range);
		}
		return new Pair<SemanticVersion, SemanticVersion>(minVersion, maxVersion);
	}

	private void pluginToplogicalSort() {
		HashMap<String, PluginDescriptor> map = new HashMap<String, PluginDescriptor>();
		HashSet<String> visited = new HashSet<String>();
		ArrayList<PluginDescriptor> sorted = new ArrayList<PluginDescriptor>();

		for (int i = 0; i != plugins.size(); ++i) {
			PluginDescriptor p = plugins.get(i);
			map.put(p.getId(), p);
		}

		for (int i = 0; i != plugins.size(); ++i) {
			PluginDescriptor p = plugins.get(i);
			if (!visited.contains(p.getId())) {
				visit(p, visited, sorted, map);
			}
		}

		plugins.clear();
		plugins.addAll(sorted);
	}

	private static void visit(PluginDescriptor p, HashSet<String> visited,
			ArrayList<PluginDescriptor> sorted, HashMap<String, PluginDescriptor> map) {

		// FIXME: this needs to detect cycles, which it currently doesn't do.

		visited.add(p.getId());
		for (SemanticDependency d : p.getDependencies()) {
			PluginDescriptor pd = map.get(d.getId());
			// FIXME: implement version checking
			if (pd == null) {
				throw new RuntimeException("missing dependency: " + d.getId());
			} else if (!visited.contains(pd.getId())) {
				visit(pd, visited, sorted, map);
			}
		}

		sorted.add(p);
	}
}
