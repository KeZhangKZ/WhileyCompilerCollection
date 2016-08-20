// Copyright (c) 2014, David J. Pearce (djp@ecs.vuw.ac.nz)
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
//    * Redistributions of source code must retain the above copyright
//      notice, this list of conditions and the following disclaimer.
//    * Redistributions in binary form must reproduce the above copyright
//      notice, this list of conditions and the following disclaimer in the
//      documentation and/or other materials provided with the distribution.
//    * Neither the name of the <organization> nor the
//      names of its contributors may be used to endorse or promote products
//      derived from this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
// ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
// WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
// DISCLAIMED. IN NO EVENT SHALL DAVID J. PEARCE BE LIABLE FOR ANY
// DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
// LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
// ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

package wycc;

import java.io.File;
import java.io.PrintStream;
import java.util.*;

import wybs.lang.Build;
import wycc.lang.Command;
import wycc.lang.Feature;
import wycc.lang.Module;
import wycc.util.AbstractConfigurable;
import wycc.util.Logger;
import wycc.util.OptArg;
import wycc.util.Pair;
import wycc.util.StdModuleContext;
import wycc.util.StdModuleManager;
import wyfs.lang.Content;


public class WyTool extends AbstractConfigurable {

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
	 * The list of registered build commands in the order of registration.
	 */
	private final HashMap<String,Command> commands = new HashMap<String,Command>();
	
	/**
	 * The list of registered content types
	 */
	private final ArrayList<Content.Type> contentTypes = new ArrayList<Content.Type>();
		
	/**
	 * List of module activators 
	 */
	private final ArrayList<Module.Activator> activators;

	/**
	 * 
	 */
	private StdModuleContext context = null;
	
	/**
	 * Determines whether or not verbose output should be displayed
	 */
	private boolean verbose;
		
	// ==================================================================
	// Constructors
	// ==================================================================
	
	public WyTool(List<Module.Activator> activators) {
		super("verbose");
		this.activators = new ArrayList<Module.Activator>();
	}

	// ==================================================================
	// Configuration
	// ==================================================================

	public void setVerbose() {
		this.verbose = true;
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
	public Command getCommand(String name) {
		return commands.get(name);
	}
	
	/**
	 * Get a collection of all commands
	 * 
	 * @return
	 */
	public Collection<Command> getCommands() {
		return commands.values();
	}
	
	/**
	 * Activate the plugin system and create the plugin manager. A default
	 * extension point is created for registering functions to be called from
	 * here.
	 *
	 * @param verbose
	 * @param locations
	 * @return
	 */
	public void activate() {
		// create the context and manager
		context = new StdModuleContext();
		
		if (verbose) {
			context.setLogger(new Logger.Default(System.err));
		}

		// create extension points		
		createTemplateExtensionPoint();
		createContentTypeExtensionPoint();
		
		// start modules		
		for(Module.Activator a : activators) {
			a.start(context);
		}
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
				System.out.println("Registering command: " + command);
				commands.put(command.getName(),command);
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
