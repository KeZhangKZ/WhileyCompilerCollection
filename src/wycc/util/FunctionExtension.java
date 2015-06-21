package wycc.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import jplug.lang.PluginContext;

public class FunctionExtension implements PluginContext.Extension {
	private Method method;
	
	public FunctionExtension(Class receiver, String name, Class... parameters) {
		
		try {
			this.method = receiver.getMethod(name, parameters);
		} catch (Exception e) {
			throw new RuntimeException("No such method: " + name, e);
		}
	}

	@Override
	public Object data() {
		return method;
	}
	
	private static final ArrayList<Method> functions = new ArrayList<Method>();
	
	public static void register(FunctionExtension fe) {
		functions.add(fe.method);
	}
	
	/**
	 * Invoke the builder main function, which should have been registered by
	 * the build system plugin.
	 *
	 * @param functions
	 * @param target
	 * @param outputDirectory
	 * @param libraries
	 */
	public static Object invoke(String name, Object... arguments) {
		Method method = null;
		for (Method m : functions) {
			if (m.getName().equals(name)) {
				method = m;
				break;
			}
		}
		if (method != null) {
			try {
				return method.invoke(null, arguments);
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			}
		} else {
			System.out.println("Error: unable to find method \"" + name + "\"");
		}
		return null;
	}
}
