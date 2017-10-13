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

import java.io.IOException;
import java.util.*;

import wybs.lang.*;
import wyfs.lang.Content;
import wyfs.lang.Path;

/**
 * <p>
 * Provides a straightforward implementation of Build.Project and a basic build
 * system supporting an arbitrary number of build rules. The object space is
 * defined by one or more "path roots" which are locations on the file system
 * where named items may be found. Such locations may be, for example,
 * directories. However, they may also be jar files, or even potentially network
 * locations.
 * </p>
 * <p>
 * The core strategy for building files is fairly simplistic, and assumes a
 * "breadth-first" compilation tree. That is, where files at one level are all
 * compiled producing a new set of files for the next level. In most cases, this
 * is fine. However, in more complex compilation pipelines this can lead to
 * compilation failures.
 * </p>
 *
 * @author David J. Pearce
 */
public class StdProject implements Build.Project {

	/**
	 * The roots of all entries known to the system which form the global
	 * namespace used by the builder(s).
	 */
	protected final ArrayList<Path.Root> roots;

	/**
	 * The rules associated with this project for transforming content. It is
	 * assumed that for any given transformation there is only one possible
	 * pathway described.
	 */
	protected final ArrayList<Build.Rule> rules;

	public StdProject(Collection<Path.Root> roots) {
		this.roots = new ArrayList<>(roots);
		this.rules = new ArrayList<>();
	}

	public StdProject(Collection<Path.Root>... roots) {
		this.rules = new ArrayList<>();
		this.roots = new ArrayList<>();
		for(Collection<Path.Root> root : roots) {
			this.roots.addAll(root);
		}
	}

	// ======================================================================
	// Configuration Interface
	// ======================================================================

	/**
	 * Add a new builder to this project.
	 *
	 * @param data.builder
	 */
	public void add(Build.Rule rule) {
		rules.add(rule);
	}

	/**
	 * Get the roots associated with this project.
	 *
	 * @return
	 */
	public List<Path.Root> roots() {
		return roots;
	}

	/**
	 * Get the build rules associated with this project.
	 *
	 * @return
	 */
	public List<Build.Rule> rules() {
		return rules;
	}

	// ======================================================================
	// Accessors
	// ======================================================================


	/**
	 * Check whether or not a given entry is contained in this root;
	 *
	 * @param entry
	 * @return
	 */
	@Override
	public boolean contains(Path.Entry<?> entry) throws IOException {
		for(int i=0;i!=roots.size();++i) {
			if(roots.get(i).contains(entry)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Check whether or not a given entry and content-type is contained in
	 * this root.
	 *
	 * @throws IOException
	 *             --- in case of some I/O failure.
	 */
	@Override
	public boolean exists(Path.ID id, Content.Type<?> ct) throws IOException {
		for(int i=0;i!=roots.size();++i) {
			if(roots.get(i).exists(id, ct)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Get the entry corresponding to a given ID and content type. If no
	 * such entry exists, return null.
	 *
	 * @param id
	 *            --- id of module to lookup.
	 * @throws IOException
	 *             --- in case of some I/O failure.
	 *
	 */
	@Override
	public <T> Path.Entry<T> get(Path.ID id, Content.Type<T> ct) throws IOException {
		for(int i=0;i!=roots.size();++i) {
			Path.Entry<T> e = roots.get(i).get(id, ct);
			if(e != null) {
				return e;
			}
		}
		return null;
	}

	/**
	 * Get all objects matching a given content filter stored in this root.
	 * In the case of no matches, an empty list is returned.
	 *
	 * @throws IOException
	 *             --- in case of some I/O failure.
	 *
	 * @param ct
	 * @return
	 */
	@Override
	public <T> ArrayList<Path.Entry<T>> get(Content.Filter<T> filter) throws IOException {
		ArrayList<Path.Entry<T>> r = new ArrayList<>();
		for(int i=0;i!=roots.size();++i) {
			r.addAll(roots.get(i).get(filter));
		}
		return r;
	}

	/**
	 * Identify all entries matching a given content filter stored in this
	 * root. In the case of no matches, an empty set is returned.
	 *
	 * @throws IOException
	 *             --- in case of some I/O failure.
	 *
	 * @param filter
	 *            --- filter to match entries with.
	 * @return
	 */
	@Override
	public <T> HashSet<Path.ID> match(Content.Filter<T> filter) throws IOException {
		HashSet<Path.ID> r = new HashSet<>();
		for(int i=0;i!=roots.size();++i) {
			r.addAll(roots.get(i).match(filter));
		}
		return r;
	}

	// ======================================================================
	// Mutators
	// ======================================================================

	/**
	 * Force root to flush entries to permanent storage (where appropriate).
	 * This is essential as, at any given moment, path entries may only be
	 * stored in memory. We must flush them to disk in order to preserve any
	 * changes that were made.
	 */
	public void flush() throws IOException {
		for(int i=0;i!=roots.size();++i) {
			roots.get(i).flush();
		}
	}

	/**
	 * Force root to refresh entries from permanent storage (where
	 * appropriate). For items which has been modified, this operation has
	 * no effect (i.e. the new contents are retained).
	 */
	public void refresh() throws IOException {
		for(int i=0;i!=roots.size();++i) {
			roots.get(i).refresh();
		}
	}

	// ======================================================================
	// Build
	// ======================================================================

	/**
	 * Build a given set of source entries, including all files which depend
	 * upon them.
	 *
	 * @param sources
	 *            --- a collection of source file entries. This will not be
	 *            modified by this method.
	 * @throws Exception
	 */
	public void build(Collection<? extends Path.Entry<?>> sources) throws Exception {
		Build.Graph graph = new StdBuildGraph();

		// Continue building all source files until there are none left. This is
		// actually quite a naive implementation, as it ignores the potential
		// need for staging dependencies.
		do {
			HashSet<Path.Entry<?>> generated = new HashSet<>();
			for (Build.Rule r : rules) {
				generated.addAll(r.apply(sources,graph));
			}
			sources = generated;
		} while (sources.size() > 0);

		// Done!
	}
}
