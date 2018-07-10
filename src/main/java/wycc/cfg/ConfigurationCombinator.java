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

import java.util.HashSet;
import java.util.Set;

import wycc.cfg.Configuration.Schema;
import wyfs.lang.Path.ID;

/**
 * Combines one or more configurations into a single configuration. The
 * different configurations must be "compatible" in the sense that they are not
 * permitted overlapping keys.
 *
 * @author David J. Pearce
 *
 */
public class ConfigurationCombinator implements Configuration {
	private final Configuration.Schema schema;
	private final Configuration[] configurations;

	public ConfigurationCombinator(Configuration... configurations) {
		this.schema = toCombinedSchema(configurations);
		this.configurations = configurations;
	}

	@Override
	public Schema getConfigurationSchema() {
		return schema;
	}

	@Override
	public <T> T get(Class<T> kind, ID key) {
		for (int i = 0; i != configurations.length; ++i) {
			Configuration config = configurations[i];
			if (config.getConfigurationSchema().isKnownKey(key)) {
				return config.get(kind, key);
			}
		}
		throw new IllegalArgumentException("invalid key access: " + key);
	}

	@Override
	public <T> void write(ID key, T value) {
		for (int i = 0; i != configurations.length; ++i) {
			Configuration config = configurations[i];
			if (config.getConfigurationSchema().isKnownKey(key)) {
				config.write(key, value);
				return;
			}
		}
		throw new IllegalArgumentException("invalid key access: " + key);
	}

	private static Schema toCombinedSchema(Configuration... configurations) {
		Schema[] schemas = new Schema[configurations.length];
		// Get array of schemas
		for (int i = 0; i != schemas.length; ++i) {
			schemas[i] = configurations[i].getConfigurationSchema();
		}
		// Sanity check schemas
		HashSet<ID> keys = new HashSet<>();
		for(int i=0;i!=schemas.length;++i) {
			Schema s = schemas[i];
			Set<ID> ks = s.getKnownKeys();
			for(ID k : ks) {
				if(keys.contains(k)) {
					throw new IllegalArgumentException("conflicting configurations for key: " + k);
				}
			}
			//
			keys.addAll(ks);
		}
		//
		return new Schema() {

			@Override
			public Set<ID> getRequiredKeys() {
				HashSet<ID> keys = new HashSet<>();
				for(int i=0;i!=schemas.length;++i) {
					keys.addAll(schemas[i].getRequiredKeys());
				}
				return keys;
			}

			@Override
			public Set<ID> getKnownKeys() {
				HashSet<ID> keys = new HashSet<>();
				for(int i=0;i!=schemas.length;++i) {
					keys.addAll(schemas[i].getKnownKeys());
				}
				return keys;
			}

			@Override
			public boolean isKnownKey(ID key) {
				for(int i=0;i!=schemas.length;++i) {
					if(schemas[i].isKnownKey(key)) {
						return true;
					}
				}
				return false;
			}

			@Override
			public KeyValueDescriptor<?> getDescriptor(ID key) {
				for (int i = 0; i != schemas.length; ++i) {
					Schema schema = schemas[i];
					//
					if (schema.isKnownKey(key)) {
						return schema.getDescriptor(key);
					}
				}
				//
				throw new IllegalArgumentException("invalid key accesss: " + key);
			}
		};
	}

}
