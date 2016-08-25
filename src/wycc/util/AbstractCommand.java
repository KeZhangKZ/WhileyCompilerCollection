package wycc.util;

import wycc.lang.Command;

public abstract class AbstractCommand<T> extends AbstractConfigurable implements Command<T> {

	public AbstractCommand(String... options) {
		super(options);
	}
	
	@Override
	public String getName() {
		String name = this.getClass().getSimpleName(); 
		return name.toLowerCase();
	}

}
