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
package wybs.lang;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.function.Predicate;

import wybs.util.AbstractCompilationUnit.Value;
import wyfs.lang.Content;
import wyfs.lang.Path;
import wyfs.util.Trie;

public interface Build {

	public interface System<T extends State> {
		public void apply(Function<T, T> transformer);
	}

	public interface Entry<T> {
		public Path.ID getID();

		public T getContent();

		public Content.Type<T> getContentType();
	}

	public interface State<S> {

		public <T> List<Entry<T>> selectAll(Content.Type<T> type);

		public <T> Entry<T> get(Content.Type<T> type, Path.ID id);

		public <T> S put(Content.Type<T> type, Path.ID id, T contents);
	}

	/**
	 * <p>
	 * Represents a top-level entity responsible for managing everything related to
	 * a given build. A build project provides a global "namespace" where named
	 * objects (e.g. source files, binary files) reside and/or are created. A build
	 * project also contains one or more build rules which determines how source
	 * files are transformed.
	 * </p>
	 * <p>
	 * For a given set of source files, a build project defines (in an abstract
	 * sense) a "build" --- that is, a specific plan of construction starting from
	 * one or more source files and producing one or more binary files. This is
	 * abstract because, in the normal course of events, the build is only known
	 * "after the fact"; that is, once all binary files are generated. This is
	 * necessary because it can be difficult to predict ahead of time what binary
	 * files will be generated from a given source file.
	 * </p>
	 * <p>
	 * Build projects have the opportunity to record the dependencies created during
	 * a build. That is, a binary file depends on those source file(s) required to
	 * build it. Recording this information is necessary if one wants to perform an
	 * incremental (re)compilation. That is, using such dependency information, one
	 * can avoid recompiling all source files from scratch.
	 * </p>
	 * <p>
	 * Finally, build projects may choose to record other information (e.g. timing
	 * and other statistical information) and/or employ different techniques (e.g.
	 * parallel builds).
	 * </p>
	 *
	 * @author David J. Pearce
	 *
	 */
	public interface Project {

		/**
		 * Get the root of the project. That is, the top-level location for the project.
		 *
		 * @return
		 */
		public Path.Root getRoot();

		/**
		 * Build this project with a given executor. In essence, this forces all tasks
		 * within the project to be submitted for execution within the executor.
		 *
		 * @param executor
		 * @param meter    used for profiling
		 * @return
		 */
		public Future<Boolean> build(ExecutorService executor, Meter meter);

		/**
		 * Refresh the project according to the latest system state. For example,
		 * refresh files from the file system which have changed.
		 *
		 * @throws IOExcweption
		 */
		public void refresh() throws IOException;

		/**
		 * Get the list of dependencies associated with this project.
		 *
		 * @return
		 */
		public List<Package> getPackages();

		/**
		 * Get the build rules associated with this project.
		 *
		 * @return
		 */
		public List<Build.Rule> getRules();

		/**
		 * Get the set of build tasks currently used by this project.
		 *
		 * @return
		 */
		public List<Build.Task> getTasks();
	}

	/**
	 * <p>
	 * A build rule is an abstraction describing how a set of one or more source
	 * files should be compiled. Each build rule is associated with a builder
	 * responsible for compiling matching files, a destination root and a mechanism
	 * for "matching source" files. For example, we could view a build rule like
	 * this:
	 * </p>
	 *
	 * <pre>
	 * WhileyCompiler :: src/:whiley/lang/*.whiley => bin/
	 * </pre>
	 *
	 * <p>
	 * Here, the builder is the <code>WhileyCompiler</code>, whilst the destination
	 * root is "bin/". Source files are taken from the root "src/" matching the
	 * regex "whiley/lang/*.whiley".
	 * </p>
	 *
	 * <p>
	 * Different build rules are free to implement the "matching" mechanism as they
	 * wish. Typically, one wants a generic way to describe a group of source files
	 * using wildcards (often called the "includes"). Occasionally, one also wants a
	 * way to exclude one or more files (oftern called the "excludes").
	 * </p>
	 *
	 * @author David J. Pearce
	 *
	 */
	public interface Rule {
		/**
		 * <p>
		 * Apply this rule to a given project root and register the results in a given
		 * build-graph. That is, identify all matching source files and their
		 * corresponding targets and register them with the graph.
		 * </p>
		 *
		 * @param tasks
		 *            The collection of tasks being constructed. New tasks arising from
		 *            this build rule should be added to this.
		 * @throws IOException
		 */
		public void apply(Collection<Build.Task> tasks) throws IOException;
	}

	/**
	 * <p>
	 * Represents a single atomic action within the context of a larger build
	 * project. A given task transforming files from one content type to another.
	 * Typically this revolves around compiling one or more source files into a
	 * given binary file, although other kinds of transformations are possible (e.g.
	 * source-to-source translations, etc).
	 * </p>
	 * <p>
	 * Every build task is associated with a given build project. Tasks within and
	 * across different projects may be executed in parallel.
	 * </p>
	 *
	 * @author David J. Pearce
	 *
	 */
	public interface Task {

		/**
		 * Prepare this task for execution. In particular, must produce a callable
		 * entity which will not block on some shared resource.
		 *
		 * @return
		 */
		public Function<Meter,Boolean> initialise() throws IOException;

		/**
		 * Get the project this build task instance is operating on.
		 *
		 * @return
		 */
		public Project project();

		/**
		 * Get the list of one or more source files for this task.
		 *
		 * @return
		 */
		public List<Path.Entry<?>> getSources();

		/**
		 * Get the target entry for this task.
		 *
		 * @return
		 */
		public Path.Entry<?> getTarget();
	}

	/**
	 * Responsible for recording detailed progress of a given task for both
	 * informational and profiling purposes. For example, providing feedback on
	 * expected time to completion in an IDE. Or, providing detailed feedback on
	 * number of steps executed by key components in a given task, etc.
	 *
	 * @author David J. Pearce
	 *
	 */
	public interface Meter {
		/**
		 * Create subtask of current task with a given name.
		 *
		 * @return
		 */
		public Meter fork(String name);

		/**
		 * Record an arbitrary step taking during this subtask for profiling purposes.
		 *
		 * @param tag
		 */
		public void step(String tag);

		/**
		 * Current (sub)task has completed.
		 */
		public void done();
	}

	public interface Stage {

	}

	/**
	 * Represents an external dependency for a project, which is typically a
	 * ZipFileRoot.
	 *
	 * @author David J. Pearce
	 *
	 */
	public interface Package {
		/**
		 * Get a parameter from the configuration of this package.
		 * @return
		 */
		public <T extends Value> T get(Class<T> kind, Trie key);
		/**
		 * Get the root associated with this package. This might be, for example, a
		 * ZipFile.
		 *
		 * @return
		 */
		public Path.Root getRoot();

	}

	public static final Build.Meter NULL_METER = new Build.Meter() {

		@Override
		public Meter fork(String name) {
			return NULL_METER;
		}

		@Override
		public void step(String tag) {

		}

		@Override
		public void done() {
		}

	};
}
