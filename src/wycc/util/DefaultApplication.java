package wycc.util;

import java.io.PrintStream;
import java.util.*;

import wycc.WyccMain;
import wycc.lang.PluginContext;

/**
 * Provides a base system for managing and configuring plugins from the
 * command-line. From this, different applications can be constructed using the
 * plugin system defined by this compiler.
 * 
 * @author David J. Pearce
 * 
 */
public abstract class DefaultApplication {
	
	/**
	 * The plugin context used by this application.
	 */
	protected final DefaultPluginContext context;
	
	/**
	 * The plugin manager used by this application.
	 */
	protected final DefaultPluginManager manager;
	
	// ==================================================================
	// Constructor
	// ==================================================================
	
	public DefaultApplication(List<String> pluginLocations) {		
		this.context = new DefaultPluginContext();
		this.manager = new DefaultPluginManager(context, pluginLocations);
	}
	
	// ==================================================================
	// Run Method
	// ==================================================================
	
	/**
	 * Configure the plugin system based on an array of command-line arguments.
	 * 
	 * @param _args
	 */
	public void configure(String[] _args) {
		// --------------------------------------------------------------
		
		
	}
	
	/**
	 * Start the plugin system by activating all plugins.
	 */
	public void start() {
		manager.start();
	}
	
	/**
	 * Stop the plugin system by deactivating all plugins.
	 */
	public void stop() {
		manager.stop();
	}	
}
