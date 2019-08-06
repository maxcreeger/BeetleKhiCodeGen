package com.beetlekhi.linker;

import com.beetlekhi.module.Command;
import com.beetlekhi.process.ExecuteCommand;

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