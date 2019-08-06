package com.beetlekhi.linker;

import com.beetlekhi.generator.ProcessOverview;
import com.beetlekhi.process.Attribute;
import com.beetlekhi.process.ExecuteCommand;
import com.beetlekhi.process.Operation;

import java.util.ArrayList;
import java.util.List;

public class LinkedOperation {

    public final ProcessOverview parentProcess;
    final Operation operation;
    public boolean running;
    List<LinkedTrigger> linkedTriggers = new ArrayList<>();
    List<LinkedErrorOccurrence> linkedErrors = new ArrayList<>();
    List<LinkedCommand> linkedCommands = new ArrayList<>();

    public LinkedOperation(ProcessOverview runtimeKhiProcess, Operation op) {
        this.parentProcess = runtimeKhiProcess;
        this.operation = op;
        parentProcess.linkedOperations.add(this);
    }

    public String makeControllerCode() {
        StringBuilder builder = new StringBuilder();
        builder.append("//")
                .append(operation.getName());
        // Send all commands
        for (ExecuteCommand cmd : operation.getExecuteCommand()) {
            String node = cmd.getNode();
            // Pass arguments
            String msg = "";
            String separator = "";
            for (Attribute arg : cmd.getAttributes()
                    .getAttribute()) {
                String rawVal = arg.getValue();
                String paddedVal = String.format("%50s", rawVal);
                paddedVal = paddedVal.replace(' ', '0');
                msg += separator + paddedVal;
                separator = " ";
            }
            // call node's method
            builder.append("\nsendMessage(")
                    .append(node)
                    .append(", \"")
                    .append(msg)
                    .append("\");");
        }
        // Await triggers
        // TODO
        return builder.toString();
    }

}