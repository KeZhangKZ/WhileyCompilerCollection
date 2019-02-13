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
import java.util.HashSet;
import java.util.List;

import wybs.lang.Build.Graph;
import wybs.lang.SyntacticHeap;
import wybs.lang.SyntacticItem;
import wybs.lang.SyntaxError;
import wybs.util.AbstractCompilationUnit;
import wybs.util.StdBuildGraph;
import wybs.util.AbstractCompilationUnit.Attribute.Span;
import wybs.util.AbstractCompilationUnit.Value;
import wycc.WyProject;
import wycc.cfg.Configuration;
import wycc.cfg.Configuration.Schema;
import wycc.lang.Command;
import wycc.util.ArrayUtils;
import wycc.util.Logger;
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
			return Arrays.asList(Command.OPTION_FLAG("verbose", "generate verbose information about the build", false),
					Command.OPTION_FLAG("brief", "generate brief output for syntax errors", false));
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
		public Command initialise(Command environment, Configuration configuration) {
			return new Build((WyProject) environment, configuration, System.out, System.err);
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
	 * Signals that brief error reporting should be used. This is primarily used to
	 * help integration with external tools. More specifically, brief output is
	 * structured so as to be machine readable.
	 */
	protected boolean brief = false;

	/**
	 * The enclosing project for this build
	 */
	private final WyProject project;

	public Build(WyProject project, Configuration configuration, OutputStream sysout, OutputStream syserr) {
		this.project = project;
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
	public boolean execute(Template template) throws Exception {
		// Extract options
		boolean verbose = template.getOptions().get("verbose", Boolean.class);
		// Identify the project root
		Path.Root root = project.getParent().getLocalRoot();
		// Create our build graph
		Graph graph = new StdBuildGraph();
		// Extract all registered platforms
		List<wybs.lang.Build.Platform> platforms = project.getTargetPlatforms();
		// Refresh the build graph
		for (int i = 0; i != platforms.size(); ++i) {
			wybs.lang.Build.Platform platform = platforms.get(i);
			Path.Root srcRoot = platform.getSourceRoot(root);
			Path.Root binRoot = platform.getTargetRoot(root);
			// Determine the list of modified source files.
			platform.refresh(graph, srcRoot, binRoot);
		}
		// Determine modified files
		ArrayList<Path.Entry<?>> sources = new ArrayList<>();
		//
		for (Path.Entry<?> source : graph.getEntries()) {
			// Get all children derived from this entry
			List<Path.Entry<?>> children = graph.getChildren(source);
			// Check for any which are out-of-date
			for (Path.Entry<?> binary : children) {
				if (binary.lastModified() < source.lastModified()) {
					// Binary modified before source.
					sources.add(source);
				}
			}
		}
		// Now rebuild everything!
		project.build(sources, graph);
		// Look for error messages
		printSyntacticMarkers(graph);
		//
		return true;
	}

	/**
	 * Print out syntactic markers for all entries in the build graph. This requires
	 * going through all entries, extracting the markers and then printing them.
	 *
	 * @param graph
	 * @throws IOException
	 */
	private void printSyntacticMarkers(wybs.lang.Build.Graph graph) throws IOException {
		// Extract all syntactic markers from entries in the build grpah
		List<SyntacticItem> items = extractSyntacticMarkers(graph.getEntries());
		// For each marker, print out error messages appropriately
		for (int i = 0; i != items.size(); ++i) {
			SyntacticItem item = items.get(i);
			List<SyntacticItem.Marker> markers = item.getAttributes(SyntacticItem.Marker.class);
			for (int j = 0; j != markers.size(); ++j) {
				// Log the error message
				printSyntacticMarkers(item, markers.get(j));
			}
		}
	}

	/**
	 * Print out an individual syntactic markers.
	 *
	 * @param marker
	 */
	private void printSyntacticMarkers(SyntacticItem parent, SyntacticItem.Marker marker) {
		syserr.println("syntax error: " + marker.getMessage());
		// What we need to do here is work out the lines which are spanned by the
		// syntactic item in question and then print them (somehow).
		Span span = parent.getAncestor(AbstractCompilationUnit.Attribute.Span.class);
		// FIXME: what to do now??
	}

	/**
	 * Traverse the various binaries which have been generated looking for error
	 * messages.
	 *
	 * @param binaries
	 * @return
	 * @throws IOException
	 */
	private List<SyntacticItem> extractSyntacticMarkers(Collection<Path.Entry<?>> binaries) throws IOException {
		List<SyntacticItem> annotated = new ArrayList<>();
		//
		for (Path.Entry<?> binary : binaries) {
			Object o = binary.read();
			// If the object in question can be decoded as a syntactic heap then we can look
			// for syntactic messages.
			if (o instanceof SyntacticHeap) {
				SyntacticHeap h = (SyntacticHeap) o;
				extractSyntacticMarkers(h.getRootItem(), annotated);
			}
		}
		//
		return annotated;
	}

	private void extractSyntacticMarkers(SyntacticItem item, List<SyntacticItem> items) {
		// Check whether this item has a marker associated with it.
		if (item.getAttribute(SyntacticItem.Marker.class) != null) {
			// At least one marked assocaited with item.
			items.add(item);
		}
		// Recursive children looking for other syntactic markers
		for (int i = 0; i != item.size(); ++i) {
			extractSyntacticMarkers(item.getOperand(i), items);
		}
	}
}
