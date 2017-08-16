package wybs.util;

import java.math.BigInteger;
import java.util.Iterator;
import java.util.List;

import wybs.lang.CompilationUnit;
import wybs.lang.NameID;
import wybs.lang.SyntacticHeap;
import wybs.lang.SyntacticItem;
import wycc.util.ArrayUtils;
import wyfs.lang.Path;
import wyfs.lang.Path.Entry;
import wyfs.util.Trie;

public class AbstractCompilationUnit<T extends CompilationUnit> extends AbstractSyntacticHeap
		implements CompilationUnit {

	public static final int CONST_null = 66;
	public static final int CONST_bool = 67;
	public static final int CONST_int = 68;
	public static final int CONST_utf8 = 69;

	public static final int ITEM_pair = 100;
	public static final int ITEM_tuple = 101;
	public static final int ITEM_ident = 103;
	public static final int ITEM_path = 104;
	public static final int ITEM_name = 105;

	protected final Path.Entry<T> entry;

	public AbstractCompilationUnit(Path.Entry<T> entry) {
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
			return (K) getOperand(0);
		}

		public V getSecond() {
			return (V) getOperand(1);
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
		/**
		 * The kind is retained to ensure the proper array kind is constructed
		 * when a tuple is cloned. It is somewhat annoying that we have to do
		 * this, but there is not other way.
		 */
		private final Class<T> kind;

		public Tuple(T... stmts) {
			super(ITEM_tuple, stmts);
			kind = (Class) stmts.getClass().getComponentType();
		}

		public Tuple(Class<T> kind, List<T> stmts) {
			super(ITEM_tuple, stmts.toArray(new SyntacticItem[stmts.size()]));
			this.kind = kind;
		}

		@Override
		public T getOperand(int i) {
			return (T) super.getOperand(i);
		}

		@Override
		public T[] getOperands() {
			return (T[]) super.getOperands();
		}

		@Override
		public Tuple<T> clone(SyntacticItem[] operands) {
			return new Tuple(ArrayUtils.toArray(kind, operands));
		}

		@Override
		public String toString() {
			String r = "";
			for (int i = 0; i != size(); ++i) {
				if (i != 0) {
					r += ",";
				}
				SyntacticItem child = getOperand(i);
				if (child == null) {
					r += "?";
				} else {
					r += child.toString();
				}
			}
			return "(" + r + ")";
		}

		@Override
		public Iterator<T> iterator() {
			// Create annonymous iterator for iterating over elements.
			return new Iterator<T>() {
				private int index = 0;
				private final SyntacticItem[] operands = getOperands();

				@Override
				public boolean hasNext() {
					return index < operands.length;
				}

				@Override
				public T next() {
					return (T) operands[index++];
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
	public static class Identifier extends AbstractSyntacticItem {
		public Identifier(String name) {
			super(ITEM_ident, name, new SyntacticItem[0]);
		}

		public String get() {
			return (String) data;
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
	public static class Name extends AbstractSyntacticItem {
		public Name(Identifier... components) {
			super(ITEM_name, components);
		}

		@Override
		public Identifier getOperand(int i) {
			return (Identifier) super.getOperand(i);
		}

		public Identifier[] getComponents() {
			return (Identifier[]) getOperands();
		}

		@Override
		public Name clone(SyntacticItem[] operands) {
			return new Name(ArrayUtils.toArray(Identifier.class, operands));
		}

		@Override
		public String toString() {
			String r = getOperand(0).get();
			for (int i = 1; i != size(); ++i) {
				r += "." + getOperand(i).get();
			}
			return r;
		}

		public NameID toNameID() {
			Trie pkg = Trie.ROOT;
			for (int i = 0; i < size() - 1; ++i) {
				pkg = pkg.append(getOperand(i).get());
			}
			String n = getOperand(size() - 1).get();
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

		public Value(int opcode) {
			super(opcode);
		}

		public Value(int opcode, Object data) {
			super(opcode, data, new SyntacticItem[0]);
		}

		//public abstract Type getType();

		@Override
		public String toString() {
			return getData().toString();
		}

		public static class Null extends Value {
			public Null() {
				super(CONST_null);
			}

//			@Override
//			public Type getType() {
//				return new Type.Null();
//			}

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
				super(CONST_bool, value);
			}

			public boolean get() {
				return (Boolean) data;
			}

//			@Override
//			public Type getType() {
//				return new Type.Bool();
//			}

			@Override
			public Bool clone(SyntacticItem[] operands) {
				return new Bool(get());
			}
		}

		public static class Int extends Value {
			public Int(BigInteger value) {
				super(CONST_int, value);
			}

			public Int(long value) {
				super(CONST_int, BigInteger.valueOf(value));
			}

//			@Override
//			public Type getType() {
//				return new Type.Int();
//			}

			public BigInteger get() {
				return (BigInteger) data;
			}

			@Override
			public Int clone(SyntacticItem[] operands) {
				return new Int(get());
			}
		}

		public static class UTF8 extends Value {
			public UTF8(byte[] bytes) {
				super(CONST_utf8, bytes);
			}

//			@Override
//			public Type getType() {
//				throw new UnsupportedOperationException();
//			}

			public byte[] get() {
				return (byte[]) data;
			}

			@Override
			public UTF8 clone(SyntacticItem[] operands) {
				return new UTF8(get());
			}
		}
	}
}
