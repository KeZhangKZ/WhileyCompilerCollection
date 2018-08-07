// Copyright 2011 The Whiley Project Developers
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package wycc.commands;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import wybs.util.StdBuildGraph;
import wycc.WyProject;
import wycc.cfg.Configuration;
import wycc.cfg.Configuration.Schema;
import wycc.lang.Command;
import wycc.util.Pair;
import wyfs.lang.Content;
import wyfs.lang.Path;
import wyfs.lang.Content.Type;

public class Build implements Command {

	/**
	 * The descriptor for this command.
	 */
	public static final Command.Descriptor DESCRIPTOR = new Command.Descriptor() {
		@Override
		public String getName() {
			return "build";
		}

		@Override
		public String getDescription() {
			return "Perform build operations on an existing project";
		}

		@Override
		public List<Option.Descriptor> getOptionDescriptors() {
			return Collections.EMPTY_LIST;
		}

		@Override
		public Schema getConfigurationSchema() {
			return Configuration.EMPTY_SCHEMA;
		}

		@Override
		public List<Descriptor> getCommands() {
			return Collections.EMPTY_LIST;
		}

		@Override
		public Command initialise(Command environment, Command.Options options,
				Configuration configuration) {
			return new Build((WyProject) environment);
		}

	};

	private final WyProject project;

	public Build(WyProject project) {
		this.project = project;
	}

	@Override
	public Descriptor getDescriptor() {
		return DESCRIPTOR;
	}

	@Override
	public void initialise() {
	}

	@Override
	public void finalise() {
	}

	@Override
	public boolean execute(List<String> args) {
		try {
			List<wybs.lang.Build.Platform> platforms = project.getParent().getBuildPlatforms();
			ArrayList<Path.Entry<?>> sources = new ArrayList<>();
			for (int i = 0; i != platforms.size(); ++i) {
				wybs.lang.Build.Platform platform = platforms.get(i);
				System.out.println("PLATFORM: " + platform.getName());
				Content.Type<?> srcType = platform.getSourceType();
				Content.Type<?> binType = platform.getTargetType();
				Path.Root srcRoot = project.getRoot(srcType);
				Path.Root binRoot = project.getRoot(binType);
				// Determine the list of modified source files.
				List modified = getModifiedSourceFiles(srcRoot, Content.filter("**", srcType), binRoot,
						platform.getTargetType());
				System.out.println("MODIFIED: " + modified);
				// Add the list of modified source files to the list.
				sources.addAll(modified);
			}
			// Finally, rebuild everything!
			project.build(sources);
			return true;
		} catch (Exception e) {
			// FIXME: do something here??
			e.printStackTrace();
			return false;
		}
	}

	// =======================================================================
	// Helpers
	// =======================================================================

	/**
	 * Generate the list of source files which need to be recompiled. By default,
	 * this is done by comparing modification times of each whiley file against its
	 * corresponding wyil file. Wyil files which are out-of-date are scheduled to be
	 * recompiled.
	 *
	 * @return
	 * @throws IOException
	 */
	public static <T, S> List<Path.Entry<T>> getModifiedSourceFiles(Path.Root sourceDir,
			Content.Filter<T> sourceIncludes, Path.Root binaryDir, Content.Type<S> binaryContentType)
			throws IOException {
		// Now, touch all source files which have modification date after
		// their corresponding binary.
		ArrayList<Path.Entry<T>> sources = new ArrayList<>();
		for (Path.Entry<T> source : sourceDir.get(sourceIncludes)) {
			// currently, I'm assuming everything is modified!
			Path.Entry<S> binary = binaryDir.get(source.id(), binaryContentType);
			// first, check whether wyil file out-of-date with source file
			if (binary == null || binary.lastModified() < source.lastModified()) {
				sources.add(source);
			}
		}

		return sources;
	}
}
