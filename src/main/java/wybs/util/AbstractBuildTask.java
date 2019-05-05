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
import java.util.List;
import java.util.concurrent.RecursiveTask;

import wybs.lang.Build;
import wybs.lang.Build.Project;
import wyfs.lang.Path;
import wyfs.lang.Path.Entry;

/**
 * An abstract build task which extends <code>RecursiveTask</code>.
 *
 * @author David J. Pearce
 *
 * @param <S>
 * @param <T>
 */
public abstract class AbstractBuildTask<S, T> implements Build.Task {
	/**
	 * The master project for identifying all resources available to the
	 * builder. This includes all modules declared in the project being compiled
	 * and/or defined in external resources (e.g. jar files).
	 */
	protected final Build.Project project;

	/**
	 * Target WyilFile being built by this compile task.
	 */
	protected final Path.Entry<T> target;

	/**
	 * List of source files used to build the target.
	 */
	protected final List<Path.Entry<S>> sources;

	public AbstractBuildTask(Build.Project project, Path.Entry<T> target, Collection<Path.Entry<S>> sources) {
		this.project = project;
		this.target = target;
		this.sources = new ArrayList<>(sources);
	}

	@Override
	public Project project() {
		return project;
	}

	@Override
	public List<Entry<?>> getSources() {
		// FIXME: does this cast make sense?
		return (List) sources;
	}

	@Override
	public Entry<?> getTarget() {
		return target;
	}

}
