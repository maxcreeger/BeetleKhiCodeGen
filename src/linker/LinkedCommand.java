package linker;

import test.beetlekhi.Command;
import test.beetlekhi.ExecuteCommand;

public class LinkedCommand {

	protected final LinkedOperation linkedOperation;
	LinkedNode target;
	ExecuteCommand exec;
	Command command;
	LinkedAttributes commandAttributes;

	public LinkedCommand(LinkedOperation linkedOperation, ExecuteCommand exec, Command command, LinkedNode target) {
		super();
		this.linkedOperation = linkedOperation;
		this.exec = exec;
		this.command = command;
		this.linkedOperation.linkedCommands.add(this);
		this.target = target;
	}
}