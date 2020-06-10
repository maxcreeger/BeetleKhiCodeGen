package parsing;

import exceptions.*;
import generator.ArduinoProgram;
import generator.ProcessOverview;
import linker.*;
import test.beetlekhi.command.Command;
import test.beetlekhi.module.*;
import test.beetlekhi.process.Error;
import test.beetlekhi.process.*;

import javax.xml.bind.JAXBException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

public class ProcessLinker {

    private final Khiprocess process;
    private final Map<String, Khimodule> moduleLookup;
    private final Map<String, Node> nodeLookup;
    private final Map<String, Operation> operationLookup;

    public ProcessLinker(List<Khimodule> repository, Khiprocess process) throws JAXBException, MissingKhiModuleException, InvalidKhiProcessException,
            InvalidKhiModuleException, UnavailableCommandException, InvalidStateException {
        this.process = process;
        this.moduleLookup = buildModuleLookup(repository);
        this.nodeLookup = buildNodeLookup(process);
        this.operationLookup = buildOperationLookup(process);
    }

    private Map<String, Node> buildNodeLookup(Khiprocess process) throws InvalidKhiProcessException {
        Map<String, Node> moduleMap = new HashMap<>();
        Optional<Nodes> nodes = ArduinoProgram.getClass(process.getNodesOrPlan(), Nodes.class);
        System.out.println("[Reading] Process Nodes:");
        if (nodes.isPresent()) {
            for (Node node : nodes.get().getNode()) {
                if (moduleMap.put(node.getName(), node) != null) {
                    throw new InvalidKhiProcessException("Duplicate Node with name '" + node.getName() + "' in process '" + process.getName() + "'");
                }
                System.out.println("  + '" + node.getName() + "' at bus " + node.getI2Caddress() + " using module '" + node.getModule() + "'");
            }
        }
        return moduleMap;
    }

    private Map<String, Operation> buildOperationLookup(Khiprocess process) throws InvalidKhiProcessException {
        Map<String, Operation> operationMap = new HashMap<>();
        Optional<Plan> plan = ArduinoProgram.getClass(process.getNodesOrPlan(), Plan.class);
        System.out.println("[Reading] Process Operations:");
        if (plan.isPresent()) {
            for (Operation operation : plan.get().getOperations().getOperation()) {
                if (operationMap.put(operation.getName(), operation) != null) {
                    throw new InvalidKhiProcessException("Duplicate Operation with name '" + operation.getName() + "' in process '" + process.getName() + "'");
                }
                System.out.println("  + Operation '" + operation.getName() + "' found with " + operation.getExecuteCommand()
                        .size() + " command(s)");
            }
        }
        return operationMap;
    }

    private static Map<String, Khimodule> buildModuleLookup(List<Khimodule> repository) throws JAXBException, InvalidStateException {
        Map<String, Khimodule> moduleLookup = new HashMap<>();
        System.out.println("[Reading] Modules form the repository:");
        for (Khimodule khimodule : repository) {
            if (moduleLookup.put(khimodule.getName(), khimodule) != null) {
                throw new InvalidStateException("Duplicate Module found with name '" + khimodule.getName() + "'");
            }
            System.out.println("  + " + khimodule.getName());
        }
        return moduleLookup;
    }

    public ProcessOverview assemble() throws MissingKhiModuleException, InvalidKhiProcessException, UnavailableCommandException {
        //Root
        ProcessOverview root = new ProcessOverview(process);

        Map<String, LinkedNode> linkedNodeLookup = linkNodesToModules(root);
        Map<String, LinkedOperation> linkedOperationLookup = prepareOperations(root);

        // Browse operations
        Map<String, LinkedCommand> linkedCommandLookup = new HashMap<>();
        System.out.println("[LINKING] ExecuteCommand to Command:");
        Optional<Plan> plan = ArduinoProgram.getClass(process.getNodesOrPlan(), Plan.class);
        if (plan.isPresent()) {
            for (Operation operation : plan.get()
                    .getOperations()
                    .getOperation()) {
                LinkedOperation linkedOperation = linkedOperationLookup.get(operation.getName());
                // Link khiModule.Command to khiProcess.ExecuteCommand
                System.out.println("  + Operation '" + operation.getName() + "' defines " + operation.getExecuteCommand()
                        .size() + " commands");
                for (ExecuteCommand exec : operation.getExecuteCommand()) {
                    LinkedNode linkedNode = linkedNodeLookup.get(exec.getNode());
                    if (linkedNode == null) {
                        throw new InvalidKhiProcessException("Process '" + process.getName() + "' attempts to execute a command on a node called '"
                                + exec.getNode() + "', but no such node was registered in the process");
                    }
                    System.out.println("  |  + Command '" + exec.getName() + "' sent to node '" + exec.getNode() + "'");

                    // Seek for a corresponding command accepted by the module
                    LinkedCommand linkedCommand = findCommand(linkedOperation, exec, linkedNode);
                    linkedCommandLookup.put(operation.getName() + "_" + exec.getName(), linkedCommand);

                    Command moduleCommand = null;
                    Optional<Communication> communication = ArduinoProgram.getClass(linkedNode.getKhiModule().getCodeOrCommunicationOrHardware(), Communication.class);
                    if (communication.isPresent()) {
                        Optional<Commands> commands = ArduinoProgram.getClass(communication.get().getCommandsOrSensorsOrEvents(), Commands.class);
                        if (commands.isPresent()) {
                            for (Command availableCommand : commands.get().getCommand()) {
                                if (exec.getName()
                                        .equals(availableCommand.getName())) {
                                    moduleCommand = availableCommand;
                                    break;
                                }
                            }
                        }
                    }
                    if (moduleCommand == null) {
                        throw new UnavailableCommandException("Operation '" + operation.getName() + "' requests command '" + exec.getName()
                                + "', but it is not provided by node '" + linkedNode.node.getName() + "' defined by module '" + linkedNode.getKhiModule()
                                .getName() + "'");
                    }
                    LinkedAttributes commandAttributes = new LinkedAttributes(linkedCommand, moduleCommand, exec);
                    // TODO do something with it
                }

                // Build triggers
                if (operation.getTriggers() != null) {
                    prepareTriggers(linkedNodeLookup, linkedOperationLookup, operation, linkedOperation);
                }
            }
        }
        return root;
    }

    private void prepareTriggers(Map<String, LinkedNode> linkedNodeLookup, Map<String, LinkedOperation> linkedOperationLookup, Operation operation,
                                 LinkedOperation linkedOperation) throws InvalidKhiProcessException {
        if (operation.getTriggers()
                .getEventListeners() != null && operation.getTriggers()
                .getEventListeners() != null) {
            for (EventListener eventListener : operation.getTriggers()
                    .getEventListeners()
                    .getEventListener()) {
                // Validate attribute 'node'
                LinkedNode linkedNode = linkedNodeLookup.get(eventListener.getNode());
                if (linkedNode == null) {
                    throw new InvalidKhiProcessException("Process '" + process.getName() + "', operation '" + operation.getName()
                            + "' defines listener '" + eventListener.getName() + " on node '" + eventListener.getNode()
                            + "' but no such node is defined in the process");
                }
                Khimodule module = linkedNode.getKhiModule();

                // Validate attribute 'event'
                Event event = findEvent(operation, eventListener, module);

                // Validate next Operation
                String nextOperationName = eventListener.getValue();
                Operation nextOperation = operationLookup.get(nextOperationName);
                if (nextOperation == null) {
                    throw new InvalidKhiProcessException("Process '" + process.getName() + "', operation '" + operation.getName()
                            + "' defines listener '" + eventListener.getName() + " which triggers the next operation '" + nextOperationName
                            + "', but no such operation is defined in the process");
                }
                LinkedOperation nextLinkedOperation = linkedOperationLookup.get(nextOperationName);
                LinkedTrigger linkedTrigger = new LinkedTrigger(eventListener, event, nextLinkedOperation);
                linkedNode.add(linkedTrigger);

                // Print
                System.out.println("  |  + Event '" + eventListener.getName() + " sent by node '" + eventListener.getNode() + "'");
                System.out.println("  |  | --> Message '" + eventListener.getEvent() + "' triggers Operation '" + eventListener.getValue() + "'");
            }
        }
        if (operation.getTriggers()
                .getErrors() != null && operation.getTriggers()
                .getErrors() != null) {
            for (Error error : operation.getTriggers()
                    .getErrors()
                    .getError()) {
                prepareErrorEvent(linkedNodeLookup, operation, linkedOperation, error);

                // Print
                System.out.println("  |  + Error '" + error.getName() + " sent by node '" + error.getNode() + "'");
                System.out.println("  |  | --> Message '" + error.getEvent() + "' triggers death of the process");

            }
        }
    }

    private void prepareErrorEvent(Map<String, LinkedNode> linkedNodeLookup, Operation operation, LinkedOperation linkedOperation, test.beetlekhi.process.Error error)
            throws InvalidKhiProcessException {
        // Validate attribute 'node'
        LinkedNode linkedNode = linkedNodeLookup.get(error.getNode());
        if (linkedNode == null) {
            throw new InvalidKhiProcessException("Process '" + process.getName() + "', operation '" + operation.getName() + "' defines error '"
                    + error.getName() + " on node '" + error.getNode() + "' but no such node is defined in the process");
        }
        Khimodule module = linkedNode.getKhiModule();

        // Validate attribute 'event'
        String eventName = error.getEvent();
        Event event = null;
        Optional<Communication> communication = ArduinoProgram.getClass(module.getCodeOrCommunicationOrHardware(), Communication.class);
        if (!communication.isPresent()) {
            throw new InvalidKhiProcessException("Process '" + process.getName() + "', operation '" + operation.getName() + "' defines error '"
                    + error.getName() + " for event '" + eventName + "' on node '" + error.getNode() + "' implemented by module '"
                    + module.getName() + "' but this module does not define any Communication at all");
        }
        Optional<Events> events = ArduinoProgram.getClass(communication.get().getCommandsOrSensorsOrEvents(), Events.class);
        if (!events.isPresent()) {
            throw new InvalidKhiProcessException("Process '" + process.getName() + "', operation '" + operation.getName() + "' defines error '"
                    + error.getName() + " for event '" + eventName + "' on node '" + error.getNode() + "' implemented by module '"
                    + module.getName() + "' but this module does not define any Event at all");
        }
        for (Event eventOfModule : events.get().getEvent()) {
            if (eventOfModule.getName().equals(eventName)) {
                event = eventOfModule;
                break;
            }
        }
        if (event == null) {
            throw new InvalidKhiProcessException("Process '" + process.getName() + "', operation '" + operation.getName() + "' defines error '"
                    + error.getName() + " for event '" + eventName + "' on node '" + error.getNode() + "' implemented by module '"
                    + module.getName() + "' but this module does not define an Event by that name");
        }
        new LinkedErrorOccurrence(linkedOperation, error, event);
    }

    private Event findEvent(Operation operation, EventListener eventListener, Khimodule module) throws InvalidKhiProcessException {
        String eventName = eventListener.getEvent();
        Event event = null;
        Optional<Communication> communication = ArduinoProgram.getClass(module.getCodeOrCommunicationOrHardware(), Communication.class);
        if (!communication.isPresent()) {
            throw new InvalidKhiProcessException("Process '" + process.getName() + "', operation '" + operation.getName()
                    + "' defines listener '" + eventListener.getName() + " for event '" + eventName + "' on node '" + eventListener.getNode()
                    + "' implemented by module '" + module.getName() + "' but this module does not define any Communication at all");
        }
        Optional<Events> events = ArduinoProgram.getClass(communication.get().getCommandsOrSensorsOrEvents(), Events.class);
        if (!events.isPresent()) {
            throw new InvalidKhiProcessException("Process '" + process.getName() + "', operation '" + operation.getName()
                    + "' defines listener '" + eventListener.getName() + " for event '" + eventName + "' on node '" + eventListener.getNode()
                    + "' implemented by module '" + module.getName() + "' but this module does not define any Event at all");
        }
        for (Event eventOfModule : events.get().getEvent()) {
            if (eventOfModule.getName()
                    .equals(eventName)) {
                event = eventOfModule;
                break;
            }
        }
        if (event == null) {
            throw new InvalidKhiProcessException("Process '" + process.getName() + "', operation '" + operation.getName()
                    + "' defines listener '" + eventListener.getName() + " for event '" + eventName + "' on node '" + eventListener.getNode()
                    + "' implemented by module '" + module.getName() + "' but this module does not define an event by that name");
        }
        return event;
    }

    private LinkedCommand findCommand(LinkedOperation linkedOperation, ExecuteCommand exec, LinkedNode linkedNode) throws InvalidKhiProcessException {
        LinkedCommand linkedCommand = null;
        Optional<Communication> communication = ArduinoProgram.getClass(linkedNode.getKhiModule().getCodeOrCommunicationOrHardware(), Communication.class);
        if (communication.isPresent()) {
            Optional<Commands> commands = ArduinoProgram.getClass(communication.get().getCommandsOrSensorsOrEvents(), Commands.class);
            if (commands.isPresent()) {
                for (Command command : commands.get().getCommand()) {
                    if (exec.getName().equals(command.getName())) {
                        linkedCommand = new LinkedCommand(linkedOperation, exec, command, linkedNode);
                        linkedNode.add(linkedCommand);
                        break;
                    }
                }
            }
        }
        if (linkedCommand == null) {
            throw new InvalidKhiProcessException("Process '" + process.getName() + "' attempts to execute a command called'" + exec.getName() + " on node '"
                    + exec.getNode() + "' implemented by module '" + linkedNode.getKhiModule()
                    .getName() + "', but this modules defines no such command");
        }
        return linkedCommand;
    }

    private Map<String, LinkedOperation> prepareOperations(ProcessOverview root) throws InvalidKhiProcessException {
        // Prepare operations
        Optional<Plan> plan = ArduinoProgram.getClass(process.getNodesOrPlan(), Plan.class);
        if (!plan.isPresent()) {
            throw new InvalidKhiProcessException("The process '" + process.getName() + "' has no Plan");
        }
        String initialOperationName = plan.get().getInitial();
        System.out.println("[LINKING] Operations:");
        Map<String, LinkedOperation> linkedOperationLookup = new HashMap<>();
        for (Operation operation : plan.get()
                .getOperations()
                .getOperation()) {
            LinkedOperation linkedOperation = new LinkedOperation(root, operation);
            linkedOperationLookup.put(operation.getName(), linkedOperation);
            System.out.println("  + Operation '" + operation.getName() + (operation.getName()
                    .equals(initialOperationName) ? " (Initial)" : ""));
            root.setInitialOperation(linkedOperationLookup.get(initialOperationName));
        }
        Operation initialOperation = operationLookup.get(initialOperationName);
        if (initialOperation == null) {
            throw new InvalidKhiProcessException("The initial phase of process '" + process.getName() + "' specifies an initial operation '"
                    + initialOperationName + "' which does not exist in the plan");
        }
        return linkedOperationLookup;
    }

    private Map<String, LinkedNode> linkNodesToModules(ProcessOverview root) throws MissingKhiModuleException {
        // Link nodes to their modules
        System.out.println("[LINKING] Nodes to Modules:");
        Map<Node, Khimodule> node2moduleLookup = new HashMap<>();
        Map<String, LinkedNode> linkedNodeLookup = new HashMap<>();
        for (Entry<String, Node> nodeEntry : nodeLookup.entrySet()) {
            Node node = nodeEntry.getValue();
            Khimodule module = moduleLookup.get(node.getModule());
            if (module == null) {
                throw new MissingKhiModuleException("The Node '" + nodeEntry.getKey() + "' requires a module with name '" + node.getModule()
                        + "', but none was found in the repository");
            }
            node2moduleLookup.put(node, module);
            LinkedNode linkedNode = new LinkedNode(root, node, module);
            linkedNodeLookup.put(node.getName(), linkedNode);
            System.out.println("  + Node '" + node.getName() + "' uses module '" + module.getName() + "'");
        }
        return linkedNodeLookup;
    }
}