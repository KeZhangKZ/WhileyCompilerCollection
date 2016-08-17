package wyms.lang;

import java.util.Arrays;
import java.util.Collection;

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
	// public String name();

	/**
	 * Every feature requires a human-readable description. This allows the set
	 * of available features to be interrogated by a user.
	 */
	// public String description();

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
		public Schema getSchema();

		/**
		 * Configure this particular feature using a given mapping of attribute
		 * names to values.
		 *
		 * @param values
		 */
		public void configure(Configuration values);
	}
	
	/**
	 * Provides a specific configuration for a feature component. Essentially,
	 * it is just a mapping from attribute names to specic values. To be
	 * accepted by the feature, this configuration must be acceptable according
	 * to the feature's schema.
	 * 
	 * @author David J. Pearce
	 *
	 */
	public interface Configuration {
		/**
		 * Set a given attribute to a given value in this configuration.
		 * 
		 * @param name
		 *            Name of the attribute in question
		 * @param value
		 *            Value to which the attribute is set
		 */
		public void set(java.lang.String name, Value value);
		
		/**
		 * Get the value assigned to a given attribute in this configuration.
		 * 
		 * @param name
		 *            Name of the attribute in question
		 * @param value
		 *            Value to which the attribute is set
		 */
		public Value get(java.lang.String name);		
	}
	
	public interface Schema {
		
		/**
		 * Get the type associated with a given attribute name.
		 * 
		 * @param name
		 * @return
		 */
		public Type get(java.lang.String name);
		
		/**
		 * Check whether a given value is acceptable for this schema or not.
		 * 
		 * @param name
		 * @param value
		 */
		public void checkValid(java.lang.String name, Value value);
	}
	
		
	/**
	 * Represents a single value used for configuring a feature.
	 * 
	 * @author David J. Pearce
	 *
	 */
	public abstract static class Value {

		/**
		 * Represents a boolean value used for configuring a feature
		 * 
		 * @author David J. Pearce
		 *
		 */
		public final static class Boolean extends Value {
			private final boolean value;

			public Boolean(boolean value) {
				this.value = value;
			}

			public boolean getValue() {
				return value;
			}

			@Override
			public boolean equals(Object o) {
				if (o instanceof Value.Boolean) {
					Value.Boolean a = (Value.Boolean) o;
					return value == a.value;
				}
				return false;
			}

			@Override
			public int hashCode() {
				return java.lang.Boolean.hashCode(value);
			}
		}

		/**
		 * Represents an integer value used for configuring a feature
		 * 
		 * @author David J. Pearce
		 *
		 */
		public final static class Integer extends Value {
			private final int value;

			public Integer(int value) {
				this.value = value;
			}

			public int getValue() {
				return value;
			}

			@Override
			public boolean equals(Object o) {
				if (o instanceof Value.Integer) {
					Value.Integer a = (Value.Integer) o;
					return value == a.value;
				}
				return false;
			}

			@Override
			public int hashCode() {
				return java.lang.Integer.hashCode(value);
			}
		}

		/**
		 * Represents a string value used for configuring a feature
		 * 
		 * @author David J. Pearce
		 *
		 */
		public static final class String extends Value {
			private final java.lang.String value;

			public String(java.lang.String value) {
				this.value = value;
			}

			public java.lang.String getValue() {
				return value;
			}

			@Override
			public boolean equals(Object o) {
				if (o instanceof Value.String) {
					Value.String a = (Value.String) o;
					return value.equals(a.value);
				}
				return false;
			}

			@Override
			public int hashCode() {
				return value.hashCode();
			}
		}

		/**
		 * Represents an array value used for configuring a feature
		 * 
		 * @author David J. Pearce
		 *
		 */
		public class Array extends Value {
			private final Value[] elements;

			public Array(Value... elements) {
				this.elements = elements;
			}

			public int size() {
				return elements.length;
			}

			public Value getElement(int i) {
				return elements[i];
			}

			@Override
			public boolean equals(Object o) {
				if (o instanceof Value.Array) {
					Value.Array a = (Value.Array) o;
					return Arrays.equals(elements, a.elements);
				}
				return false;
			}

			@Override
			public int hashCode() {
				return Arrays.hashCode(elements);
			}
		}

	}
	
	/**
	 * Represents a single type of value used for configuring a feature.
	 * 
	 * @author David J. Pearce
	 *
	 */
	public abstract static class Type {		
		public abstract boolean accept(Value v);

		/**
		 * The type of boolean configuration values
		 * 
		 * @author David J. Pearce
		 *
		 */		
		public static class Boolean extends Type {
			private Boolean() {
			}

			@Override
			public boolean accept(Value v) {
				return v instanceof Value.Boolean;
			}

			@Override
			public boolean equals(Object o) {
				return o instanceof Boolean;
			}

			@Override
			public int hashCode() {
				return 0;
			}
		}

		/**
		 * The type of integer configuration values
		 * 
		 * @author David J. Pearce
		 *
		 */
		public static class Integer extends Type {
			private Integer() {
			}

			@Override
			public boolean accept(Value v) {
				return v instanceof Value.Integer;
			}

			@Override
			public boolean equals(Object o) {
				return o instanceof Integer;
			}

			@Override
			public int hashCode() {
				return 1;
			}
		}

		/**
		 * The type of string configuration values
		 * 
		 * @author David J. Pearce
		 *
		 */
		public static class String extends Type {
			private String() {
			}

			@Override
			public boolean accept(Value v) {
				return v instanceof Value.String;
			}

			@Override
			public boolean equals(Object o) {
				return o instanceof String;
			}

			@Override
			public int hashCode() {
				return 2;
			}
		}

		/**
		 * The type of array configuration values
		 * 
		 * @author David J. Pearce
		 *
		 */
		public static class Array extends Type {
			private final Type element;

			public Array(Type element) {
				this.element = element;
			}

			public Type getElement() {
				return element;
			}

			@Override
			public boolean accept(Value v) {
				if (v instanceof Value.Array) {
					Value.Array arr = (Value.Array) v;
					for (int i = 0; i != arr.size(); ++i) {
						if (!element.accept(arr.getElement(i))) {
							return false;
						}
					}
					return true;
				}
				return false;
			}

			@Override
			public boolean equals(Object o) {
				if (o instanceof Array) {
					Array a = (Array) o;
					return element.equals(a.element);
				}
				return false;
			}

			@Override
			public int hashCode() {
				int e = element.hashCode();
				return e * e;
			}
		}
	}
}
