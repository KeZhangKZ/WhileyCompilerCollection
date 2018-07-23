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
import wyfs.lang.Path.ID;

public class Help implements Command {
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
		public Command initialise(Environment environment) {
			System.out.println("Help.initialise() called");
			// TODO Auto-generated method stub
			return null;
		}
	};
	//
	private final PrintStream out;
	private final List<Command> commands;

	public Help(PrintStream out, List<Command> commands) {
		this.commands = commands;
		this.out = out;
	}

	@Override
	public Descriptor getDescriptor() {
		return DESCRIPTOR;
	}

	@Override
	public void initialise(Configuration configuration) {
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
			Command command = null;
			for (Command c : commands) {
				if (c.getDescriptor().getName().equals(args.get(0))) {
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

	protected void printCommandDetails(Command command) {
		Command.Descriptor d = command.getDescriptor();
		out.println("NAME");
		out.println("\t" + d.getName());
		out.println();
		out.println("DESCRIPTION");
		out.println("\t" + d.getDescription());
		out.println();
		out.println("OPTIONS");
		Configuration.Schema schema = d.getConfigurationSchema();
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
		int maxWidth = determineCommandNameWidth(commands);
		out.println("Commands:");
		for (Command c : commands) {
			Command.Descriptor d = c.getDescriptor();
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
	 * characters already.
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
	 * @param commands
	 * @return
	 */
	private static int determineCommandNameWidth(List<Command> commands) {
		int max = 0;
		for (Command c : commands) {
			max = Math.max(max, c.getDescriptor().getName().length());
		}
		return max;
	}
}
