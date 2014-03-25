package wycc.lang;

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
public class Plugin {
	/**
	 * The plugin identifier, which should be unique for this plugin (upto
	 * versioning).
	 */
	private String id;
	
	/**
	 * The version number of this plugin, which is a triple of the form
	 * (major,minor,micro) and usually written major.minor.micro (e.g. 1.0.3).
	 */
	private Version version;
	
	/**
	 * The name of the class responsible for activating this plugin.
	 */
	private String activator;
	
	/**
	 * The list of other plugins that this plugin depends upon. These plugins
	 * must be loaded before this plugin can be loaded.
	 */	
	private ArrayList<Dependency> dependencies;	
	
	public Plugin(String id, Version version,
			String activator, List<Dependency> dependencies) {
		this.id = id;
		this.version = version;
		this.activator = activator;
		this.dependencies = new ArrayList<Dependency>(dependencies);
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
	 * Get the list of dependencies for this plugin.
	 * @return
	 */
	public List<Dependency> getDependencies() {
		return Collections.unmodifiableList(dependencies);
	}
	
	public static final class Version implements Comparable<Version> {
		/**
		 * The major version number of this plugin. Plugins with the same identifier
		 * and identical major versions may not be backwards compatible,
		 */
		private int major;
		
		/**
		 * The minor version number of this plugin. Plugins with the same identifier
		 * and identical major versions should be backwards compatible,
		 */
		private int minor;
		
		/**
		 * The micro version number of this plugin. Plugins with the same identifier
		 * and identical major versions should be backwards compatible,
		 */
		private int micro;

		public boolean equals(Object o) {
			if (o instanceof Version) {
				Version v = (Version) o;
				return major == v.major && minor == v.minor && micro == v.micro;
			}
			return false;
		}

		public int hashCode() {
			return major ^ minor ^ micro;
		}

		@Override
		public int compareTo(Version o) {
			// TODO Auto-generated method stub
			return 0;
		}		
	}
	
	public static class Dependency {
		private String id;		
	}	
}
