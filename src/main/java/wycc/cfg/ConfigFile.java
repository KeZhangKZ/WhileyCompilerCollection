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
package wycc.cfg;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import wybs.lang.SyntacticElement;
import wybs.util.AbstractCompilationUnit;
import wycc.cfg.Configuration.KeyValueDescriptor;
import wyfs.lang.Content;
import wyfs.lang.Path;
import wyfs.lang.Path.Filter;
import wyfs.lang.Path.ID;
import wyfs.util.Trie;

public class ConfigFile extends AbstractCompilationUnit<ConfigFile> {
	// =========================================================================
	// Content Type
	// =========================================================================

	public static final Content.Type<ConfigFile> ContentType = new Content.Type<ConfigFile>() {
		public Path.Entry<ConfigFile> accept(Path.Entry<?> e) {
			if (e.contentType() == this) {
				return (Path.Entry<ConfigFile>) e;
			}
			return null;
		}

		@Override
		public ConfigFile read(Path.Entry<ConfigFile> e, InputStream inputstream) throws IOException {
			ConfigFileLexer lexer = new ConfigFileLexer(e);
			ConfigFileParser parser = new ConfigFileParser(e, lexer.scan());
			return parser.read();
		}

		@Override
		public void write(OutputStream output, ConfigFile value) {
			// for now
			throw new UnsupportedOperationException();
		}

		@Override
		public String toString() {
			return "Content-Type: wyml";
		}

		@Override
		public String getSuffix() {
			return "toml";
		}
	};

	// =========================================================================
	// Constructors
	// =========================================================================

	/**
	 * The list of declarations which make up this configuration.
	 */
	private ArrayList<Declaration> declarations;

	public ConfigFile(Path.Entry<ConfigFile> entry) {
		super(entry);
		//
		this.declarations = new ArrayList<>();
	}

	public static interface Declaration {

	}

	public List<Declaration> getDeclarations() {
		return declarations;
	}

	/**
	 * Turn the configuration file into a series of nested maps.
	 *
	 * @return
	 */
	public Map<String, Object> toMap() {
		HashMap<String, Object> map = new HashMap<>();
		toMap(declarations, map);
		return map;
	}

	/**
	 * Construct a configuration wrapper for this file. This ensures that the
	 * contents of the file meets a give configuration schema.
	 *
	 * @param schema
	 * @return
	 */
	public Configuration toConfiguration(Configuration.Schema schema) {
		return new Wrapper(schema);
	}

	private static void toMap(List<Declaration> declarations, Map<String, Object> map) {
		for (Declaration d : declarations) {
			if (d instanceof KeyValuePair) {
				KeyValuePair kvp = (KeyValuePair) d;
				map.put(kvp.getKey(), kvp.getValue());
			} else {
				Section s = (Section) d;
				HashMap<String, Object> submap = new HashMap<>();
				toMap(s.contents, submap);
				map.put(s.getName(), submap);
			}
		}
	}

	public static class Section extends SyntacticElement.Impl implements Declaration {
		private final String name;
		private final ArrayList<Declaration> contents;

		public Section(String name) {
			this.name = name;
			this.contents = new ArrayList<>();
		}

		public String getName() {
			return name;
		}

		public List<Declaration> getContents() {
			return contents;
		}
	}

	/**
	 * Maps a given key to a given value.
	 *
	 * @author David J. Pearce
	 *
	 */
	public static class KeyValuePair extends SyntacticElement.Impl implements Declaration {
		private final String key;
		private Object value;

		public KeyValuePair(String key, Object value) {
			this.key = key;
			this.value = value;
		}

		public String getKey() {
			return key;
		}

		public Object getValue() {
			return value;
		}
	}

	private KeyValuePair getKeyValuePair(ID key, List<Declaration> decls) {
		for(int i=0;i!=decls.size();++i) {
			Declaration decl = decls.get(i);
			if(key.size() > 1 && decl instanceof Section) {
				Section s = (Section) decl;
				if(s.getName().equals(key.get(0))) {
					return getKeyValuePair(key.subpath(1, key.size()),s.getContents());
				}
			} else if(decl instanceof KeyValuePair && key.size() == 1){
				KeyValuePair p = (KeyValuePair) decl;
				if(p.getKey().equals(key.get(0))) {
					return p;
				}
			}
		}
		throw new IllegalArgumentException("invalid key access \"" + key + "\"");
	}

	private void insert(ID key, Object value, List<Declaration> decls) {
		for(int i=0;i!=decls.size();++i) {
			Declaration decl = decls.get(i);
			if(key.size() > 1 && decl instanceof Section) {
				Section s = (Section) decl;
				if(s.getName().equals(key.get(0))) {
					insert(key.subpath(1, key.size()), value, s.getContents());
				}
			} else if(decl instanceof KeyValuePair && key.size() == 1){
				KeyValuePair p = (KeyValuePair) decl;
				if(p.getKey().equals(key.get(0))) {
					p.value = value;
					return;
				}
			}
		}
		if(key.size() == 1) {
			declarations.add(new KeyValuePair(key.get(0),value));
		} else {
			throw new IllegalArgumentException("invalid key access \"" + key + "\"");
		}
	}

	private class Wrapper implements Configuration {
		/**
		 * The schema to which this configuration file adheres.
		 */
		private final Configuration.Schema schema;

		public Wrapper(Configuration.Schema schema) {
			this.schema = schema;
			// FIXME: should validate schema right here
		}

		@Override
		public Schema getConfigurationSchema() {
			return schema;
		}

		@Override
		public <T> T get(Class<T> kind, ID key) {
			// Get the descriptor for this key
			Configuration.KeyValueDescriptor<?> descriptor = schema.getDescriptor(key);
			// Sanity check the expected kind
			if (kind != descriptor.getType()) {
				throw new IllegalArgumentException("incompatible key access: expected " + kind.getSimpleName() + " got "
						+ descriptor.getType().getSimpleName());
			}
			// Find the key-value pair
			KeyValuePair kvp = getKeyValuePair(key, declarations);
			// Convert into value
			return (T) kvp.getValue();
		}

		@Override
		public <T> void write(ID key, T value) {
			// Get the descriptor for this key
			Configuration.KeyValueDescriptor descriptor = schema.getDescriptor(key);
			// Sanity check the expected kind
			Class<?> kind = descriptor.getType();
			//
			if (!kind.isInstance(value)) {
				throw new IllegalArgumentException("incompatible key access: expected " + kind.getSimpleName() + " got "
						+ descriptor.getType().getSimpleName());
			} else if(!descriptor.isValid(value)) {
				throw new IllegalArgumentException("incompatible key access: value does not match expected invariant");
			}
			// Update the relevant key-value pair
			insert(key,value,declarations);
		}

		@Override
		public List<ID> matchAll(Path.Filter filter) {
			ArrayList<ID> matches = new ArrayList<>();
			match(Trie.ROOT,filter,declarations,matches);
			return matches;
		}

		private void match(Trie id, Path.Filter filter, List<Declaration> declarations, ArrayList<ID> matches) {
			for (int i = 0; i != declarations.size(); ++i) {
				Declaration decl = declarations.get(i);
				if (decl instanceof Section) {
					Section section = (Section) decl;
					match(id.append(section.getName()), filter, section.getContents(), matches);
				} else if (decl instanceof KeyValuePair) {
					KeyValuePair kvp = (KeyValuePair) decl;
					Trie match = id.append(kvp.getKey());
					if (filter.matches(match)) {
						matches.add(match);
					}
				}
			}
		}
	}
}
