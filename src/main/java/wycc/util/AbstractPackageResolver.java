package wycc.util;

import java.io.File;
import java.io.IOException;

import wybs.util.ResolveError;
import wycc.lang.ConfigFile;
import wycc.lang.PackageResolver;
import wycc.lang.SemanticVersion;
import wyfs.lang.Path;
import wyfs.util.Trie;

public abstract class AbstractPackageResolver implements PackageResolver {
	public static final Path.ID BUILD_FILE_NAME = Trie.fromString("wy.toml");
	protected final Logger logger;
	protected final Path.Root root;

	public AbstractPackageResolver(Logger logger, Path.Root root) {
		this.logger = logger;
		this.root = root;
	}

	public Path.Root resolve(String name, SemanticVersion version) throws ResolveError {
		Path.Root packageRoot = getPackageRoot(name,version);
		try {
			Path.Entry<ConfigFile> buildFileEntry = packageRoot.get(BUILD_FILE_NAME, ConfigFile.ContentType);
			ConfigFile buildFile;
			if (buildFileEntry == null) {
				// Indicates the given package does not currently exist.
				// Therefore, download and expand it.
				expandPackage(name, version, packageRoot);
				buildFileEntry = packageRoot.get(BUILD_FILE_NAME, ConfigFile.ContentType);
				buildFile = checkBuildFile(name, version, buildFileEntry);
				// Resolve any package dependencies
				resolvePackageDependencies(buildFile);
				// Build the package
				buildPackage(buildFile,packageRoot);
			} else {
				// Indicates package already existed. Therefore, assume build
				// file was correctly defined.
				buildFile = buildFileEntry.read();
			}
			return getTargetRoot(buildFile,packageRoot);
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
	 */
	public Path.Root getPackageRoot(String name, SemanticVersion version) {
		Path.ID path = Trie.ROOT.append(name).append(version.toString());
		return root.getRelativeRoot(path);
	}

	/**
	 * Determine the target root for this package. That is the location where
	 * all compile WyIL files are stored. This is essentially the output of
	 * package resolution, as these are then added to the enclosing project.
	 *
	 * @param buidlFile
	 * @param packageRoot
	 * @return
	 */
	public Path.Root getTargetRoot(ConfigFile buidlFile, Path.Root packageRoot) {
		// FIXME: need to be more sophisticated. That is, actually look at the
		// buildFile and construct an appropriate root.
		return packageRoot.getRelativeRoot(Trie.ROOT.append("bin").append("wyil"));
	}

	/**
	 * Perform basic sanity checking on the given build file. For example, that
	 * the name and version match. Furthermore, that it can be parsed and
	 * contains enough useful information.
	 *
	 * @param entry
	 * @return
	 * @throws ResolveError
	 */
	public ConfigFile checkBuildFile(String name, SemanticVersion version, Path.Entry<ConfigFile> entry)
			throws ResolveError {
		if (entry == null) {
			throw new ResolveError("Package missing build file: " + name + ":" + version);
		} else {
			try {
				return entry.read();
			} catch (IOException e) {
				throw new ResolveError(e.getMessage());
			}
		}
	}

	/**
	 * Resolve any packages required by this package. This needs to be done in
	 * such a way as to ensure that all packages are resolved in the right
	 * order.
	 *
	 * @param buildFile
	 */
	public void resolvePackageDependencies(ConfigFile buildFile) {

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
	abstract void expandPackage(String name, SemanticVersion version, Path.Root packageRoot);

	/**
	 * Build a package using the given build file configuration and package
	 * root.
	 *
	 * @param buildFile
	 * @param packageRoot
	 */
	public void buildPackage(ConfigFile buildFile, Path.Root packageRoot) {

	}
}
