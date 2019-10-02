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
package wycc.util;

import java.io.IOException;
import java.util.Set;

import wycc.cfg.ConfigFile;
import wycc.cfg.Configuration;
import wycc.lang.Command;
import wycc.lang.Package;
import wycc.lang.SemanticVersion;
import wyfs.lang.Content;
import wyfs.lang.Path;
import wyfs.lang.Path.Root;
import wyfs.util.Trie;
import wyfs.util.ZipFile;
import wyfs.util.ZipFileRoot;

/**
 *
 * @author djp
 *
 */
public class LocalPackageRepository implements Package.Repository {
	private final Command.Environment environment;
	private final Package.Repository parent;
	private final Content.Registry registry;
	private final Path.Root root;

	public LocalPackageRepository(Command.Environment environment, Content.Registry registry, Path.Root root) {
		this(environment,null,registry,root);
	}

	public LocalPackageRepository(Command.Environment environment, Package.Repository parent, Content.Registry registry, Path.Root root) {
		this.parent = parent;
		this.registry = registry;
		this.root = root;
		this.environment = environment;
	}

	@Override
	public Package.Repository getParent() {
		return parent;
	}

	@Override
	public Set<SemanticVersion> list(String pkg) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public AbstractPackage get(String pkg, SemanticVersion version) throws IOException {
		Trie id = Trie.fromString(pkg + "-v" + version);
		// Attempt to resolve it.
		if (!root.exists(id, ZipFile.ContentType)) {
			// FIXME: handle better error handling.
			throw new IllegalArgumentException("missing dependency \"" + id + "\"");
		} else {
			// Extract entry for ZipFile
			Path.Entry<ZipFile> zipfile = root.get(id, ZipFile.ContentType);
			// Construct root representing this ZipFile
			Path.Root pkgRoot = new ZipFileRoot(zipfile, registry);
			// Extract configuration from package
			Path.Entry<ConfigFile> entry = pkgRoot.get(Trie.fromString("wy"), ConfigFile.ContentType);
			if (entry == null) {
				throw new IllegalArgumentException("corrupt package (missing wy.toml) \"" + id + "-" + version + "\"");
			} else {
				// Read package configuration
				ConfigFile pkgcfg = pkgRoot.get(Trie.fromString("wy"), ConfigFile.ContentType).read();
				// Add package to this project
				return new AbstractPackage(pkgRoot, pkgcfg.toConfiguration(Package.SCHEMA, false));
			}
		}
	}

	@Override
	public void put(ZipFile pkg, String name, SemanticVersion version) throws IOException {
		// Determine fully qualified package name
		Trie qpn = Trie.fromString(name + "-v" + version);
		// Dig out the file!
		Path.Entry<ZipFile> entry = root.create(qpn, ZipFile.ContentType);
		// Write the contents
		entry.write(pkg);
		// Flush
		entry.flush();
		//
		environment.getLogger().logTimedMessage("Installed " + entry.location(), 0, 0);
	}

	private static class AbstractPackage implements wybs.lang.Build.Package {
		private final Path.Root root;
		private final Configuration configuration;

		public AbstractPackage(Path.Root root, Configuration configuration) {
			this.root = root;
			this.configuration = configuration;
		}

		@Override
		public Configuration getConfiguration() {
			return configuration;
		}

		@Override
		public Root getRoot() {
			return root;
		}
	}
}
