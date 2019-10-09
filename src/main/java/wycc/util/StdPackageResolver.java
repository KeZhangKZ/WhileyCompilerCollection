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
import java.util.ArrayList;
import java.util.List;

import wybs.lang.Build;
import wycc.lang.Package;
import wycc.lang.Package.Repository;
import wycc.lang.SemanticVersion;
import wyfs.lang.Path.Root;

/**
 * Provides a default and relatively simplistic approach for resolving packages.
 *
 * @author David J. Pearce
 *
 */
public class StdPackageResolver implements Package.Resolver {
	private final Package.Repository repository;

	public StdPackageResolver(Package.Repository repository) {
		this.repository = repository;
	}

	@Override
	public List<Build.Package> resolve(List<Pair<String, String>> dependencies) throws IOException {
		// FIXME: this is really dumb
		ArrayList<Build.Package> packages = new ArrayList<>();
		for (Pair<String, String> dep : dependencies) {
			String name = dep.first();
			SemanticVersion version = new SemanticVersion(dep.second());
			Build.Package pkg = repository.get(name, version);
			if(pkg != null) {
				packages.add(pkg);
			}
		}
		return packages;
	}

	@Override
	public Repository getRepository() {
		return repository;
	}

}
