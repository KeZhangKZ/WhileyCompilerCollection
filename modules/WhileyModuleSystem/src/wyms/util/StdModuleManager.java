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
import wyms.lang.Module;
import wyms.lang.Descriptor;
import wyms.lang.SemanticDependency;
import wyms.lang.SemanticVersion;


public class StdModuleManager {

	/**
	 * Logging stream, which is null by default.
	 */
	private Logger logger = Logger.NULL;

	/**
	 * The list of locations into where we will search for module
	 */
	private ArrayList<String> locations = new ArrayList<String>();

	/**
	 * The list of activated modules
	 */
	private ArrayList<Descriptor> modules = new ArrayList<Descriptor>();

	private HashMap<Class<? extends Module>,Module> instances = new HashMap<>();
	
	/**
	 * The module context used to manage extension points for modules.
	 *
	 * @param locations
	 */
	private Module.Context context;

	public StdModuleManager(Module.Context context,
			Collection<String> locations) {
		this.locations.addAll(locations);
		this.context = context;
	}

	public void setLogger(Logger logger) {
		this.logger = logger;
	}

	/**
	 * Get instance of given module within this context, or null if no
	 * instance available.
	 * 
	 * @param module
	 * @return
	 */
	public <T extends Module> T getInstance(Class<T> module) {
		return (T) instances.get(module);
	}
	
	/**
	 * Scan and activate all modules on the search path. As part of this, all
	 * module dependencies will be checked.
	 */
	public void start() {
		// First, scan for any modules in the given directory.
		scan();

		// Second, arrange modules in a topological order to resolve dependencies
		moduleToplogicalSort();

		// Second, construct the URLClassLoader which will be used to load
		// classes within the modules.
		URL[] urls = new URL[modules.size()];
		for(int i=0;i!=modules.size();++i) {
			urls[i] = modules.get(i).getLocation();
		}
		URLClassLoader loader = new URLClassLoader(urls);

		// Third, active the modules. This will give them the opportunity to
		// register whatever extensions they like.
		activateModules(loader);
	}

	/**
	 * Deactivate all modules previously activated.
	 */
	public void stop() {
		deactiveModules();
	}

	/**
	 * Activate all modules in the order of occurrence in the given list. It is
	 * assumed that all dependencies are already resolved prior to this and all
	 * modules are topologically sorted.
	 */
	private void activateModules(URLClassLoader loader) {
		for (int i = 0; i != modules.size(); ++i) {
			Descriptor module = modules.get(i);
			try {
				Class c = loader.loadClass(module.getActivator());
				Module.Activator self = (Module.Activator) c.newInstance();				
				Module instance = self.start(context);
				instances.put(c, instance);
				logger.logTimedMessage("Activated module " + module.getId() + " (v" + module.getVersion() + ")", 0, 0);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Deactivate all modules in the reverse order of occurrence in the given
	 * list. It is assumed that all dependencies are already resolved prior to
	 * this and all modules are topologically sorted.
	 */
	private void deactiveModules() {

		// TODO!

	}

	/**
	 * Scan a given directory for modules. A module is a jar file which contains
	 * an appropriate module.xml file. This method does not start any modules,
	 * it simply extracts the appropriate meta-data from their module.xml file.
	 */
	private void scan() {
		for(String location : locations) {
			File moduleDir = new File(location);
			if (moduleDir.exists() && moduleDir.isDirectory()) {
				for (String n : moduleDir.list()) {
					if (n.endsWith(".jar")) {
						try {
							URL url = new File(location + File.separator + n)
									.toURI().toURL();
							Descriptor module = parseModuleManifest(url);
							if (module != null) {
								modules.add(module);
							}
						} catch (MalformedURLException e) {
							// This basically shouldn't happen, since we're
							// constructing the URL directly from the directory
							// name and the name of a located file.
						}
					}
				}
			}
		}
		logger.logTimedMessage("Found " + modules.size() + " modules", 0,0);
	}

	/**
	 * Open a given module Jar and attempt to extract the module meta-data from
	 * the manifest. If the manifest doesn't contain the appropriate
	 * information, then it's ignored an null is returned.
	 *
	 * @param bundleURL
	 * @return
	 */
	private static Descriptor parseModuleManifest(URL bundleURL) {
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
				bundleDependencies = parseModuleDependencies(requireBundle);
			}
			return new Descriptor(bundleName, bundleId, bundleVersion, bundleURL,
					bundleActivator, bundleDependencies);
		} catch (IOException e) {
			// Just ignore this jar file ... something is wrong.
		}
		return null;
	}

	/**
	 * Parse the Require-Bundle string to generate a list of module
	 * dependencies.
	 *
	 * @param requireBundle
	 * @return
	 */
	private static List<SemanticDependency> parseModuleDependencies(String requireBundle) {
		String[] deps = requireBundle.split(",");
		ArrayList<SemanticDependency> dependencies = new ArrayList<SemanticDependency>();
		for(int i=0;i!=deps.length;++i) {
			SemanticDependency dep = parseModuleDependency(deps[i]);
			dependencies.add(dep);
		}
		return dependencies;
	}

	private static SemanticDependency parseModuleDependency(String dependency) {
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

	private void moduleToplogicalSort() {
		HashMap<String, Descriptor> map = new HashMap<String, Descriptor>();
		HashSet<String> visited = new HashSet<String>();
		ArrayList<Descriptor> sorted = new ArrayList<Descriptor>();

		for (int i = 0; i != modules.size(); ++i) {
			Descriptor p = modules.get(i);
			map.put(p.getId(), p);
		}

		for (int i = 0; i != modules.size(); ++i) {
			Descriptor p = modules.get(i);
			if (!visited.contains(p.getId())) {
				visit(p, visited, sorted, map);
			}
		}

		modules.clear();
		modules.addAll(sorted);
	}

	private static void visit(Descriptor p, HashSet<String> visited,
			ArrayList<Descriptor> sorted, HashMap<String, Descriptor> map) {

		// FIXME: this needs to detect cycles, which it currently doesn't do.

		visited.add(p.getId());
		for (SemanticDependency d : p.getDependencies()) {
			Descriptor pd = map.get(d.getId());
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
