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
		String methodName = "set" + capitalise(name);
		try {
			Method m = findMethod(this.getClass(), methodName);
			if (m != null && value != null) {
				m.invoke(this, value);
				return;
			} else if (m != null) {
				m.invoke(this);
				return;
			}
		} catch (SecurityException e) {
			throw new IllegalArgumentException(e);
		} catch (IllegalAccessException e) {
			throw new IllegalArgumentException(e);
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException(e);
		} catch (InvocationTargetException e) {
			throw new IllegalArgumentException(e);
		}
		throw new IllegalArgumentException("No such method: " + methodName);
	}

	@Override
	public Object get(String name) {
		String methodName = "get" + capitalise(name);
		try {
			Method m = findMethod(this.getClass(), methodName);
			if (m != null) {
				return m.invoke(this);
			}
		} catch (SecurityException e) {
			throw new IllegalArgumentException(e);
		} catch (IllegalAccessException e) {
			throw new IllegalArgumentException(e);
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException(e);
		} catch (InvocationTargetException e) {
			throw new IllegalArgumentException(e);
		}
		throw new IllegalArgumentException("No such method: " + methodName);
	}

	@Override
	public String describe(String name) {
		try {
			String methodName = "describe" + capitalise(name);
			Method m = findMethod(this.getClass(), methodName);
			if (m == null) {
				throw new IllegalArgumentException("No such method: " + methodName);
			} else {
				return (String) m.invoke(this);
			}
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

	/**
	 * Traverse the class hierarchy looking for a method with a specific name.
	 * This starts by looking in the given class for a method with the given
	 * name. If no such method is found, then it looks in the super class. Note
	 * that it doesn't look into interfaces however.
	 *
	 * @param clazz
	 * @param name
	 * @return
	 */
	private static Method findMethod(Class<?> clazz, String name) {
		try {
			if (clazz != null) {
				for (Method m : clazz.getMethods()) {
					if (m.getName().equals(name)) {
						return m;
					}
				}

				return findMethod(clazz.getSuperclass(), name);
			} else {
				// didn't find it
				return null;
			}
		} catch (SecurityException e) {
			throw new IllegalArgumentException(e);
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException(e);
		}
	}
}
