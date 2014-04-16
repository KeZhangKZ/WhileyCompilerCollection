package wybs;

import wybs.lang.Builder;
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
		// ==================================================================
		// Create Builder extension point
		// ==================================================================
		context.create("wybs.Builder", new PluginContext.ExtensionPoint() {

			@Override
			public void register(PluginContext.Extension extension) {
				// Should this accept a builder class, or something else?
				Builder builder = (Builder) extension.data();
				
			}
		});
	}
	
	public void stop(PluginContext context) {
		
	}
}
