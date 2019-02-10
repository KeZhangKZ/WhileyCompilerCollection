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

import java.io.*;
import java.util.*;
import java.util.zip.*;

import wyfs.lang.Content;
import wyfs.lang.Content.Filter;
import wyfs.lang.Content.Type;
import wyfs.lang.Path;
import wyfs.lang.Path.Entry;
import wyfs.lang.Path.ID;
import wyfs.lang.Path.RelativeRoot;
import wyfs.lang.Path.Root;

/**
 * Provides an implementation of <code>Path.Root</code> for representing the
 * contents of a zip file.
 *
 * @author David J. Pearce
 *
 */
public final class ZipFileRoot extends AbstractRoot<ZipFileRoot.Folder> implements Path.Root {
	private final Path.Entry<ZipFile> entry;
	private Path.Item[] contents;

	public ZipFileRoot(Path.Entry<ZipFile> entry, Content.Registry contentTypes) throws IOException {
		super(contentTypes);
		this.entry = entry;
		refresh();
	}

	@Override
	public <T> Path.Entry<T> create(Path.ID id, Content.Type<T> ct) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void flush() {
		// no-op, since zip files are read-only.
	}

	@Override
	public void refresh() throws IOException {
		// Reread the contents of the zip file
		ZipFile file = entry.read();
		// Create new array of contents
		this.contents = new Path.Item[file.size()];
		// Extract all items from the ZipFile
		for (int i = 0; i != file.size(); ++i) {
			ZipFile.Entry e = file.get(i);
			String filename = e.getName();
			int lastSlash = filename.lastIndexOf('/');
			Trie pkg = lastSlash == -1 ? Trie.ROOT : Trie.fromString(filename.substring(0, lastSlash));
			if (!e.isDirectory()) {
				int lastDot = filename.lastIndexOf('.');
				String name = lastDot >= 0 ? filename.substring(lastSlash + 1, lastDot) : filename;
				// String suffix = lastDot >= 0 ? filename.substring(lastDot + 1) : null;
				Trie id = pkg.append(name);
				Entry<?> pe = new Entry<>(id, e);
				contentTypes.associate(pe);
				contents[i] = pe;
			} else {
				// folder
				contents[i] = new Folder(pkg);
			}
		}
	}

	@Override
	protected Folder root() {
		return new Folder(Trie.ROOT);
	}

	@Override
	public String toString() {
		return entry.location();
	}

	/**
	 * Represents a directory on a physical file system.
	 *
	 * @author David J. Pearce
	 *
	 */
	public final class Folder extends AbstractFolder {
		public Folder(Path.ID id) {
			super(id);
		}

		@Override
		protected Path.Item[] contents() throws IOException {
			// This algorithm is straightforward. I use a two loops instead of a
			// single loop with ArrayList to avoid allocating on the heap.
			int count = 0 ;
			for(int i=0;i!=contents.length;++i) {
				Path.Item item = contents[i];
				if(item.id().parent() == id) {
					count++;
				}
			}

			Path.Item[] myContents = new Path.Item[count];
			count=0;
			for(int i=0;i!=contents.length;++i) {
				Path.Item item = contents[i];
				if(item.id().parent() == id) {
					myContents[count++] = item;
				}
			}

			return myContents;
		}

		@Override
		public <T> wyfs.lang.Path.Entry<T> create(Path.ID id, Content.Type<T> ct) {
			throw new UnsupportedOperationException();
		}
	}

	private static final class Entry<T> extends AbstractEntry<T> implements Path.Entry<T> {
		private final ZipFile.Entry entry;

		public Entry(Trie mid, ZipFile.Entry entry) {
			super(mid);
			this.entry = entry;
		}

		@Override
		public String location() {
			return entry.getName();
		}

		@Override
		public long lastModified() {
			return entry.getTime();
		}

		@Override
		public boolean isModified() {
			// cannot modify something in a Jar file.
			return false;
		}

		@Override
		public void touch() {
			throw new UnsupportedOperationException();
		}

		@Override
		public String suffix() {
			String suffix = "";
			String filename = entry.getName();
			int pos = filename.lastIndexOf('.');
			if (pos > 0) {
				suffix = filename.substring(pos + 1);
			}
			return suffix;
		}

		@Override
		public InputStream inputStream() throws IOException {
			return entry.getInputStream();
		}

		@Override
		public OutputStream outputStream() throws IOException {
			throw new UnsupportedOperationException();
		}

		@Override
		public void write(T contents) {
			throw new UnsupportedOperationException();
		}
	}
}
