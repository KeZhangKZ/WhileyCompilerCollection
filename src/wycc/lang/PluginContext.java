package wycc.lang;

/**
 * A PluginContext provides a mechanism for plugins to interact with their
 * environment. In particular, it allows them to register extension points which
 * provide the critical mechanism for adding new functionality.
 * 
 * @author David J. Pearce
 * 
 */
public interface PluginContext {
	
	/**
	 * Responsible for registering an extension within the system.
	 * 
	 * @param extension
	 * @param implementation
	 */
	public void register(String extension, Class<?> implementation);
	
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
		 * @param implementation
		 */
		public void register(Class<?> implementation);
	}
}
