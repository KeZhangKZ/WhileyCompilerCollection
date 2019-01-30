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
		Path.Root getRoot();

		/**
		 * Get the list of dependencies associated with this project.
		 *
		 * @return
		 */
		public List<Package> getPackages();

		/**
		 * Get the default logger assocated with this project
		 */
		public Logger getLogger();
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
		 * Apply this rule to a given compilation group, producing a set of generated or
		 * modified files. This set may be empty if the rule does not match against any
		 * source file in the group.
		 * </p>
		 *
		 * @param group The set of files currently being compiled.
		 * @param graph The build graph being constructed
		 * @return The set of files generated by this rule (which may be empty, but
		 *         cannot be <code>null</code>).
		 * @throws IOException
		 */
		public Set<Path.Entry<?>> apply(Collection<? extends Path.Entry<?>> group, Build.Graph graph)
				throws IOException;
	}

	/**
	 * <p>
	 * A build graph captures the relationships between compilation units. There are
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
	public interface Graph {
		/**
		 * Determine the entries (the parents) that a given entry (the child) derives
		 * from. An entry is said to derive from another if it generated from the other
		 * during the compilation process. For example, a source file is compiled into a
		 * binary or intermediate file. The latter is said to be derived from the
		 * former.
		 *
		 * @param child The child entry which is derived from zero or more parent
		 *              entries.
		 * @return
		 */
		List<Path.Entry<?>> getParents(Path.Entry<?> child);

		/**
		 * Determine the entries (the children) that a given entry (the parent) derives.
		 * An entry is said to derive from another if it generated from the other during
		 * the compilation process. For example, a source file is compiled into a binary
		 * or intermediate file. The latter is said to be derived from the former.
		 *
		 * @param parent The parent entry which derives zero or more children
		 *
		 * @return
		 */
		List<Path.Entry<?>> getChildren(Path.Entry<?> parent);

		/**
		 * Get the list of entries (i.e. nodes) in the build graph.
		 *
		 * @return
		 */
		Set<Path.Entry<?>> getEntries();

		/**
		 * Register a derivation from one file (the parent) to another (the child). A
		 * derivation indicates that one file (e.g. a source file) generated another
		 * (e.g. a binary file) during the compilation process.
		 *
		 * @param parent
		 * @param child
		 */
		void connect(Path.Entry<?> parent, Path.Entry<?> child);
	}

	/**
	 * <p>
	 * Represents a single atomic action within the context of a larger build
	 * project. A given task transforming files from one content type to another.
	 * Typically this revolves around compiling a source file into one or more
	 * binary files, although other kinds of transformations are possible (e.g.
	 * source-to-source translations, etc).
	 * </p>
	 * <p>
	 * Every build task is associated with a given build project. There will be at
	 * most one instance of a build task for a given project. Different projects
	 * will not share task instances. This means that per-project caching within a
	 * given task is possible, though care must be taken.
	 * </p>
	 * <p>
	 * Every build task has a unique name which identifies the task. This allows the
	 * task to be configured and/or to ensure required platform dependencies are
	 * met.
	 * </p>
	 *
	 * @author David J. Pearce
	 *
	 */
	public interface Task extends Feature {

		/**
		 * The unique identifier for this task through which it can be referred.
		 *
		 * @return
		 */
		// public String id();

		/**
		 * Get the project this build task instance is operating on.
		 *
		 * @return
		 */
		public Project project();

		/**
		 * Build a given set of source files to produce target files in specified
		 * locations. A delta represents a list of pairs (s,t), where s is a source file
		 * and t is the destination root for all generated files. Each file may be
		 * associated with a different destination directory, in order to support e.g.
		 * multiple output directories.
		 *
		 * @param delta --- the set of files to be built.
		 * @param graph --- The build graph being constructed
		 * @return --- the set of files generated or modified.
		 */
		public Set<Path.Entry<?>> build(Collection<Pair<Path.Entry<?>, Path.Root>> delta, Build.Graph graph)
				throws IOException;
	}

	/**
	 * Represents an action that may be applied to a module. Such actions typically
	 * either check that a module is valid (with respect to some particular
	 * concern), or apply optimisations to the module. Examples include <i>constant
	 * propagation</i> and <i>definite assignment analysis</i>.
	 *
	 * @author David J. Pearce
	 *
	 */
	public interface Stage<T> extends Feature {
		/**
		 * Apply this transform to the given module. Modifications are made to the
		 * module in-place. To easy integration with other frameworks (e.g. Eclipse),
		 * any exception may be thrown.
		 *
		 * @param file --- compilation unit to be transformed
		 * @throws Exception --- some kind of failure occurred.
		 */
		public void apply(T file) throws IOException;
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
		 * Update the build platform with a new configuration.
		 *
		 * @param configuration Configuration options matching the platform's schema
		 */
		public void apply(Configuration configuration);

		/**
		 * Initialise this platform to produce a build task which can be used for
		 * compiling.
		 *
		 * @param project Enclosing project for this build task
		 * @return
		 */
		public Build.Task initialise(Build.Project project);

		/**
		 * Refresh the build graph to represent the compilation structure used by this
		 * build platform.
		 *
		 * @param graph The build graph to be updated
		 * @param src   the source root
		 * @param bin   the binary root
		 */
		public void refresh(Build.Graph graph, Path.Root src, Path.Root bin) throws IOException;

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
		 * Get the source filter for this build platform. This identifies all files
		 * reachable from the source root which constitute source files.
		 *
		 * @return
		 */
		public Content.Filter<?> getSourceFilter();

		/**
		 * Get the target filter for this build platform. This identifies all files
		 * reachable from the source root which constitute target (e.g. binary) files.
		 *
		 * @return
		 */
		public Content.Filter<?> getTargetFilter();

		/**
		 * Get the root for all source files used by the build platform.
		 *
		 * @param root The project root.
		 * @return
		 */
		public Path.Root getSourceRoot(Path.Root root) throws IOException;

		/**
		 * Get the root for all target files produced by the build platform.
		 *
		 * @param root The project root.
		 * @return
		 */
		public Path.Root getTargetRoot(Path.Root root) throws IOException;

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
