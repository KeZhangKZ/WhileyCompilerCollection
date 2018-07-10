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

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import wybs.lang.SyntaxError;
import wycc.cfg.ConfigFile;
import wycc.commands.Build;
import wycc.commands.Help;
import wycc.lang.Command;
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
 * Provides a command-line interface to the Whiley Compiler Collection. This is
 * responsible for various tasks, such as loading various configuration files
 * from disk, activating plugins, parsing command-line arguments and actually
 * activating the tool itself.
 *
 * @author David J. Pearce
 *
 */
public class WyMain {

	// ==================================================================
	// Main Method
	// ==================================================================

	public static void main(String[] args) throws IOException {
		String whileyhome = System.getenv("WHILEYHOME");
		if (whileyhome == null) {
			System.err.println("error: WHILEYHOME environment variable not set");
			System.exit(-1);
		}
		// Construct instance of the wy tool
		WyTool tool = new WyTool();
		// Register default commands (e.g. help, clean, build, etc)
		registerDefaultCommands(tool, tool.getRegistry());
		// Read the global configuration file
		ConfigFile global = readConfigFile("config", new DirectoryRoot(whileyhome, tool.getRegistry()));
		// Active plugins to ensure that all platforms and content types are registered.
		//activateDefaultPlugins(tool, global);
		// Determine project root
		Path.Root root = determineProjectRoot(tool.getRegistry());
		// Read the local configuration file
		ConfigFile local = readConfigFile("wy", root);
		// Construct the command environment
		Command.Environment environment = constructCommandEnvironment(tool, global,local);
		// Process command-line options
		Command.Template pipeline = new CommandParser(tool, tool.getRegistry()).parse(args);
		// Execute the command (if applicable)
		if (pipeline == null) {
			// Not applicable, print usage information via the help sub-system.
			tool.getCommand("help").execute(Collections.EMPTY_LIST);
		} else {
			// FIXME: obviously broken because can have multiple levels of commands.
			Command command = pipeline.getCommand();
			System.out.println("COMMAND: " + command.getClass().getName());
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

	/**
	 * Determine where the root of this project is. This is the nearest enclosing
	 * directory containing a "wy.toml" file. The point is that we may be operating
	 * in some subdirectory of the project and want the tool to automatically search
	 * out the real root for us.
	 *
	 * @return
	 * @throws IOException
	 */
	private static Path.Root determineProjectRoot(Content.Registry registry) throws IOException {
		// Determine current working directory
		File dir = new File(System.getProperty("user.dir"));
		// Traverse up the directory hierarchy
		while (dir.exists() && dir.isDirectory()) {
			File wyf = new File(dir + File.separator + "wy.toml");
			if (wyf.exists()) {
				return new DirectoryRoot(dir, registry);
			}
			// Traverse back up the directory hierarchy looking for a suitable directory.
			dir = dir.getParentFile();
		}
		// If we get here then it means we didn't find a root, therefore just use
		// current directory.
		return new DirectoryRoot(".", registry);
	}

	/**
	 * Attempt to read a configuration file from a given root.
	 *
	 * @param name
	 * @param root
	 * @return
	 * @throws IOException
	 */
	private static ConfigFile readConfigFile(String name, Path.Root root) throws IOException {
		Path.Entry<ConfigFile> global = root.get(Trie.fromString(name), ConfigFile.ContentType);
		if (global == null) {
			System.err.println("Unable to read configuration file " + name + ".toml");
			System.exit(-1);
		} else {
			try {
				return global.read();
			} catch (SyntaxError e) {
				e.outputSourceError(System.err, false);
				System.exit(-1);
			}
		}
		return null;
	}

	/**
	 * Register the set of default commands that are included automatically
	 *
	 * @param tool
	 */
	private static void registerDefaultCommands(WyTool tool, Content.Registry registry) {
		// The list of default commands available in the tool
		Command[] defaultCommands = { new Help(System.out, tool.getCommands()), new Build(registry) };
		// Register the default commands available in the tool
		Module.Context context = tool.getContext();
		for (Command c : defaultCommands) {
			context.register(wycc.lang.Command.class, c);
		}
	}

	/**
	 * Activate the default set of plugins which the tool uses. Currently this list
	 * is statically determined, but eventually it will be possible to dynamically
	 * add plugins to the system.
	 *
	 * @param verbose
	 * @param locations
	 * @return
	 */
	private static void activateDefaultPlugins(WyTool tool, ConfigFile global) {
		Map<String, Object> plugins = (Map<String, Object>) global.toMap().get("plugins");
		if (plugins != null) {
			Module.Context context = tool.getContext();
			// create the context and manager

			// start modules
			for (String name : plugins.keySet()) {
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

	private static Command.Environment constructCommandEnvironment(WyTool tool, ConfigFile global, ConfigFile local) {

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
