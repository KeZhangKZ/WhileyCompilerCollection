package wybs.util;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.List;

import wybs.lang.Build;
import wyfs.lang.Content;
import wyfs.lang.Content.Type;
import wyfs.lang.Path;
import wyfs.lang.Path.Entry;
import wyfs.lang.Path.ID;
import wyfs.util.ArrayUtils;
import wyfs.util.Trie;

public class FileBuildSystem extends AbstractBuildRepository {

	public final static FileFilter NULL_FILTER = new FileFilter() {
		@Override
		public boolean accept(File file) {
			return true;
		}
	};

	private final FileFilter filter;
	private final Content.Registry registry;
	private final File dir;
	private final HashSet<ID> dirty;
	
	public FileBuildSystem(Content.Registry registry, File dir) throws IOException {
		this(registry, dir, NULL_FILTER);
	}
	
	public FileBuildSystem(Content.Registry registry, File dir, FileFilter filter) throws IOException {
		super(initialise(registry, dir, filter));
		this.filter = filter;
		this.registry = registry;
		this.dir = dir;
		this.dirty = new HashSet<>();
	}
	
	/**
	 * Construct the initial state from the contents of the build directory.
	 * @param registry
	 * @param dir
	 * @param filter
	 * @return
	 * @throws IOException 
	 */
	private static State initialise(Content.Registry registry, File dir, FileFilter filter) throws IOException {
		java.nio.file.Path root = dir.toPath();
		// First extract all files rooted in this directory
		List<File> files = findAll(64, dir, filter, new ArrayList<>());
		// Second convert them all into entries as appropriate
		Build.Entry[] entries = new Build.Entry[files.size()];
		// 
		for (int i = 0; i != entries.length; ++i) {
			File ith = files.get(i);
			String filename = root.relativize(ith.toPath()).toString().replace(File.separatorChar, '/');
			entries[i] = read(filename, files.get(i), registry);
		}
		// Remove any files we didn't recognise.
		entries = ArrayUtils.removeAll(entries, null);
		// Done
		return new State(entries);
	}
	
	/**
	 * Read a given file from the filesystem and convert it into an entry using the
	 * given content registry. If the registry doesn't recognised the file, then
	 * this just returns <code>null</code>.
	 * 
	 * @param filename
	 * @param file
	 * @param registry
	 * @return
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	private static Build.Entry read(String filename, File file, Content.Registry registry) throws IOException {
		// Determine file suffix
		String suffix = "";
		int pos = filename.lastIndexOf('.');
		if (pos > 0) {
			suffix = filename.substring(pos + 1);
		}
		// Compute ID
		Path.ID id = Trie.fromString(filename.substring(0, filename.length() - (suffix.length() + 1)));
		// Extract appropriate content type (if applicable)
		Content.Type<?> type = registry.contentType(suffix);
		// Read entry (if applicable)
		if (type == null) {
			return null;
		} else {
			System.out.println("READING: " + file.getAbsolutePath());
			// FIXME: this cast should not be necessary!
			return (Build.Entry) type.read(id, new FileInputStream(file));
		}
	}
	
	/**
	 * Extract all files starting from a given directory.
	 * 
	 * @param dir
	 * @return
	 */
	private static List<File> findAll(int n, File dir, FileFilter filter, List<File> files) {
		if (n > 0 && dir.exists() && dir.isDirectory()) {
			File[] contents = dir.listFiles(filter);
			for (int i = 0; i != contents.length; ++i) {
				File ith = contents[i];
				//
				if (ith.isDirectory()) {
					findAll(n - 1, ith, filter, files);
				} else {
					files.add(ith);
				}
			}
		}
		return files;
	}	
}
