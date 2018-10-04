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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import wybs.io.SyntacticHeapPrinter;
import wybs.lang.SyntacticHeap;
import wycc.WyProject;
import wycc.cfg.Configuration;
import wycc.lang.Command;
import wycc.lang.Command.Descriptor;
import wycc.lang.Command.Option;
import wyfs.lang.Content;
import wyfs.lang.Path;
import wyfs.util.Trie;

public class Inspect implements Command {

	public static final Configuration.Schema SCHEMA = Configuration
			.fromArray();

	public static final List<Option.Descriptor> OPTIONS = Arrays.asList();

	/**
	 * The descriptor for this command.
	 */
	public static final Command.Descriptor DESCRIPTOR = new Command.Descriptor() {
		@Override
		public String getName() {
			return "inspect";
		}

		@Override
		public String getDescription() {
			return "Inspect a given project file";
		}

		@Override
		public List<Option.Descriptor> getOptionDescriptors() {
			return OPTIONS;
		}

		@Override
		public Configuration.Schema getConfigurationSchema() {
			return SCHEMA;
		}

		@Override
		public List<Descriptor> getCommands() {
			return Collections.EMPTY_LIST;
		}

		@Override
		public Command initialise(Command environment, Configuration configuration) {
			// FIXME: should have some framework for output, rather than hard-coding
			// System.out.
			return new Inspect(System.out, (WyProject) environment, configuration);
		}
	};

	private final PrintStream out;
	private final WyProject project;
	private final Configuration configuration;
	private final int width = 16;

	public Inspect(PrintStream out, WyProject project, Configuration configuration) {
		this.project = project;
		this.configuration = configuration;
		this.out = out;
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
		List<String> files = template.getArguments();
		for (String file : files) {
			Content.Type<?> ct = getContentType(file);
			Path.Entry<?> entry = getEntry(file, ct);
			if(entry == null) {
				out.println("unknown file: " + file);
			} else {
				inspect(entry, ct);
			}
		}
		return true;
	}

	/**
	 * Determine the content type for this file.
	 *
	 * @param file
	 * @return
	 */
	private Content.Type<?> getContentType(String file) {
		List<Content.Type<?>> cts = project.getParent().getContentTypes();
		for (int i = 0; i != cts.size(); ++i) {
			Content.Type<?> ct = cts.get(i);
			String suffix = "." + ct.getSuffix();
			if (file.endsWith(suffix)) {
				return ct;
			}
		}
		// Default is just a binary file
		return Content.BinaryFile;
	}

	/**
	 * Get the entry associated with this file.
	 *
	 * @param file
	 * @param ct
	 * @return
	 * @throws IOException
	 */
	public Path.Entry<?> getEntry(String file, Content.Type<?> ct) throws IOException {
		// Strip suffix
		file = file.replace("." + ct.getSuffix(), "");
		// Determine path id
		Path.ID id = Trie.fromString(file);
		// Get the file from the repository root
		return project.getParent().getLocalRoot().get(id, ct);
	}

	/**
	 * Inspect a given path entry.
	 *
	 * @param entry
	 * @param ct
	 * @throws IOException
	 */
	private void inspect(Path.Entry<?> entry, Content.Type<?> ct) throws IOException {
		Object o = entry.read();
		if(o instanceof SyntacticHeap) {
			new SyntacticHeapPrinter(new PrintWriter(out)).print((SyntacticHeap) o);
		} else {
			inspectBinaryFile(readAllBytes(entry.inputStream()));
		}
	}

	/**
	 * Inspect a given binary file. That is a file for which we don't have a better
	 * inspector.
	 *
	 * @param bytes
	 */
	private void inspectBinaryFile(byte[] bytes) {
		for (int i = 0; i < bytes.length; i += width) {
			out.print(String.format("0x%04X ", i));
			// Print out databytes
			for (int j = 0; j < width; ++j) {
				if(j+i < bytes.length) {
					out.print(String.format("%02X ", bytes[i+j]));
				} else {
					out.print("   ");
				}
			}
			//
			for (int j = 0; j < width; ++j) {
				if(j+i < bytes.length) {
					char c = (char) bytes[i+j];
					if(c >= 32 && c < 128) {
						out.print(c);
					} else {
						out.print(".");
					}
				}
			}
			//
			out.println();
		}
	}

	private static byte[] readAllBytes(InputStream in) throws IOException {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		int nRead;
		// Read bytes in max 1024 chunks
		byte[] data = new byte[1024];
		// Read all bytes from the input stream
		while ((nRead = in.read(data, 0, data.length)) != -1) {
			buffer.write(data, 0, nRead);
		}
		// Done
		buffer.flush();
		return buffer.toByteArray();
	}
}

