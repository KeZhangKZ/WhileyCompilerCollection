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
import wycc.util.Logger;
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
	 * The top-level root for the project. Everything is below this.
	 */
	protected final Path.Root root;

	/**
	 * The set of packages on which this project depends.
	 */
	protected final ArrayList<Build.Package> packages;

	/**
	 * The rules associated with this project for transforming content. It is
	 * assumed that for any given transformation there is only one possible pathway
	 * described.
	 */
	protected final ArrayList<Build.Rule> rules;

	/**
	 * The standard logger associated with this project.
	 */
	protected Logger logger;

	public StdProject(Path.Root root) {
		this.root = root;
		this.rules = new ArrayList<>();
		this.packages = new ArrayList<>();
		this.logger = Logger.NULL;
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
	 * Get the top-level root associated with this project.
	 *
	 * @return
	 */
	@Override
	public Path.Root getRoot() {
		return root;
	}

	/**
	 * Get the build rules associated with this project.
	 *
	 * @return
	 */
	public List<Build.Rule> getRules() {
		return rules;
	}

	/**
	 * Get the packages (i.e. dependencies) associated with this projects.
	 *
	 * @return
	 */
	@Override
	public List<Build.Package> getPackages() {
		return packages;
	}

	@Override
	public Logger getLogger() {
		return logger;
	}

	public void setLogger(Logger logger) {
		this.logger = logger;
	}

	// ======================================================================
	// Accessors
	// ======================================================================

	// ======================================================================
	// Mutators
	// ======================================================================

	/**
	 * Force root to flush entries to permanent storage (where appropriate). This is
	 * essential as, at any given moment, path entries may only be stored in memory.
	 * We must flush them to disk in order to preserve any changes that were made.
	 */
	public void flush() throws IOException {
		root.flush();
	}

	/**
	 * Force root to refresh entries from permanent storage (where appropriate). For
	 * items which has been modified, this operation has no effect (i.e. the new
	 * contents are retained).
	 */
	public void refresh() throws IOException {
		root.refresh();
	}

	// ======================================================================
	// Build
	// ======================================================================

	/**
	 * Build a given set of source entries, including all files which depend upon
	 * them.
	 *
	 * @param sources
	 *            --- a collection of source file entries. This will not be modified
	 *            by this method.
	 * @throws Exception
	 */
	public void build(Collection<? extends Path.Entry<?>> sources, Build.Graph graph) throws Exception {
		// Continue building all source files until there are none left. This is
		// actually quite a naive implementation, as it ignores the potential
		// need for staging dependencies.
		do {
			HashSet<Path.Entry<?>> generated = new HashSet<>();
			for (Build.Rule r : rules) {
				generated.addAll(r.apply(sources, graph));
			}
			sources = generated;
		} while (sources.size() > 0);

		// Done!
	}
}
