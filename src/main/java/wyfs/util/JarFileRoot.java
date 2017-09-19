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
import java.util.jar.*;

import wyfs.lang.Content;
import wyfs.lang.Path;
import wyfs.lang.Content.Type;

/**
 * Provides an implementation of <code>Path.Root</code> for representing the
 * contents of a jar file.
 *
 * @author David J. Pearce
 *
 */
public final class JarFileRoot extends AbstractRoot<JarFileRoot.Folder> implements Path.Root {
	private final File dir;
	private Path.Item[] jfContents;

	public JarFileRoot(String dir, Content.Registry contentTypes) throws IOException {
		super(contentTypes);
		this.dir = new File(dir);
		refresh();
	}

	public JarFileRoot(File dir, Content.Registry contentTypes) throws IOException {
		super(contentTypes);
		this.dir = dir;
		refresh();
	}

	@Override
	public <T> Path.Entry<T> create(Path.ID id, Content.Type<T> ct) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void flush() {
		// no-op, since jar files are read-only.
	}

	@Override
	public void refresh() throws IOException {
		JarFile jf = new JarFile(dir);
		Enumeration<JarEntry> entries = jf.entries();
		this.jfContents = new Path.Item[jf.size()];
		int i = 0;
		while (entries.hasMoreElements()) {
			JarEntry e = entries.nextElement();
			String filename = e.getName();
			int lastSlash = filename.lastIndexOf('/');
			Trie pkg = lastSlash == -1 ? Trie.ROOT : Trie.fromString(filename.substring(0, lastSlash));
			if(!e.isDirectory()) {
				int lastDot = filename.lastIndexOf('.');
				String name = lastDot >= 0 ? filename.substring(lastSlash + 1, lastDot) : filename;
				String suffix = lastDot >= 0 ? filename.substring(lastDot + 1) : null;
				Trie id = pkg.append(name);
				Entry pe = new Entry(id, jf, e);
				contentTypes.associate(pe);
				jfContents[i++] = pe;
			} else {
				// folder
				jfContents[i++] = new Folder(pkg);
			}
		}
	}

	@Override
	protected Folder root() {
		return new Folder(Trie.ROOT);
	}

	@Override
	public String toString() {
		return dir.getPath();
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
			for(int i=0;i!=jfContents.length;++i) {
				Path.Item item = jfContents[i];
				if(item.id().parent() == id) {
					count++;
				}
			}

			Path.Item[] myContents = new Path.Item[count];
			count=0;
			for(int i=0;i!=jfContents.length;++i) {
				Path.Item item = jfContents[i];
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
		private final JarFile parent;
		private final JarEntry entry;

		public Entry(Trie mid, JarFile parent, JarEntry entry) {
			super(mid);
			this.parent = parent;
			this.entry = entry;
		}

		@Override
		public String location() {
			return parent.getName();
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
			return parent.getInputStream(entry);
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
