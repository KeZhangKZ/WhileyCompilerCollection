package wybs;

import java.util.HashMap;

import jplug.lang.PluginActivator;
import jplug.lang.PluginContext;
import wybs.lang.BuildPlatform;
import wybs.lang.BuildTask;

/**
 * The activator for the Whiley Build System plugin. This doesn't really do much!
 * 
 * @author David J. Pearce
 * 
 */
public class Activator implements PluginActivator {
	
	private HashMap<String,BuildTask> tasks = new HashMap<String,BuildTask>();
	
	private HashMap<String,BuildPlatform> platforms = new HashMap<String,BuildPlatform>();
	
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
				tasks.put(task.id(), task);

				context.logTimedMessage("Registered build task: " + task.id(),
						0, 0);
			}
		});
		
		// ==================================================================
		// Create BuildPlatform extension point
		// ==================================================================
		context.create("wybs.BuildPlatform",
				new PluginContext.ExtensionPoint() {

			@Override
			public void register(PluginContext.Extension extension) {
				// Should this accept a builder class, or something
				// else?
				BuildPlatform platform = (BuildPlatform) extension
						.data();
				platforms.put(platform.id(), platform);

				context.logTimedMessage("Registered build platform: "
						+ platform.id(), 0, 0);
			}
		});
	}
	
	public void stop(PluginContext context) {
		
	}
}
