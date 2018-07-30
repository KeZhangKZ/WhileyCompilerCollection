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
import java.util.function.Predicate;

import wycc.cfg.Configuration;
import wycc.lang.Command.Option;
import wyfs.lang.Content;
import wyfs.lang.Path;

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
	 * Execute this command with the given arguments. Every invocation of this
	 * function occurs after a single call to <code>initialise()</code> and before
	 * any calls are made to <code>finalise()</code>. Observer, however, that this
	 * command may be executed multiple times.
	 */
	public boolean execute(List<String> args);

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
		public List<Option.Descriptor> getOptionDescriptors();

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
		 *            Provides access to the various runtime features provided by the
		 *            environment.
		 * @param options
		 *            List of option modifiers for this command.
		 * @param configuration
		 *            Provides access to the various important details gleaned from the
		 *            configuration, such as the set of available build platforms and
		 *            content types.
		 * @return
		 */
		public Command initialise(Environment environment, Options options, Configuration configuration);
	}

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

		/**
		 * Get the list of available command descriptors.
		 *
		 * @return
		 */
		public List<Command.Descriptor> getCommandDescriptors();


		/**
		 * The system root identifies the location of all files and configuration data
		 * that are global to all users.
		 */
		public Path.Root getSystemRoot();

		/**
		 * The global root identifies the location of all user-specific but project
		 * non-specific files and other configuration data. For example, this is where
		 * the cache of installed packages lives.
		 */
		public Path.Root getGlobalRoot();

		/**
		 * The root of the project itself. From this, all relative paths within the
		 * project are determined. For example, the location of source files or the the
		 * build configuration file, etc.
		 */
		public Path.Root getLocalRoot();

	}

	/**
	 * A generic interface for access command options.
	 *
	 * @author David J. Pearce
	 *
	 */
	public interface Options {
		/**
		 * Get the value associate with a given named option.
		 *
		 * @param kind
		 * @return
		 */
		public <T> T get(String name, Class<T> kind);
	}

	/**
	 * Describes a configurable option for a given command.
	 *
	 * @author David J. Pearce
	 *
	 */
	public interface Option {

		/**
		 * Get the descriptor from which this instance was created.
		 *
		 * @return
		 */
		public Option.Descriptor getDescriptor();

		/**
		 * Get the value associate with this option.
		 *
		 * @param kind
		 * @return
		 */
		public <T> T get(Class<T> kind);

		/**
		 * Provides a descriptor for the option.
		 *
		 * @author David J. Pearce
		 *
		 */
		public interface Descriptor {
			/**
			 * Get the option name.
			 *
			 * @return
			 */
			public String getName();

			/**
			 * Get the description for the argument
			 * @return
			 */
			public String getArgumentDescription();

			/**
			 * Get a suitable description for the option.
			 *
			 * @return
			 */
			public String getDescription();

			/**
			 * Get the default value for this option (or null if no suitable default).
			 *
			 * @return
			 */
			public Object getDefaultValue();

			/**
			 * Construct a given option from a given argument string.
			 *
			 * @param arg
			 * @return
			 */
			public Option Initialise(String arg);
		}
	}

	public interface Template {
		/**
		 * Get the command being described by this template.
		 *
		 * @return
		 */
		public Command.Descriptor getCommandDescriptor();

		/**
		 * Get the options described by this template, in the order in which they should
		 * be applied.
		 *
		 * @return
		 */
		public Command.Options getOptions();

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

	/**
	 * A generic class for handling option descriptors.
	 *
	 * @author David J. Pearce
	 *
	 */
	public static abstract class AbstractOptionDescriptor implements Option.Descriptor {
		private final String name;
		private final String argDescription;
		private final String description;
		private final Object defaultValue;

		AbstractOptionDescriptor(String name, String argDescription, String description, Object defaultValue) {
			this.name = name;
			this.argDescription = argDescription;
			this.description = description;
			this.defaultValue = defaultValue;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public String getArgumentDescription() {
			return argDescription;
		}

		@Override
		public String getDescription() {
			return description;
		}

		@Override
		public Object getDefaultValue() {
			return defaultValue;
		}
	}

	public static class OptionValue implements Option {
		private final Option.Descriptor descriptor;
		private final Object contents;

		public OptionValue(Option.Descriptor descriptor, Object contents) {
			this.descriptor = descriptor;
			this.contents = contents;
		}

		@Override
		public Descriptor getDescriptor() {
			return descriptor;
		}

		@Override
		public <T> T get(Class<T> kind) {
			if(kind.isInstance(contents)) {
				return (T) contents;
			} else {
				throw new IllegalArgumentException(
						"expected option value " + kind.getSimpleName() + ", got " + contents);
			}
		}

		@Override
		public String toString() {
			return descriptor.getName() + "=" + contents;
		}
	}

	/**
	 * An integer option which cannot be negative.
	 *
	 * @param name
	 * @param argument
	 * @param description

	 * @return
	 */
	public static Option.Descriptor OPTION_NONNEGATIVE_INTEGER(String name, String description) {
		return OPTION_INTEGER(name, "<n>", description + " (non-negative)", (n) -> (n >= 0), null);
	}

	/**
	 * An integer option which cannot be negative.
	 *
	 * @param name
	 * @param argument
	 * @param description
	 * @param defaultValue
	 *            the default value to use
	 * @return
	 */
	public static Option.Descriptor OPTION_NONNEGATIVE_INTEGER(String name, String description, int defaultValue) {
		return OPTION_INTEGER(name, "<n>", description + " (non-negative, default " + defaultValue + ")",
				(n) -> (n >= 0), defaultValue);
	}


	/**
	 * An integer option which must be positive.
	 *
	 * @param name
	 * @param argument
	 * @param description
	 * @param defaultValue
	 *            the default value to use
	 * @return
	 */
	public static Option.Descriptor OPTION_POSITIVE_INTEGER(String name, String description, int defaultValue) {
		return OPTION_INTEGER(name, "<n>", description + " (positive, default " + defaultValue + ")", (n) -> (n > 0), defaultValue);
	}

	/**
	 * An integer option with a constraint
	 *
	 * @param name
	 * @param description
	 * @return
	 */
	public static Option.Descriptor OPTION_INTEGER(String name, String argument, String description,
			Predicate<Integer> constraint, Integer defaultValue) {
		return new AbstractOptionDescriptor(name, argument, description, defaultValue) {
			@Override
			public Option Initialise(String arg) {
				int value = Integer.parseInt(arg);
				if (constraint.test(value)) {
					return new OptionValue(this, value);
				} else {
					throw new IllegalArgumentException("invalid integer value");
				}
			}
		};
	}
}
