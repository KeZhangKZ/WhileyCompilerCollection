package wycc.util;

import wycc.lang.Command;

public abstract class AbstractCommand extends AbstractConfigurable implements Command {

	public AbstractCommand(String... options) {
		super(options);
	}
	
	@Override
	public String getName() {
		return this.getClass().getName();
	}

}
