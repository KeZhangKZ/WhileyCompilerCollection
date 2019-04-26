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

import java.io.IOException;

import wybs.lang.Build;
import wyfs.lang.Path;

public class SequentialBuildExecutor extends AbstractBuildExecutor {

	@Override
	public boolean build() throws IOException {
		boolean changed = true;
		// Iterate until all tasks have completed.
		// FIXME: this is not a good way of doing things
		while(changed) {
			changed = false;
			for(int i=0;i!=tasks.size();++i) {
				// FIXME: doesn't make sense
				Build.Task task = tasks.get(i);
				if(ready(task) && !task.apply()) {
					return false;
				}
			}
		}
		//
		return true;
	}

	/**
	 * Check whether a given task is ready to be built. This is currently determined
	 * by having at least one source whose timestamp is in front of the target.
	 *
	 * @param task
	 * @return
	 */
	private boolean ready(Build.Task task) {
		// FIXME: this is not a great solution in the long run for several reasons.
		// Firstly, its possible that a given source will be rebuilt in the near future
		// as a result of some other task and we should be waiting for that.
		Path.Entry<?> target = task.getTarget();
		for(Path.Entry<?> s : task.getSources()) {
			if(s.lastModified() > target.lastModified()) {
				return true;
			}
		}
		return false;
	}
}
