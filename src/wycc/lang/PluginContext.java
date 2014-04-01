package wycc.lang;

/**
 * A PluginContext provides a mechanism for plugins to interact with their
 * environment. In particular, it allows them to register extension points which
 * provide the critical mechanism for adding new functionality.
 * 
 * @author David J. Pearce
 * 
 */
public interface PluginContext {
	
	public void registerExtension(String extension, Class<?> implementation);
}
