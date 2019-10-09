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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import wybs.lang.*;
import wybs.lang.Build.Environment;
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
 *
 * @author David J. Pearce
 */
public class SequentialBuildProject implements Build.Project {
	/**
	 * The environment in which this project is executing.
	 */
	protected final Build.Environment environment;
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
	 * The set of instantiated build tasks in topological order. This is
	 * <code>null</code> when the project is dirty and the build graph needs to be
	 * refreshed.
	 */
	protected Build.Task[] tasks;

	/**
	 * The set of task instances for each task.
	 */
	protected Callable<Boolean>[] instances;

	public SequentialBuildProject(Build.Environment environment, Path.Root root) {
		this.root = root;
		this.rules = new ArrayList<>();
		this.packages = new ArrayList<>();
		this.environment = environment;
	}

	@Override
	public Environment getEnvironment() {
		return environment;
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
	@Override
	public List<Build.Rule> getRules() {
		return rules;
	}

	@Override
	public List<Build.Task> getTasks() {
		return Arrays.asList(tasks);
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
	 * items which have been modified, this operation has no effect (i.e. the new
	 * contents are retained).
	 */
	@Override
	public void refresh() throws IOException {
		// Refresh the root to ensure all filesystem changes are recognised.
		root.refresh();
		// Construct temporary list of tasks
		ArrayList<Build.Task> tmp = new ArrayList<>();
		// Match all rules to produce the list of tasks
		for (Build.Rule rule : rules) {
			rule.apply(tmp);
		}
		// Topologically sort tasks
		this.tasks = topologicalSort(tmp);
		// Initialise task instances
		this.instances = initialiseAll(tasks);
	}

	// ======================================================================
	// Build
	// ======================================================================

	/**
	 * Build a given set of source entries, including all files which depend upon
	 * them.
	 * @throws IOException
	 *
	 * @throws Exception
	 */
	@Override
	public Future<Boolean> build(ExecutorService executor) {
		return executor.submit(() -> execute(executor, instances));
	}

	// ======================================================================
	// Helpers
	// ======================================================================

	/**
	 * Execute a given set of build task instances in strict sequence. In other
	 * words, do not allow concurrent execution of tasks which are independent of
	 * each other.
	 *
	 * @param executor
	 * @param callables
	 * @return
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	private static boolean execute(ExecutorService executor, Callable<Boolean>... callables)
			throws InterruptedException, ExecutionException {
		// Execute each task in sequential order. Since tasks are topologically sorted,
		// we know tasks are executed in the correct order.
		for (int i = 0; i != callables.length; ++i) {
			//
			Future<Boolean> f = executor.submit(callables[i]);
			if (!f.get()) {
				return false;
			}
		}
		return true;
	}

	private static Callable<Boolean>[] initialiseAll(Build.Task... tasks) throws IOException {
		Callable<Boolean>[] instances = new Callable[tasks.length];
		for(int i=0;i!=tasks.length;++i) {
			instances[i] = tasks[i].initialise();
		}
		return instances;
	}

	/**
	 * A very simple implementation of a topological sort over build tasks.
	 *
	 * @param tasks
	 */
	private static Build.Task[] topologicalSort(List<Build.Task> tasks) {
		// Indicates whether tasks placed on stack or not.
		BitSet placed = new BitSet();
		// Holds topological ordering of tasks
		ArrayList<Build.Task> stack = new ArrayList<>();
		// Place each task in turn. This recursively ensures the ancestors for each task
		// are placed first.
		for (int i = 0; i != tasks.size(); ++i) {
			if (!placed.get(i)) {
				topologicalSort(i, tasks, placed, stack);
			}
		}
		//
		// Write out tasks as an array
		return stack.toArray(new Build.Task[stack.size()]);
	}

	/**
	 * Place the task at the give index into the topological order. The key is that
	 * we must first ensure all ancestors are placed before we can place this task.
	 *
	 * @param index
	 *            Index in tasks list of the task being placed.
	 * @param tasks
	 *            The original list of tasks to be topological sorted.
	 * @param placed
	 *            Identifies tasks which have now been placed in the final ordering.
	 * @param order
	 *            The final ordering of tasks where the ancestors of a task are
	 *            guaranteed to come before (i.e. have earlier indices) the task
	 *            itself.
	 */
	private static void topologicalSort(int index, List<Build.Task> tasks, BitSet placed, List<Build.Task> order) {
		// Extract task being placed
		Build.Task task = tasks.get(index);
		List<Path.Entry<?>> ancestors = task.getSources();
		// First, place all ancestors
		for (int i = 0; i != tasks.size(); ++i) {
			Build.Task t = tasks.get(i);
			// Check whether is an unplaced ancestor
			if (ancestors.contains(t.getTarget()) && !placed.get(i)) {
				// Yes, therefore place it
				topologicalSort(i, tasks, placed, order);
			}
		}
		// Finally, place this task
		placed.set(index);
		order.add(task);
	}
}
