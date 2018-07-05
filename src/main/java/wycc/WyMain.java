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
package wycc;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import wybs.lang.SyntaxError;
import wycc.commands.Build;
import wycc.commands.Clean;
import wycc.commands.Help;
import wycc.lang.Command;
import wycc.lang.ConfigFile;
import wycc.lang.Feature.ConfigurationError;
import wycc.lang.Module;
import wycc.util.CommandParser;
import wycc.util.Logger;
import wycc.util.Pair;
import wyfs.lang.Content;
import wyfs.lang.Path;
import wyfs.util.DirectoryRoot;
import wyfs.util.Trie;

/**
 * Provides a command-line interface to the Whiley Compiler Collection. This
 * supports loading and configuring modules, as well as compiling files.
 *
 * @author David J. Pearce
 *
 */
public class WyMain {
	/**
	 * Default implementation of a content registry. This associates whiley and
	 * wyil files with their respective content types.
	 *
	 * @author David J. Pearce
	 *
	 */
	public static class Registry implements Content.Registry {
		@Override
		public void associate(Path.Entry e) {
			String suffix = e.suffix();

			if (suffix.equals("wyml")) {
				e.associate(ConfigFile.ContentType, null);
			} else if (suffix.equals("toml")) {
				e.associate(ConfigFile.ContentType, null);
			}
		}

		@Override
		public String suffix(Content.Type<?> t) {
			return t.getSuffix();
		}
	}

	// ==================================================================
	// Main Method
	// ==================================================================

	public static void main(String[] args) throws IOException {
		String whileyhome = System.getenv("WHILEYHOME");
		if(whileyhome == null) {
			System.err.println("error: WHILEYHOME environment variable not set");
			System.exit(-1);
		}
		WyTool tool = constructWyTool(whileyhome);
		// Process command-line options
		Command.Template pipeline = new CommandParser(tool,tool.getRegistry()).parse(args);
		// Execute the command (if applicable)
		if (pipeline == null) {
			// Not applicable, print usage information via the help sub-system.
			tool.getCommand("help").execute(Collections.EMPTY_LIST);
		} else {
			Command command = pipeline.getCommand();
			// Initialise the command
			command.initialise(pipeline.getOptions());
			// Execute command with given arguments
			command.execute(pipeline.getArguments());
			// Tear down command
			command.finalise();
			// Done
			System.exit(0);
		}
	}

	// ==================================================================
	// Helpers
	// ==================================================================

	private static WyTool constructWyTool(String whileyhome) throws IOException {
		WyTool tool = new WyTool();
		Registry registry = new Registry();
		// Register default commands
		registerDefaultCommands(tool,registry);
		// Attempt to read global configuration
		DirectoryRoot globalConfigDir = new DirectoryRoot(whileyhome, registry);
		ConfigFile global = readConfigFile("config", globalConfigDir);
		if (global == null) {
			System.err.println("Unable to read global configuration file");
		} else {
			activateDefaultPlugins(tool, global);
		}
		return tool;
	}

	private static ConfigFile readConfigFile(String name, Path.Root root) throws IOException {
		Path.Entry<ConfigFile> global = root.get(Trie.fromString(name), ConfigFile.ContentType);
		if (global != null) {
			try {
				return global.read();
			} catch (SyntaxError e) {
				e.outputSourceError(System.err, false);
				System.exit(-1);
			}
		}
		//
		return null;
	}

	/**
	 * Register the set of default commands that are included automatically
	 *
	 * @param tool
	 */
	private static void registerDefaultCommands(WyTool tool, Content.Registry registry) {
		// The list of default commands available in the tool
		Command<?>[] defaultCommands = {
				new Help(System.out,tool.getCommands()),
				new Build(registry),
				new Clean(registry,Logger.NULL)
		};
		// Register the default commands available in the tool
		Module.Context context = tool.getContext();
		for(Command<?> c : defaultCommands) {
			context.register(wycc.lang.Command.class,c);
		}
	}

	/**
	 * Activate the default set of plugins which the tool uses. Currently this
	 * list is statically determined, but eventually it will be possible to
	 * dynamically add plugins to the system.
	 *
	 * @param verbose
	 * @param locations
	 * @return
	 */
	private static void activateDefaultPlugins(WyTool tool, ConfigFile global) {
		Map<String,Object> plugins = (Map<String,Object>) global.toMap().get("plugins");
		if(plugins != null) {
			Module.Context context = tool.getContext();
			// create the context and manager

			// start modules
			for(String name : plugins.keySet()) {
				String activator = (String) plugins.get(name);
				try {
					Class<?> c = Class.forName(activator);
					Module.Activator instance = (Module.Activator) c.newInstance();
					instance.start(context);
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				} catch (InstantiationException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
			}
		} else {
			System.out.println("No plugin configuration found");
			System.exit(-1);
		}
	}

	/**
	 * Parse an option which is either a string of the form "--name" or
	 * "--name=data". Here, name is an arbitrary string and data is a string
	 * representing a data value.
	 *
	 * @param arg
	 *            The option argument to be parsed.
	 * @return
	 */
	private static Pair<String,Object> parseOption(String arg) {
		arg = arg.substring(2);
		String[] split = arg.split("=");
		Object data = null;
		if(split.length > 1) {
			data = parseData(split[1]);
		}
		return new Pair<>(split[0],data);
	}

	/**
	 * Parse a given string representing a data value into an instance of Data.
	 *
	 * @param str
	 *            The string to be parsed.
	 * @return
	 */
	private static Object parseData(String str) {
		if (str.equals("true")) {
			return true;
		} else if (str.equals("false")) {
			return false;
		} else if (Character.isDigit(str.charAt(0))) {
			// number
			return Integer.parseInt(str);
		} else {
			return str;
		}
	}

	/**
	 * Print a complete stack trace. This differs from
	 * Throwable.printStackTrace() in that it always prints all of the trace.
	 *
	 * @param out
	 * @param err
	 */
	private static void printStackTrace(PrintStream out, Throwable err) {
		out.println(err.getClass().getName() + ": " + err.getMessage());
		for(StackTraceElement ste : err.getStackTrace()) {
			out.println("\tat " + ste.toString());
		}
		if(err.getCause() != null) {
			out.print("Caused by: ");
			printStackTrace(out,err.getCause());
		}
	}
}
