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
package wybs.lang;

import wyfs.lang.Path;

public interface CompilationUnit extends SyntacticHeap {
	/**
	 * Get the path entry with which this compilation unit is associated. This
	 * may be a physical file on disk, a binary image stored in a jar file or an
	 * entry in a virtual file system.
	 *
	 * @return
	 */
	public Path.Entry<? extends CompilationUnit> getEntry();

	public interface Identifier extends SyntacticItem {
		public String get();
	}

	/**
	 * Represents a <i>partial-</i> or <i>fully-qualified</i> name within a
	 * compilation unit. That is, a sequence of one or more identifiers.
	 *
	 * @author David J. Pearce
	 *
	 */
	public interface Name extends SyntacticItem {
		@Override
		public Identifier getOperand(int x);
	}

	/**
	 * Represents a declaration of some kind within a compilation unit.
	 *
	 * @author David J. Pearce
	 *
	 */
	public interface Declaration extends SyntacticItem {

	}
}
