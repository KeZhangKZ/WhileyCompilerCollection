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

import java.lang.reflect.Array;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

import wybs.lang.CompilationUnit;
import wybs.lang.NameID;
import wybs.lang.SyntacticHeap;
import wybs.lang.SyntacticItem;
import wybs.lang.SyntacticItem.Data;
import wybs.lang.SyntacticItem.Operands;
import wybs.lang.SyntacticItem.Schema;
import wycc.util.ArrayUtils;
import wyfs.lang.Path;
import wyfs.lang.Path.Entry;
import wyfs.util.Trie;

public class AbstractCompilationUnit<T extends CompilationUnit> extends AbstractSyntacticHeap
		implements CompilationUnit {

	// ITEMS: 0000000 (0) -- 00001111 (15)
	public static final int ITEM_null = 0;
	public static final int ITEM_bool = 1;
	public static final int ITEM_int = 2;
	public static final int ITEM_utf8 = 3;
	public static final int ITEM_pair = 4;
	public static final int ITEM_tuple = 5;
	public static final int ITEM_ident = 6;
	public static final int ITEM_name = 7;

	public static final int ATTR_span = 8;
	public static final int ITEM_byte = 15; // deprecated

	protected final Path.Entry<T> entry;

	public AbstractCompilationUnit(Path.Entry<T> entry) {
		this.entry = entry;
	}

	public AbstractCompilationUnit(Path.Entry<T> entry, CompilationUnit other) {
		super(other);
		this.entry = entry;
	}

	@Override
	public Entry<T> getEntry() {
		return entry;
	}

	@Override
	public SyntacticHeap getParent() {
		return null;
	}

	/**
	 * Represents a pair of items in a compilation unit.
	 *
	 * @author David J. Pearce
	 *
	 * @param <K>
	 * @param <V>
	 */
	public static class Pair<K extends SyntacticItem, V extends SyntacticItem>
			extends AbstractSyntacticItem {
		public Pair(K lhs, V rhs) {
			super(ITEM_pair, lhs, rhs);
		}

		public K getFirst() {
			return (K) get(0);
		}

		public V getSecond() {
			return (V) get(1);
		}

		@Override
		public Pair<K, V> clone(SyntacticItem[] operands) {
			return new Pair<>((K) operands[0], (V) operands[1]);
		}

		@Override
		public String toString() {
			return "(" + getFirst() + ", " + getSecond() + ")";
		}
	}

	/**
	 * Represents a sequence of zero or more items in a compilation unit.
	 *
	 * @author David J. Pearce
	 *
	 * @param <T>
	 */
	public static class Tuple<T extends SyntacticItem> extends AbstractSyntacticItem implements Iterable<T> {

		public Tuple(T... stmts) {
			super(ITEM_tuple, stmts);
		}

		public Tuple(Collection<T> stmts) {
			super(ITEM_tuple, ArrayUtils.toArray(SyntacticItem.class,stmts));
		}

		@Override
		public T get(int i) {
			return (T) super.get(i);
		}

		public <S extends SyntacticItem> Tuple<S> map(Function<T,S> fn) {
		  int size = size();
      SyntacticItem[] elements = new SyntacticItem[size];
      for (int i = 0; i != size; ++i) {
        elements[i] = fn.apply(get(i));
      }
      return new Tuple<>((S[]) elements);
		}

		@Override
		public Tuple<T> clone(SyntacticItem[] operands) {
			return new Tuple(ArrayUtils.toArray(SyntacticItem.class, operands));
		}

		@Override
		public String toString() {
			return "(" + toBareString() + ")";
		}

		public String toBareString() {
			String r = "";
			for (int i = 0; i != size(); ++i) {
				if (i != 0) {
					r += ",";
				}
				SyntacticItem child = get(i);
				if (child == null) {
					r += "?";
				} else {
					r += child.toString();
				}
			}
			return r;
		}

		@Override
		public Iterator<T> iterator() {
			// Create annonymous iterator for iterating over elements.
			return new Iterator<T>() {
				private int index = 0;

				@Override
				public boolean hasNext() {
					return index < size();
				}

				@Override
				public T next() {
					return (T) get(index++);
				}

			};
		}
	}

	/**
	 * Represents an <i>identifier</i> in a compilation unit. For example, this
	 * could be used to represent a variable access. Or, it could be part of a
	 * partially or fully qualified name.
	 *
	 * @author David J. Pearce
	 *
	 */
	public static class Identifier extends AbstractSyntacticItem implements CompilationUnit.Identifier {
		public Identifier(String name) {
			super(ITEM_ident, name.getBytes(StandardCharsets.UTF_8), new SyntacticItem[0]);
		}

		public Identifier(byte[] bytes) {
			super(ITEM_ident, bytes, new SyntacticItem[0]);
		}

		@Override
		public String get() {
			// FIXME: could cache this
			return new String(data,StandardCharsets.UTF_8);
		}

		@Override
		public Identifier clone(SyntacticItem[] operands) {
			return new Identifier(get());
		}

		@Override
		public String toString() {
			return get();
		}
	}

	/**
	 * Represents a <i>partial-</i> or <i>fully-qualified</i> name within a
	 * compilation unit.
	 *
	 * @author David J. Pearce
	 *
	 */
	public static class Name extends AbstractSyntacticItem implements CompilationUnit.Name {
		public Name(Identifier... components) {
			super(ITEM_name, components);
		}

		@Override
		public Identifier get(int i) {
			return (Identifier) super.get(i);
		}

		@Override
		public Identifier[] getAll() {
			return (Identifier[]) super.getAll();
		}

		public Identifier getLast() {
			return get(size()-1);
		}

		public Identifier[] getPath() {
			Identifier[] components = new Identifier[size()-1];
			for(int i=0;i!=components.length;++i) {
				components[i] = get(i);
			}
			return components;
		}

		@Override
		public Name clone(SyntacticItem[] operands) {
			return new Name(ArrayUtils.toArray(Identifier.class, operands));
		}

		@Override
		public String toString() {
			String r = get(0).get();
			for (int i = 1; i != size(); ++i) {
				r += "::" + get(i).get();
			}
			return r;
		}

		@Override
		public NameID toNameID() {
			Trie pkg = Trie.ROOT;
			for (int i = 0; i < size() - 1; ++i) {
				pkg = pkg.append(get(i).get());
			}
			String n = get(size() - 1).get();
			return new NameID(pkg, n);
		}
	}

	/**
	 * Represents a raw value within a compilation unit. This is not a
	 * source-level item, though could be a component of a source-level item
	 * (e.g. a constant expression).
	 *
	 * @author David J. Pearce
	 *
	 */
	public abstract static class Value extends AbstractSyntacticItem {

		public Value(int opcode, byte... data) {
			super(opcode, data, new SyntacticItem[0]);
		}

		//public abstract Type getType();

		@Override
		public String toString() {
			return getData().toString();
		}

		public static class Null extends Value {
			public Null() {
				super(ITEM_null);
			}

			@Override
			public Null clone(SyntacticItem[] operands) {
				return new Null();
			}

			@Override
			public String toString() {
				return "null";
			}
		}

		public static class Bool extends Value {
			public Bool(boolean value) {
				super(ITEM_bool, (byte) (value ? 1 : 0));
			}

			public boolean get() {
				return (data[0] == 1);
			}

			@Override
			public Bool clone(SyntacticItem[] operands) {
				return new Bool(get());
			}

			@Override
			public String toString() {
				return Boolean.toString(get());
			}
		}

		public static class Byte extends Value {
			public Byte(byte value) {
				super(ITEM_byte, value);
			}

			public byte get() {
				return data[0];
			}

			@Override
			public Byte clone(SyntacticItem[] operands) {
				return new Byte(get());
			}
		}


		public static class Int extends Value {

			public Int(long value) {
				this(BigInteger.valueOf(value));
			}

			public Int(BigInteger value) {
				super(ITEM_int, value.toByteArray());
			}

			public Int(byte[] bytes) {
				super(ITEM_int, bytes);
			}

			public BigInteger get() {
				return new BigInteger(data);
			}

			@Override
			public Int clone(SyntacticItem[] operands) {
				return new Int(get());
			}

			@Override
			public String toString() {
				return get().toString();
			}
		}

		public static class UTF8 extends Value {
			public UTF8(byte[] bytes) {
				super(ITEM_utf8, bytes);
			}

			public byte[] get() {
				return (byte[]) data;
			}

			@Override
			public UTF8 clone(SyntacticItem[] operands) {
				return new UTF8(get());
			}

			@Override
			public String toString() {
				return new String(get());
			}
		}
	}

	// ============================================================
	// Attributes
	// ============================================================

	/**
	 * Attributes represent various additional pieces of information inferred
	 * about a given item in the heap.  For example, source line information.
	 *
	 * @author David J. Pearce
	 *
	 */
	public interface Attribute {

		/**
		 * A span associates a given syntactic item with a contiguous region of
		 * text in the original source file.
		 *
		 * @author David J. Pearce
		 *
		 */
		public class Span extends AbstractSyntacticItem implements Attribute {

			public Span(SyntacticItem item, int start, int end) {
				this(item, new Value.Int(start), new Value.Int(end));
			}

			public Span(SyntacticItem target, Value.Int start, Value.Int end) {
				super(ATTR_span, target, start, end);
			}

			/**
			 * Get the item that this span is associated with.
			 *
			 * @return
			 */
			public SyntacticItem getItem() {
				return get(0);
			}

			/**
			 * Get the integer offset from the start of the stream where this
			 * span begins.
			 *
			 * @return
			 */
			public Value.Int getStart() {
				return (Value.Int) get(1);
			}

			/**
			 * Get the integer offset from the start of the stream where this
			 * span ends.
			 *
			 * @return
			 */
			public Value.Int getEnd() {
				return (Value.Int) get(2);
			}

			@Override
			public Span clone(SyntacticItem[] operands) {
				return new Span(operands[0], (Value.Int) operands[1], (Value.Int) operands[2]);
			}
		}
	}

	// =========================================================================
	// Schema
	// =========================================================================

	private static volatile SyntacticItem.Schema[] SCHEMA = null;

	public static SyntacticItem.Schema[] getSchema() {
		if(SCHEMA == null) {
			SCHEMA = createSchema();
		}
		return SCHEMA;
	}

	private static SyntacticItem.Schema[] createSchema() {
		SyntacticItem.Schema[] schema = new SyntacticItem.Schema[256];
		// ==========================================================================
		schema[ITEM_null] = new Schema(Operands.ZERO,Data.ZERO, "ITEM_null") {
			@Override
			public SyntacticItem construct(int opcode, SyntacticItem[] operands, byte[] data) {
				return new Value.Null();
			}
		};
		// ==========================================================================
		schema[ITEM_bool] = new Schema(Operands.ZERO,Data.ONE, "ITEM_bool") {
			@Override
			public SyntacticItem construct(int opcode, SyntacticItem[] operands, byte[] data) {
				return new Value.Bool(data[0] == 1);
			}
		};
		// ==========================================================================
		schema[ITEM_int] = new Schema(Operands.ZERO,Data.MANY, "ITEM_int") {
			@Override
			public SyntacticItem construct(int opcode, SyntacticItem[] operands, byte[] data) {
				return new Value.Int(data);
			}
		};
		// ==========================================================================
		schema[ITEM_utf8] = new Schema(Operands.ZERO,Data.MANY, "ITEM_utf8") {
			@Override
			public SyntacticItem construct(int opcode, SyntacticItem[] operands, byte[] data) {
				return new Value.UTF8(data);
			}
		};
		// ==========================================================================
		schema[ITEM_pair] = new Schema(Operands.TWO,Data.ZERO, "ITEM_pair") {
			@Override
			public SyntacticItem construct(int opcode, SyntacticItem[] operands, byte[] data) {
				return new Pair<>(operands[0],operands[1]);
			}
		};
		// ==========================================================================
		schema[ITEM_tuple] = new Schema(Operands.MANY,Data.ZERO, "ITEM_tuple") {
			@Override
			public SyntacticItem construct(int opcode, SyntacticItem[] operands, byte[] data) {
				return new Tuple<>(operands);
			}
		};
		// ==========================================================================
		schema[ITEM_ident] = new Schema(Operands.ZERO,Data.MANY, "ITEM_ident") {
			@Override
			public SyntacticItem construct(int opcode, SyntacticItem[] operands, byte[] data) {
				return new Identifier(data);
			}
		};
		// ==========================================================================
		schema[ITEM_name] = new Schema(Operands.MANY,Data.ZERO, "ITEM_name") {
			@Override
			public SyntacticItem construct(int opcode, SyntacticItem[] operands, byte[] data) {
				return new Name(ArrayUtils.toArray(Identifier.class, operands));
			}
		};
		// ==========================================================================
		schema[ITEM_byte] = new Schema(Operands.ZERO,Data.ONE, "ITEM_byte") {
			@Override
			public SyntacticItem construct(int opcode, SyntacticItem[] operands, byte[] data) {
				return new Value.Byte(data[0]);
			}
		};
		// ==========================================================================
		schema[ATTR_span] = new Schema(Operands.THREE, Data.ZERO, "ATTR_span") {
			@Override
			public SyntacticItem construct(int opcode, SyntacticItem[] operands, byte[] data) {
				return new Attribute.Span(operands[0], (Value.Int) operands[1], (Value.Int) operands[2]);
			}
		};

		return schema;
	}
}
