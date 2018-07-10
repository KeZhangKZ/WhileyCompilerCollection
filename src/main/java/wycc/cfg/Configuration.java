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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import wyfs.lang.Path;
import wyfs.lang.Path.ID;

/**
 * A configuration provides a generic key-value store for which the backing is
 * not specifically determined. For example, it could be backed by a database or
 * simply a configuration file.
 *
 * @author David J. Pearce
 *
 */
public interface Configuration {

	/**
	 * Get the schema associated with the given configuration.
	 *
	 * @return
	 */
	public Schema getConfigurationSchema();

	/**
	 * Get the value associated with a given key. If no such key exists, an
	 * exception is raised. Every value returned is valid with respect to the
	 * schema.
	 */
	public <T> T get(Class<T> kind, Path.ID key);

	/**
	 * Associate a given value with a given key in the configuration. This will
	 * create a new key if none existed before. The given value must conform to the
	 * schema for this configuration, otherwise an exception is raised.
	 *
	 * @param key
	 * @param value
	 */
	public <T> void write(Path.ID key, T value);

	/**
	 * Determines what values are permitted and required for this configuration.
	 *
	 * @author David J. Pearce
	 *
	 */
	public interface Schema {
		/**
		 * Get the minimum set of required keys for this schema.
		 * @return
		 */
		public Set<Path.ID> getRequiredKeys();

		/**
		 * Get the complete set of keys known to this schema.
		 * @return
		 */
		public Set<Path.ID> getKnownKeys();

		/**
		 * Check whether the give key is known to this schema or not.
		 *
		 * @param key
		 * @return
		 */
		public boolean isKnownKey(Path.ID key);

		/**
		 * Get the descriptor associated with a given key.
		 *
		 * @param key
		 * @return
		 */
		public KeyValueDescriptor<?> getDescriptor(Path.ID key);
	}

	/**
	 * Construct a schema from a given array of KeyValueDescriptors.
	 *
	 * @param required
	 *            The set of required key-value pairs.
	 * @param optional
	 *            The set of optional key-value pairs.
	 * @return
	 */
	public static Schema fromArray(KeyValueDescriptor<?>[] required, KeyValueDescriptor<?>[] optional) {
		HashMap<Path.ID, KeyValueDescriptor<?>> map = new HashMap<>();
		HashSet<Path.ID> keys = new HashSet<>();
		// Check that all required keys have matching descriptors.
		for (int i = 0; i != required.length; ++i) {
			Path.ID key = required[i].getKey();
			keys.add(key);
			if (map.containsKey(key)) {
				throw new RuntimeException("multiple key-value descriptions for " + key);
			}
			map.put(key, required[i]);
		}
		// Put all other descriptors in
		for (int i = 0; i != required.length; ++i) {
			Path.ID key = optional[i].getKey();
			if (map.containsKey(key)) {
				throw new RuntimeException("multiple key-value descriptions for " + key);
			}
			map.put(key, optional[i]);
		}
		// Finally construct the schema
		return new Schema() {
			@Override
			public Set<Path.ID> getRequiredKeys() {
				return keys;
			}

			@Override
			public KeyValueDescriptor<?> getDescriptor(Path.ID key) {
				KeyValueDescriptor<?> d = map.get(key);
				if(d == null) {
					throw new IllegalArgumentException("invalid key \"" + key + "\"");
				}
				return d;
			}

			@Override
			public Set<ID> getKnownKeys() {
				return map.keySet();
			}

			@Override
			public boolean isKnownKey(ID key) {
				return map.containsKey(key);
			}
		};
	}

	/**
	 * Provides a generic mechanism for describing a key-value pair and ensuring
	 * that all values in a given configuration conform. This includes ensuring they
	 * have the right type, and that they meet given constraints.
	 *
	 * @author David J. Pearce
	 *
	 */
	public interface KeyValueDescriptor<T> {
		/**
		 * Get the key associated with this descriptor.
		 *
		 * @return
		 */
		public Path.ID getKey();

		/**
		 * Get the description associated with this descriptor.
		 *
		 * @return
		 */
		public String getDescription();

		/**
		 * Get the type associated with this validator, which could be e.g.
		 * <code>String</code>, <code>Boolean</code> or <code>Integer</code>.
		 *
		 * @return
		 */
		public Class<T> getType();

		/**
		 * Check whether a given value is actual valid. For example, integer values may
		 * be prevented from being negative, etc. Likewise, string values representing
		 * version numbers may need to conform to a given regular expression, etc.
		 *
		 * @param value
		 * @return
		 */
		public boolean isValid(T value);
	}

	/**
	 * A simple base class for arbitrary validators.
	 *
	 * @author David J. Pearce
	 *
	 * @param <T>
	 */
	public static abstract class AbstractDescriptor<T> implements KeyValueDescriptor<T> {
		private Path.ID key;
		private String description;
		private Class<T> type;

		public AbstractDescriptor(Path.ID key, String description, Class<T> type) {
			this.type = type;
		}

		@Override
		public Path.ID getKey() {
			return key;
		}

		@Override
		public String getDescription() {
			return description;
		}

		@Override
		public Class<T> getType() {
			return type;
		}

		@Override
		public boolean isValid(T value) {
			return true;
		}
	}

	/**
	 * Represents an unbound string key-valid pair. That is, any string is
	 * permitted.
	 *
	 * @param key
	 * @param description
	 * @return
	 */
	public static KeyValueDescriptor<String> UNBOUND_STRING(Path.ID key, String description) {
		return new AbstractDescriptor<String>(key,description,String.class) {

		};
	}

	/**
	 * Represents an unbound integer key-valid pair. That is, any integer is
	 * permitted.
	 *
	 * @param key
	 * @param description
	 * @return
	 */
	public static KeyValueDescriptor<Integer> UNBOUND_INTEGER(Path.ID key, String description) {
		return new AbstractDescriptor<Integer>(key,description,Integer.class) {

		};
	}

	/**
	 * Represents an unbound boolean key-valid pair. That is, any boolean is
	 * permitted.
	 *
	 * @param key
	 * @param description
	 * @return
	 */
	public static KeyValueDescriptor<Boolean> UNBOUND_BOOLEAN(Path.ID key, String description) {
		return new AbstractDescriptor<Boolean>(key,description,Boolean.class) {

		};
	}

	/**
	 * Returns an integer key-value descriptor which ensures the given value is
	 * greater or equal to a given lower bound.
	 *
	 * @param key
	 * @param description
	 * @param low
	 *            No valid value is below this bound.
	 * @return
	 */
	public static KeyValueDescriptor<Integer> BOUND_INTEGER(Path.ID key, String description, final int low) {
		 return new AbstractDescriptor<Integer>(key, description, Integer.class) {
				@Override
				public boolean isValid(Integer value) {
					return value >= low;
				}
		 };
	}

	/**
	 * Returns an integer key-value descriptor which ensures the given value is
	 * greater-or-equal to a given lower bound and less-or-equal to a given upper
	 * bound.
	 *
	 * @param key
	 * @param description
	 * @param low
	 *            No valid value is below this bound.
	 * @param high
	 *            No valid value is above this bound.
	 * @return
	 */
	public static KeyValueDescriptor<Integer> BOUND_INTEGER(Path.ID key, String description, final int low,
			final int high) {
		return new AbstractDescriptor<Integer>(key, description, Integer.class) {
			@Override
			public boolean isValid(Integer value) {
				return value >= low && value <= high;
			}
		};
	}
}
