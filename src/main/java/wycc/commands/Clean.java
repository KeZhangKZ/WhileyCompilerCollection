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

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import wybs.lang.Build;
import wycc.WyProject;
import wycc.cfg.Configuration;
import wycc.cfg.Configuration.Schema;
import wycc.lang.Command;
import wycc.lang.Command.Descriptor;
import wycc.lang.Command.Option;
import wycc.util.Logger;
import wyfs.lang.Content;
import wyfs.lang.Path;

public class Clean implements Command {
	/**
	 * The descriptor for this command.
	 */
	public static final Command.Descriptor DESCRIPTOR = new Command.Descriptor() {
		@Override
		public String getName() {
			return "clean";
		}

		@Override
		public String getDescription() {
			return "Remove all target (i.e. binary) files";
		}

		@Override
		public List<Option.Descriptor> getOptionDescriptors() {
			return Collections.EMPTY_LIST;
		}

		@Override
		public Schema getConfigurationSchema() {
			return Configuration.EMPTY_SCHEMA;
		}

		@Override
		public List<Descriptor> getCommands() {
			return Collections.EMPTY_LIST;
		}

		@Override
		public Command initialise(Command environment, Configuration configuration) {
			return new Clean((WyProject) environment);
		}

	};

	/**
	 * The enclosing project for this build
	 */
	private final WyProject project;

	/**
	 * Logger
	 */
	private Logger logger;

	public Clean(WyProject project) {
		this.project = project;
		this.logger = project.getBuildProject().getLogger();
	}

	@Override
	public Descriptor getDescriptor() {
		return DESCRIPTOR;
	}

	@Override
	public void initialise() {
	}

	@Override
	public void finalise() {
		// Nothing to do here either
	}

	@Override
	public boolean execute(Template template) {
		try {
			// Identify the project root
			Path.Root root = project.getParent().getLocalRoot();
			// Extract all registered platforms
			List<Build.Platform> platforms = project.getTargetPlatforms();
			//
			for (int i = 0; i != platforms.size(); ++i) {
				Build.Platform platform = platforms.get(i);
				Path.Root binRoot = platform.getTargetRoot(root);
				Content.Filter<?> binFilter = platform.getTargetFilter();
				// Remove all files being cleaned
				int count = binRoot.remove(binFilter);
				logger.logTimedMessage("cleaned  " + binRoot.toString() + " ... removed " + count + " file(s)", 0, 0);
			}
			//
			return true;
		} catch (Exception e) {
			// FIXME: do something here??
			e.printStackTrace();
			return false;
		}
	}

}
