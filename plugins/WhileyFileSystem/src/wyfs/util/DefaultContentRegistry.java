package wyfs.util;

import java.util.HashMap;
import java.util.Map;

import wyfs.lang.Content;
import wyfs.lang.Path;

/**
 * Default implementation of a content registry. This associates a given set
 * of content types and suffixes. The intention is that plugins register new
 * content types and these will end up here.
 * 
 * @author David J. Pearce
 * 
 */
public class DefaultContentRegistry implements Content.Registry {
	private HashMap<String, Content.Type> contentTypes = new HashMap<String, Content.Type>();
	
	public void register(Content.Type contentType, String suffix) {
		contentTypes.put(suffix, contentType);
	}
	
	public void unregister(Content.Type contentType, String suffix) {
		contentTypes.remove(suffix);
	}
	
	public void associate(Path.Entry e) {
		String suffix = e.suffix();
		Content.Type ct = contentTypes.get(suffix);
		if (ct != null) {
			e.associate(ct, null);
		}
	}
	
	public String suffix(Content.Type<?> t) {
		for (Map.Entry<String, Content.Type> p : contentTypes.entrySet()) {
			if (p.getValue() == t) {
				return p.getKey();
			}
		}
		// Couldn't find it!
		return null;
	}
}
