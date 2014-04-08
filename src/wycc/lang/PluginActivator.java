package wycc.lang;

/**
 * Represents a class designated as the unique "activator" for a given plugin.
 * This activator is used to control aspects of the plugin (e.g. resources
 * allocated) as it is started and stopped,
 * 
 * @author David J. Pearce
 * 
 */
public interface PluginActivator {

	/**
	 * This method is called when the plugin is begun. This gives the plugin an
	 * opportunity to register one or more extension points in the compiler.
	 * 
	 * @param context
	 */
	public void start(PluginContext context);
	
	/**
	 * This method is called when the plugin is stopped. Any resources used by
	 * the plugin should be freed at this point. This includes any registered
	 * extension points, which should be unregistered.
	 * 
	 * @param context
	 */
	public void stop(PluginContext context);
}
