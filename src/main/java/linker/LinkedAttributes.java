package linker;

import exceptions.UnavailableCommandException;
import test.beetlekhi.command.Command;
import test.beetlekhi.command.Attribute;
import test.beetlekhi.process.ExecuteCommand;

import java.util.HashMap;
import java.util.Map;

public class LinkedAttributes {
	protected final LinkedCommand linkedCommand;
	public Command moduleCommand;
	public ExecuteCommand exec;
	public Map<String, String> assignments = new HashMap<>();

	public LinkedAttributes(LinkedCommand runtimeCommand, Command moduleCommand, ExecuteCommand exec) throws UnavailableCommandException {
		this.linkedCommand = runtimeCommand;
		this.moduleCommand = moduleCommand;
		this.exec = exec;
		if (exec.getAttributes() != null) {
			exec: for (Attribute execAttribute : exec.getAttributes()
														.getAttribute()) {
				if (moduleCommand.getAttributes() != null) {
					for (test.beetlekhi.command.Attribute availAttribute : moduleCommand.getAttributes()
																	.getAttribute()) {
						if (availAttribute.getName()
											.equals(execAttribute.getName())) {
							assignments.put(execAttribute.getName(), execAttribute.getValue());
							System.out.println("  |  |  + Attribute '" + execAttribute.getName() + "' of type '" + execAttribute.getType() + "' : '"
									+ execAttribute.getValue() + "'");
							continue exec;
						}
					}
					throw new UnavailableCommandException("Operation '" + this.linkedCommand.linkedOperation.operation.getName() + "' requests command '"
							+ moduleCommand.getName() + "' on module '" + exec.getNode() + "' but the command does not accept an attribute named '"
							+ execAttribute.getName() + "'");
				}
			}
		}
		runtimeCommand.commandAttributes = this;
	}
}