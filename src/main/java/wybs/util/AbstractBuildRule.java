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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import wybs.lang.Build;
import wycc.util.Pair;
import wyfs.lang.Content;
import wyfs.lang.Path;

/**
 * <p>
 * Provides a straightforward, yet flexible build rule implementation. This
 * build rule supports both include and exclude filters. It is expected that
 * this rule is sufficiently flexible for the majority of situations
 * encountered.
 * </p>
 * <p>
 * <b>NOTE</b>: instances of this class are immutable, although objects they
 * reference may not be (e.g. builders).
 * </p>
 *
 * @author David J. Pearce
 *
 */
public abstract class AbstractBuildRule<S,T> implements Build.Rule {
	/**
	 * The source root containing all files which might be built using this
	 * rule. However, whether or not files contained in this root will actually
	 * be built depends on the includes and excludes filters.
	 */
	protected final Path.Root source;

	/**
	 * A content filter used to determine which files contained in the source
	 * root should be built by this rule.  Maybe null.
	 */
	protected final Content.Filter<S> includes;

	/**
	 * A content filter used to determine which files contained in the source
	 * root should be not built by this rule.  Maybe null.
	 */
	protected final Content.Filter<S> excludes;

	/**
	 * Construct a standard build rule.
	 *
	 * @param builder
	 *            The build task used to build files using this rule.
	 * @param srcRoot
	 *            The source root containing all files which might be built
	 *            using this rule. However, whether or not files contained in
	 *            this root will actually be built depends on the includes and
	 *            excludes filters.
	 * @param includes
	 *            A content filter used to determine which files contained in
	 *            the source root should be built by this rule. Maybe null.
	 * @param excludes
	 *            A content filter used to determine which files contained in
	 *            the source root should be not built by this rule. Maybe null.
	 * @param targetRoot
	 *            The destination root into which all files built using this
	 *            rule are placed.
	 */
	public AbstractBuildRule(Path.Root srcRoot, Content.Filter<S> includes, Content.Filter<S> excludes) {
		this.source = srcRoot;
		this.includes = includes;
		this.excludes = excludes;
	}

	@Override
	public void apply(Build.Executor executor) throws IOException {
		//
		ArrayList<Path.Entry<S>> matches = new ArrayList<>();
		// Determine the set of matching files
		for (Path.Entry<S> e : source.get(includes)) {
			if (excludes != null && excludes.matches(e.id(), e.contentType())) {
				continue;
			}
			matches.add(e);
		}
		// process matches according to concrete strategy
		apply(executor,matches);
	}

	/**
	 * Process a given set of matches according to this rule. For example, we may
	 * map each source to a corresponding binary target; or, we may map several (or
	 * all) of the matches to a given binary target.
	 *
	 * @param executor
	 * @param matches
	 */
	protected abstract void apply(Build.Executor executor, List<Path.Entry<S>> matches) throws IOException;
}