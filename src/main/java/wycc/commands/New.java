package wycc.commands;

import java.util.Map;

import wycc.util.AbstractProjectCommand;
import wycc.util.Logger;
import wyfs.lang.Content.Registry;

/**
 * Responsible for creating an initially empty project.
 *
 * @author David J. Pearce
 *
 */
public class New extends AbstractProjectCommand<New.Result> {

	/**
	 * Result kind for this command
	 *
	 */
	public enum Result {
		SUCCESS,
		ERRORS,
		INTERNAL_FAILURE
	}

	public New(Registry registry, Logger logger) {
		super(registry, logger);
		// TODO Auto-generated constructor stub
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Result execute(String... args) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected void finaliseConfiguration(Map<String, Object> configuration) {
		// TODO Auto-generated method stub

	}
}
