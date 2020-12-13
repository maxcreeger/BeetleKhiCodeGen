package generator;

import linker.*;
import test.beetlekhi.module.Khimodule;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class MasterProgram implements ICodeGenerator{

    final ProcessOverview overview;

    public MasterProgram(ProcessOverview overview) {
        this.overview = overview;
    }

    @Override
    public String constructProgramCode() {
        StringBuilder program = new StringBuilder(4000);
        program.append("// ").append(overview.getProcess().getName());
        program.append(constructIncludeStatements());
        program.append(constructI2cAddresses());
        program.append(constructStateMachine());
        program.append(constructTriggersReceived());
        program.append(constructSetupMethod());
        program.append(constructLoopMethod());
        program.append(constructSendMessages());
        program.append(constructComputeTransitions());
        program.append(constructDeactivateSteps());
        program.append(constructActivateSteps());
        program.append(constructActions());

        // End of file
        program.append("\n");
        return program.toString();
    }

    public void saveToFile(File file) throws IOException {
        if (file.getParentFile().exists() || file.getParentFile().mkdirs()) {
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(this.toString().getBytes());
        }
    }

    private StringBuilder constructIncludeStatements() {
        StringBuilder includes = new StringBuilder();
        includes.append("\n\n// Includes");
        includes.append("\n#include <Wire.h>\n#include <stdio.h>\n#include <EEPROM.h>");
        return includes;
    }

    private StringBuilder constructI2cAddresses() {
        StringBuilder addressBlock = new StringBuilder();
        addressBlock.append("\n\n// Constants");
        addressBlock.append("\n#define BROADCAST_ADDRESS 0x01;");
        addressBlock.append("\n#define TOTAL_NODE_COUNT ").append(overview.getLinkedNodes().size()).append(";");
        addressBlock.append("\nbyte slavesAssigned   = 0x00;");
        addressBlock.append("\nbyte slavesIdentified = 0x00;");
        addressBlock.append("\n// Table of slave addresses");
        for (LinkedNode node : overview.getLinkedNodes()) {
            addressBlock.append("\nbyte ").append(node.getName()).append("_Address = 0x00;");
        }
        return addressBlock;
    }

    private StringBuilder constructStateMachine() {
        StringBuilder stateMachine = new StringBuilder();
        stateMachine.append("\n\n// State machine. Available states:");
        int nbSteps = overview.getLinkedOperations().size() + 1; // + 1 for init

        // Steps
        stateMachine.append("\n#define nbSteps ").append(nbSteps).append(";");
        stateMachine.append("\n#define step_khi_emer 0;");
        int i = 1;
        for (LinkedOperation operation : overview.getLinkedOperations()) {
            stateMachine.append("\n#define step_").append(operation.getName()).append(" ").append(i++).append(";");
        }

        // Transitions
        int nbTransitions = overview.getLinkedOperations()
                .stream()
                .mapToInt(operation -> operation.getLinkedTriggers().size())
                .sum() + 1; // + 1 for init
        stateMachine.append("\n#define nbTransitions ").append(nbTransitions).append(";");
        i = 0;
        for (LinkedOperation operation : overview.getLinkedOperations()) {
            for (LinkedTrigger trigger : operation.getLinkedTriggers()) {
                stateMachine.append("\n#define tran_").append(operation.getName()).append("_").append(trigger.getNextLinkedOperation().getName()).append(" ").append(i++).append(";");
            }
        }
        stateMachine.append("\n#define initialStep step_").append(overview.getInit().getName()).append(";");
        stateMachine.append("\n// State values");
        stateMachine.append("\nbool transitions[nbTransitions];");
        stateMachine.append("\nint stateStartTime[nbSteps];");
        stateMachine.append("\nbool steps[nbSteps + 2];");
        stateMachine.append("\nint currentState = initialStep;");
        return stateMachine;
    }

    private StringBuilder constructTriggersReceived() {
        StringBuilder triggersReceived = new StringBuilder();
        triggersReceived.append("\n\n// Events Received:");
        Map<String, Set<String>> nodeEvents = new HashMap<>();
        // init the mapping node to Event
        for (LinkedNode node : overview.getLinkedNodes()) {
            nodeEvents.put(node.getName(), new HashSet<>());
        }
        // Populate the Mapping node to Event
        for (LinkedOperation linkedOperation : overview.getLinkedOperations()) {
            for (LinkedTrigger linkedTrigger : linkedOperation.getLinkedTriggers()) {
                for (LinkedTrigger.LinkedEventListener linkedEventListener : linkedTrigger.getAllInternalEvents()) {
                    nodeEvents.get(linkedEventListener.getEmitterNode()).add(linkedEventListener.getEvent().getName());
                }
            }
            for (LinkedErrorOccurrence linkedErrorOccurrence : linkedOperation.getLinkedErrors()) {
                String nodeName = linkedErrorOccurrence.getFailedNode().getName();
                String event = linkedErrorOccurrence.getEvent().getName();
                nodeEvents.get(nodeName).add(event);
            }
        }
        // Use the Mapping node to Event
        for (Map.Entry<String, Set<String>> entry : nodeEvents.entrySet()) {
            triggersReceived.append("\n// + Events from node '").append(entry.getKey()).append("'");
            if (entry.getValue().isEmpty()) {
                triggersReceived.append("\n//   -> No Events for this Node");
            } else {
                for (String event : entry.getValue()) {
                    triggersReceived.append("\nbool ").append(entry.getKey()).append("_").append(event).append(" = false;");
                }
            }
        }
        return triggersReceived;
    }

    private StringBuilder constructSetupMethod() {
        StringBuilder setup = new StringBuilder();
        setup.append("\n\n// Setup");
        setup.append("\nvoid setup() {");
        setup.append("\n  for(int i=0; i<nbSteps; i++) {");
        setup.append("\n    steps[i] = false;");
        setup.append("\n  }");
        setup.append("\n  steps[initialStep] = true;");
        setup.append("\n  Wire.begin();                              // Join i2c bus as Master");
        setup.append("\n  Wire.onRequest(requestClearAssignments);   //   make this function execute when ping requested from master,");
        setup.append("\n  Wire.onReceive(receiveClearAssignments);   //   and make this function execute to receive new address.");
        setup.append("\n}");
        return setup;
    }

    private StringBuilder constructLoopMethod() {
        StringBuilder loop = new StringBuilder();
        loop.append("\n\nvoid loop() {");
        loop.append("\n  if(slavesAssigned < TOTAL_NODE_COUNT) {");
        loop.append("\n    checkNewSlave();");
        loop.append("\n    return;");
        loop.append("\n  }");
        loop.append("\n  if(slavesIdentified < TOTAL_NODE_COUNT) {");
        loop.append("\n    identifySlaves();");
        loop.append("\n    return;");
        loop.append("\n  }");
        loop.append("\n  sendMessages();");
        loop.append("\n  pollErrors(); // TODO check if any error occurred in any module");
        loop.append("\n  pollSensors(); // TODO poll Sensor values");
        loop.append("\n  computeTransitions();");
        loop.append("\n  deactivateSteps();");
        loop.append("\n  activateSteps();");
        loop.append("\n}");
        return loop;
    }

    private StringBuilder constructSendMessages() {
        StringBuilder sendMessages = new StringBuilder();
        sendMessages.append("\n\nvoid sendMessages() {");
        sendMessages.append("\n  // TODO: send messages here");
        sendMessages.append("\n}");
        return sendMessages;
    }

    private StringBuilder constructComputeTransitions() {
        StringBuilder program = new StringBuilder();
        program.append("\n\nvoid computeTransitions() {");
        for (LinkedOperation operation : overview.getLinkedOperations()) {
            for (LinkedTrigger linkedTrigger : operation.getLinkedTriggers()) {
                StringBuilder condition = linkedTrigger.constructTransitionCondition();
                String transitionName = "tran_" + operation.getName() + "_" + linkedTrigger.getNextLinkedOperation().getName();
                String stepName = "step_" + operation.getName();
                program.append("\n  transitions[").append(transitionName).append("] = steps[").append(stepName).append("] && ").append(condition).append(";");
            }
        }
        program.append("\n}");
        return program;
    }

    private StringBuilder constructDeactivateSteps() {
        StringBuilder program = new StringBuilder();
        program.append("\n\nvoid deactivateSteps() {");
        for (LinkedOperation operation : overview.getLinkedOperations()) {
            for (LinkedTrigger trigger : operation.getLinkedTriggers()) {
                String transitionName = "tran_" + operation.getName() + "_" + trigger.getNextLinkedOperation().getName();
                String stepName = "step_" + operation.getName();
                program.append("\n  if(transitions[").append(transitionName).append("]) steps[").append(stepName).append("] = false;");
            }
        }
        program.append("\n}");
        return program;
    }

    private StringBuilder constructActivateSteps() {
        StringBuilder program = new StringBuilder();
        program.append("\n\nvoid activateSteps() {");
        for (LinkedOperation operation : overview.getLinkedOperations()) {
            for (LinkedTrigger trigger : operation.getLinkedTriggers()) {
                program.append(constructActivateStepsBlock(operation, trigger.getNextLinkedOperation()));
            }
        }
        program.append("\n}");
        return program;
    }

    static StringBuilder constructActivateStepsBlock(LinkedOperation operation, LinkedOperation nextOperation) {
        StringBuilder program = new StringBuilder();
        String transitionName = "tran_" + operation.getName() + "_" + nextOperation.getName();
        program.append("\n  if(transitions[").append(transitionName).append("]) {");
        program.append("\n    steps[step_").append(nextOperation.getName()).append("] = true;");
        program.append("\n    stateStartTime[step_").append(nextOperation.getName()).append("] = millis();");
        nextOperation.getLinkedCommands().forEach(command ->
                program.append(constructExecuteCommand(command)));
        program.append("\n    transitions[").append(transitionName).append("] = false;");
        program.append("\n  }");
        return program;
    }

    static StringBuilder constructExecuteCommand(LinkedCommand command) {
        StringBuilder program = new StringBuilder();
        String callParameters = command.constructMethodCall();
        callParameters = command.getTarget().getName() + "_Address" + (callParameters.isEmpty() ? "" : ", " + callParameters);
        program.append("\n    execute_").append(command.getName()).append("(").append(callParameters).append(");");
        return program;
    }

    private StringBuilder constructActions() {
        StringBuilder program = new StringBuilder();
        Set<Khimodule> modulesUnique = overview.getLinkedNodes()
                .stream()
                .map(LinkedNode::getKhiModule)
                .collect(Collectors.toSet());
        Map<Khimodule, List<LinkedNode>> modules = new HashMap<>();
        overview.getLinkedNodes().forEach(
                node -> modules.computeIfAbsent(
                        node.getKhiModule(), module -> {
                            List<LinkedNode> nodes = new ArrayList<>();
                            nodes.add(node);
                            return nodes;
                        }
                )
        );
        Set<LinkedCommand> usedCommandsUnique = overview.getLinkedOperations()
                .stream()
                .map(LinkedOperation::getLinkedCommands)
                .flatMap(List::stream)
                .collect(Collectors.toSet());
        program.append("\n\n// Actions of Modules");
        for (Khimodule module : modulesUnique) {
            program.append("\n// Actions for Modules of type ").append(module.getName());
            for (LinkedNode nodes : modules.get(module)) {
                for (LinkedCommand command : nodes.getAvailableCommands()) {
                    if (usedCommandsUnique.contains(command)) {
                        program.append(command.constructMasterCommand());
                    }
                }
                program.append("\n");
            }
        }
        return program;
    }

    @Override
    public String toString() {
        return constructProgramCode();
    }
}