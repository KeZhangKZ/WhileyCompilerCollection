package wycc.lang;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import wybs.lang.SyntacticElement;
import wybs.util.AbstractCompilationUnit;
import wycc.io.BuildFileLexer;
import wycc.io.BuildFileParser;
import wyfs.lang.Content;
import wyfs.lang.Path;

public class BuildFile extends AbstractCompilationUnit {
	// =========================================================================
	// Content Type
	// =========================================================================

	public static final Content.Type<BuildFile> ContentType = new Content.Type<BuildFile>() {
		public Path.Entry<BuildFile> accept(Path.Entry<?> e) {
			if (e.contentType() == this) {
				return (Path.Entry<BuildFile>) e;
			}
			return null;
		}

		@Override
		public BuildFile read(Path.Entry<BuildFile> e, InputStream inputstream)
				throws IOException {
			BuildFileLexer lexer = new BuildFileLexer(e);
			BuildFileParser parser = new BuildFileParser(e, lexer.scan());
			return parser.read();
		}

		@Override
		public void write(OutputStream output, BuildFile value) {
			// for now
			throw new UnsupportedOperationException();
		}

		@Override
		public String toString() {
			return "Content-Type: whiley";
		}

		@Override
		public String getSuffix() {
			return "wy";
		}
	};

	// =========================================================================
	// Constructors
	// =========================================================================

	private ArrayList<Declaration> declarations;

	public BuildFile(Path.Entry<BuildFile> entry) {
		super(entry);
		this.declarations = new ArrayList<>();
	}

	public interface Declaration {

	}

	public class Section extends SyntacticElement.Impl implements Declaration {
		private final String name;
		private final ArrayList<Declaration> contents;

		public Section(String name) {
			this.name = name;
			this.contents = new ArrayList<>();
		}

		public String getName() {
			return name;
		}

		public List<Declaration> getContents() {
			return contents;
		}
	}

	public class KeyValuePair extends SyntacticElement.Impl implements Declaration {
		private final String key;
		private final String value;

		public KeyValuePair(String key, String value) {
			this.key = key;
			this.value = value;
		}

		public String getKey() {
			return key;
		}

		public String getValue() {
			return value;
		}
	}
}
