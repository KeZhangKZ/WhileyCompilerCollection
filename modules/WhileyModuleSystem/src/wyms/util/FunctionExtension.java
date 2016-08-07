package wyms.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import wyms.lang.Feature;
import wyms.lang.Plugin;

public class FunctionExtension implements Feature {
	private Plugin receiver;
	private Method method;

	public FunctionExtension(Plugin receiver, String name, java.lang.Class... parameters) {
		this.receiver = receiver;
		try {
			this.method = receiver.getClass().getMethod(name, parameters);
		} catch (Exception e) {
			throw new RuntimeException("No such method: " + name, e);
		}
	}

	@Override
	public String name() {
		return method.getName();
	}

	@Override
	public String description() {
		return null;
	}

	private static final ArrayList<FunctionExtension> functions = new ArrayList<FunctionExtension>();

	public static void register(FunctionExtension fe) {
		functions.add(fe);
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
		FunctionExtension fn = null;
		for (FunctionExtension f : functions) {
			if (f.method.getName().equals(name)) {
				fn = f;
				break;
			}
		}
		if (fn != null) {
			try {
				return fn.method.invoke(fn.receiver, arguments);
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
