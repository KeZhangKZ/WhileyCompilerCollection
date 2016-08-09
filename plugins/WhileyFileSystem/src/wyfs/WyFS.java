package wyfs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import wyfs.lang.Content;
import wyfs.lang.Path;
import wyms.lang.Feature;
import wyms.lang.Module;

/**
 * Represents an instance of the WhileyBuildSystem module, and contains all
 * state local to a given instance.
 * 
 * @author David J. Pearce
 *
 */
public class WyFS implements Module {

	/**
	 * The features map contains those features registered by this module.  
	 */
	private HashMap<String,Feature> features = new HashMap<String,Feature>();
	
	/**
	 * The content registry maintained by this module
	 */
	private Registry registry = new Registry();
	
	// ========================================================================
	// Accessors
	// ========================================================================

	@Override
	public Feature get(String... id) {
		return features.get(id);
	}

	@Override
	public Collection<Feature> features() {
		return features.values();
	}

	public Content.Registry getContentRegistry() {
		return registry;
	}
	
	// ========================================================================
	// Activation
	// ========================================================================

	private void start(final Module.Context context) {
		registerContentTypeExtensionPoint(context);
	}

	private <T> void registerContentTypeExtensionPoint(final Module.Context context) {
		context.create(Content.Type.class, new Module.ExtensionPoint<Content.Type>() {
			@Override
			public void register(Content.Type contentType) {
				registry.register(contentType);
				context.logTimedMessage("Registered content type: " + contentType.getClass().getName(), 0, 0);
			}
		});
	}

	public static class Activator implements Module.Activator {

		public Module start(final Module.Context context) {
			final WyFS thisModule = new WyFS();			
			thisModule.start(context);			
			return thisModule;
		}

		public void stop(Module module, Module.Context context) {
			// Nothing really to do for this module
		}
	}

	// ========================================================================
	// Content Registry
	// ========================================================================

	/**
	 * Default implementation of a content registry. This associates whiley and
	 * wyil files with their respective content types.
	 *
	 * @author David J. Pearce
	 *
	 */
	public static class Registry implements Content.Registry {
		private ArrayList<Content.Type<?>> types = new ArrayList<>();

		@Override
		public void associate(Path.Entry<?> e) {
			String suffix = e.suffix();
			for (Content.Type t : types) {
				if (suffix.equals(t.getSuffix())) {
					e.associate(t, null);
				}
			}
		}
		@Override
		public String suffix(Content.Type<?> t) {
			for (Content.Type<?> ct : types) {
				if (t == ct) {
					return ct.getSuffix();
				}
			}
			return null;
		}

		private void register(Content.Type<?> ct) {
			types.add(ct);
		}
	}
}
