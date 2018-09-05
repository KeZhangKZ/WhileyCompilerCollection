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
package wycc.commands;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import wybs.util.AbstractCompilationUnit.Value;
import wycc.WyProject;
import wycc.cfg.Configuration;
import wycc.cfg.Configuration.Schema;
import wycc.lang.Command;
import wycc.util.ZipFile;
import wyfs.lang.Content;
import wyfs.lang.Path;
import wyfs.util.Trie;
import wybs.lang.Build;

public class Install implements Command {
	public static final Trie BUILD_INCLUDES = Trie.fromString("build/includes");

	/**
	 * The descriptor for this command.
	 */
	public static final Command.Descriptor DESCRIPTOR = new Command.Descriptor() {
		@Override
		public String getName() {
			return "install";
		}

		@Override
		public String getDescription() {
			return "Install package into local repository";
		}

		@Override
		public List<Option.Descriptor> getOptionDescriptors() {
			return Collections.EMPTY_LIST;
		}

		@Override
		public Schema getConfigurationSchema() {
			return Configuration.EMPTY_SCHEMA;
		}

		@Override
		public List<Descriptor> getCommands() {
			return Collections.EMPTY_LIST;
		}

		@Override
		public Command initialise(Command environment, Command.Options options, Configuration configuration) {
			return new Install((WyProject) environment, options, configuration, System.out, System.err);
		}

	};

	/**
	 * Provides a generic place to which normal output should be directed. This
	 * should eventually be replaced.
	 */
	private final PrintStream sysout;

	/**
	 * Provides a generic place to which error output should be directed. This
	 * should eventually be replaced.
	 */
	private final PrintStream syserr;

	/**
	 * The enclosing project for this build
	 */
	private final WyProject project;

	/**
	 * Access to configuration attributes
	 */
	private final Configuration configuration;

	/**
	 * List of include filters
	 */
	private final Value.UTF8[] includes;

	public Install(WyProject project, Command.Options options, Configuration configuration, OutputStream sysout,
			OutputStream syserr) {
		this.project = project;
		this.sysout = new PrintStream(sysout);
		this.syserr = new PrintStream(syserr);
		this.configuration = configuration;
		this.includes = configuration.get(Value.Array.class, BUILD_INCLUDES).toArray(Value.UTF8.class);
	}

	@Override
	public Descriptor getDescriptor() {
		return DESCRIPTOR;
	}

	@Override
	public void initialise() {
		// Nothing to do here?
	}

	@Override
	public void finalise() {
		// Nothing to do here?
	}

	@Override
	public boolean execute(List<String> args) {
		try {
			// Determine list of files to go in package
			List<Path.Entry<?>> files = determinePackageContents();
			// Construct zip file context representing package
			ZipFile zf = createPackageBytes(files);
			// Determine the target location for the package file
			Path.Entry<ZipFile> target = getPackageFile();
			// Physically write out the zip file
			target.write(zf);
			// Flush it to disk
			target.flush();
			// Done
			System.out.println("WROTE: " + files);
			return true;
		} catch (IOException e) {
			e.printStackTrace(syserr);
			return false;
		}
	}

	private List<Path.Entry<?>> determinePackageContents() throws IOException {
		// Determine includes filter
		Content.Filter includes = createIncludesFilter();
		//
		ArrayList<Path.Entry<?>> files = new ArrayList<>();
		// Determine local root of project
		Path.Root root = project.getParent().getLocalRoot();
		// Determine active build platforms
		List<Build.Platform> platforms = project.getTargetPlatforms();
		// Determine roots for all platforms
		for (int i = 0; i != platforms.size(); ++i) {
			Build.Platform platform = platforms.get(i);
			// Determine binary root for platform
			Path.Root bindir = platform.getTargetRoot(root);
			// Target filter matches compiled files
			Content.Filter<?> filter = Content.and(includes,platform.getTargetFilter());
			// Find all files
			files.addAll(bindir.get(filter));
		}
		return files;
	}

	private ZipFile createPackageBytes(List<Path.Entry<?>> files) throws IOException {
		ZipFile zf = new ZipFile();
		// Add each file to zip file
		for (int i = 0; i != files.size(); ++i) {
			Path.Entry<?> file = files.get(i);
			// Construct filename for given entry
			String filename = file.id().toString() + "." + file.contentType().getSuffix();
			// Extract bytes representing entry
			byte[] contents = readFileContents(file);
			zf.add(new ZipEntry(filename), contents);
		}
		//
		return zf;
	}

	private byte[] readFileContents(Path.Entry<?> file) throws IOException {
		InputStream in = file.inputStream();
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		int nRead;
		// Read bytes in max 1024 chunks
		byte[] data = new byte[1024];
		// Read all bytes from the input stream
		while ((nRead = in.read(data, 0, data.length)) != -1) {
			buffer.write(data, 0, nRead);
		}
		// Done
		buffer.flush();
		return buffer.toByteArray();
	}

	private Path.Entry<ZipFile> getPackageFile() throws IOException {
		// Extract package name from configuration
		Value.UTF8 name = configuration.get(Value.UTF8.class, Trie.fromString("package/name"));
		// Extract package version from
		Value.UTF8 version = configuration.get(Value.UTF8.class, Trie.fromString("package/version"));
		// Determine fully qualified package name
		Trie pkg = Trie.fromString(name + "-v" + version);
		// Dig out the file!
		return project.getRepositoryRoot().create(pkg, ZipFile.ContentType);
	}

	private Content.Filter<?> createIncludesFilter() {
		Content.Filter filter = null;
		for(int i=0;i!=includes.length;++i) {
			Content.Filter f = createFilter(includes[i].toString());
			if(filter == null) {
				filter = f;
			} else {
				filter = Content.or(filter, f);
			}
		}
		return filter;
	}

	private Content.Filter createFilter(String filter) {
		String[] split = filter.split("\\.");
		//
		Content.Type contentType = getContentType(split[1]);
		//
		return Content.filter(split[0], contentType);
	}

	private Content.Type getContentType(String suffix) {
		List<Content.Type<?>> cts = project.getParent().getContentTypes();
		//
		for (int i = 0; i != cts.size(); ++i) {
			Content.Type<?> ct = cts.get(i);
			if (ct.getSuffix().equals(suffix)) {
				// hit
				return ct;
			}
		}
		// miss
		throw new IllegalArgumentException("unknown content-type: " + suffix);
	}
}
