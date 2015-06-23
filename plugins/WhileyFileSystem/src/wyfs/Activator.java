package wyfs;

import java.io.File;
import java.lang.reflect.Method;
import java.util.List;

import jplug.lang.PluginActivator;
import jplug.lang.PluginContext;
import wycc.util.FunctionExtension;
import wyfs.lang.Content;
import wyfs.util.DefaultContentRegistry;

/**
 * The activator for the Whiley FIle System plugin. This doesn't really do much!
 *
 * @author David J. Pearce
 *
 */
public class Activator implements PluginActivator 	{

	private static DefaultContentRegistry registry = new DefaultContentRegistry();

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
				return new FunctionExtension(this.getClass(),"getContentType",String.class);
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
	public static Content.Registry getContentRegistry() {
		return registry;
	}
}
