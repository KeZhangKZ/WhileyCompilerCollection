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

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A Syntactic Element represents any part of a source file which is relevant to
 * the syntactic structure of the file, and in particular parts we may wish to
 * add information too (e.g. line numbers, types, etc).
 *
 * @author David J. Pearce
 *
 */
public interface SyntacticElement {

	/**
     * Get the list of attributes associated with this syntactice element.
     *
     * @return
     */
	public List<Attribute> attributes();

	/**
     * Get the first attribute of the given class type. This is useful
     * short-hand.
     *
     * @param c
     * @return
     */
	public <T extends Attribute> T attribute(Class<T> c);

	public class Impl  implements SyntacticElement {
		private List<Attribute> attributes;

		public Impl() {
			// I use copy on write here, since for the most part I don't expect
			// attributes to change, and hence can be safely aliased. But, when they
			// do change I need fresh copies.
			attributes = new CopyOnWriteArrayList<>();
		}

		public Impl(Attribute x) {
			attributes = new ArrayList<>();
			attributes.add(x);
		}

		public Impl(Collection<Attribute> attributes) {
			this.attributes = new ArrayList<>(attributes);
		}

		public Impl(Attribute[] attributes) {
			this.attributes = new ArrayList<>(Arrays.asList(attributes));
		}

		@Override
		public List<Attribute> attributes() { return attributes; }

		@Override
		public <T extends Attribute> T attribute(Class<T> c) {
			for(Attribute a : attributes) {
				if(c.isInstance(a)) {
					return (T) a;
				}
			}
			return null;
		}
	}
}
