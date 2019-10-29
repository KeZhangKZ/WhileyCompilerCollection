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
package wybs.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import wybs.lang.SyntacticHeap;
import wybs.lang.SyntacticHeap.Schema;
import wybs.lang.SyntacticItem;

public class SyntacticHeapSchemas {

	/**
	 * Construct a base-level schema.
	 *
	 * @param major
	 * @param minor
	 * @param items
	 * @return
	 */
	public static class Builder {
		private final Schema schema;
		private final List<Action> delta;

		public Builder(Schema schema) {
			this.schema = schema;
			this.delta = new ArrayList<>();
		}

		/**
		 * Register a new section within this builder. This change is backwards
		 * compatible.
		 *
		 * @param name
		 * @param size
		 * @return
		 */
		public void register(String section, int size) {
			delta.add(new Action.Register(section, size));
		}

		/**
		 * Add a new opcode within a given section. This change is backwards compatible.
		 *
		 * @param Section
		 * @param name
		 * @param schema
		 * @return
		 */
		public void add(String section, String name, wybs.lang.SyntacticItem.Schema schema) {
			delta.add(new Action.Add(section, name, schema));
		}

		/**
		 * Replace an existing opcode in a given section with a new schema. This change
		 * is not backwards compatible.
		 *
		 * @param Section
		 * @param name
		 * @param schema
		 * @return
		 */
		public void replace(String section, String name, wybs.lang.SyntacticItem.Schema schema) {
			delta.add(new Action.Replace(section, name, schema));
		}

		/**
		 * Finalise the changes made through this builder into a new schema with an
		 * appropriate version.
		 *
		 * @return
		 */
		public SyntacticHeap.Schema done() {

		}
	}

	/**
	 * Represents an arbitrary action on a schema
	 *
	 * @author David J. Pearce
	 *
	 */
	private static abstract class Action {

		public abstract boolean isBackwardsCompatible();

		public abstract Section[] apply(Section[] sections);

		private static class Register extends Action {
			protected final String section;
			private final int size;

			public Register(String section, int size) {
				this.section = section;
				this.size = size;
			}

			@Override
			public boolean isBackwardsCompatible() {
				return true;
			}

			@Override
			public Section[] apply(Section[] sections) {
				// Check the new section doesn't already exist!
				if (lookup(sections,section) >= 0) {
					throw new IllegalArgumentException("duplicate schema section: " + section);
				}
				// Add the new section to the end
				sections = Arrays.copyOf(sections, sections.length + 1);
				sections[sections.length - 1] = new Section(section, new Opcode[size]);
				return sections;
			}
		}

		private static class Add extends Action {
			private final String section;
			private final String name;
			private final SyntacticItem.Schema schema;

			public Add(String section, String name, SyntacticItem.Schema schema) {
				this.section = section;
				this.name = name;
				this.schema = schema;
			}

			@Override
			public boolean isBackwardsCompatible() {
				return true;
			}

			@Override
			public Section[] apply(Section[] sections) {
				// Find section in question
				int i = lookup(sections, section);
				Section section = sections[i];
				// Check opcode not already allocate
				if (lookup(section, name) >= 0) {
					throw new IllegalArgumentException("duplicate opcode: " + section + ":" + name);
				}
				//
				Opcode[] opcodes = Arrays.copyOf(section.opcodes, section.opcodes.length + 1);
				opcodes[opcodes.length - 1] = new Opcode(name, schema);
				sections[i] = new Section(section.name, opcodes);
				return sections;
			}
		}

		private static class Replace extends Action {
			protected final String section;
			protected final String name;
			protected final SyntacticItem.Schema schema;

			public Replace(String section, String name, SyntacticItem.Schema schema) {
				this.section = section;
				this.name = name;
				this.schema = schema;
			}

			@Override
			public boolean isBackwardsCompatible() {
				return false;
			}

			@Override
			public Section[] apply(Section[] sections) {
				// Find section in question
				int i = lookup(sections, section);
				Section section = sections[i];
				// Check opcode not already allocate
				if (lookup(section, name) < 0) {
					throw new IllegalArgumentException("missing opcode: " + section + ":" + name);
				}
				//
				section.opcodes[i] = new Opcode(name, schema);
				return sections;
			}

		}

		private static int lookup(Section[] sections, String section) {
			for (int i = 0; i != sections.length; ++i) {
				Section s = sections[i];
				if (s.name.equals(section)) {
					return i;
				}
			}
			return -1;
		}

		private static int lookup(Section section, String name) {
			for(int i=0;i!=section.opcodes.length;++i) {
				Opcode opcode = section.opcodes[i];
				if(opcode.name.equals(name)) {
					return i;
				}
			}
			return -1;
		}
	}

	private static class Section {
		private final String name;
		private final Opcode[] opcodes;

		public Section(String name, Opcode[] opcodes) {
			this.name = name;
			this.opcodes = opcodes;
		}
	}

	private static class Opcode {
		private final String name;
		private final SyntacticItem.Schema schema;

		public Opcode(String name, SyntacticItem.Schema schema) {
			this.name = name;
			this.schema = schema;
		}
	}
}

