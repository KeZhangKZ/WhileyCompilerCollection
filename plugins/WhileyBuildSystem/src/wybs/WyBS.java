package wybs;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import wybs.lang.Build;
import wybs.util.StdBuildRule;
import wybs.util.StdProject;
import wyfs.lang.Content;
import wyfs.lang.Path;
import wyfs.util.DirectoryRoot;
import wyfs.util.JarFileRoot;
import wyms.lang.Feature;
import wyms.lang.Module;
import wyms.util.FunctionExtension;

/**
 * Represents an instance of the WhileyBuildSystem module, and contains all
 * state local to a given instance.
 * 
 * @author David J. Pearce
 *
 */
public class WyBS implements Module {

	/**
	 * The tasks map contains the set of build tasks registered by other
	 * modules.
	 */
	private HashMap<Class<? extends Build.Task>, Build.Task> tasks = new HashMap<>();

	/**
	 * The tasks map contains the set of build platforms register by other
	 * modules.
	 */
	private HashMap<String, Build.Platform> platforms = new HashMap<>();

	/**
	 * The features map contains those features registered by this module.  
	 */
	private HashMap<String,Feature> features = new HashMap<String,Feature>();

	// ========================================================================
	// Accessors
	// ========================================================================

	@Override
	public Feature get(String... id) {
		return features.get(id);
	}

	@Override
	public Collection<Feature> features() {
		return features.values();
	}

	@Override
	public String name() {
		return "wybs";
	}

	@Override
	public String description() {
		// TODO Auto-generated method stub
		return null;
	}

	public Build.Platform getBuildPlatform(String id) {
		return platforms.get(id);
	}
	
	@SuppressWarnings("unchecked")
	public <T extends Build.Task> T getBuildTask(Class<T> id) {
		return (T) tasks.get(id);
	}
	
	// ========================================================================
	// Activation
	// ========================================================================

	private void start(final Module.Context context) {
		registerBuildTaskExtensionPoint(context);
		registerBuildPlatformExtensionPoint(context);
	}

	private void registerBuildPlatformExtensionPoint(final Module.Context context) {
		context.create(Build.Platform.class, new Module.ExtensionPoint<Build.Platform>() {
			@Override
			public void register(Build.Platform platform) {
				platforms.put(platform.id(), platform);
				context.logTimedMessage("Registered build platform: " + platform.id(), 0, 0);
			}
		});
	}

	private void registerBuildTaskExtensionPoint(final Module.Context context) {
		context.create(Build.Task.class, new Module.ExtensionPoint<Build.Task>() {
			@Override
			public void register(Build.Task task) {
				tasks.put(task.getClass(), task);
				context.logTimedMessage("Registered build task: " + task.id(), 0, 0);
			}
		});
	}
	
	public static class Activator implements Module.Activator {
		
		public Module start(final Module.Context context) {
			final WyBS thisModule = new WyBS();			
			thisModule.start(context);			
			return thisModule;
		}

		public void stop(Module module, Module.Context context) {
			// Nothing really to do for this module
		}
	}
}
