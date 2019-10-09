package wycc.util;

import java.io.IOException;
import java.util.Set;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import wybs.util.AbstractCompilationUnit.Value;
import wycc.cfg.Configuration;
import wycc.lang.Command;
import wycc.lang.Package;
import wycc.lang.Package.Repository;
import wycc.lang.SemanticVersion;
import wyfs.lang.Content;
import wyfs.lang.Path;
import wyfs.util.Trie;
import wyfs.util.ZipFile;

public class RemotePackageRepository extends LocalPackageRepository {

	public static final Trie REPOSITORY_URL = Trie.fromString("repository/url");
	public static final Trie REPOSITORY_ROUTE = Trie.fromString("repository/route");
	public static final Trie REPOSITORY_COOKIE = Trie.fromString("repository/cookie");

	/**
	 * Schema for global configuration (i.e. which applies to all projects for a given user).
	 */
	public static Configuration.Schema REMOTE_REPOSITORY_SCHEMA = Configuration.fromArray(
			Configuration.UNBOUND_STRING(REPOSITORY_URL, "remote url", false),
			Configuration.UNBOUND_STRING(REPOSITORY_ROUTE, "remote route (template)", false),
			Configuration.UNBOUND_STRING(REPOSITORY_COOKIE, "remote cookie (for authentication)", false));

	/**
	 * The route defines a template from which to construct the complete url to the
	 * given package. Specifically, the variables <code>${NAME}$</code> and
	 * <code>${VERSION}</code> are replaced accordingly.
	 */
	private String route = "/${NAME}/${VERSION}/${NAME}-v${VERSION}.zip";
	/**
	 * The URI defines the base location from which to construct the complete URL to
	 * request the package from.
	 */
	private String uri = "https://github.com/Whiley/Repository/raw/master";
	/**
	 * The Cookie (if given) will be added to all HTTP requests.
	 */
	private String cookie = null;

	public RemotePackageRepository(Command.Environment environment,Content.Registry registry, Path.Root root) {
		this(environment,null,registry,root);
	}

	public RemotePackageRepository(Command.Environment environment, Package.Repository parent, Content.Registry registry, Path.Root root) {
		super(environment,parent,registry,root);
		// Check whether URL configuration given
		if(environment.hasKey(REPOSITORY_URL)) {
			this.uri = environment.get(Value.UTF8.class, REPOSITORY_URL).toString();
		}
		// Check whether route configuration given
		if(environment.hasKey(REPOSITORY_ROUTE)) {
			this.route = environment.get(Value.UTF8.class, REPOSITORY_ROUTE).toString();
		}
		// Check whether cookie configuration given
		if(environment.hasKey(REPOSITORY_COOKIE)) {
			this.cookie = environment.get(Value.UTF8.class, REPOSITORY_COOKIE).toString();
		}
	}

	@Override
	public Set<SemanticVersion> list(String pkg) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public AbstractPackage get(String name, SemanticVersion version) throws IOException {
		// Check for local version of this package
		AbstractPackage pkg = super.get(name, version);
		// Did we find it?
		if (pkg == null) {
			// Nope, so get from remote
			ZipFile zf = getRemote(name, version);
			//
			if (zf != null) {
				// Store in local repository
				super.put(zf, name, version);
				// Read it out
				return super.get(name, version);
			}
		}
		// Done
		return pkg;
	}

	@Override
	public void put(ZipFile pkg, String name, SemanticVersion version) throws IOException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	/**
	 * Attempt to load a given package from the remote repository. This may fail if
	 * no such package exists.
	 *
	 * @param name
	 * @param version
	 * @return
	 * @throws UnsupportedOperationException
	 * @throws IOException
	 */
	private ZipFile getRemote(String name, SemanticVersion version) throws UnsupportedOperationException, IOException {
		String url = uri + route.replace("${NAME}", name).replace("${VERSION}", version.toString());
		// NOTE: connection pooling might be a better idea for performance reasons.
		CloseableHttpClient httpclient = HttpClients.createDefault();
		HttpGet httpget = new HttpGet(url);
		// Configure get request (if necessary)
		if(cookie != null) {
			httpget.addHeader("Cookie", cookie);
		}
		// Now perform the request
		CloseableHttpResponse response = httpclient.execute(httpget);
		try {
			if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				environment.getLogger().logTimedMessage("Downloaded " + url, 0, 0);
				return new ZipFile(response.getEntity().getContent());
			} else {
				environment.getLogger().logTimedMessage("Failed downloading " + url, 0, 0);
				return null;
			}
		} finally {
			response.close();
		}
	}
}
