package linker;

import test.beetlekhi.command.Command;
import test.beetlekhi.process.ExecuteCommand;

public class LinkedCommand {

    protected final linker.LinkedOperation linkedOperation;
    linker.LinkedNode target;
    ExecuteCommand exec;
    Command command;
    linker.LinkedAttributes commandAttributes;

    public LinkedCommand(linker.LinkedOperation linkedOperation, ExecuteCommand exec, Command command, linker.LinkedNode target) {
        super();
        this.linkedOperation = linkedOperation;
        this.exec = exec;
        this.command = command;
        this.linkedOperation.linkedCommands.add(this);
        this.target = target;
    }
}