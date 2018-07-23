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
import java.util.*;

import wycc.cfg.Configuration;
import wycc.cfg.Configuration.Schema;
import wycc.lang.Command;
import wyfs.lang.Content;
import wyfs.lang.Path;

public class WyTool implements Command {
	/**
	 * Options specifically required by the tool.
	 */
	public static Configuration.Schema SCHEMA = Configuration.fromArray();

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
	 * The master registry which provides knowledge of all file types used within
	 * the system.
	 */
	protected final Content.Registry registry;

	/**
	 * The system root identifies the location of all files and configuration data
	 * that are global to all users.
	 */
	protected Path.Root systemRoot;
	/**
	 * The global root identifies the location of all user-specific but project
	 * non-specific files and other configuration data. For example, this is where
	 * the cache of installed packages lives.
	 */
	protected Path.Root globalRoot;
	/**
	 * The root of the project itself. From this, all relative paths within the
	 * project are determined. For example, the location of source files or the the
	 * build configuration file, etc.
	 */
	protected Path.Root localRoot;

	// ==================================================================
	// Constructors
	// ==================================================================

	public WyTool(Content.Registry registry) {
		this.registry = registry;
	}

	// ==================================================================
	// Command stuff
	// ==================================================================

	@Override
	public Command.Descriptor getDescriptor() {
		// FIXME: this is broken because it doesn't include sub-descriptors.
		return getDescriptor(registry,Collections.EMPTY_LIST);
	}

	@Override
	public void initialise() throws IOException {
		// Activate all plugins
		// Configure project
		// Find dependencies
	}

	@Override
	public void finalise() throws IOException {
		// Flush any roots
		// Deactivate plugins
		// Write back configuration files?
	}

	@Override
	public boolean execute(List<String> args) {
		//
		return false;
	}

	// ==================================================================
	// Other
	// ==================================================================

	/**
	 * Get the content registry associated with this tool instance.
	 *
	 * @return
	 */
	public Content.Registry getRegistry() {
		return registry;
	}

	// ==================================================================
	// Helpers
	// ==================================================================


	public static final Command.Descriptor getDescriptor(Content.Registry registry, List<Command.Descriptor> descriptors) {
		return new Descriptor(registry,descriptors);
	}

	private static class Descriptor implements Command.Descriptor {
		private final Content.Registry registry;
		private final List<Command.Descriptor> descriptors;

		public Descriptor(Content.Registry registry, List<Command.Descriptor> descriptors) {
			this.registry = registry;
			this.descriptors = descriptors;
		}

		@Override
		public Schema getConfigurationSchema() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Command initialise(Configuration configuration) {
			return new WyTool(registry);
		}

		@Override
		public String getName() {
			return "wy";
		}

		@Override
		public String getDescription() {
			return "Command-line interface for the Whiley Compiler Collection";
		}

		@Override
		public List<Command.Descriptor> getCommands() {
			return descriptors;
		}
	};

}
