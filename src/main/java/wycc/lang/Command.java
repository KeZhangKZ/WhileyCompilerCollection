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
package wycc.lang;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import wycc.cfg.Configuration;
import wyfs.lang.Content;

/**
 * A command which can be executed (e.g. from the command-line)
 *
 * @author David J. Pearce
 *
 */
public interface Command {

	/**
	 * Get a descriptor for this command.
	 *
	 * @return
	 */
	public Descriptor getDescriptor();

	/**
	 * Perform whatever initialisation is necessary for a given configuration.
	 *
	 * @param configuration
	 *            The overall system configuration.
	 */
	public void initialise(Configuration configuration) throws IOException;

	/**
	 * Perform whatever destruction is necessary whence the command is complete.
	 */
	public void finalise() throws IOException;

	/**
	 * Execute this command with the given arguments. Every invocation of this
	 * function occurs after a single call to <code>initialise()</code> and before
	 * any calls are made to <code>finalise()</code>. Observer, however, that this
	 * command may be executed multiple times.
	 */
	public boolean execute(List<String> args);

	/**
	 * The environment provides access to the various bits of useful information.
	 *
	 * @author David J. Pearce
	 *
	 */
	public interface Environment {
		/**
		 * Get the content registry used in the enclosing environment.
		 *
		 * @return
		 */
		public Content.Registry getContentRegistry();

		/**
		 * Get the list of content types used in the enclosing environment.
		 *
		 * @return
		 */
		public List<Content.Type<?>> getContentTypes();
	}

	/**
	 * Provides a descriptive information about this command. This includes
	 * information such as the name of the command, a description of the command as
	 * well as the set of arguments which are accepted.
	 *
	 * @author David J. Pearce
	 *
	 */
	public interface Descriptor extends Feature {
		/**
		 * Get the name of this command. This should uniquely identify the command in
		 * question.
		 *
		 * @return
		 */
		public String getName();

		/**
		 * Get a description of this command.
		 *
		 * @return
		 */
		public String getDescription();

		/**
		 * Get the list of configurable options for this command.
		 *
		 * @return
		 */
		public Configuration.Schema getConfigurationSchema();

		/**
		 * Get descriptors for any sub-commands of this command.
		 *
		 * @return
		 */
		public List<Descriptor> getCommands();

		/**
		 * Initialise the corresponding command in a given environment.
		 *
		 * @param environment
		 *            Enclosing environment for this tool. This provides access to the
		 *            various important details cleaned from the configuration, such as
		 *            the set of available build platforms and content types.
		 * @return
		 */
		public Command initialise(Environment environment);
	}

	public interface Template {
		/**
		 * Get the command being described by this template.
		 *
		 * @return
		 */
		public Command.Descriptor getCommandDescriptor();

		/**
		 * Get the arguments described by this template, in the order in which they
		 * should be applied.
		 *
		 * @return
		 */
		public List<String> getArguments();

		/**
		 * Get the child template (if any) given for this template. If no template, then
		 * this returns <code>null</code>.
		 *
		 * @return
		 */
		public Template getChild();
	}
}
