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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
	private ArrayList<Edge> edges = new ArrayList<>();

	@Override
	public List<Entry<?>> getParents(Entry<?> child) {
		ArrayList<Entry<?>> parents = new ArrayList<>();
		for (int i = 0; i != edges.size(); ++i) {
			Edge e = edges.get(i);
			if (e.child == child) {
				parents.add(e.parent);
			}
		}
		return parents;
	}

	@Override
	public List<Entry<?>> getChildren(Entry<?> parent) {
		ArrayList<Entry<?>> children = new ArrayList<>();
		for (int i = 0; i != edges.size(); ++i) {
			Edge e = edges.get(i);
			if (e.parent == parent) {
				children.add(e.child);
			}
		}
		return children;
	}

	@Override
	public Set<Entry<?>> getEntries() {
		HashSet<Entry<?>> entries = new HashSet<>();
		for (int i = 0; i != edges.size(); ++i) {
			Edge e = edges.get(i);
			entries.add(e.parent);
			entries.add(e.child);
		}
		return entries;
	}

	@Override
	public void connect(Path.Entry<?> parent, Path.Entry<?> child) {
		edges.add(new Edge(parent,child));
	}

	private static class Edge {
		private final Path.Entry<?> parent;
		private final Path.Entry<?> child;

		public Edge(Path.Entry<?> parent, Path.Entry<?> child) {
			this.parent = parent;
			this.child = child;
		}
	}
}
