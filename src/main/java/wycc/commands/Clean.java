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
import java.util.List;
import java.util.Map;
import java.util.Set;

import wycc.util.AbstractProjectCommand;
import wycc.util.Logger;
import wyfs.lang.Path;
import wyfs.lang.Path.ID;
import wyfs.lang.Content.Filter;
import wyfs.lang.Content.Registry;
import wyfs.lang.Content.Type;

/**
 * Responsible for removing all intermediate (e.g. binary) files generating
 * during the build process, excluding those generated for any dependencies.
 *
 * @author David J. Pearce
 *
 */
public class Clean extends AbstractProjectCommand<Clean.Result> {
	/**
	 * Result kind for this command
	 *
	 */
	public enum Result {
		SUCCESS,
		ERRORS,
		INTERNAL_FAILURE
	}

	public Clean(Registry registry, Logger logger) {
		super(registry, logger);
	}

	@Override
	public Descriptor getDescriptor() {
		return new Descriptor() {
			@Override
			public String getName() {
				return "clean";
			}

			@Override
			public String getDescription() {
				return "Removing any intermediate (e.g. binary) files.";
			}
		};
	}


	@Override
	public Result execute(String... args) {
		for(Path.Root root : binRoots.values()) {
			try {
				clean(root);
			} catch(IOException e) {
				return Result.INTERNAL_FAILURE;
			}
		}
		return Result.SUCCESS;
	}

	protected void clean(Path.Root root) throws IOException {
		// Determine all file matches
		Set<Path.ID> matches = root.match(new Filter<Object>() {

			@Override
			public boolean matches(ID id, Type ct) {
				return true;
			}

			@Override
			public boolean matchesSubpath(ID id) {
				return true;
			}

		});
		// Erase all file matches
		for(Path.ID id : matches) {
			// HOW TO DELETE?
			System.out.println("Attempting to clean ... " + id);
		}
	}

	@Override
	protected void finaliseConfiguration(Map<String, Object> configuration) {

	}

}
