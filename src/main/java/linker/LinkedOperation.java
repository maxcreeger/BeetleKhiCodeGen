package linker;

import test.beetlekhi.command.Attribute;
import test.beetlekhi.process.ExecuteCommand;
import test.beetlekhi.process.Khiprocess;
import test.beetlekhi.process.Operation;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an {@link Operation} specified by a {@link Khiprocess} in which the relating Triggers, Error handling and
 * Commands are resolved with a matching implementation by a module.
 */
public class LinkedOperation {

    final Operation operation;
    public boolean running;
    private final List<LinkedTrigger> linkedTriggers = new ArrayList<>();
    private final List<LinkedErrorOccurrence> linkedErrors = new ArrayList<>();
    private final List<LinkedCommand> linkedCommands = new ArrayList<>();

    public LinkedOperation(Operation op) {
        this.operation = op;
    }

    public List<LinkedTrigger> getLinkedTriggers() {
        return linkedTriggers;
    } // TODO make immutable

    public List<LinkedErrorOccurrence> getLinkedErrors() {
        return linkedErrors;
    }// TODO make immutable

    public List<LinkedCommand> getLinkedCommands() {
        return linkedCommands;
    }// TODO make immutable

    public void add(LinkedTrigger trigger) {
        linkedTriggers.add(trigger);
    }

    public String getName() {
        return operation.getName();
    }

    public Operation getOperation() {
        return operation;
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