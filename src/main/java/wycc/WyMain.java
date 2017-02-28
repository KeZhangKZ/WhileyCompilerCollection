package wycc;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import wycc.commands.Build;
import wycc.lang.Command;
import wycc.lang.Feature.ConfigurationError;
import wycc.lang.Module;
import wycc.util.Pair;

/**
 * Provides a command-line interface to the Whiley Compiler Collection. This
 * supports loading and configuring modules, as well as compiling files.
 *
 * @author David J. Pearce
 *
 */
public class WyMain {

	/**
	 * The static list of default plugin activators which this tool uses. Whilst
	 * this list is currently statically fixed, eventually we'll be able to
	 * register plugins dynamically.
	 */
	private static final String[] ACTIVATOR_NAMES = {
			"wyc.Activator",
			"wyjc.Activator"
	};

	// ==================================================================
	// Main Method
	// ==================================================================

	public static void main(String[] args) {
		WyTool tool = new WyTool();

		// register default commands and activate default plugins
		registerDefaultCommands(tool);
		activateDefaultPlugins(tool);

		// process command-line options
		Command command = null;
		ArrayList<String> commandArgs = new ArrayList<>();

		// Parse command-line options and determine the command to execute
		boolean success = true;
		//
		for(int i=0;i!=args.length;++i) {
			String arg = args[i];
			//
			if (arg.startsWith("--")) {
				Pair<String,Object> option = parseOption(arg);
				success &= applyOption(option,tool,command);
			} else if(command == null) {
				command = tool.getCommand(arg);
			} else {
				commandArgs.add(arg);
			}
		}

		// Execute the command (if applicable)
		if (command == null) {
			// Not applicable, print usage information
			usage(tool);
		} else if(!success) {
			// There was some problem during configuration
			System.exit(1);
		} else {
			// Yes, execute the given command
			args = commandArgs.toArray(new String[commandArgs.size()]);
			command.execute(args);
			System.exit(0);
		}
	}

	// ==================================================================
	// Helpers
	// ==================================================================

	private static boolean applyOption(Pair<String,Object> option, WyTool tool, Command command) {
		try {
			if(command == null) {
				// Configuration option for the tool
				tool.set(option.first(), option.second());
			} else {
				// Configuration option for the command
				command.set(option.first(), option.second());
			}
			return true;
		} catch (ConfigurationError e) {
			System.out.print("ERROR: ");
			System.out.println(e.getMessage());
			return false;
		}
	}

	/**
	 * Register the set of default commands that are included automatically
	 *
	 * @param tool
	 */
	private static void registerDefaultCommands(WyTool tool) {
		// The list of default commands available in the tool
		Command[] defaultCommands = {
				//new Build()
		};
		// Register the default commands available in the tool
		Module.Context context = tool.getContext();
		for(Command c : defaultCommands) {
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
	private static void activateDefaultPlugins(WyTool tool) {
		Module.Context context = tool.getContext();
		// create the context and manager

		// start modules
		for(String name : ACTIVATOR_NAMES) {
			try {
				Class<?> c = Class.forName(name);
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
	}


	/**
	 * Print usage information to the console.
	 */
	private static void usage(WyTool tool) {
		System.out.println("usage: wy [--verbose] command [<options>] [<args>]");
		int maxWidth = determineCommandNameWidth(tool.getCommands());
		for(Command c : tool.getCommands()) {
			System.out.print(rightPad(c.getName(),maxWidth));
			System.out.println("\t" + c.getDescription());
			for(String option : c.getOptions()) {
				System.out.print(leftPad("[--" + option +"]",maxWidth));
				System.out.println("\t" + c.describe(option));
			}
			System.out.println();
		}
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
