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

		public NameID toNameID();
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
