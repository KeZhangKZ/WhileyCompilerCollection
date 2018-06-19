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

import java.util.Map;

import wycc.util.AbstractProjectCommand;
import wycc.util.Logger;
import wyfs.lang.Content.Registry;

/**
 * Responsible for removing all intermediate (e.g. binary) files generating
 * during the build process, excluding those generated for any dependencies.
 *
 * @author David J. Pearce
 *
 */
public class Clean extends AbstractProjectCommand<Clean.Result> {
	/**
	 * Result kind for this command
	 *
	 */
	public enum Result {
		SUCCESS,
		ERRORS,
		INTERNAL_FAILURE
	}

	public Clean(Registry registry, Logger logger) {
		super(registry, logger);
	}

	@Override
	public String getName() {
		return "clean";
	}

	@Override
	public String getDescription() {
		return "Responsible for removing all intermediate (e.g. binary) files generating\n" +
				"during the build process, excluding those generated for any dependencies.";
	}

	@Override
	public Result execute(String... args) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected void finaliseConfiguration(Map<String, Object> configuration) {

	}

}
