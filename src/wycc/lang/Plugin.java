package wycc.lang;

import java.util.ArrayList;
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
	
	/**
	 * The name of the class responsible for activating this plugin.
	 */
	private String activator;
	
	/**
	 * The list of other plugins that this plugin depends upon. These plugins
	 * must be loaded before this plugin can be loaded.
	 */	
	private ArrayList<Dependency> dependencies;	
	
	public Plugin(String id, int major, int minor, int micro,
			String activator, List<Dependency> dependencies) {
		this.id = id;
		this.major = major;
		this.minor = minor;
		this.micro = micro;
		this.activator = activator;
		this.dependencies = new ArrayList<Dependency>(dependencies);
	}
	
	public static class Dependency {
		private String id;		
	}
}
