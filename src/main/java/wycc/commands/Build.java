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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import wycc.lang.Command;
import wycc.lang.Command.Descriptor;
import wyfs.lang.Content;
import wyfs.lang.Path;
import wyfs.util.Trie;

public class Build implements Command {
	//
	private final Content.Registry registry;

	private static final String REGISTRY_DIR = System.getProperty("user.home")
			+ "/.wy/registry".replaceAll("/", File.separator);
	private static final Path.ID BUILD_FILE = Trie.fromString("wy");

	public Build(Content.Registry registry) {
		this.registry = registry;
	}

	@Override
	public Descriptor getDescriptor() {
		return new Descriptor() {

			@Override
			public String getName() {
				return "build";
			}

			@Override
			public String getDescription() {
				return "Perform build operations on an existing project";
			}

			@Override
			public List<Option> getOptions() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public Command initialise(Environment environment) {
				// TODO Auto-generated method stub
				return null;
			}
		};
	}

	@Override
	public List<Command> getSubcommands() {
		return Collections.EMPTY_LIST;
	}

	@Override
	public boolean execute(List<String> args) {
		return false;
	}

	@Override
	public void initialise(List<Option.Instance> configuration) {
		// TODO Auto-generated method stub
	}

	@Override
	public void finalise() {
		// TODO Auto-generated method stub

	}

//	public void build() {
//		try {
//			Path.Root packageRoot = getPackageRoot(name,version);
//			Path.Entry<ConfigFile> buildFileEntry = packageRoot.get(BUILD_FILE_NAME, ConfigFile.ContentType);
//			ConfigFile buildFile;
//			if (buildFileEntry == null) {
//				// Indicates the given package does not currently exist.
//				// Therefore, download and expand it.
//				expandPackage(name, version, packageRoot);
//				buildFileEntry = packageRoot.get(BUILD_FILE_NAME, ConfigFile.ContentType);
//				buildFile = checkBuildFile(name, version, buildFileEntry);
//				// Resolve any package dependencies
//				resolvePackageDependencies(buildFile);
//				// Build the package
//				buildPackage(buildFile,packageRoot);
//			} else {
//				// Indicates package already existed. Therefore, assume build
//				// file was correctly defined.
//				buildFile = buildFileEntry.read();
//			}
//			return getTargetRoot(buildFile,packageRoot);
//		} catch (IOException e) {
//			throw new ResolveError(e.getMessage());
//		}
//	}
//
//
//	/**
//	 * Perform basic sanity checking on the given build file. For example, that
//	 * the name and version match. Furthermore, that it can be parsed and
//	 * contains enough useful information.
//	 *
//	 * @param entry
//	 * @return
//	 * @throws ResolveError
//	 */
//	public ConfigFile checkBuildFile(String name, SemanticVersion version, Path.Entry<ConfigFile> entry)
//			throws ResolveError {
//		if (entry == null) {
//			throw new ResolveError("Package missing build file: " + name + ":" + version);
//		} else {
//			try {
//				return entry.read();
//			} catch (IOException e) {
//				throw new ResolveError(e.getMessage());
//			}
//		}
//	}
//
//	/**
//	 * Resolve any packages required by this package. This needs to be done in
//	 * such a way as to ensure that all packages are resolved in the right
//	 * order.
//	 *
//	 * @param buildFile
//	 */
//	public void resolvePackageDependencies(ConfigFile buildFile) {
//
//	}
//
//
//	/**
//	 * Determine the target root for this package. That is the location where
//	 * all compile WyIL files are stored. This is essentially the output of
//	 * package resolution, as these are then added to the enclosing project.
//	 *
//	 * @param buidlFile
//	 * @param packageRoot
//	 * @return
//	 */
//	public Path.Root getTargetRoot(ConfigFile buidlFile, Path.Root packageRoot) {
//		// FIXME: need to be more sophisticated. That is, actually look at the
//		// buildFile and construct an appropriate root.
//		return packageRoot.getRelativeRoot(Trie.ROOT.append("bin").append("wyil"));
//	}


	// =======================================================================
	// Helpers
	// =======================================================================

	/**
	 * Generate the list of source files which need to be recompiled. By
	 * default, this is done by comparing modification times of each whiley file
	 * against its corresponding wyil file. Wyil files which are out-of-date are
	 * scheduled to be recompiled.
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
