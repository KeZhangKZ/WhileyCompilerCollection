package wyfs;

import wycc.lang.PluginActivator;
import wycc.lang.PluginContext;

/**
 * The activator for the Whiley FIle System plugin. This doesn't really do much!
 * 
 * @author David J. Pearce
 * 
 */
public class Activator implements PluginActivator 	{
		
	public Activator() {
		
	}
	
	public void start(PluginContext context) {
		System.out.println("WYFS Activator.start() called");
	}
	
	public void stop(PluginContext context) {
		System.out.println("WYFS Activator.stop() called");
	}
}
