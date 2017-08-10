// Copyright (c) 2011, David J. Pearce (djp@ecs.vuw.ac.nz)
// All rights reserved.
//
// This software may be modified and distributed under the terms
// of the BSD license.  See the LICENSE file for details.
package wycc.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import wybs.lang.Build;
import wybs.util.StdProject;
import wycc.lang.Command;
import wyfs.lang.Content;
import wyfs.lang.Path;
import wyfs.util.DirectoryRoot;
import wyfs.util.JarFileRoot;
import wyfs.util.VirtualRoot;

/**
 * Provides an abstract command from which other commands for controlling the
 * Whiley compiler can be derived. Specifically, this class handles all the
 * issues related to managing the various project roots, etc.
 *
 * @author David J. Pearce
 *
 */
public abstract class AbstractProjectCommand<T> implements Command<T> {

	/**
	 * The master project content type registry. This is needed for the build
	 * system to determine the content type of files it finds on the file
	 * system.
	 */
	public final Content.Registry registry;

	/**
	 * The locations in which (e.g. whiley) source files are found.
	 */
	protected Map<String,Path.Root> srcRoots;

	/**
	 * The locations in which (e.g. wyil) intermediate binary files are found.
	 */
	protected Map<String,Path.Root> binRoots;

	/**
	 * The locations in which (e.g. stdlib) external files are found.
	 */
	protected Map<String,Path.Root> extRoots;

	/**
	 * The project which controls the namespacing
	 */
	protected Build.Project project;

	/**
	 * The logger used for logging system events
	 */
	protected Logger logger;

	/**
	 * Construct a new instance of this command.
	 *
	 * @param registry
	 *            The content registry being used to match files to content
	 *            types.
	 * @throws IOException
	 */
	public AbstractProjectCommand(Content.Registry registry, Logger logger) {
		this.registry = registry;
		this.logger = logger;
	}

	// =======================================================================
	// Configuration Options
	// =======================================================================

	@Override
	public String[] getOptions() {
		return new String[]{""};
	}

	@Override
	public String describe(String option) {
		throw new IllegalArgumentException("invalid option \"" + option + "\"");
	}

	@Override
	public void set(String option, Object value) throws ConfigurationError {
		throw new IllegalArgumentException("invalid option \"" + option + "\"");
	}

	@Override
	public Object get(String name) {
		// TODO Auto-generated method stub
		return null;
	}

	// =======================================================================
	// Configuration
	// =======================================================================

	/**
	 * Construct a new temporary project. This project is temporary because it
	 * only exists for the life of an execution of this command.
	 *
	 * @return
	 * @throws IOException
	 */
	@Override
	public void initialise(Map<String,Object> configuration) {
		// Load build file

		// Finalise configuration
		finaliseConfiguration(configuration);
		// Add roots and construct project
		ArrayList<Path.Root> roots = new ArrayList<>();
		//
		roots.addAll(srcRoots.values());
		roots.addAll(binRoots.values());
		roots.addAll(extRoots.values());
		//
		this.project = new StdProject(roots);
	}

	public void finalise() {
		// Flush all roots
		for (Path.Root bin : binRoots.values()) {
			try {
				bin.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Finalise the given configuration to ensure it is an consistent state.
	 * This means, in particular, that roots which have not been defined by the
	 * user are created as necessary.
	 */
	protected void finaliseConfiguration(Map<String,Object> configuration) throws IOException {
		whileydir = getDirectoryRoot(whileydir,new DirectoryRoot(".",registry));
		wyildir = getDirectoryRoot(wyildir,whileydir);
		wyaldir = getAbstractRoot(wyaldir);
		wycsdir = getAbstractRoot(wycsdir);
	}

	/**
	 * Construct a root which must correspond to a physical directory.
	 *
	 * @throws IOException
	 *
	 */
	protected DirectoryRoot getDirectoryRoot(DirectoryRoot dir, DirectoryRoot defaulT) throws IOException {
		if(dir != null) {
			return dir;
		} else {
			return defaulT;
		}
	}

	/**
	 * Construct a root which is either virtual or corresponds to a physical
	 * directory.
	 *
	 * @throws IOException
	 *
	 */
	protected Path.Root getAbstractRoot(Path.Root dir) throws IOException {
		if(dir != null) {
			return dir;
		} else {
			return new VirtualRoot(registry);
		}
	}

}
