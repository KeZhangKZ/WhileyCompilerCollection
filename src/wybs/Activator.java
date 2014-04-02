package wybs;

import wycc.lang.PluginActivator;
import wycc.lang.PluginContext;

/**
 * The activator for the Whiley Build System plugin. This doesn't really do much!
 * 
 * @author David J. Pearce
 * 
 */
public class Activator implements PluginActivator {
		
	public Activator() {
		
	}
	
	public void start(PluginContext context) {
		System.out.println("WYBS Activator.start() called");
	}
	
	public void stop(PluginContext context) {
		System.out.println("WYBS Activator.stop() called");
	}
}
