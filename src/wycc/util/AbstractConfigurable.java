package wycc.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import wycc.lang.Feature;

/**
 * Provides a simple mechanism for implementing the configurable interface.
 * Specifically, this employs reflection to identify appropriate getters/setters
 * which are then considered part of the configuration.
 * 
 * @author David J. Pearce
 *
 */
public abstract class AbstractConfigurable implements Feature.Configurable {
	private String[] options;
	
	public AbstractConfigurable(String... options) {
		this.options = options;
	}
	
	@Override
	public String[] getOptions() {
		return options;
	}

	@Override
	public void set(String name, Object value) {
		try {
			Method m = this.getClass().getMethod("set" + capitalise(name));
			if(value != null) {
				m.invoke(this,value);
			} else {
				m.invoke(this);
			}
		} catch (SecurityException e) {
			throw new IllegalArgumentException(e);
		} catch (IllegalAccessException e) {
			throw new IllegalArgumentException(e);
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException(e);
		} catch (NoSuchMethodException e) {
			throw new IllegalArgumentException(e);
		} catch (InvocationTargetException e) {
			throw new IllegalArgumentException(e);
		} 
	}

	@Override
	public Object get(String name) {
		try {
			Method m = this.getClass().getMethod("get" + capitalise(name));
			return m.invoke(this);
		} catch (NoSuchMethodException e) {
			throw new IllegalArgumentException(e);
		} catch (SecurityException e) {
			throw new IllegalArgumentException(e);
		} catch (IllegalAccessException e) {
			throw new IllegalArgumentException(e);
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException(e);
		} catch (InvocationTargetException e) {
			throw new IllegalArgumentException(e);
		}
	}
	
	@Override
	public String describe(String name) {
		try {			
			Method m = this.getClass().getMethod("describe" + capitalise(name));
			return (String) m.invoke(this);
		} catch (NoSuchMethodException e) {
			throw new IllegalArgumentException(e);
		} catch (SecurityException e) {
			throw new IllegalArgumentException(e);
		} catch (IllegalAccessException e) {
			throw new IllegalArgumentException(e);
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException(e);
		} catch (InvocationTargetException e) {
			throw new IllegalArgumentException(e);
		}
	}
	
	/**
	 * Make the first letter of the string a captial.
	 * @param str
	 * @return
	 */
	private static String capitalise(String str) {
		String rest = str.substring(1);
		char c = Character.toUpperCase(str.charAt(0));
		return c + rest;
}
}
