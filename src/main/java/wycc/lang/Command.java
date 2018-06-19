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
import java.util.Map;

/**
 * A command which can be executed (e.g. from the command-line)
 *
 * @author David J. Pearce
 *
 */
public interface Command<T> extends Feature.Configurable {
	/**
	 * Get the name of this command. This should uniquely identify the command
	 * in question.
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
	 * Perform whatever initialisation is necessary for a given configuration.
	 *
	 * @param configuration
	 *            --- The list of configuration options passed to this command
	 */
	public void initialise(Map<String, Object> configuration) throws IOException;

	/**
	 * Perform whatever destruction is necessary whence the command is complete.
	 */
	public void finalise() throws IOException;

	/**
	 * Execute this command with the given arguments. Every invocation of this
	 * function occurs after a single call to <code>initialise()</code> and
	 * before any calls are made to <code>finalise()</code>. Observer, however,
	 * that this command may be executed multiple times.
	 */
	public T execute(String... args);
}
