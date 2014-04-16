package wycc.lang;

import wycc.util.Logger;

/**
 * A PluginContext provides a mechanism for plugins to interact with their
 * environment. In particular, it allows them to register extension points which
 * provide the critical mechanism for adding new functionality.
 * 
 * @author David J. Pearce
 * 
 */
public interface PluginContext extends Logger {
	
	/**
	 * Responsible for registering an extension within the system.
	 * 
	 * @param id
	 *            The unique id of the extension point (e.g.
	 *            "wyfs.ContentType").
	 * @param extension
	 *            The implementation of the given extension point.
	 */
	public void register(String id, Extension extension);
	
	/**
	 * Create a new extension point which subsequent plugins can register
	 * extensions for.
	 * 
	 * @param extension
	 * @param ep
	 */
	public void create(String extension, ExtensionPoint ep);
	
	/**
	 * An extension point in the plugin is a named entity provided by one
	 * plugin, which other plugins can register extensions for.
	 * 
	 * @author David J. Pearce
	 * 
	 */
	public interface ExtensionPoint {
		
		/**
		 * Notify extension point that a new extension has been registered for
		 * it.
		 * 
		 * @param extension
		 *            The extension implementation to register with this
		 *            extension point.
		 */
		public void register(Extension extension);
	}
	
	/**
	 * An extension provides an implementation of an extension point. Each
	 * extension has a unique name, which allows us to configure it specifically
	 * after registration. It also allows us to see what extensions are
	 * registered for what extension points.
	 * 
	 * @author David J. Pearce
	 * 
	 */
	public interface Extension {		
		/**
		 * Returns the unique identifier for this extension.
		 * 
		 * @return
		 */
		public String id();
		
		/**
		 * Returns the actual payload implementing this extension.
		 * 
		 * @return
		 */
		public Object data();
	}
}
