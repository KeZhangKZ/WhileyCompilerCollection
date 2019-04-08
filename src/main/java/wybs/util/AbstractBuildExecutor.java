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
package wybs.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import wybs.lang.Build;
import wybs.lang.Build.Task;
import wycc.util.Logger;
import wyfs.lang.Path;
import wyfs.lang.Path.Entry;

/**
 * Provides an abstract implementation of the Build.Graph interface which
 * concrete implementations can then build from.
 *
 * @author David J. Pearce
 *
 */
public abstract class AbstractBuildExecutor implements Build.Executor {
	/**
	 * The derived from relation maps child entries to the parents they are
	 * derived from.
	 */
	protected ArrayList<Build.Task> tasks = new ArrayList<>();
	/**
	 * Generic logger
	 */
	protected Logger logger = Logger.NULL;

	public AbstractBuildExecutor setLogger(Logger logger) {
		this.logger = logger;
		return this;
	}

	@Override
	public Build.Task getTask(Path.Entry<?> target) {
		for(int i=0;i!=tasks.size();++i) {
			Build.Task task = tasks.get(i);
			if(task.getTarget() == target) {
				return task;
			}
		}
		return null;
	}

	@Override
	public List<Path.Entry<?>> getTargets() {
		ArrayList<Path.Entry<?>> targets = new ArrayList<>();
		for(int i=0;i!=tasks.size();++i) {
			targets.add(tasks.get(i).getTarget());
		}
		return targets;
	}

	@Override
	public void submit(Build.Task task) {
		// FIXME: should replace existing tasks for same target?
		tasks.add(task);
	}

}
