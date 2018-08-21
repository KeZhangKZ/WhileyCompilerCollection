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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import wyfs.lang.Path;
import wyfs.lang.Path.Filter;
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
	 * Determine all matching keys in this configuration.
	 *
	 * @param filter
	 * @return
	 */
	public List<Path.ID> matchAll(Path.Filter filter);

	/**
	 * Determines what values are permitted and required for this configuration.
	 *
	 * @author David J. Pearce
	 *
	 */
	public interface Schema {
		/**
		 * Check whether the give key is known to this schema or not.
		 *
		 * @param key
		 * @return
		 */
		public boolean isKey(Path.ID key);

		/**
		 * Get the descriptor associated with a given key.
		 *
		 * @param key
		 * @return
		 */
		public KeyValueDescriptor<?> getDescriptor(Path.ID key);

		/**
		 * Get the list of all descriptors in this schema.
		 *
		 * @return
		 */
		public List<KeyValueDescriptor<?>> getDescriptors();
	}

	/**
	 * Represents a simple empty configuration. This is useful for handling cases
	 * where e.g. a configuration file cannot be located.
	 */
	public static final Configuration EMPTY = new Configuration() {

		@Override
		public Schema getConfigurationSchema() {
			return EMPTY_SCHEMA;
		}

		@Override
		public <T> T get(Class<T> kind, ID key) {
			throw new IllegalArgumentException("invalid key access: " + key);
		}

		@Override
		public <T> void write(ID key, T value) {
			throw new IllegalArgumentException("invalid key access: " + key);
		}

		@Override
		public List<ID> matchAll(Filter filter) {
			return Collections.EMPTY_LIST;
		}

	};

	/**
	 * A simple schema which contains no keys.
	 */
	public static final Configuration.Schema EMPTY_SCHEMA = new Configuration.Schema() {

		@Override
		public boolean isKey(ID key) {
			return false;
		}

		@Override
		public KeyValueDescriptor<?> getDescriptor(ID key) {
			throw new IllegalArgumentException("invalid key: " + key);
		}

		@Override
		public List<KeyValueDescriptor<?>> getDescriptors() {
			return Collections.EMPTY_LIST;
		}
	};

	/**
	 * Construct a schema from a given array of KeyValueDescriptors.
	 *
	 * @param required
	 *            The set of required key-value pairs.
	 * @param optional
	 *            The set of optional key-value pairs.
	 * @return
	 */
	public static Schema fromArray(KeyValueDescriptor<?>... descriptors) {
		// Finally construct the schema
		return new Schema() {

			@Override
			public KeyValueDescriptor<?> getDescriptor(Path.ID key) {
				for(int i=0;i!=descriptors.length;++i) {
					KeyValueDescriptor<?> descriptor = descriptors[i];
					if(descriptor.getFilter().matches(key)) {
						return descriptor;
					}
				}
				throw new IllegalArgumentException("invalid key \"" + key + "\"");
			}

			@Override
			public boolean isKey(ID key) {
				for(int i=0;i!=descriptors.length;++i) {
					KeyValueDescriptor<?> descriptor = descriptors[i];
					if(descriptor.getFilter().matches(key)) {
						return true;
					}
				}
				return false;
			}

			@Override
			public List<KeyValueDescriptor<?>> getDescriptors() {
				return Arrays.asList(descriptors);
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
		 * Get the key filter associated with this descriptor.
		 *
		 * @return
		 */
		public Path.Filter getFilter();

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
		private Path.Filter key;
		private String description;
		private Class<T> type;

		public AbstractDescriptor(Path.Filter key, String description, Class<T> type) {
			this.key = key;
			this.description = description;
			this.type = type;
		}

		@Override
		public Path.Filter getFilter() {
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
	public static KeyValueDescriptor<String> UNBOUND_STRING(Path.Filter key, String description) {
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
	public static KeyValueDescriptor<Integer> UNBOUND_INTEGER(Path.Filter key, String description) {
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
	public static KeyValueDescriptor<Boolean> UNBOUND_BOOLEAN(Path.Filter key, String description) {
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
	public static KeyValueDescriptor<Integer> BOUND_INTEGER(Path.Filter key, String description, final int low) {
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
	public static KeyValueDescriptor<Integer> BOUND_INTEGER(Path.Filter key, String description, final int low,
			final int high) {
		return new AbstractDescriptor<Integer>(key, description, Integer.class) {
			@Override
			public boolean isValid(Integer value) {
				return value >= low && value <= high;
			}
		};
	}
}
