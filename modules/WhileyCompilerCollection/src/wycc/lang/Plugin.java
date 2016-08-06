package wycc.lang;

import wycc.util.Logger;

public interface Plugin extends Feature.Container {

	/**
	 * A Plugin Context provides a mechanism for plugins to interact with their
	 * environment. In particular, it allows them to register extension points which
	 * provide the critical mechanism for adding new functionality.
	 *
	 * @author David J. Pearce
	 *
	 */
	public interface Context extends Logger {

		/**
		 * Responsible for registering a feature as implementing an extension
		 * within the system.
		 *
		 * @param id
		 *            The unique id of the extension point (e.g.
		 *            "wyfs.ContentType").
		 * @param extension
		 *            The implementation of the given extension point.
		 */
		public void register(String id, Feature extension);

		/**
		 * Create a new extension point which subsequent plugins can register
		 * extensions for.
		 *
		 * @param extension
		 * @param ep
		 */
		public void create(String extension, ExtensionPoint ep);
	}

	/**
	 * An extension point in the plugin is a named entity provided by one
	 * plugin, which other plugins can register extensions for.
	 *
	 * @author David J. Pearce
	 *
	 */
	public interface ExtensionPoint {

		/**
		 * Notify extension point that a new extension has been registered for
		 * it.
		 *
		 * @param extension
		 *            The extension implementation to register with this
		 *            extension point.
		 */
		public void register(Feature feature);
	}

	/**
	 * Represents a class designated as the unique "activator" for a given plugin.
	 * This activator is used to control aspects of the plugin (e.g. resources
	 * allocated) as it is started and stopped,
	 *
	 * @author David J. Pearce
	 *
	 */
	public interface Activator {

		/**
		 * This method is called when the plugin is begun. This gives the plugin an
		 * opportunity to register one or more extension points in the compiler.
		 *
		 * @param context
		 */
		public Plugin start(Context context);

		/**
		 * This method is called when the plugin is stopped. Any resources used by
		 * the plugin should be freed at this point. This includes any registered
		 * extension points, which should be unregistered.
		 *
		 * @param context
		 */
		public void stop(Plugin plugin, Context context);
	}
}
