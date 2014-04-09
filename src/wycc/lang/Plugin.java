package wycc.lang;

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
public class Plugin {
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
	private final Version version;

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
	private ArrayList<Dependency> dependencies;

	public Plugin(String name, String id, Version version, URL location,
			String activator, List<Dependency> dependencies) {
		this.name = name;
		this.id = id;
		this.version = version;
		this.location = location;
		this.activator = activator;
		this.dependencies = new ArrayList<Dependency>(dependencies);
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
	public Version getVersion() {
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
	public List<Dependency> getDependencies() {
		return Collections.unmodifiableList(dependencies);
	}

	/**
	 * Represents a version number with three components: the major component;
	 * the minor component; and, the micro component. For example, "1.0.3" is a
	 * version number whose major component is "1", whose minor component is "0"
	 * and whose micro component is "3".
	 * 
	 * @author David J. Pearce
	 * 
	 */
	public static final class Version implements Comparable<Version> {
		/**
		 * The major version number of this plugin. Plugins with the same
		 * identifier and identical major versions may not be backwards
		 * compatible,
		 */
		private int major;

		/**
		 * The minor version number of this plugin. Plugins with the same
		 * identifier and identical major versions should be backwards
		 * compatible,
		 */
		private int minor;

		/**
		 * The micro version number of this plugin. Plugins with the same
		 * identifier and identical major versions should be backwards
		 * compatible,
		 */
		private int micro;

		/**
		 * Construct a version from a string in the format "xxx.yyy.zzz", where
		 * "xxx" is the major number, "yyy" the minor number and "zzz" the micro
		 * number.
		 * 
		 * @param versionString
		 */
		public Version(String versionString) {
			String[] components = versionString.split("\\.");
			if (components.length != 3) {
				throw new IllegalArgumentException("Invalid version string \""
						+ versionString + "\"");
			}
			this.major = Integer.parseInt(components[0]);
			this.minor = Integer.parseInt(components[1]);
			this.micro = Integer.parseInt(components[2]);
		}

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
			if (major < o.major) {
				return -1;
			} else if (major > o.major) {
				return 1;
			} else if (minor < o.minor) {
				return -1;
			} else if (minor > o.minor) {
				return 1;
			} else if (micro < o.micro) {
				return -1;
			} else if (micro > o.micro) {
				return 1;
			} else {
				return 0;
			}
		}

		public String toString() {
			return major + "." + minor + "." + micro;
		}
	}

	/**
	 * Represents a dependency from one plugin to another.  
	 * 
	 * @author David J. Pearce
	 * 
	 */
	public static class Dependency {
		/**
		 * The unique plugin identifier.
		 */
		private String id;
		
		/**
		 * The minimum version number permitted, or null if no lower bound.
		 */
		private Version minVersion;
		
		/**
		 * The maximum version number permitted, or null if no upper bound.
		 */
		private Version maxVersion;
		
		public Dependency(String id, Version min, Version max) {
			this.id = id;
			this.minVersion = min;
			this.maxVersion = max;
		}
		
		public boolean matches(String id, Version version) {
			return this.id.equals(id)
					&& (this.minVersion == null || this.minVersion
							.compareTo(version) <= 0)
					&& (this.maxVersion == null || this.maxVersion
							.compareTo(version) >= 0);
		}
		
		public String toString() {
			String min = minVersion != null ? minVersion.toString() : "_";
			String max = maxVersion != null ? maxVersion.toString() : "_";			
			return id + "[" + min + "," + max + "]";
		}
	}
}
