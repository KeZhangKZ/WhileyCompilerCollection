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
package wycc.util;

import java.util.ArrayList;
import java.util.List;

import wycc.lang.Command;
import wycc.lang.Command.Option;
import wycc.lang.Command.Option.Instance;
import wycc.lang.Command.Template;
import wyfs.lang.Content;

/**
 * <p>
 * A generic mechanism for parsing command-line options, which is perhaps
 * reminiscent of optarg, etc. The key here is the structure of command-line
 * arguments:
 * </p>
 *
 * <pre>
 * wy <tool / project options> (command <options> <values>)*
 * </pre>
 *
 * <p>
 * Each level corresponds to a deeper command within the hierarchy. Furthermore,
 * each also corresponds to entries in a configuration file as well.
 * </p>
 *
 * @author David J. Pearce
 *
 */
public class CommandParser {
	/**
	 * The list of command roots.
	 */
	private final Command root;

	private final Content.Registry registry;

	public CommandParser(Command root, Content.Registry registry) {
		this.root = root;
		this.registry = registry;
	}

	/**
	 * Parse a given set of command-line arguments to produce an appropriate command
	 * template.
	 *
	 * @param args
	 */
	protected Command.Template parse(String[] args) {
		return parse(root,args,0);
	}

	/**
	 * Parse a given set of command-line arguments starting from a given index
	 * position to produce an appropriate command template.
	 *
	 * @param args
	 * @param index
	 */
	protected Command.Template parse(Command root, String[] args, int index) {
		ArrayList<Command.Option.Instance> options = new ArrayList<>();
		ArrayList<String> arguments = new ArrayList<>();
		//
		Command.Template sub = null;
		while (index < args.length) {
			String arg = args[index];
			if (isLongOption(arg)) {
				options.add(parseLongOption(root, args[index]));
			} else if (isCommand(arg, root.getSubcommands())) {
				Command cmd = getCommand(arg, root.getSubcommands());
				sub = parse(cmd, args, index + 1);
				break;
			} else {
				arguments.add(arg);
			}
			index = index + 1;
		}
		//
		return new ConcreteTemplate(root, options,arguments,sub);
	}

	protected boolean isLongOption(String arg) {
		return arg.startsWith("--");
	}

	public Option.Instance parseLongOption(Command cmd, String arg) {
		throw new IllegalArgumentException("to do");
	}

	protected boolean isCommand(String arg, List<Command> commands) {
		for (int i = 0; i != commands.size(); ++i) {
			Command cmd = commands.get(i);
			if (arg.equals(cmd.getDescriptor().getName())) {
				return true;
			}
		}
		return false;
	}

	protected Command getCommand(String arg, List<Command> commands) {
		for (int i = 0; i != commands.size(); ++i) {
			Command cmd = commands.get(i);
			if (arg.equals(cmd.getDescriptor().getName())) {
				return cmd;
			}
		}
		throw new IllegalArgumentException("invalid command: " + arg);
	}

	protected static class ConcreteTemplate implements Command.Template {
		private final Command cmd;
		private final List<Option.Instance> options;
		private final List<String> arguments;
		private final Command.Template sub;

		public ConcreteTemplate(Command cmd, List<Option.Instance> options, List<String> arguments, Command.Template sub) {
			this.cmd = cmd;
			this.options = options;
			this.arguments = arguments;
			this.sub = sub;
		}

		@Override
		public Command getCommand() {
			return cmd;
		}

		@Override
		public List<Instance> getOptions() {
			return options;
		}

		@Override
		public List<String> getArguments() {
			return arguments;
		}

		@Override
		public Template getChild() {
			return sub;
		}

	}
}
