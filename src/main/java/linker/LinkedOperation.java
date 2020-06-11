package linker;

import generator.ProcessOverview;
import test.beetlekhi.command.Attribute;
import test.beetlekhi.process.ExecuteCommand;
import test.beetlekhi.process.Operation;

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
        builder.append("//").append(operation.getName());
        // Send all commands
        for (ExecuteCommand cmd : operation.getExecuteCommand()) {
            String node = cmd.getNode();
            // Pass arguments
            StringBuilder msg = new StringBuilder();
            String separator = "";
            for (Attribute arg : cmd.getAttributes()
                    .getAttribute()) {
                String rawVal = arg.getValue();
                String paddedVal = String.format("%" + arg.getLength() + "s", rawVal);
                paddedVal = paddedVal.replace(' ', '0');
                msg.append(separator).append(paddedVal);
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
        // TODO in controller, Listen to and expect Triggers to fire
        return builder.toString();
    }

}