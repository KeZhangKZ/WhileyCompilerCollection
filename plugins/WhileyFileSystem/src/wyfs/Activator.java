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
		
		// ==================================================================
		// Create ContentType extension point
		// ==================================================================
		context.create("wyfs.ContentType", new PluginContext.ExtensionPoint() {

			@Override
			public void register(Class<?> implementation) {
				System.out.println("NEW CONTENT TYPE REGISTERED");
			}

		});
		
		
	}
	
	public void stop(PluginContext context) {
		
	}
}
