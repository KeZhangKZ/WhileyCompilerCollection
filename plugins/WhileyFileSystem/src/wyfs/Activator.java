package wyfs;

import jplug.lang.PluginActivator;
import jplug.lang.PluginContext;
import wyfs.lang.Content;
import wyfs.util.DefaultContentRegistry;

/**
 * The activator for the Whiley FIle System plugin. This doesn't really do much!
 * 
 * @author David J. Pearce
 * 
 */
public class Activator implements PluginActivator 	{
	
	private DefaultContentRegistry registry = new DefaultContentRegistry();
	
	public Activator() {
		
	}
	
	public void start(final PluginContext context) {

		// ==================================================================
		// Create ContentType extension point
		// ==================================================================
		context.create("wyfs.ContentType", new PluginContext.ExtensionPoint() {

			@Override
			public void register(PluginContext.Extension extension) {
				Content.Type contentType = (Content.Type) extension
						.data();
				
				// TODO: need to get the suffix out of the content type!!
				
				registry.register(contentType, null);
				context.logTimedMessage("Registered "
						+ contentType, 0, 0);
			}

		});
		
		
	}
	
	public void stop(PluginContext context) {
		
	}
}
