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

import java.io.PrintStream;
import java.util.Collections;
import java.util.List;

import wycc.cfg.Configuration;
import wycc.cfg.Configuration.KeyValueDescriptor;
import wycc.lang.Command;
import wyfs.lang.Path;
import wyfs.lang.Path.ID;
import wyfs.util.Trie;

public class Help implements Command {
	/**
	 * Identifies the configuration key which holds the list of system commands.
	 * This is necessary for determining what commands are available.
	 */
	private static Path.ID SYSTEM_COMMANDS_ID = Trie.fromString("system/commands");

	/**
	 * The descriptor for this command.
	 */
	public static final Command.Descriptor DESCRIPTOR = new Command.Descriptor() {
		@Override
		public String getName() {
			return "help";
		}

		@Override
		public String getDescription() {
			return "Display help information";
		}

		@Override
		public Configuration.Schema getConfigurationSchema() {
			return Configuration.EMPTY_SCHEMA;
		}

		@Override
		public List<Descriptor> getCommands() {
			return Collections.EMPTY_LIST;
		}

		@Override
		public Command initialise(Configuration configuration) {
			List<Command.Descriptor> descriptors = configuration.get(List.class, SYSTEM_COMMANDS_ID);
			// FIXME: should have some framework for output, rather than hard-coding
			// System.out.
			return new Help(System.out, descriptors);
		}
	};
	//
	private final PrintStream out;
	private final List<Command.Descriptor> descriptors;

	public Help(PrintStream out, List<Command.Descriptor> descriptors) {
		System.out.println("DESCRIPTORS: " + descriptors);
		this.descriptors = descriptors;
		this.out = out;
	}

	@Override
	public Descriptor getDescriptor() {
		return DESCRIPTOR;
	}

	@Override
	public void initialise() {
		// Nothing to do here
	}

	@Override
	public void finalise() {
		// Nothing to do here
	}

	@Override
	public boolean execute(List<String> args) {
		System.out.println("Help.execute " + args + " called");
		if (args.size() == 0) {
			printUsage();
		} else {
			// Search for the command
			Command.Descriptor command = null;
			for (Command.Descriptor c : descriptors) {
				if (c.getName().equals(args.get(0))) {
					command = c;
					break;
				}
			}
			//
			if (command == null) {
				out.println("No entry for " + args.get(0));
			} else {
				printCommandDetails(command);
			}
		}
		//
		return true;
	}

	protected void printCommandDetails(Command.Descriptor descriptor) {
		out.println("NAME");
		out.println("\t" + descriptor.getName());
		out.println();
		out.println("DESCRIPTION");
		out.println("\t" + descriptor.getDescription());
		out.println();
		out.println("OPTIONS");
		Configuration.Schema schema = descriptor.getConfigurationSchema();
		List<Configuration.KeyValueDescriptor<?>> descriptors = schema.getDescriptors();
		for (int i = 0; i != descriptors.size(); ++i) {
			Configuration.KeyValueDescriptor<?> option = descriptors.get(i);
			out.println("\t--" + option.getFilter());
			out.println("\t\t" + option.getDescription());
		}
	}

	/**
	 * Print usage information to the console.
	 */
	protected void printUsage() {
		out.println("usage: wy [--verbose] command [<options>] [<args>]");
		out.println();
		int maxWidth = determineCommandNameWidth(descriptors);
		out.println("Commands:");
		for (Command.Descriptor d : descriptors) {
			out.print("  ");
			out.print(rightPad(d.getName(), maxWidth));
			out.println("   " + d.getDescription());
		}
		out.println();
		out.println("Run `wy help COMMAND` for more information on a command");
	}

	/**
	 * Right pad a given string with spaces to ensure the resulting string is
	 * exactly n characters wide. This assumes the given string has at most n
	 * characters already.
	 *
	 * @param str
	 *            String to right-pad
	 * @param n
	 *            Width of resulting string
	 * @return
	 */
	public static String rightPad(String str, int n) {
	     return String.format("%1$-" + n + "s", str);
	}

	/**
	 * Left pad a given string with spaces to ensure the resulting string is
	 * exactly n characters wide. This assumes the given string has at most n
	 * characters already.  No, this is not its own library.
	 *
	 * @param str
	 *            String to left-pad
	 * @param n
	 *            Width of resulting string
	 * @return
	 */
	public static String leftPad(String str, int n) {
	     return String.format("%1$" + n + "s", str);
	}

	/**
	 * Determine the maximum width of any configured command name
	 *
	 * @param descriptors
	 * @return
	 */
	private static int determineCommandNameWidth(List<Command.Descriptor> descriptors) {
		int max = 0;
		for (Command.Descriptor d : descriptors) {
			max = Math.max(max, d.getName().length());
		}
		return max;
	}
}
