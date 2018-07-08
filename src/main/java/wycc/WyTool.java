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
import wycc.lang.Command;
import wycc.lang.ConfigFile;
import wycc.lang.Command.Option.Instance;
import wycc.lang.Feature;
import wycc.lang.Module;
import wycc.util.Logger;
import wycc.util.StdModuleContext;
import wyfs.lang.Content;
import wyfs.lang.Path;
import wyfs.lang.Content.Type;
import wyfs.lang.Path.Entry;
import wyfs.util.DirectoryRoot;
import wyfs.util.Trie;

public class WyTool implements Command {
	public static final Path.ID BUILD_FILE_NAME = Trie.fromString("wy");
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
	private final ArrayList<Command> commands;

	/**
	 * The list of registered content types
	 */
	private final ArrayList<Content.Type<?>> contentTypes;

	/**
	 *
	 */
	private StdModuleContext context = null;

	/**
	 * The root of the project in question. From this, all relative paths are
	 * determined.
	 */
	protected Path.Root projectRoot;

	/**
	 * The master registry which provides knowledge of all file types used within
	 * the system.
	 */
	protected Content.Registry registry = new Content.Registry() {

		@Override
		public String suffix(Type<?> t) {
			return t.getSuffix();
		}

		@Override
		public void associate(Entry<?> e) {
			for (Content.Type<?> ct : contentTypes) {
				if (ct.getSuffix().equals(e.suffix())) {
					e.associate((Content.Type) ct, null);
					return;
				}
			}
			e.associate((Content.Type) Content.BinaryFile, null);
		}
	};

	// ==================================================================
	// Constructors
	// ==================================================================

	public WyTool() {
		this.commands = new ArrayList<>();
		this.contentTypes = new ArrayList<>();
		this.context = new StdModuleContext();
		// Add default content types
		this.contentTypes.add(ConfigFile.ContentType);
		// create extension points
		createTemplateExtensionPoint();
		createContentTypeExtensionPoint();
	}

	// ==================================================================
	// Command stuff
	// ==================================================================

	@Override
	public Descriptor getDescriptor() {
		// FIXME: unsure what the descriptor for this should do!
		return null;
	}

	@Override
	public List<Command> getCommands() {
		return commands;
	}

	@Override
	public void initialise(List<Instance> options) throws IOException {
		locateProjectRoot();
		// Load project build file
		loadBuildFile();
		// Configure project
		// Find dependencies
	}

	@Override
	public void finalise() throws IOException {
		// Flush any roots
	}

	@Override
	public boolean execute(List<String> args) {
		return getCommand("help").execute(args);
	}

	// ==================================================================
	// Other
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
	public Command getCommand(String name) {
		for (int i = 0; i != commands.size(); ++i) {
			Command c = commands.get(i);
			if (c.getDescriptor().getName().equals(name)) {
				return c;
			}
		}
		return null;
	}

	/**
	 * Get the content registry associated with this tool instance.
	 *
	 * @return
	 */
	public Content.Registry getRegistry() {
		// TODO: fixme
		return null;
	}

	// ==================================================================
	// Helpers
	// ==================================================================


	/**
	 * Attempt to determine where the project root is. That might be the current
	 * directory. But, if not, then we traverse up the directory tree looking
	 * for a build file (e.g. wy.toml).
	 * @throws IOException
	 */
	protected void locateProjectRoot() throws IOException {
		// FIXME: should recurse up the directory tree
		this.projectRoot = new DirectoryRoot(".", registry);
	}

	/**
	 * Load the project file (e.g. wy.toml) which describes this project. This is
	 * necessary to extract key information about the project (e.g. what
	 * dependencies are required).
	 *
	 * @throws IOException
	 */
	protected void loadBuildFile() throws IOException {
		// FIXME: having loaded the file, what do we do next?
		Path.Entry<ConfigFile> buildFileEntry = projectRoot.get(BUILD_FILE_NAME, ConfigFile.ContentType);
		System.out.println("GOT BUILD FILE: " + buildFileEntry);
		if(buildFileEntry != null) {
			ConfigFile config = buildFileEntry.read();
			for(ConfigFile.Declaration d : config.getDeclarations()) {
				if(d instanceof ConfigFile.Section) {
					ConfigFile.Section  s = (ConfigFile.Section) d;
					System.out.println("SECTION: " + s.getName());
				}
			}
		}
	}


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
