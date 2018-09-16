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

import wybs.lang.Build;
import wybs.util.AbstractCompilationUnit.Value;
import wycc.WyProject;
import wycc.cfg.Configuration;
import wycc.cfg.Configuration.Schema;
import wycc.lang.Command;
import wyfs.lang.Path;
import wyfs.util.Trie;

public class Run implements Command {
	public static final Trie BUILD_MAIN = Trie.fromString("build/main");

	/**
	 * The descriptor for this command.
	 */
	public static final Command.Descriptor DESCRIPTOR = new Command.Descriptor() {
		@Override
		public String getName() {
			return "run";
		}

		@Override
		public String getDescription() {
			return "Execute method in package";
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
			return new Run((WyProject) environment, configuration, System.out, System.err);
		}

	};

	/**
	 * Provides a generic place to which normal output should be directed. This
	 * should eventually be replaced.
	 */
	private final PrintStream sysout;

	/**
	 * Provides a generic place to which error output should be directed. This
	 * should eventually be replaced.
	 */
	private final PrintStream syserr;

	/**
	 * The enclosing project for this build
	 */
	private final WyProject project;

	/**
	 * Access to configuration attributes
	 */
	private final Configuration configuration;

	private final Value.UTF8 method;

	public Run(WyProject project, Configuration configuration, OutputStream sysout,
			OutputStream syserr) {
		this.project = project;
		this.sysout = new PrintStream(sysout);
		this.syserr = new PrintStream(syserr);
		this.configuration = configuration;
		if(configuration.hasKey(BUILD_MAIN)) {
			this.method = configuration.get(Value.UTF8.class, BUILD_MAIN);
		} else {
			this.method = null;
		}
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

	}

	@Override
	public boolean execute(Template template) {
		if(method == null) {
			sysout.println("Must specific method signature via build/main attribute or command-line option");
			return false;
		} else {
			Path.ID target = Trie.fromString(method.toString().replace("::", "/"));
			System.out.println("TARGET: " + target);
			// FIXME: should have command-line option for build platform
			Build.Platform platform = getBuildPlatform("whiley");
			// Execute the given function
			platform.execute(project.getBuildProject(), target.parent(), target.last());
			// Done
			return true;
		}
	}

	private Build.Platform getBuildPlatform(String name) {
		for (Build.Platform platform : project.getTargetPlatforms()) {
			if (platform.getName().equals(name)) {
				return platform;
			}
		}
		throw new IllegalArgumentException("unknown build platform: " + name);
	}

}
