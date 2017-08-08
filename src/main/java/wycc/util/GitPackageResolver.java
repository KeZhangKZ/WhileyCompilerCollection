package wycc.util;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;

import wybs.util.ResolveError;
import wycc.lang.SemanticVersion;
import wyfs.lang.Path.Root;
import wyfs.util.DirectoryRoot;

public class GitPackageResolver extends AbstractPackageResolver {
	/**
	 * The registry maps package names to their git URLs.
	 */
	private HashMap<String, String> registry;

	public GitPackageResolver(Logger logger, Root root, Map<String, String> registry) {
		super(logger, root);
		this.registry = new HashMap<>(registry);
	}

	@Override
	public void expandPackage(String name, SemanticVersion version, Root packageRoot) throws ResolveError {
		String packageURL = registry.get(name);
		if (packageURL == null) {
			// Unknown package encountered.
			throw new ResolveError("unknown package \"" + name + "\"");
		} else if (packageRoot instanceof DirectoryRoot) {
			DirectoryRoot dir = (DirectoryRoot) packageRoot;
			// Download package from git and install into this directory root.
			try {
				Git.cloneRepository().setURI(packageURL).setDirectory(dir.location()).call();
			} catch (InvalidRemoteException e) {
				throw new ResolveError(e.getMessage(), e);
			} catch (TransportException e) {
				throw new ResolveError(e.getMessage(), e);
			} catch (GitAPIException e) {
				throw new ResolveError(e.getMessage(), e);
			}
			// Done
		} else {
			throw new ResolveError("unable to cope with non-directory package root");
		}
	}
}
