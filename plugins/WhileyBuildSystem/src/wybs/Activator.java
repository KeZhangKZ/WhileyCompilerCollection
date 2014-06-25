package wybs;

import java.util.HashMap;

import jplug.lang.PluginActivator;
import jplug.lang.PluginContext;
import wybs.lang.BuildTask;

/**
 * The activator for the Whiley Build System plugin. This doesn't really do much!
 * 
 * @author David J. Pearce
 * 
 */
public class Activator implements PluginActivator {
	
	private HashMap<String,BuildTask> tasks = new HashMap<String,BuildTask>();
	
	public Activator() {
		
	}
	
	public void start(final PluginContext context) {
		// ==================================================================
		// Create Builder extension point
		// ==================================================================
		context.create("wybs.BuildTask", new PluginContext.ExtensionPoint() {

			@Override
			public void register(PluginContext.Extension extension) {
				// Should this accept a builder class, or something else?
				BuildTask task = (BuildTask) extension.data();
				tasks.put(task.id(),task);
				
				context.logTimedMessage("Registered build task: "
						+ task.id(), 0, 0);
			}
		});
	}
	
	public void stop(PluginContext context) {
		
	}
}
