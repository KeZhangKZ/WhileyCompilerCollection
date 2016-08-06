package wyps.util;

import java.util.HashMap;

import wyps.util.Logger;
import wyps.lang.Feature;
import wyps.lang.Plugin;

public class DefaultPluginContext implements Plugin.Context {

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
	public final HashMap<String, Plugin.ExtensionPoint> extensionPoints = new HashMap<String, Plugin.ExtensionPoint>();

	// ==================================================================
	// Methods
	// ==================================================================

	public void setLogger(Logger logger) {
		this.logger = logger;
	}

	@Override
	public void register(String id, Feature feature) {
		Plugin.ExtensionPoint ep = extensionPoints.get(id);
		if(ep == null) {
			throw new RuntimeException("Missing extension point: " + id);
		} else {
			ep.register(feature);
		}
	}

	@Override
	public void create(String extension, Plugin.ExtensionPoint ep) {
		if(extensionPoints.containsKey(extension)) {
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
