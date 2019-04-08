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
import java.util.Set;
import java.util.function.Predicate;

import wybs.util.AbstractCompilationUnit.Value;
import wycc.cfg.Configuration;
import wycc.lang.Feature;
import wycc.util.Logger;
import wycc.util.Pair;
import wyfs.lang.Content;
import wyfs.lang.Path;

public interface Build {

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
		 * Get the build executor used by this project.
		 *
		 * @return
		 */
		public Build.Executor getExecutor();

		/**
		 * Get the list of dependencies associated with this project.
		 *
		 * @return
		 */
		public List<Package> getPackages();

		/**
		 * Get the default logger associated with this project
		 */
		public Logger getLogger();

		/**
		 * Get the build rules associated with this project.
		 *
		 * @return
		 */
		public List<Build.Rule> getRules();
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
		 * corresponding targets and register them with the graph.</p>
		 *
		 * @return A build graph fragment mapping source files to targets.
		 * @throws IOException
		 */
		public void apply(Build.Executor graph) throws IOException;
	}

	/**
	 * <p>
	 * A build executor captures the relationships between compilation units. There are
	 * two relationships of interest: <i>vertical</i> and <i>horizontal</i>
	 * dependencies. For example one (or more) files being compiled to produce a
	 * generated or <i>derived</i> file corresponds to a vertical dependency between
	 * the original and the derived files. Horizontal dependencies correspond to
	 * situations where one file at the same level uses symbols from another.
	 * </p>
	 *
	 * @author David J. Pearce
	 *
	 */
	public interface Executor {
		/**
		 * Attempt to build all components in the correct order according to the graph.
		 *
		 * @return true if the build succeeded
		 */
		public boolean build() throws IOException;

		/**
		 * Get the task for this build entry.
		 *
		 * @return
		 */
		public Build.Task getTask(Path.Entry<?> target);

		/**
		 * Get the targets registered for this executor.
		 *
		 * @return
		 */
		public List<Path.Entry<?>> getTargets();

		/**
		 * Submit a given task to the build executor to be executed. Such a task may
		 * itself divide down into a number of smaller tasks, etc.
		 *
		 * @param task
		 *            The task used to build a target from one or more source(s)
		 */
		public void submit(Build.Task task);
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
		 * Execute this task which either succeeds or fails.
		 *
		 * @return
		 */
		public boolean apply() throws IOException;

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
	 * Provides a high-level concept of a target platform. These are registered by
	 * various backends to support different compilation targets.
	 *
	 * @author David J. Pearce
	 *
	 */
	public interface Platform extends Feature {
		/**
		 * Get the unique name identifying this platform.
		 *
		 * @return
		 */
		public String getName();

		/**
		 * Get the configuration schema for this build platform. This specifies the
		 * permitted set of options for the platform, including their types, etc.
		 *
		 * @return
		 */
		public Configuration.Schema getConfigurationSchema();

		/**
		 * Initialise this platform to produce a build task which can be used for
		 * compiling.
		 *
		 * @param project
		 *            Enclosing project for this build task
		 * @return
		 */
		public void initialise(Configuration configuration, Build.Project project) throws IOException;

		/**
		 * Get the source type for this build platform.
		 *
		 * @return
		 */
		public Content.Type<?> getSourceType();

		/**
		 * Get the target type for this build platform.
		 *
		 * @return
		 */
		public Content.Type<?> getTargetType();

		/**
		 * Execute a given function in the generated code for this platform.
		 *
		 * @param project
		 * @param path
		 * @param name
		 * @param args
		 */
		public void execute(Build.Project project, Path.ID path, String name, Value... args) throws IOException;
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
		 * Get the configuration associated with this package. This is determined by the
		 * <code>wy.toml</code>.
		 *
		 * @return
		 */
		public Configuration getConfiguration();

		/**
		 * Get the root associated with this package. This might be, for example, a
		 * ZipFile.
		 *
		 * @return
		 */
		public Path.Root getRoot();
	}
}
