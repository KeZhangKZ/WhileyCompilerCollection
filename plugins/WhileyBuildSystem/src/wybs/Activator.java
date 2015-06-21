package wybs;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;

import jplug.lang.PluginActivator;
import jplug.lang.PluginContext;
import wybs.lang.BuildPlatform;
import wybs.lang.BuildProject;
import wybs.lang.BuildTask;
import wybs.util.StdBuildRule;
import wybs.util.StdProject;
import wycc.util.FunctionExtension;
import wyfs.lang.Content;
import wyfs.lang.Path;
import wyfs.util.DirectoryRoot;
import wyfs.util.JarFileRoot;

/**
 * The activator for the Whiley Build System plugin. This doesn't really do much!
 *
 * @author David J. Pearce
 *
 */
public class Activator implements PluginActivator {

	private static HashMap<String,BuildTask> tasks = new HashMap<String,BuildTask>();

	private static HashMap<String,BuildPlatform> platforms = new HashMap<String,BuildPlatform>();

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
				return new FunctionExtension(this.getClass(),"builderMain", String.class, File.class,
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
	public static void builderMain(String target, File outputDirectory,
			List<File> libraries, List<File> sourceFiles) {
		Content.Registry registry = wyfs.Activator.getContentRegistry();
		BuildPlatform platform = platforms.get(target);
		// The output root is the destination for all compiled files.
		DirectoryRoot outputRoot = new DirectoryRoot(outputDirectory,registry);
		// Construct the roots for every library supplied.
		ArrayList<Path.Root> libraryRoots = new ArrayList<Path.Root>();
		for(File lib : libraries) {
			libraryRoots.add(new JarFileRoot(lib,registry));
		}
		// Create the build project
		BuildProject project = createBuildProject(platform, outputRoot, outputRoot, libraryRoots);
	}

	public static BuildProject createBuildProject(BuildPlatform platform,
			Path.Root srcRoot, Path.Root binRoot, List<Path.Root> roots) {
		roots.add(srcRoot);
		roots.add(binRoot);
		// TODO: add virtual roots here for intermediate file formats
		// Construct the project
		StdProject project = new StdProject(roots);
		// Include allf files
		Content.Filter includes = Content.filter("**", platform.sourceType());
		Content.Filter excludes = null;
		// Add all necessary build rules
		for (String buiderName : platform.builders()) {
			// TODO --- get instance
			BuildTask.Instance buildInstance;
			StdBuildRule rule = new StdBuildRule(buildInstance,srcRoot,includes,excludes,binRoot);
			project.add(rule);
		}
		// Done
		return project;
	}
}
