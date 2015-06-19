package wyfs;

import java.io.File;
import java.lang.reflect.Method;
import java.util.List;

import jplug.lang.PluginActivator;
import jplug.lang.PluginContext;
import wyfs.lang.Content;
import wyfs.util.DefaultContentRegistry;

/**
 * The activator for the Whiley FIle System plugin. This doesn't really do much!
 *
 * @author David J. Pearce
 *
 */
public class Activator implements PluginActivator 	{

	private DefaultContentRegistry registry = new DefaultContentRegistry();

	public Activator() {

	}

	public void start(final PluginContext context) {

		// ==================================================================
		// Create ContentType extension point
		// ==================================================================
		context.create("wyfs.ContentType", new PluginContext.ExtensionPoint() {

			@Override
			public void register(PluginContext.Extension extension) {
				Content.Type contentType = (Content.Type) extension
						.data();

				// TODO: need to get the suffix out of the content type!!

				registry.register(contentType, null);
				context.logTimedMessage("Registered content type:"
						+ contentType, 0, 0);
			}

		});

		// ==================================================================
		// Register builderMain entry point
		// ==================================================================
		context.register("wycc.functions", new PluginContext.Extension() {
			public Object data() {
				return getMethod("getContentRegistry");
			}
		});
	}

	public void stop(PluginContext context) {

	}

	/**
	 * Get access to the content registry.
	 *
	 * @return
	 */
	public Content.Registry getContentRegistry() {
		return registry;
	}

	/**
	 * This simply returns a reference to a given name. If the method doesn't
	 * exist, then it will throw a runtime exception.
	 *
	 * @param name
	 * @param paramTypes
	 * @return
	 */
	public Method getMethod(String name, Class... paramTypes) {
		try {
			return this.getClass().getMethod(name, paramTypes);
		} catch (Exception e) {
			throw new RuntimeException("No such method: " + name, e);
		}
	}
}
