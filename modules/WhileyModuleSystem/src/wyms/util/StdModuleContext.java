package wyms.util;

import java.util.HashMap;

import wycommon.util.Logger;
import wyms.lang.Feature;
import wyms.lang.Module;

public class StdModuleContext implements Module.Context {

	/**
	 * Logging stream, which is null by default.
	 */
	private Logger logger = Logger.NULL;

	/**
	 * The extension points represent registered implementations of interfaces.
	 * Each extension point represents a class that will be instantiated and
	 * configured, and will contribute to some function within the compiler. The
	 * main extension points are: <i>Routes</i>, <i>Builders</i> and
	 * <i>ContentTypes</i>.
	 */
	public final HashMap<Class<?>, Module.ExtensionPoint<?>> extensionPoints = new HashMap<>();

	// ==================================================================
	// Methods
	// ==================================================================

	public void setLogger(Logger logger) {
		this.logger = logger;
	}

	@Override
	public <T extends Feature> void register(Class<T> ep, T feature) {
		Module.ExtensionPoint<T> container = (Module.ExtensionPoint<T>) extensionPoints.get(ep);
		if (ep == null) {
			throw new RuntimeException("Missing extension point: " + ep.getCanonicalName());
		} else {
			container.register(feature);
		}
	}

	@Override
	public <T extends Feature> void create(Class<T> extension, Module.ExtensionPoint<T> ep) {
		if (extensionPoints.containsKey(extension)) {
			throw new RuntimeException("Extension point already exists: " + extension);
		} else {
			extensionPoints.put(extension, ep);
		}
	}

	@Override
	public void logTimedMessage(String msg, long time, long memory) {
		logger.logTimedMessage(msg, time, memory);
	}
}
