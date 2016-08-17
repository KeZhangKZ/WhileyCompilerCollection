package wycc.util;

import java.util.HashMap;
import java.util.Map;

import wycc.lang.Feature;

public class StdSchema implements Feature.Schema {
	private HashMap<String,Feature.Kind> schema;
	
	public StdSchema(Map<String,Feature.Kind> schema) {
		this.schema = new HashMap<String,Feature.Kind>(schema);
	}
	
	@Override
	public Feature.Kind get(String name) { 
		return schema.get(name);
	}

	@Override
	public void checkValid(String name, Feature.Data value) {
		Feature.Kind type = schema.get(name);
		if(type == null) {
			throw new IllegalArgumentException("no such attribute: " + name);
		} else if(!type.accept(value)) {
			throw new IllegalArgumentException("invalid value type for attribute: " + name + ", should have type " + type);
		} 
	}

}
