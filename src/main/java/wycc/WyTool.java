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
import java.io.PrintStream;
import java.util.*;

import wybs.lang.Build;
import wycc.lang.Command;
import wycc.lang.Feature;
import wycc.lang.Module;
import wycc.util.Logger;
import wycc.util.OptArg;
import wycc.util.Pair;
import wycc.util.StdModuleContext;
import wycc.util.StdModuleManager;
import wyfs.lang.Content;


public class WyTool {

	/**
	 * The major version for this module application
	 */
	public static final int MAJOR_VERSION;

	/**
	 * The minor version for this module application
	 */
	public static final int MINOR_VERSION;

	/**
	 * The minor revision for this module application
	 */
	public static final int MINOR_REVISION;

	/**
	 * Extract version information from the enclosing jar file.
	 */
	static {
		// determine version numbering from the MANIFEST attributes
		String versionStr = WyTool.class.getPackage().getImplementationVersion();
		if (versionStr != null) {
			String[] bits = versionStr.split("-");
			String[] pts = bits[0].split("\\.");
			MAJOR_VERSION = Integer.parseInt(pts[0]);
			MINOR_VERSION = Integer.parseInt(pts[1]);
			MINOR_REVISION = Integer.parseInt(pts[2]);
		} else {
			System.err.println("WARNING: version numbering unavailable");
			MAJOR_VERSION = 0;
			MINOR_VERSION = 0;
			MINOR_REVISION = 0;
		}
	}

	// ==================================================================
	// Instance Fields
	// ==================================================================

	/**
	 * The list of commands registered with this tool.
	 */
	private final ArrayList<Command<?>> commands;

	/**
	 * The list of registered content types
	 */
	private final ArrayList<Content.Type<?>> contentTypes;

	/**
	 *
	 */
	private StdModuleContext context = null;

	// ==================================================================
	// Constructors
	// ==================================================================

	public WyTool() {
		this.commands = new ArrayList<>();
		this.contentTypes = new ArrayList<>();
		this.context = new StdModuleContext();
		// create extension points
		createTemplateExtensionPoint();
		createContentTypeExtensionPoint();
	}

	// ==================================================================
	// Configuration
	// ==================================================================

	public void setVerbose() {
		context.setLogger(new Logger.Default(System.err));
	}

	public void set(String option, Object value) throws Feature.ConfigurationError {
		switch(option) {
		case "verbose":
			setVerbose();
			break;
		default:
			throw new Feature.ConfigurationError("unknown option encountered");
		}
	}

	// ==================================================================
	// Methods
	// ==================================================================

	/**
	 * Get the module context associated with this tool instance
	 *
	 * @return
	 */
	public Module.Context getContext() {
		return context;
	}

	/**
	 * Get a particular command.
	 *
	 * @param name
	 * @return
	 */
	public Command<?> getCommand(String name) {
		for (int i = 0; i != commands.size(); ++i) {
			Command<?> c = commands.get(i);
			if (c.getDescriptor().getName().equals(name)) {
				return c;
			}
		}
		return null;
	}

	/**
	 * Get a collection of all commands
	 *
	 * @return
	 */
	public List<Command<?>> getCommands() {
		return commands;
	}

	// ==================================================================
	// Helpers
	// ==================================================================

	/**
	 * Create the Build.Template extension point. This is where plugins register
	 * their primary functionality for constructing a specific build project.
	 *
	 * @param context
	 * @param templates
	 */
	private void createTemplateExtensionPoint() {
		context.create(Command.class, new Module.ExtensionPoint<Command>() {
			@Override
			public void register(Command command) {
				commands.add(command);
			}
		});
	}

	/**
	 * Create the Content.Type extension point.
	 *
	 * @param context
	 * @param templates
	 */
	private void createContentTypeExtensionPoint() {
		context.create(Content.Type.class, new Module.ExtensionPoint<Content.Type>() {
			@Override
			public void register(Content.Type contentType) {
				contentTypes.add(contentType);
			}
		});
	}
}
