package wyms.util;

import java.util.HashMap;
import wyms.lang.Feature;

public class StdConfiguration implements Feature.Configuration {
	private final Feature.Schema schema;
	private final HashMap<String, Feature.Value> values = new HashMap<String, Feature.Value>();

	public StdConfiguration(Feature.Schema schema) {
		this.schema = schema;
	}
	
	/**
	 * Set a given attribute to a given value in this configuration.
	 * 
	 * @param name
	 *            Name of the attribute in question
	 * @param value
	 *            Value to which the attribute is set
	 */
	public void set(java.lang.String name, Feature.Value value) {
		schema.checkValid(name,value);
		values.put(name,value);
	}

	/**
	 * Get the value assigned to a given attribute in this configuration.
	 * 
	 * @param name
	 *            Name of the attribute in question
	 * @param value
	 *            Value to which the attribute is set
	 */
	@Override
	public Feature.Value get(String name) {
		return values.get(name);
	}
}
