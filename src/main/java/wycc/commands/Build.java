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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import wybs.lang.Build.Graph;
import wybs.lang.SyntacticHeap;
import wybs.lang.SyntacticItem;
import wybs.util.AbstractCompilationUnit;
import wybs.util.StdBuildGraph;
import wybs.util.AbstractCompilationUnit.Attribute.Span;
import wybs.util.AbstractCompilationUnit.Attribute;
import wycc.WyProject;
import wycc.cfg.Configuration;
import wycc.cfg.Configuration.Schema;
import wycc.lang.Command;
import wyfs.lang.Path;

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
		for (int i = 0; i != platforms.size(); ++i) {
			wybs.lang.Build.Platform platform = platforms.get(i);
			Path.Root srcRoot = platform.getSourceRoot(root);
			Path.Root binRoot = platform.getTargetRoot(root);
			for (Path.Entry<?> binary : binRoot.get(platform.getTargetFilter())) {
				printSyntacticMarkers(graph.getParents(binary), binary);
			}
		}
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
	private void printSyntacticMarkers(Collection<Path.Entry<?>> sources, Path.Entry<?> target) throws IOException {
		// Extract all syntactic markers from entries in the build grpah
		List<SyntacticItem.Marker> items = extractSyntacticMarkers(target);
		// For each marker, print out error messages appropriately
		for (int i = 0; i != items.size(); ++i) {
			// Log the error message
			printSyntacticMarkers(sources, items.get(i));
		}
	}

	/**
	 * Print out an individual syntactic markers.
	 *
	 * @param marker
	 */
	private void printSyntacticMarkers(Collection<Path.Entry<?>> sources, SyntacticItem.Marker marker) {
		//
		Path.Entry<?> source = getSourceEntry(sources,marker.getSource());
		//
		Span span = marker.getTarget().getAncestor(AbstractCompilationUnit.Attribute.Span.class);
		// Read the enclosing line so we can print it
		EnclosingLine line = readEnclosingLine(source, span);
		// Sanity check we found it
		if(line != null) {
			// print the error message
			syserr.println(source.location() + ":" + line.lineNumber + ": " + marker.getMessage());
			// Finally print the line highlight
			printLineHighlight(syserr, line);
		} else {
			syserr.println(source.location() + ":?: " + marker.getMessage());
		}
	}

	/**
	 * Traverse the various binaries which have been generated looking for error
	 * messages.
	 *
	 * @param binaries
	 * @return
	 * @throws IOException
	 */
	private List<SyntacticItem.Marker> extractSyntacticMarkers(Path.Entry<?>... binaries) throws IOException {
		List<SyntacticItem.Marker> annotated = new ArrayList<>();
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

	private void extractSyntacticMarkers(SyntacticItem item, List<SyntacticItem.Marker> items) {
		// Check whether this item has a marker associated with it.
		if (item instanceof SyntacticItem.Marker) {
			// At least one marked assocaited with item.
			items.add((SyntacticItem.Marker) item);
		}
		// Recursive children looking for other syntactic markers
		for (int i = 0; i != item.size(); ++i) {
			extractSyntacticMarkers(item.getOperand(i), items);
		}
	}

	private Path.Entry<?> getSourceEntry(Collection<Path.Entry<?>> sources, Path.ID id) {
		String str = id.toString();
		//
		for (Path.Entry<?> s : sources) {
			// FIXME: this is obviously a bad hack for now
			String sid = s.id().toString();
			if (sid.endsWith(str)) {
				return s;
			}
		}
		return null;
	}


	private void printLineHighlight(PrintStream output,
			EnclosingLine enclosing) {
		// NOTE: in the following lines I don't print characters
		// individually. The reason for this is that it messes up the
		// ANT task output.
		String str = enclosing.lineText;

		if (str.length() > 0 && str.charAt(str.length() - 1) == '\n') {
			output.print(str);
		} else {
			// this must be the very last line of output and, in this
			// particular case, there is no new-line character provided.
			// Therefore, we need to provide one ourselves!
			output.println(str);
		}
		str = "";
		for (int i = 0; i < enclosing.columnStart(); ++i) {
			if (enclosing.lineText.charAt(i) == '\t') {
				str += "\t";
			} else {
				str += " ";
			}
		}
		for (int i = enclosing.columnStart(); i <= enclosing.columnEnd(); ++i) {
			str += "^";
		}
		output.println(str);
	}

	private static EnclosingLine readEnclosingLine(Path.Entry<?> entry, Attribute.Span location) {
		int spanStart = location.getStart().get().intValue();
		int spanEnd = location.getEnd().get().intValue();
		int line = 0;
		int lineStart = 0;
		int lineEnd = 0;
		StringBuilder text = new StringBuilder();
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(entry.inputStream(), "UTF-8"));

			// first, read whole file
			int len = 0;
			char[] buf = new char[1024];
			while ((len = in.read(buf)) != -1) {
				text.append(buf, 0, len);
			}

			while (lineEnd < text.length() && lineEnd <= spanStart) {
				lineStart = lineEnd;
				lineEnd = parseLine(text, lineEnd);
				line = line + 1;
			}
		} catch (IOException e) {
			return null;
		}
		lineEnd = Math.min(lineEnd, text.length());

		return new EnclosingLine(spanStart, spanEnd, line, lineStart, lineEnd,
				text.substring(lineStart, lineEnd));
	}

	private static int parseLine(StringBuilder buf, int index) {
		while (index < buf.length() && buf.charAt(index) != '\n') {
			index++;
		}
		return index + 1;
	}

	private static class EnclosingLine {
		private int lineNumber;
		private int start;
		private int end;
		private int lineStart;
		private int lineEnd;
		private String lineText;

		public EnclosingLine(int start, int end, int lineNumber, int lineStart, int lineEnd, String lineText) {
			this.start = start;
			this.end = end;
			this.lineNumber = lineNumber;
			this.lineStart = lineStart;
			this.lineEnd = lineEnd;
			this.lineText = lineText;
		}

		public int columnStart() {
			return start - lineStart;
		}

		public int columnEnd() {
			return Math.min(end, lineEnd) - lineStart;
		}
	}
}
