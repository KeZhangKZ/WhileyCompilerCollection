package wyps.lang;

/**
 * Represents a dependency from one plugin to another.  
 * 
 * @author David J. Pearce
 * 
 */
public class SemanticDependency {
	/**
	 * The unique plugin identifier.
	 */
	private String id;
	
	/**
	 * The minimum version number permitted, or null if no lower bound.
	 */
	private SemanticVersion minVersion;
	
	/**
	 * The maximum version number permitted, or null if no upper bound.
	 */
	private SemanticVersion maxVersion;
	
	public SemanticDependency(String id, SemanticVersion min, SemanticVersion max) {
		this.id = id;
		this.minVersion = min;
		this.maxVersion = max;
	}
	
	public String getId() {
		return id;
	}
	
	public boolean matches(String id, SemanticVersion version) {
		return this.id.equals(id)
				&& (this.minVersion == null || this.minVersion
						.compareTo(version) <= 0)
				&& (this.maxVersion == null || this.maxVersion
						.compareTo(version) >= 0);
	}
	
	public String toString() {
		String min = minVersion != null ? minVersion.toString() : "_";
		String max = maxVersion != null ? maxVersion.toString() : "_";			
		return id + "[" + min + "," + max + "]";
	}
}