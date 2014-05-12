package wycc.lang;

import java.util.Map;

/**
 * A plugin configurable is a registered component of a plugin which can be
 * configured. Every configurable has a name, and provides a configure function.
 * 
 * @author David J. Pearce
 * 
 */
public interface PluginConfigurable {
	
	/**
	 * Get the fully qualified name of this configurable component.
	 * 
	 * @return
	 */
	public String getId();

	/**
	 * Get the short name of this configurable component. The short name is more
	 * convenient to use. However, there is a great risk of a name clash arising
	 * and, in such case, the fully qualified name must be used.
	 * 
	 * @return
	 */
	public String getShortId();
	
	/**
	 * Return the configuration template. That is, the map of attributes and
	 * their permitted values for this configurable component.
	 * 
	 * @return
	 */
	public Map<String,Class> getTemplate();
	
	/**
	 * Configure this particular configurable component using a given mapping of
	 * attribute names to values.
	 * 
	 * @param values
	 */
	public void configure(Map<String,Object> values);

}
