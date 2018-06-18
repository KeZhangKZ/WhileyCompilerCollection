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
import java.util.List;
import java.util.Map;

import wycc.WyTool;
import wycc.lang.Command;

public class Help implements Command<String> {
	private final PrintStream out;
	private final List<Command> commands;

	public Help(PrintStream out, List<Command> commands) {
		this.commands = commands;
		this.out = out;
	}

	@Override
	public String[] getOptions() {
		return new String[]{};
	}

	@Override
	public String describe(String name) {
		throw new UnsupportedOperationException("");
	}

	@Override
	public void set(String name, Object value) throws ConfigurationError {
		throw new UnsupportedOperationException("");
	}

	@Override
	public Object get(String name) {
		throw new UnsupportedOperationException("");
	}

	@Override
	public String getName() {
		return "help";
	}

	@Override
	public String getDescription() {
		return "Display help information";
	}

	@Override
	public void initialise(Map<String, Object> configuration) {
		// Nothing to do here
	}

	@Override
	public void finalise() {
		// Nothing to do here
	}

	@Override
	public String execute(String... args) {
		if(args.length == 0) {
			printUsage();
		} else {
			// Search for the command
			Command command = null;
			for(Command c : commands) {
				if(c.getName().equals(args[0])) {
					command = c;
					break;
				}
			}
			//
			if(command == null) {
				System.out.println("No entry for " + args[0]);
			} else {
				printCommandDetails(command);
			}
		}
		//
		return null;
	}

	protected void printCommandDetails(Command command) {
		System.out.println("NAME");
		System.out.println("\t" + command.getName());
		System.out.println();
		System.out.println("DESCRIPTION");
		System.out.println("\t" + command.getDescription());
		System.out.println();
		System.out.println("OPTIONS");
		String[] options = command.getOptions();
		for(int i=0;i!=options.length;++i) {
			String option = options[i];
			System.out.println("\t--" + option);
			System.out.println("\t\t" + command.describe(option));
		}
	}

	/**
	 * Print usage information to the console.
	 */
	protected void printUsage() {
		System.out.println("usage: wy [--verbose] command [<options>] [<args>]");
		System.out.println();
		int maxWidth = determineCommandNameWidth(commands);
		System.out.println("Commands:");
		for(Command c : commands) {
			System.out.print("  ");
			System.out.print(rightPad(c.getName(),maxWidth));
			System.out.println("   " + c.getDescription());
		}
		System.out.println();
		System.out.println("Run `wy help COMMAND` for more information on a command");
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
		for(Command c : commands) {
			max = Math.max(max, c.getName().length());
		}
		return max;
	}
}
