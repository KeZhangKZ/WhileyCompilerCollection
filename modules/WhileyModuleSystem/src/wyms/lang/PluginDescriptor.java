package wyms.lang;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents meta-information about plugins. That includes its version,
 * location, and dependencies. This information is obtained from the plugin.xml
 * file required for each plugin.
 * 
 * @author David J. Pearce
 * 
 */
public class PluginDescriptor {
	/**
	 * The plugin name, which should provide a human-readable descriptive name
	 * for this plugin.
	 */
	private String name;

	/**
	 * The plugin identifier, which should be unique for this plugin (upto
	 * versioning).
	 */
	private final String id;

	/**
	 * The version number of this plugin, which is a triple of the form
	 * (major,minor,micro) and usually written major.minor.micro (e.g. 1.0.3).
	 */
	private final SemanticVersion version;

	/**
	 * The location of the plugin jar file.
	 */
	private final URL location;

	/**
	 * The name of the class responsible for activating this plugin.
	 */
	private final String activator;

	/**
	 * The list of other plugins that this plugin depends upon. These plugins
	 * must be loaded before this plugin can be loaded.
	 */
	private ArrayList<SemanticDependency> dependencies;

	public PluginDescriptor(String name, String id, SemanticVersion version, URL location,
			String activator, List<SemanticDependency> dependencies) {
		this.name = name;
		this.id = id;
		this.version = version;
		this.location = location;
		this.activator = activator;
		this.dependencies = new ArrayList<SemanticDependency>(dependencies);
	}

	/**
	 * Get the name of this plugin
	 * 
	 * @return
	 */
	public String getName() {
		return name;
	}

	/**
	 * Get the id of this plugin
	 * 
	 * @return
	 */
	public String getId() {
		return id;
	}

	/**
	 * Get the version of this plugin
	 * 
	 * @return
	 */
	public SemanticVersion getVersion() {
		return version;
	}

	/**
	 * Get the name of the activator class for this plugin. This class is
	 * instantiated when the plugin begins and used to control the start-up and
	 * shutdown of the plugin.
	 * 
	 * @return
	 */
	public String getActivator() {
		return activator;
	}

	/**
	 * Return the location of the plugin jar.
	 * 
	 * @return
	 */
	public URL getLocation() {
		return location;
	}

	/**
	 * Get the list of dependencies for this plugin.
	 * 
	 * @return
	 */
	public List<SemanticDependency> getDependencies() {
		return Collections.unmodifiableList(dependencies);
	}
}
