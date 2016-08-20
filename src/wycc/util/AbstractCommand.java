package wycc.util;

import wycc.lang.Command;

public abstract class AbstractCommand extends AbstractConfigurable implements Command {

	public AbstractCommand(String... options) {
		super(options);
	}
	
	@Override
	public String getName() {
		String name = this.getClass().getSimpleName(); 
		return name.toLowerCase();
	}

}
