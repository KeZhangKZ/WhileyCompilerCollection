package wybs;

import java.io.File;
import java.lang.reflect.Method;
import java.util.List;
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

		// ==================================================================
		// Register builderMain entry point
		// ==================================================================
		context.register("wycc.functions", new PluginContext.Extension() {
			public Object data() {
				return getMethod("builderMain", String.class, File.class,
						List.class);
			}
		});
	}

	public void stop(PluginContext context) {

	}

	/**
	 * This is the entry point for the builder, and is called directly from the
	 * WhileyCompilerCollection. The job of this function is to construct an
	 * appropriate project, and entirely manage the compilation of that project.
	 *
	 * @param targetPlatform
	 *            --- The name of the target platform to generate code for.
	 * @param outputDirectory
	 *            --- The output directory into which to place generated files.
	 *            Note, in the case of files in packages, this directory is the
	 *            root of the package directory structure.
	 * @param libraries
	 *            --- Any additional libraries to include on the WhileyPath.
	 */
	public static void builderMain(String targetPlatform, File outputDirectory, List<File> libraries) {
		System.out.println(targetPlatform);
		System.out.println(outputDirectory);
		System.out.println(libraries);
	}

	/**
	 * This simply returns a reference to a given name. If the method doesn't
	 * exist, then it will throw a runtime exception.
	 *
	 * @param name
	 * @param paramTypes
	 * @return
	 */
	public Method getMethod(String name, Class... paramTypes) {
		try {
			return this.getClass().getMethod(name, paramTypes);
		} catch (Exception e) {
			throw new RuntimeException("No such method: " + name, e);
		}
	}
}
