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
package wybs.util;

import java.util.HashMap;

import wybs.lang.Build;
import wyfs.lang.Path;
import wyfs.lang.Path.Entry;

/**
 * Provides a straightforward implementation of the Build.Graph interface.
 *
 * @author David J. Pearce
 *
 */
public class StdBuildGraph implements Build.Graph {
	/**
	 * The derived from relation maps child entries to the parents they are
	 * derived from.
	 */
	private HashMap<Path.Entry<?>, Path.Entry<?>> derivedFrom = new HashMap<>();

	@Override
	public Entry<?> parent(Entry<?> child) {
		return derivedFrom.get(child);
	}

	@Override
	public void registerDerivation(Path.Entry<?> parent, Path.Entry<?> child) {
		derivedFrom.put(child, parent);
	}
}
