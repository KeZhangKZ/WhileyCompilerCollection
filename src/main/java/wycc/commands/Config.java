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

import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import wycc.cfg.Configuration;
import wycc.cfg.Configuration.Schema;
import wycc.lang.Command;
import wyfs.lang.Path;
import wyfs.util.Trie;

/**
 * Provides interface for manipulating either the system, global or local
 * configurations. For example, listing the current registry and/or setting
 * various keys.
 *
 * @author David J. Pearce
 *
 */
public class Config implements Command {
	// ================================================================================
	// config
	// ================================================================================

	public static final Configuration.Schema SCHEMA = Configuration.fromArray();
	/**
	 * The descriptor for this command.
	 */
	public static final Command.Descriptor DESCRIPTOR = new Command.Descriptor() {
		@Override
		public String getName() {
			return "config";
		}

		@Override
		public String getDescription() {
			return "Get or set configuration options";
		}

		@Override
		public List<Option.Descriptor> getOptionDescriptors() {
			return Collections.EMPTY_LIST;
		}

		@Override
		public Configuration.Schema getConfigurationSchema() {
			return SCHEMA;
		}

		@Override
		public List<Descriptor> getCommands() {
			return Arrays.asList(new Descriptor[] {
					LIST_DESCRIPTOR
			});
		}

		@Override
		public Command initialise(Command.Environment environment, Command.Options options,
				Configuration configuration) {
			return new Config(System.out, configuration);
		}
	};
	//
	private final PrintStream out;
	private final Configuration configuration;

	public Config(PrintStream out, Configuration configuration) {
		this.out = out;
		this.configuration = configuration;
	}

	@Override
	public Descriptor getDescriptor() {
		return DESCRIPTOR;
	}

	@Override
	public boolean execute(List<String> args) {
		Help.print(System.out,DESCRIPTOR);
		return false;
	}

	// ================================================================================
	// config list
	// ================================================================================

	/**
	 * Descriptor for the list sub-command.
	 */
	public static final Command.Descriptor LIST_DESCRIPTOR = new Command.Descriptor() {

		@Override
		public String getName() {
			return "list";
		}

		@Override
		public String getDescription() {
			return "list current configuration";
		}

		@Override
		public List<Option.Descriptor> getOptionDescriptors() {
			return Collections.EMPTY_LIST;
		}

		@Override
		public Schema getConfigurationSchema() {
			return Configuration.fromArray();
		}

		@Override
		public List<Descriptor> getCommands() {
			return Collections.EMPTY_LIST;
		}

		@Override
		public Command initialise(Environment environment, Command.Options options, Configuration configuration) {
			// TODO Auto-generated method stub
			return null;
		}

	};

	//
	public static class ListCmd implements Command {
		private final PrintStream out;
		private final Configuration configuration;

		public ListCmd(PrintStream out, Configuration configuration) {
			this.out = out;
			this.configuration = configuration;
		}

		@Override
		public Descriptor getDescriptor() {
			return LIST_DESCRIPTOR;
		}

		@Override
		public boolean execute(List<String> args) {
			for(Path.ID key : configuration.matchAll(Trie.fromString("**"))) {
				out.print(key);
				out.print("=");
				out.println(configuration.get(Object.class, key));
			}
			return false;
		}
	}
}
