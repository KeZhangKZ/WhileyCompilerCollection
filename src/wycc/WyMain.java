package wycc;

import java.io.PrintStream;
import java.util.ArrayList;

import wycc.lang.Command;
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
	
	private static final String[] ACTIVATOR_NAMES = {
			"wyc.Activator"
	};
	
	private static final ArrayList<Module.Activator> ACTIVATORS = new ArrayList<Module.Activator>();
	
	static {
		// here is where we would search for the activators
	}

	// ==================================================================
	// Main Method
	// ==================================================================

	public static void main(String[] args) {
		WyTool tool = new WyTool(ACTIVATORS);
		
		// register default commands
		registerDefaultCommands(tool);
		
		Command command = null;
		ArrayList<String> commandArgs = new ArrayList<String>();
		
		// Parse command-line options and determine the command to execute
		for(int i=0;i!=args.length;++i) {
			String arg = args[i];
			//
			if (arg.startsWith("--")) {
				Pair<String,Object> option = parseOption(arg);
				if(command == null) {
					// Configuration option for the tool
					tool.set(option.first(), option.second());
				} else {
					// Configuration option for the command
					command.set(option.first(), option.second());
				}
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
		} else {
			// Yes, execute the given command
			args = commandArgs.toArray(new String[commandArgs.size()]);
			command.execute(args);
		}
	}
	
	// ==================================================================
	// Helpers
	// ==================================================================

	/**
	 * Register the set of default commands that are included automatically
	 * 
	 * @param tool
	 */
	private static void registerDefaultCommands(WyTool tool) {
		tool.getContext().register(wycc.lang.Command.class,new Build());		
	}
	
	/**
	 * Print usage information to the console.
	 */
	private static void usage(WyTool tool) {
		System.out.println("usage: wy [--verbose] command [<options>] [<args>]");
		for(Command c : tool.getCommands()) {
			System.out.println(c.getName());
			for(String option : c.getOptions()) {
				System.out.println("--" + c + " " + c.describe(option));
			}
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
		return new Pair<String,Object>(split[0],data);
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
}
