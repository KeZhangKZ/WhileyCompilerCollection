package wycc.util;

import java.io.IOException;
import wybs.util.ResolveError;
import wycc.lang.Package;
import wycc.lang.SemanticVersion;
import wyfs.lang.Path;
import wyfs.util.Trie;

/**
 * The local package install maintains a local copy of all packages which can
 * then be resolved against. It also maintains a list of installers which can be
 * used in an attempt to obtain missing packages (e.g. by downloading them).
 *
 * @author David J. Pearce
 *
 */
public class LocalPackageResolver implements Package.Resolver {
	public static final Path.ID BUILD_FILE_NAME = Trie.fromString("wy.toml");
	protected final Logger logger;
	protected final Path.Root root;

	public LocalPackageResolver(Logger logger, Path.Root root) {
		this.logger = logger;
		this.root = root;
	}

	@Override
	public Path.Root resolve(String name, SemanticVersion version) throws ResolveError {
		try {
			Path.Root packageRoot = getPackageRoot(name, version);
			// FIXME: need to provide mechanism for checking whether package
			// already exists or not.

			// Indicates the given package does not currently exist.
			// Therefore, download and expand it.
			expandPackage(name, version, packageRoot);
			return packageRoot;
		} catch (IOException e) {
			throw new ResolveError(e.getMessage());
		}
	}

	/**
	 * Determine the package root. This is relative to the registry root and
	 * determines where the package will be stored. By default, this is given by
	 * the path <code>name/version/</code>.
	 *
	 * @return
	 * @throws IOException
	 */
	public Path.Root getPackageRoot(String name, SemanticVersion version) throws IOException {
		Path.ID path = Trie.ROOT.append(name).append(version.toString());
		return root.createRelativeRoot(path);
	}

	/**
	 * Expand a given package into the given package root. This may require
	 * obtaining the package from somewhere (e.g. downloading it from a registry
	 * or online repository such as GitHub).
	 *
	 * @param name
	 * @param version
	 * @param packageRoot
	 */
	abstract void expandPackage(String name, SemanticVersion version, Path.Root packageRoot) throws ResolveError;
}
