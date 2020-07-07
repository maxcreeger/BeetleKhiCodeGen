package parsing;

import beetlkhi.utils.xsd.ElementFilter;
import exceptions.*;
import generator.ProcessOverview;
import linker.*;
import test.beetlekhi.command.Command;
import test.beetlekhi.module.*;
import test.beetlekhi.process.Error;
import test.beetlekhi.process.*;

import javax.xml.bind.JAXBException;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

public class ProcessLinker {

    private final Khiprocess process;
    private final ModuleLinker moduleLinker;
    private final NodeLinker nodeLinker;
    private final OperationLinker operationLinker;
    private final TriggerLinker triggerLinker;
    private final CommandLinker commandLinker;

    public ProcessLinker(List<Khimodule> moduleRepository, Khiprocess process) throws JAXBException, MissingKhiModuleException, InvalidKhiProcessException,
            InvalidKhiModuleException, UnavailableCommandException, InvalidStateException {
        this.process = process;
        this.moduleLinker = new ModuleLinker(moduleRepository);
        this.nodeLinker = new NodeLinker(process);
        this.operationLinker = new OperationLinker(process);
        this.triggerLinker = new TriggerLinker();
        this.commandLinker = new CommandLinker();
    }

    public static class ModuleLinker {

        private final Map<String, Khimodule> moduleLookup;

        public ModuleLinker(List<Khimodule> repository) throws InvalidKhiProcessException, InvalidStateException {
            this.moduleLookup = buildModuleLookup(repository);
        }

        Map<String, Khimodule> buildModuleLookup(List<Khimodule> repository) throws InvalidStateException {
            Map<String, Khimodule> moduleLookup = new HashMap<>();
            System.out.println("[Reading] " + repository.size() + " Modules from the repository:");
            for (Khimodule khimodule : repository) {
                if (moduleLookup.put(khimodule.getName(), khimodule) != null) {
                    throw new InvalidStateException("Duplicate Module found with name '" + khimodule.getName() + "'");
                }
                System.out.println("  + " + khimodule.getName());
            }
            System.out.println();
            return moduleLookup;
        }

        public Map<String, Khimodule> getModuleLookup() {
            return moduleLookup;
        }

        public Khimodule lookupModule(String moduleName) {
            return moduleLookup.get(moduleName);
        }

    }

    public class NodeLinker {

        private final Map<String, Node> nodeLookup;
        private final Map<Node, Khimodule> node2moduleLookup = new HashMap<>();
        private final Map<String, LinkedNode> linkedNodeLookup;

        public NodeLinker(Khiprocess process) throws InvalidKhiProcessException, MissingKhiModuleException {
            this.nodeLookup = buildNodeLookup(process);
            this.linkedNodeLookup = linkNodesToModules();
        }

        Map<String, Node> buildNodeLookup(Khiprocess process) throws InvalidKhiProcessException {
            Map<String, Node> moduleMap = new HashMap<>();
            Optional<Nodes> nodes = ElementFilter.getClass(process.getNodesOrPlan(), Nodes.class);
            if (nodes.isPresent() && !nodes.get().getNode().isEmpty()) {
                System.out.println("[Reading] " + nodes.get().getNode().size() + " Process Nodes:");
                for (Node node : nodes.get().getNode()) {
                    if (moduleMap.put(node.getName(), node) != null) {
                        throw new InvalidKhiProcessException("Duplicate Node with name '" + node.getName() + "' in process '" + process.getName() + "'");
                    }
                    System.out.println("  + '" + node.getName() + "'\t at bus " + node.getI2Caddress() + " using module '" + node.getModule() + "'");
                }
                System.out.println();
            } else {
                System.out.println("[Reading] No nodes in the Process\n");
            }
            return moduleMap;
        }

        Map<String, LinkedNode> linkNodesToModules() throws MissingKhiModuleException {
            // Link nodes to their modules
            System.out.println("[LINKING] " + nodeLookup.size() + " Nodes to " + moduleLinker.getModuleLookup().size() + " Modules:");
            Map<Node, Khimodule> node2moduleLookup = new HashMap<>();
            Map<String, LinkedNode> linkedNodeLookup = new HashMap<>();
            for (Entry<String, Node> nodeEntry : nodeLookup.entrySet()) {
                Node node = nodeEntry.getValue();
                Khimodule module = moduleLinker.getModuleLookup().get(node.getModule());
                if (module == null) {
                    throw new MissingKhiModuleException("The Node '" + nodeEntry.getKey() + "' requires a module with name '" + node.getModule()
                            + "', but no module was found in the repository with that name. Available names: " + moduleLinker.getModuleLookup().keySet());
                }
                node2moduleLookup.put(node, module);
                LinkedNode linkedNode = new LinkedNode(node, module);
                linkedNodeLookup.put(node.getName(), linkedNode);
                System.out.println("  + Node '" + node.getName() + "\t uses module '" + module.getName() + "'");
            }
            System.out.println();
            return linkedNodeLookup;
        }

        public Map<String, Node> getNodeLookup() {
            return nodeLookup;
        }

        public Map<String, LinkedNode> getLinkedNodeLookup() {
            return linkedNodeLookup;
        }

        public Node lookupNode(String nodeName) {
            return nodeLookup.get(nodeName);
        }

        public LinkedNode lookupLinkedNode(String nodeName) {
            return linkedNodeLookup.get(nodeName);
        }

    }

    public class OperationLinker {

        private final Map<String, Operation> operationLookup;
        private final Map<String, LinkedOperation> linkedOperationLookup;
        private final LinkedOperation initialOperation;

        public OperationLinker(Khiprocess process) throws InvalidKhiProcessException {
            Plan plan = ElementFilter.getClass(process.getNodesOrPlan(), Plan.class).orElseThrow(() -> new InvalidKhiProcessException("No Process Operations defined"));
            this.operationLookup = buildOperationLookup(plan);
            this.linkedOperationLookup = buildLinkedOperationLookup();
            String initialOperationName = plan.getInitial();
            this.initialOperation = linkedOperationLookup.get(initialOperationName);
            if (initialOperation == null) {
                throw new InvalidKhiProcessException("Initial Operation named '" + initialOperationName + "' not found");
            } else {
                System.out.println("  + Initial operation: '" + initialOperationName + "'");
            }
            System.out.println();
        }

        Map<String, Operation> buildOperationLookup(Plan plan) throws InvalidKhiProcessException {
            Map<String, Operation> operationMap = new HashMap<>();
            if (plan.getOperations().getOperation().isEmpty()) {
                System.out.println("[Reading] No Process Operations defined");
            } else {
                System.out.println("[Reading] " + plan.getOperations().getOperation().size() + " Process Operations:");
                for (Operation operation : plan.getOperations().getOperation()) {
                    if (operationMap.put(operation.getName(), operation) != null) {
                        throw new InvalidKhiProcessException("Duplicate Operation with name '" + operation.getName() + "' in process '" + process.getName() + "'");
                    }
                    System.out.println("  + Operation '" + operation.getName() + "'\t found with " + operation.getExecuteCommand().size() + " command(s)");
                }
                System.out.println();
            }
            return operationMap;
        }

        Map<String, LinkedOperation> buildLinkedOperationLookup() throws InvalidKhiProcessException {
            // Prepare operations
            Optional<Plan> plan = ElementFilter.getClass(process.getNodesOrPlan(), Plan.class);
            if (!plan.isPresent()) {
                throw new InvalidKhiProcessException("The process '" + process.getName() + "' has no Plan");
            }
            String initialOperationName = plan.get().getInitial();
            System.out.println("[LINKING] " + plan.get().getOperations().getOperation().size() + " Operations:");
            Map<String, LinkedOperation> linkedOperationLookup = new HashMap<>();
            for (Operation operation : plan.get()
                    .getOperations()
                    .getOperation()) {
                LinkedOperation linkedOperation = new LinkedOperation(operation);
                linkedOperationLookup.put(operation.getName(), linkedOperation);
                System.out.println("  + Operation '" + operation.getName());
            }
            Operation initialOperation = operationLookup.get(initialOperationName);
            if (initialOperation == null) {
                throw new InvalidKhiProcessException("The initial phase of process '" + process.getName() + "' specifies an initial operation '"
                        + initialOperationName + "' which does not exist in the plan. Existing operations: " + operationLookup.keySet());
            }
            System.out.println(" -> Initial Operation name: " + initialOperationName);
            return linkedOperationLookup;
        }

        public Map<String, Operation> getOperationLookup() {
            return operationLookup;
        }

        public Operation lookupOperation(String operationName) {
            return operationLookup.get(operationName);
        }

        public Map<String, LinkedOperation> getLinkedOperationLookup() {
            return linkedOperationLookup;
        }

        public LinkedOperation lookupLinkedOperation(String operationName) {
            return linkedOperationLookup.get(operationName);
        }

    }

    class TriggerLinker {

        TriggerLinker() throws InvalidKhiProcessException {
            System.out.println("[LINKING] " + operationLinker.getLinkedOperationLookup().values().stream()
                    .map(LinkedOperation::getOperation)
                    .filter(Objects::nonNull)
                    .map(Operation::getTriggers)
                    .filter(Objects::nonNull)
                    .map(Triggers::getTrigger)
                    .filter(Objects::nonNull)
                    .mapToInt(List::size)
                    .sum() + " Triggers");
            for (LinkedOperation linkedOperation : operationLinker.getLinkedOperationLookup().values()) {
                prepareTriggers(linkedOperation);
            }
            System.out.println();
        }

        void prepareTriggers(LinkedOperation linkedOperation) throws InvalidKhiProcessException {
            Operation operation = linkedOperation.getOperation();
            System.out.println("  + Operation " + operation.getName());
            if (operation.getTriggers() != null && operation.getTriggers().getTrigger() != null) {
                for (Trigger trigger : operation.getTriggers().getTrigger()) {
                    LinkedTrigger linkedTrigger = new LinkedTrigger(trigger, operation.getName(), operationLinker, nodeLinker, process.getName());
                    linkedOperation.add(linkedTrigger);
                }
            }
            if (operation.getTriggers() != null && operation.getTriggers()
                    .getErrors() != null) {
                for (Error error : operation.getTriggers()
                        .getErrors()
                        .getError()) {
                    prepareErrorEvent(operation, linkedOperation, error);

                    // Print
                    System.out.println("  |  + Error '" + error.getName() + "' when receiving of signal '" + error.getEvent() + "' sent by node '" + error.getNode() + "'");
                    System.out.println("  |  | --> Message '" + error.getEvent() + "' triggers death of the process");

                }
            }
        }

        private void prepareErrorEvent(Operation operation, LinkedOperation linkedOperation, Error error) throws InvalidKhiProcessException {
            // Validate attribute 'node'
            LinkedNode linkedNode = nodeLinker.lookupLinkedNode(error.getNode());
            if (linkedNode == null) {
                throw new InvalidKhiProcessException("Process '" + process.getName() + "', operation '" + operation.getName() + "' defines error '"
                        + error.getName() + " on node '" + error.getNode() + "' but no such node is defined in the process. Available nodes: " + nodeLinker.getNodeLookup().keySet().stream().map(Object::toString).collect(Collectors.toList()));
            }
            Khimodule module = linkedNode.getKhiModule();

            // Validate attribute 'event'
            String eventName = error.getEvent();
            Event event = null;
            Optional<Communication> communication = ElementFilter.getClass(module.getCodeOrCommunicationOrHardware(), Communication.class);
            if (!communication.isPresent()) {
                throw new InvalidKhiProcessException("Process '" + process.getName() + "', operation '" + operation.getName() + "' defines error '"
                        + error.getName() + " for event '" + eventName + "' on node '" + error.getNode() + "' implemented by module '"
                        + module.getName() + "' but this module does not define any Communication at all");
            }
            Optional<Events> events = ElementFilter.getClass(communication.get().getCommandsOrSensorsOrEvents(), Events.class);
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
            LinkedErrorOccurrence errorOccurrence = new LinkedErrorOccurrence(error, event, nodeLinker);
            linkedOperation.getLinkedErrors().add(errorOccurrence);
        }

    }

    class CommandLinker {

        public CommandLinker() throws InvalidKhiProcessException, UnavailableCommandException {
            prepareCommands();
        }

        void prepareCommands() throws InvalidKhiProcessException, UnavailableCommandException {
            // Browse operations
            Map<String, LinkedCommand> linkedCommandLookup = new HashMap<>();
            System.out.println("[LINKING] Process ExecuteCommand to Node Command:");
            for (LinkedOperation linkedOperation : operationLinker.getLinkedOperationLookup().values()) {
                // Link khiModule.Command to khiProcess.ExecuteCommand
                Operation operation = linkedOperation.getOperation();
                System.out.println("  + Operation '" + operation.getName() + "' defines " + operation.getExecuteCommand().size() + " command(s)");
                for (ExecuteCommand exec : operation.getExecuteCommand()) {
                    LinkedNode linkedNode = nodeLinker.lookupLinkedNode(exec.getNode());
                    if (linkedNode == null) {
                        throw new InvalidKhiProcessException("Process '" + process.getName() + "' attempts to execute a command on a node called '"
                                + exec.getNode() + "', but no such node was registered in the process. Available nodes: " + nodeLinker.getNodeLookup().keySet().stream().map(Object::toString).collect(Collectors.toList()));
                    }
                    System.out.println("  |  + Command '" + exec.getName() + "' can be sent sent to node '" + exec.getNode() + "'");

                    // Seek for a corresponding command accepted by the module
                    LinkedCommand linkedCommand = findCommand(linkedOperation, exec, linkedNode);
                    linkedCommandLookup.put(operation.getName() + "_" + exec.getName(), linkedCommand);

                    Command moduleCommand = null;
                    Optional<Communication> communication = ElementFilter.getClass(linkedNode.getKhiModule().getCodeOrCommunicationOrHardware(), Communication.class);
                    if (communication.isPresent()) {
                        Optional<Commands> commands = ElementFilter.getClass(communication.get().getCommandsOrSensorsOrEvents(), Commands.class);
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
                    // TODO do something with linked commandAttributes, maybe add it to linked command?
                }
            }
        }
    }

    public Khiprocess getProcess() {
        return process;
    }

    public ModuleLinker getModuleLinker() {
        return moduleLinker;
    }

    public NodeLinker getNodeLinker() {
        return nodeLinker;
    }

    public OperationLinker getOperationLinker() {
        return operationLinker;
    }

    /**
     * Finds a {@link Command} in a {@link Khimodule} which answers to an {@link ExecuteCommand} order sent by a {@link Khiprocess}
     *
     * @param linkedOperation the {@link LinkedOperation} context
     * @param exec            The {@link ExecuteCommand} specification that needs to be matched by an implementation in the module
     * @param linkedNode      a {@link LinkedNode} in which to search for the {@link Command}
     * @return a {@link LinkedCommand} representing the {@link ExecuteCommand} bound to aa {@link Command}
     * @throws InvalidKhiProcessException if no matching {@link Command} can be found
     */
    private LinkedCommand findCommand(LinkedOperation linkedOperation, ExecuteCommand exec, LinkedNode linkedNode) throws
            InvalidKhiProcessException {
        LinkedCommand linkedCommand = null;
        Optional<Communication> communication = ElementFilter.getClass(linkedNode.getKhiModule().getCodeOrCommunicationOrHardware(), Communication.class);
        if (communication.isPresent()) {
            Optional<Commands> commands = ElementFilter.getClass(communication.get().getCommandsOrSensorsOrEvents(), Commands.class);
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
                    .getName() + "', but this module defines no such command");
        }
        return linkedCommand;
    }

    public ProcessOverview assemble() {
        //Root
        ProcessOverview root = new ProcessOverview(process);
        for (LinkedOperation linkedOperation : operationLinker.getLinkedOperationLookup().values()) {
            root.getLinkedOperations().add(linkedOperation);
        }
        for (LinkedNode linkedNode : nodeLinker.getLinkedNodeLookup().values()) {
            root.add(linkedNode);
        }
        root.setInitialOperation(operationLinker.initialOperation);
        return root;
    }

}
