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
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import wybs.lang.SyntacticItem;
import wybs.lang.SyntaxError;
import wybs.util.StdBuildGraph;
import wybs.util.AbstractCompilationUnit.Value;
import wycc.WyProject;
import wycc.cfg.Configuration;
import wycc.cfg.Configuration.Schema;
import wycc.lang.Command;
import wycc.util.ArrayUtils;
import wycc.util.Pair;
import wyfs.lang.Content;
import wyfs.lang.Path;
import wyfs.util.Trie;
import wyfs.lang.Content.Type;

public class Build implements Command {
	/**
	 * The descriptor for this command.
	 */
	public static final Command.Descriptor DESCRIPTOR = new Command.Descriptor() {
		@Override
		public String getName() {
			return "build";
		}

		@Override
		public String getDescription() {
			return "Perform build operations on an existing project";
		}

		@Override
		public List<Option.Descriptor> getOptionDescriptors() {
			return Arrays.asList(
					Command.OPTION_FLAG("verbose","generate verbose information about the build",false),
					Command.OPTION_FLAG("brief","generate brief output for syntax errors",false)
					);
		}

		@Override
		public Schema getConfigurationSchema() {
			return Configuration.EMPTY_SCHEMA;
		}

		@Override
		public List<Descriptor> getCommands() {
			return Collections.EMPTY_LIST;
		}

		@Override
		public Command initialise(Command environment, Command.Options options, Configuration configuration) {
			return new Build((WyProject) environment, options, configuration, System.out, System.err);
		}

	};

	/**
	 * Provides a generic place to which normal output should be directed. This
	 * should eventually be replaced.
	 */
	private final PrintStream sysout;

	/**
	 * Provides a generic place to which error output should be directed. This
	 * should eventually be replaced.
	 */
	private final PrintStream syserr;

	/**
	 * Signals that verbose output should be produced.
	 */
	protected boolean verbose = false;

	/**
	 * Signals that brief error reporting should be used. This is primarily used
	 * to help integration with external tools. More specifically, brief output
	 * is structured so as to be machine readable.
	 */
	protected boolean brief = false;

	/**
	 * The enclosing project for this build
	 */
	private final WyProject project;

	public Build(WyProject project, Command.Options options, Configuration configuration, OutputStream sysout,
			OutputStream syserr) {
		this.project = project;
		this.verbose = options.get("verbose", Boolean.class);
		this.sysout = new PrintStream(sysout);
		this.syserr = new PrintStream(syserr);
	}

	@Override
	public Descriptor getDescriptor() {
		return DESCRIPTOR;
	}

	@Override
	public void initialise() {
	}

	@Override
	public void finalise() {
	}

	@Override
	public boolean execute(List<String> args) {
		try {
			// Identify the project root
			Path.Root root = project.getParent().getLocalRoot();
			// Extract all registered platforms
			List<wybs.lang.Build.Platform> platforms = project.getTargetPlatforms();
			ArrayList<Path.Entry<?>> sources = new ArrayList<>();
			// Construct build rules for each platform in turn.
			for (int i = 0; i != platforms.size(); ++i) {
				wybs.lang.Build.Platform platform = platforms.get(i);
				Content.Type<?> binType = platform.getTargetType();
				Path.Root srcRoot = platform.getSourceRoot(root);
				Path.Root binRoot = platform.getTargetRoot(root);
				// Determine the list of modified source files.
				List modified = getModifiedSourceFiles(srcRoot, platform.getSourceFilter(), binRoot, binType);
				// Add the list of modified source files to the list.
				sources.addAll(modified);
			}
			// Finally, rebuild everything!
			project.build(sources);
			return true;
		}  catch (SyntaxError e) {
			SyntacticItem element = e.getElement();
			e.outputSourceError(syserr, false);
			if (verbose) {
				printStackTrace(syserr, e);
			}
			return false;
		} catch (Exception e) {
			// FIXME: do something here??
			e.printStackTrace();
			return false;
		}
	}

	// =======================================================================
	// Helpers
	// =======================================================================

	/**
	 * Generate the list of source files which need to be recompiled. By default,
	 * this is done by comparing modification times of each whiley file against its
	 * corresponding wyil file. Wyil files which are out-of-date are scheduled to be
	 * recompiled.
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

	/**
	 * Print a complete stack trace. This differs from Throwable.printStackTrace()
	 * in that it always prints all of the trace.
	 *
	 * @param out
	 * @param err
	 */
	private static void printStackTrace(PrintStream out, Throwable err) {
		out.println(err.getClass().getName() + ": " + err.getMessage());
		for (StackTraceElement ste : err.getStackTrace()) {
			out.println("\tat " + ste.toString());
		}
		if (err.getCause() != null) {
			out.print("Caused by: ");
			printStackTrace(out, err.getCause());
		}
	}
}
