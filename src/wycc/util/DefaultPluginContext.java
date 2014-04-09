package wycc.util;

import java.util.HashMap;
import wycc.lang.PluginContext;

public class DefaultPluginContext implements PluginContext {

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
	public final HashMap<String, ExtensionPoint> extensionPoints = new HashMap<String, ExtensionPoint>();

	// ==================================================================
	// Methods
	// ==================================================================
	
	public void setLogger(Logger logger) {
		this.logger = logger;
	}
	
	@Override
	public void register(String extension, Object implementation) {
		ExtensionPoint ep = extensionPoints.get(extension);
		if(ep == null) {
			throw new RuntimeException("Missing extension point: " + extension);
		} else {
			ep.register(implementation);
		}
	}

	@Override
	public void create(String extension, ExtensionPoint ep) {		
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
