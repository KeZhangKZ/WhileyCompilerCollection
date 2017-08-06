package wycc.lang;

import wybs.util.ResolveError;
import wyfs.lang.Path;

public interface PackageResolver {
	/**
	 * Resolve a give package name and version to produce a root to the package
	 * the binary WyIL files of the package. This root can then be used
	 * subsequently in compiling a Whiley project.
	 *
	 * @param name
	 *            The name of the package to resolve.
	 * @param version
	 *            The semantic vesion of the package to resolve.
	 * @return
	 * @throws ResolveError
	 */
	public Path.Root resolve(String name, SemanticVersion version) throws ResolveError;
}
