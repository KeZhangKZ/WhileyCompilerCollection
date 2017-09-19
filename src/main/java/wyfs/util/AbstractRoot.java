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
package wyfs.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import wyfs.lang.Content;
import wyfs.lang.Path;
import wyfs.lang.Path.Entry;
import wyfs.lang.Path.Folder;
import wyfs.lang.Path.ID;
import wyfs.lang.Path.Root;

/**
 * Provides a simple implementation of <code>Path.Root</code>. This maintains a
 * cache all entries contained in the root.
 *
 * @author David J. Pearce
 *
 */
public abstract class AbstractRoot<T extends Folder> implements Root {
	protected final Content.Registry contentTypes;
	protected final T root;

	public AbstractRoot(Content.Registry contentTypes) {
		this.contentTypes = contentTypes;
		this.root = root();
	}

	@Override
	public boolean contains(Path.Entry<?> e) throws IOException {
		return root.contains(e);
	}

	@Override
	public boolean exists(ID id, Content.Type<?> ct) throws IOException{
		return root.exists(id,ct);
	}

	@Override
	public <T> Path.Entry<T> get(ID id, Content.Type<T> ct) throws IOException{
		Path.Entry<T> e = root.get(id,ct);
		return e;
	}

	@Override
	public <T> List<Entry<T>> get(Content.Filter<T> filter) throws IOException{
		ArrayList<Entry<T>> entries = new ArrayList<>();
		root.getAll(filter, entries);
		return entries;
	}

	@Override
	public <T> Set<Path.ID> match(Content.Filter<T> filter) throws IOException{
		HashSet<Path.ID> ids = new HashSet<>();
		root.getAll(filter, ids);
		return ids;
	}

	@Override
	public <T> Path.Entry<T> create(Path.ID id, Content.Type<T> ct) throws IOException {
		return root.create(id,ct);
	}

	@Override
	public void refresh() throws IOException{
		root.refresh();
	}

	@Override
	public void flush() throws IOException{
		root.flush();
	}

	/**
	 * Get the root folder for this abstract root. Note that this should be
	 * loaded from scratch, and not cached in any way. This ensures that
	 * invoking AbstractRoot.refresh() does indeed refresh entries.
	 *
	 * @return
	 */
	protected abstract T root();
}