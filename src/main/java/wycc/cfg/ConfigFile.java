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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import wybs.lang.SyntacticItem;
import wybs.lang.SyntaxError;
import wybs.util.AbstractCompilationUnit;
import wybs.util.AbstractSyntacticItem;
import wyfs.lang.Content;
import wyfs.lang.Path;
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
	// Node kinds
	// =========================================================================

	public static final int DECL_mask = 0b00010000;
	public static final int DECL_section = DECL_mask + 0;
	public static final int DECL_keyvalue = DECL_mask + 1;

	// =========================================================================
	// Constructors
	// =========================================================================

	/**
	 * The list of declarations which make up this configuration.
	 */
	private Tuple<Declaration> declarations;

	public ConfigFile(Path.Entry<ConfigFile> entry) {
		super(entry);
		//
		this.declarations = new Tuple<>();
	}

	public ConfigFile(Path.Entry<ConfigFile> entry, Tuple<Declaration> declarations) {
		super(entry);
		//
		this.declarations = declarations;
		//
		allocate(declarations);
	}

	public static interface Declaration extends SyntacticItem {

	}

	public Tuple<Declaration> getDeclarations() {
		return declarations;
	}

	public void setDeclarations(Tuple<Declaration> declarations) {
		this.declarations = declarations;
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

	public static class Section extends AbstractSyntacticItem implements Declaration {

		public Section(Identifier name, Tuple<Declaration> contents) {
			super(DECL_section, name, contents);
		}

		public Identifier getName() {
			return (Identifier) get(0);
		}

		public Tuple<Declaration> getContents() {
			return (Tuple) get(1);
		}

		@Override
		public SyntacticItem clone(SyntacticItem[] operands) {
			return new Section((Identifier) operands[0], (Tuple<Declaration>) operands[1]);
		}
	}

	/**
	 * Maps a given key to a given value.
	 *
	 * @author David J. Pearce
	 *
	 */
	public static class KeyValuePair extends AbstractSyntacticItem implements Declaration {

		public KeyValuePair(Identifier key, Value value) {
			super(DECL_keyvalue, key, value);
		}

		public Identifier getKey() {
			return (Identifier) get(0);
		}

		public Value getValue() {
			return (Value) get(1);
		}

		@Override
		public SyntacticItem clone(SyntacticItem[] operands) {
			return new KeyValuePair((Identifier) operands[0], (Value) operands[1]);
		}
	}

	private KeyValuePair getKeyValuePair(ID key, Tuple<Declaration> decls) {
		for(int i=0;i!=decls.size();++i) {
			Declaration decl = decls.get(i);
			if(key.size() > 1 && decl instanceof Section) {
				Section s = (Section) decl;
				if (s.getName().toString().equals(key.get(0))) {
					return getKeyValuePair(key.subpath(1, key.size()), s.getContents());
				}
			} else if(decl instanceof KeyValuePair && key.size() == 1){
				KeyValuePair p = (KeyValuePair) decl;
				if (p.getKey().toString().equals(key.get(0))) {
					return p;
				}
			}
		}
		throw new IllegalArgumentException("invalid key access \"" + key + "\"");
	}

	private void insert(ID key, Object value, Tuple<Declaration> decls) {
		throw new UnsupportedOperationException();
		// FIXME: needs to be updated
//		for(int i=0;i!=decls.size();++i) {
//			Declaration decl = decls.get(i);
//			if(key.size() > 1 && decl instanceof Section) {
//				Section s = (Section) decl;
//				if(s.getName().equals(key.get(0))) {
//					insert(key.subpath(1, key.size()), value, s.getContents());
//				}
//			} else if(decl instanceof KeyValuePair && key.size() == 1){
//				KeyValuePair p = (KeyValuePair) decl;
//				if(p.getKey().equals(key.get(0))) {
//					p.value = value;
//					return;
//				}
//			}
//		}
//		if(key.size() == 1) {
//			declarations.add(new KeyValuePair(key.get(0),value));
//		} else {
//			throw new IllegalArgumentException("invalid key access \"" + key + "\"");
//		}
	}

	private class Wrapper implements Configuration {
		/**
		 * The schema to which this configuration file adheres.
		 */
		private final Configuration.Schema schema;

		public Wrapper(Configuration.Schema schema) {
			this.schema = schema;
			validate();
		}

		@Override
		public Schema getConfigurationSchema() {
			return schema;
		}

		@Override
		public <T> T get(Class<T> kind, ID key) {
			// Get the descriptor for this key
			Configuration.KeyValueDescriptor<?> descriptor = schema.getDescriptor(key);
			// Find the key-value pair
			KeyValuePair kvp = getKeyValuePair(key, declarations);
			// Extract the value
			Object value = kvp.getValue();
			// Sanity check the expected kind
			if (!kind.isInstance(value)) {
				throw new IllegalArgumentException("incompatible key access: expected " + kind.getSimpleName() + " got "
						+ descriptor.getType().getSimpleName());
			}
			// Convert into value
			return (T) value;
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
			insert(key, value, declarations);
		}

		@Override
		public List<ID> matchAll(Path.Filter filter) {
			ArrayList<ID> matches = new ArrayList<>();
			match(Trie.ROOT,filter,declarations,matches);
			return matches;
		}

		private void match(Trie id, Path.Filter filter, Tuple<Declaration> declarations, ArrayList<ID> matches) {
			for (int i = 0; i != declarations.size(); ++i) {
				Declaration decl = declarations.get(i);
				if (decl instanceof Section) {
					Section section = (Section) decl;
					match(id.append(section.getName().toString()), filter, section.getContents(), matches);
				} else if (decl instanceof KeyValuePair) {
					KeyValuePair kvp = (KeyValuePair) decl;
					Trie match = id.append(kvp.getKey().toString());
					if (filter.matches(match)) {
						matches.add(match);
					}
				}
			}
		}

		private void validate() {
			List<KeyValueDescriptor<?>> descriptors = schema.getDescriptors();
			// Matched holds all concrete key-value pairs which are matched. This allows us
			// to identify any which were not matched and, hence, are invalid
			Set<Path.ID> matched = new HashSet<>();
			// Validate all descriptors against given values.
			for (int i = 0; i != descriptors.size(); ++i) {
				KeyValueDescriptor descriptor = descriptors.get(i);
				// Sanity check the expected kind
				Class<?> kind = descriptor.getType();
				// Identify all matching keys
				List<Path.ID> results = matchAll(descriptor.getFilter());
				// Sanity check whether required
				if(results.size() == 0 && descriptor.isRequired()) {
					throw new SyntaxError("missing key value: " + descriptor.getFilter(), getEntry(), null);
				}
				// Check all matching keys
				for (Path.ID id : results) {
					// Find corresponding key value pair.
					KeyValuePair kvp = getKeyValuePair(id, declarations);
					Value value = kvp.getValue();
					if (!kind.isInstance(value)) {
						throw new SyntaxError(
								"invalid key value (expected " + kind.getSimpleName() + "): " + id + " = " + value,
								getEntry(), kvp);
					} else if (!descriptor.isValid(value)) {
						// Identified invalid key-value pair
						throw new SyntaxError("invalid key value: " + id + " = " + value, getEntry(), kvp);
					}
				}
				// Remember every matched attribute
				matched.addAll(results);
			}
			// Check whether any unmatched key-valid pairs exist or not
			List<Path.ID> all = matchAll(Trie.fromString("**/*"));
			for(int i=0;i!=all.size();++i) {
				Path.ID id = all.get(i);
				if(!matched.contains(id)) {
					// Found unmatched attribute
					KeyValuePair kvp = getKeyValuePair(id, declarations);
					throw new SyntaxError("invalid key: " + id, getEntry(), kvp.getKey());
				}
			}
			// Done
		}
	}
}
