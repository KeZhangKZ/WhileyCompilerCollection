package wycc.commands;

import java.util.Map;

import wycc.lang.Command.Descriptor;
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
	public Descriptor getDescriptor() {
		return new Descriptor() {
			@Override
			public String getName() {
				return "new";
			}

			@Override
			public String getDescription() {
				return "Create a new Whiley package.";
			}
		};
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
