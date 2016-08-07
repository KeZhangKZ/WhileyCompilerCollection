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
import wyms.lang.Plugin;
import wyms.util.FunctionExtension;

public class WybsPlugin implements Plugin {

	/**
	 * The tasks map contains the set of build tasks registered by other plugins.
	 */
	private HashMap<String,Build.Task> tasks = new HashMap<String,Build.Task>();

	/**
	 * The tasks map contains the set of build platforms register by other plugins.
	 */
	private HashMap<String,Build.Platform> platforms = new HashMap<String,Build.Platform>();

	/**
	 * The features map contains those features registered by this plugin.  
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

	// ========================================================================
	// Function Features
	// ========================================================================
	
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
	public void main(String targetPlatform, File outputDirectory,
			List<File> libraries, List<File> sourceFiles) throws IOException {
		Content.Registry registry = (Content.Registry) FunctionExtension.invoke("getContentRegistry");
		System.out.println("Found registry : " + registry);
		System.out.println("Searching for build platform...");
		/*
		Build.Platform platform = platforms.get(targetPlatform);
		// The output root is the destination for all compiled files.		
		DirectoryRoot outputRoot = new DirectoryRoot(outputDirectory,registry);
		// Construct the roots for every library supplied.
		ArrayList<Path.Root> libraryRoots = new ArrayList<Path.Root>();
		for(File lib : libraries) {
			libraryRoots.add(new JarFileRoot(lib,registry));
		}
		// Create the build project
		BuildProject project = createBuildProject(platform, outputRoot, outputRoot, libraryRoots);
		System.out.println("Created build project...");
		*/
	}

	public Build.Project createBuildProject(Build.Platform platform, Path.Root srcRoot, Path.Root binRoot,
			List<Path.Root> roots) {
		roots.add(srcRoot);
		roots.add(binRoot);
		// TODO: add virtual roots here for intermediate file formats
		// Construct the project
		StdProject project = new StdProject(roots);
		// Include all files
		Content.Filter includes = Content.filter("**", platform.sourceType());
		Content.Filter excludes = null;
		// Add all necessary build rules
		for (String taskName : platform.builders()) {
			BuildTask task = tasks.get(taskName);
			BuildTask.Instance buildInstance = task.instantiate(project);
			StdBuildRule rule = new StdBuildRule(buildInstance, srcRoot, includes, excludes, binRoot);
			project.add(rule);
		}
		// Done
		return project;
	}
	
	// ========================================================================
	// Activation
	// ========================================================================

	private void start(final Plugin.Context context) {
		registerBuildTaskExtensionPoint(context);
		registerBuildPlatformExtensionPoint(context);
		registerMainFeature(context);
	}

	private void registerBuildPlatformExtensionPoint(final Plugin.Context context) {
		context.create("wybs.Build.Platform", new Plugin.ExtensionPoint() {

			@Override
			public void register(Feature feature) {
				Build.Platform platform = (Build.Platform) feature;
				platforms.put(platform.id(), platform);
				context.logTimedMessage("Registered build platform: "
						+ platform.id(), 0, 0);
			}
		});
	}

	private void registerBuildTaskExtensionPoint(final Plugin.Context context) {
		context.create("wybs.BuildTask", new Plugin.ExtensionPoint() {
			
			@Override
			public void register(Feature feature) {
				BuildTask task = (BuildTask) feature;
				tasks.put(task.id(), task);
				context.logTimedMessage("Registered build task: " + task.id(),
						0, 0);
			}
		});
	}
	
	private void registerMainFeature(final Plugin.Context context) {
		FunctionExtension mainFn = new FunctionExtension(this, "main",
				String.class, File.class, List.class, List.class);
		features.put(mainFn.name(),mainFn);
		context.register("wycc.functions",mainFn);
	}
	
	public static class Activator implements Plugin.Activator {
		
		public Plugin start(final Plugin.Context context) {
			final WybsPlugin thisPlugin = new WybsPlugin();			
			thisPlugin.start(context);			
			return thisPlugin;
		}

		public void stop(Plugin plugin, Plugin.Context context) {
			// Nothing really to do for this plugin
		}
	}
}
