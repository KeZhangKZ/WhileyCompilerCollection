package wycc;

import java.io.File;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;

public class Main {
	
	/**
	 * Identifies the location where plugins are stored.
	 */
	public static final String PLUGINS_DIR = "lib/plugins/";
	
	public static void main(String[] args) {
		ArrayList<URL> plugins = new ArrayList<URL>();
		
		File pluginDir = new File(PLUGINS_DIR);
		for(String n : pluginDir.list()) {
			if(n.endsWith(".jar")) {
				try {
					URL url = new File("plugins/" + n).toURI().toURL();
					plugins.add(url);					
					System.out.println("ADDED PLUGIN: " + url);
				} catch(MalformedURLException e) {
					System.err.println("Ignoring plugin: " + n);
				}
			}
		}
				
		URL[] urls = plugins.toArray(new URL[plugins.size()]);
		URLClassLoader loader = new URLClassLoader(urls);
		System.out.println("FOUND: " + loader.findResource("wydefault.Activator"));
		try {
			System.out.println(Arrays.toString(urls));
			Class c = loader.loadClass("wydefault.Activator");
			Method m = c.getMethod("activate");
			Object self = c.newInstance();
			m.invoke(self);			
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
}
