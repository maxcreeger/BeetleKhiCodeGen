package linker;

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

}