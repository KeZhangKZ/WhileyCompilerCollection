package wycc.lang;

/**
 * A command which can be executed (e.g. from the command-line)
 * 
 * @author David J. Pearce
 *
 */
public interface Command extends Feature.Configurable {
	/**
	 * Get the name of this command. This should uniquely identify the command
	 * in question.
	 * 
	 * @return
	 */
	public String getName();
	
	/**
	 * Get a description of this command.
	 * @return
	 */
	public String getDescription();
	
	/**
	 * Execute this command with the given arguments.
	 */
	public void execute(String... args);
}
