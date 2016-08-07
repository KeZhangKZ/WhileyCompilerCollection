package wyms.lang;

import java.util.Collection;
import java.util.Map;

/**
 * <p>
 * Represents a registered component provided by a given module. The
 * intuition is that a feature provides some functionality which could be
 * configured, etc. Features essentially form an object hierarchy within the
 * module system, and provide systematic way for users to configure a module
 * system.
 * </p>
 *
 * @author David J. Pearce
 *
 */
public interface Feature {

	/**
	 * Every feature has a unique name. The fully-qualified name of a feature is
	 * constructed from those features which contain it.
	 *
	 * @return
	 */
	public String name();

	/**
	 * Every feature requires a human-readable description. This allows the set
	 * of available features to be interrogated by a user.
	 */
	public String description();

	/**
	 * A feature container is a feature which may contain other "sub-features".
	 *
	 * @author David J. Pearce
	 *
	 */
	public interface Container extends Feature {
		/**
		 * Get a given feature within this container. The feature identifier is
		 * relative to this feature, but may extend into subcontainers.
		 *
		 * @param id
		 * @return
		 */
		public Feature get(String... id);

		/**
		 * Return a collection of the features contained directly within this
		 * feature.
		 *
		 * @return
		 */
		public Collection<Feature> features();
	}

	/**
	 * A configurable feature is one which has settings that may be configured
	 * dynamically. Not all features are configurable (though most are).
	 *
	 * @author David J. Pearce
	 *
	 */
	public interface Configurable extends Feature {
		/**
		 * Return the configuration template. That is, the map of attributes and
		 * their permitted values for this configurable feature.
		 *
		 * @return
		 */
		public Map<String, Template> getTemplate();

		/**
		 * Configure this particular feature using a given mapping of attribute
		 * names to values.
		 *
		 * @param values
		 */
		public void configure(Map<String, Object> values);
	}

	/**
	 * A feature template is a special kind of container, with the intuition that a
	 * feature class can instantiate features dynamically. The configuration of
	 * a feature class will be reflected at the point of creation in its
	 * instances. Furthermore, a feature class "contains" its instances.
	 *
	 * @author David J. Pearce
	 *
	 */
	public interface Template extends Container {
		/**
		 * Create a new instance of this feature class using the current
		 * configuration.
		 *
		 * @return
		 */
		public Instance instantiate();
	}

	/**
	 * A feature instance is a feature which was created dynamically from a
	 * feature class.
	 *
	 * @author David J. Pearce
	 *
	 */
	public interface Instance extends Feature {
	}
}
