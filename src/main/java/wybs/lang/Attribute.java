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

/**
 * Represents a piece of meta-information that may be associated with a WYIL
 * bytecode or declaration. For example, the location of the element in the
 * source code which generated this bytecode.
 *
 * @author David J. Pearce
 *
 */
public interface Attribute {

	/**
	 * Represents a location in the source code of a Whiley Module. For example,
	 * this may be the location which generated a particular bytecode, or the
	 * location of a particular type declaration.
	 *
	 * @author David J. Pearce
	 *
	 */
	public final static class Source implements Attribute {
		public final int start;	 // starting character index
		public final int end;	 // end character index
		public final int line;   // line number

		public Source(int start, int end, int line) {
			this.start = start;
			this.end = end;
			this.line = line;
		}

		@Override
		public String toString() {
			return "@" + start + ":" + end;
		}
	}

	/**
	 * Represents an originating source location for a given syntactic element.
	 * This typically occurs if some element from one file is included in
	 * another element from another file.
	 *
	 * @author David J. Pearce
	 *
	 */
	public final static class Origin implements Attribute {
		public final String filename;
		public final int start;	 // starting character index
		public final int end;	 // end character index
		public final int line;   // line number

		public Origin(String filename, int start, int end, int line) {
			this.filename = filename;
			this.start = start;
			this.end = end;
			this.line = line;
		}

		@Override
		public String toString() {
			return filename + "@" + start + ":" + end;
		}
	}
}
