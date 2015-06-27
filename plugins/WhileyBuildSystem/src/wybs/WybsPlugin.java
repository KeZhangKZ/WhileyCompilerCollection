package wybs;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

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
import jplug.lang.Feature;
import jplug.lang.Plugin;

public class WybsPlugin implements Plugin {

	/**
	 * The tasks map contains the set of build tasks registered by other plugins.
	 */
	private HashMap<String,BuildTask> tasks = new HashMap<String,BuildTask>();

	/**
	 * The tasks map contains the set of build platforms register by other plugins.
	 */
	private HashMap<String,BuildPlatform> platforms = new HashMap<String,BuildPlatform>();

	/**
	 * The features map contains those features registered by this plugin.  
	 */
	private HashMap<String,Feature> features = new HashMap<String,Feature>();

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
	// Main Entry Point
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
		Content.Registry registry = wyfs.Activator.getContentRegistry();
		System.out.println("Searching for build platform...");
		BuildPlatform platform = platforms.get(targetPlatform);
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
	}

	public BuildProject createBuildProject(BuildPlatform platform,
			Path.Root srcRoot, Path.Root binRoot, List<Path.Root> roots) {
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
			StdBuildRule rule = new StdBuildRule(buildInstance,srcRoot,includes,excludes,binRoot);
			project.add(rule);
		}
		// Done
		return project;
	}
	
	// ========================================================================
	// Activation
	// ========================================================================

	private void registerFeatures(final Plugin.Context context) {
		registerBuildTaskExtensionPoint(context);
		registerBuildPlatformExtensionPoint(context);
		registerMainFeature(context);
	}

	private void registerBuildPlatformExtensionPoint(final Plugin.Context context) {
		context.create("wybs.BuildPlatform", new Plugin.ExtensionPoint() {

			@Override
			public void register(Feature feature) {
				BuildPlatform platform = (BuildPlatform) feature;
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
		FunctionExtension mainFn = new FunctionExtension(this.getClass(), "main",
				String.class, File.class, List.class); 
		features.put(mainFn.name(),mainFn);
		context.register("wycc.functions",mainFn);
	}
	
	public static class Activator implements Plugin.Activator {
		
		public Plugin start(final Plugin.Context context) {
			final WybsPlugin thisPlugin = new WybsPlugin();			
			thisPlugin.registerFeatures(context);			
			return thisPlugin;
		}

		public void stop(Plugin plugin, Plugin.Context context) {
			// Nothing really to do for this plugin
		}
	}
}
