package wycc.lang;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import wybs.lang.SyntacticElement;
import wybs.util.AbstractCompilationUnit;
import wycc.io.ConfigFileLexer;
import wycc.io.ConfigFileParser;
import wyfs.lang.Content;
import wyfs.lang.Path;

public class ConfigFile extends AbstractCompilationUnit {
	// =========================================================================
	// Content Type
	// =========================================================================

	public static final Content.Type<ConfigFile> ContentType = new Content.Type<ConfigFile>() {
		public Path.Entry<ConfigFile> accept(Path.Entry<?> e) {
			if (e.contentType() == this) {
				return (Path.Entry<ConfigFile>) e;
			}
			return null;
		}

		@Override
		public ConfigFile read(Path.Entry<ConfigFile> e, InputStream inputstream)
				throws IOException {
			ConfigFileLexer lexer = new ConfigFileLexer(e);
			ConfigFileParser parser = new ConfigFileParser(e, lexer.scan());
			return parser.read();
		}

		@Override
		public void write(OutputStream output, ConfigFile value) {
			// for now
			throw new UnsupportedOperationException();
		}

		@Override
		public String toString() {
			return "Content-Type: wy";
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

	public ConfigFile(Path.Entry<ConfigFile> entry) {
		super(entry);
		this.declarations = new ArrayList<>();
	}

	public static interface Declaration {

	}

	public List<Declaration> getDeclarations() {
		return declarations;
	}

	/**
	 * Turn the configuration file into a series of nested maps.
	 *
	 * @return
	 */
	public Map<String,Object> toMap() {
		HashMap<String,Object> map = new HashMap<>();
		toMap(declarations,map);
		return map;
	}

	private static void toMap(List<Declaration> declarations, Map<String, Object> map) {
		for (Declaration d : declarations) {
			if (d instanceof KeyValuePair) {
				KeyValuePair kvp = (KeyValuePair) d;
				map.put(kvp.getKey(), kvp.getValue());
			} else {
				Section s = (Section) d;
				HashMap<String, Object> submap = new HashMap<>();
				toMap(s.contents, submap);
				map.put(s.getName(), submap);
			}
		}
	}

	public static class Section extends SyntacticElement.Impl implements Declaration {
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

	public static class KeyValuePair extends SyntacticElement.Impl implements Declaration {
		private final String key;
		private final Object value;

		public KeyValuePair(String key, Object value) {
			this.key = key;
			this.value = value;
		}

		public String getKey() {
			return key;
		}

		public Object getValue() {
			return value;
		}
	}
}
