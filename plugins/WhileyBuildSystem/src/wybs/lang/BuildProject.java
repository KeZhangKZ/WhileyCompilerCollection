package wybs.lang;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import wyfs.lang.Content;
import wyfs.lang.Path;
import wyfs.lang.Path.Entry;
import wyfs.lang.Path.ID;

/**
 * <p>
 * Represents a top-level entity responsible for managing everything related
 * to a given build. A build project provides a global "namespace" where
 * named objects (e.g. source files, binary files) reside and/or are
 * created. A build project also contains one or more build rules which
 * determines how source files are transformed.
 * </p>
 * <p>
 * For a given set of source files, a build project defines (in an abstract
 * sense) a "build" --- that is, a specific plan of construction starting
 * from one or more source files and producing one or more binary files.
 * This is abstract because, in the normal course of events, the build is
 * only known "after the fact"; that is, once all binary files are
 * generated. This is necessary because it can be difficult to predict ahead
 * of time what binary files will be generated from a given source file.
 * </p>
 * <p>
 * Build projects have the opportunity to record the dependencies created
 * during a build. That is, a binary file depends on those source file(s)
 * required to build it. Recording this information is necessary if one
 * wants to perform an incremental (re)compilation. That is, using such
 * dependency information, one can avoid recompiling all source files from
 * scratch.
 * </p>
 * <p>
 * Finally, build projects may choose to record other information (e.g.
 * timing and other statistical information) and/or employ different
 * techniques (e.g. parallel builds).
 * </p>
 * 
 * @author David J. Pearce
 * 
 */
public interface BuildProject {

	/**
	 * Check whether or not a given entry is contained in this root;
	 * 
	 * @param entry
	 * @return
	 */
	public boolean contains(Path.Entry<?> entry) throws IOException;

	/**
	 * Check whether or not a given entry and content-type is contained in
	 * this root.
	 * 
	 * @throws IOException
	 *             --- in case of some I/O failure.
	 */
	public boolean exists(Path.ID id, Content.Type<?> ct)
			throws IOException;

	/**
	 * Get the entry corresponding to a given ID and content type. If no
	 * such entry exists, return null.
	 * 
	 * @param id
	 *            --- id of module to lookup.
	 * @throws IOException
	 *             --- in case of some I/O failure.
	 */
	public <T> Path.Entry<T> get(Path.ID id, Content.Type<T> ct)
			throws IOException;

	/**
	 * Get all objects matching a given content filter stored in this root.
	 * In the case of no matches, an empty list is returned.
	 * 
	 * @throws IOException
	 *             --- in case of some I/O failure.
	 * 
	 * @param ct
	 * @return
	 */
	public <T> List<Path.Entry<T>> get(Content.Filter<T> ct)
			throws IOException;

	/**
	 * Identify all entries matching a given content filter stored in this
	 * root. In the case of no matches, an empty set is returned.
	 * 
	 * @throws IOException
	 *             --- in case of some I/O failure.
	 * 
	 * @param filter
	 *            --- filter to match entries with.
	 * @return
	 */
	public <T> Set<Path.ID> match(Content.Filter<T> filter)
			throws IOException;

	/**
	 * Create an entry of a given content type at a given path, derived from
	 * zero or more entries. If the entry already exists, then it is just
	 * returned. An entry is derived from another entry if it is, in some
	 * way, generated from that entry (e.g. it is compiled from that file).
	 * 
	 * @param id
	 *            --- Path.ID for the new entry
	 * @param ct
	 *            --- content type of the new entry
	 * @param sources
	 *            --- entries from which this entry is derived.
	 * @return
	 * @throws IOException
	 */
	public <T> Path.Entry<T> create(Path.ID id, Content.Type<T> ct,
			Path.Entry<?>... sources) throws IOException;
}