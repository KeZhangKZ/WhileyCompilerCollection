package wycc.util;

import java.util.ArrayList;
import java.util.HashMap;

import wycc.lang.PluginContext;

public class DefaultPluginContext implements PluginContext {

	/**
	 * The extension points represent registered implementations of interfaces.
	 * Each extension point represents a class that will be instantiated and
	 * configured, and will contribute to some function within the compiler. The
	 * main extension points are: <i>Routes</i>, <i>Builders</i> and
	 * <i>ContentTypes</i>.
	 */
	public final HashMap<String, ArrayList<Class<?>>> extensionPoints = new HashMap<String, ArrayList<Class<?>>>();

	// ==================================================================
	// Methods
	// ==================================================================
	
	@Override
	public void register(String extension, Class<?> implementation) {
		ArrayList<Class<?>> currentExtensions = extensionPoints.get(extension);
		if(currentExtensions == null) {
			currentExtensions = new ArrayList<Class<?>>();
			extensionPoints.put(extension,currentExtensions);
		}
		currentExtensions.add(implementation);
	}

	@Override
	public void create(String extension, ExtensionPoint ep) {
		// TODO Auto-generated method stub
		
	}

	
}
