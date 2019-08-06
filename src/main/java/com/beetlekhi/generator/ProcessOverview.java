package com.beetlekhi.generator;

import com.beetlekhi.exceptions.InvalidKhiProcessException;
import com.beetlekhi.exceptions.InvalidStateException;
import com.beetlekhi.exceptions.UnavailableCommandException;
import com.beetlekhi.linker.LinkedNode;
import com.beetlekhi.linker.LinkedOperation;
import com.beetlekhi.process.Khiprocess;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ProcessOverview {

    private final Khiprocess process;
    private LinkedOperation init;
    public final Set<LinkedOperation> linkedOperations = new HashSet<>();
    public final Set<LinkedNode> linkedNodes = new HashSet<>();

    public ProcessOverview(Khiprocess root) {
        super();
        this.process = root;
    }

    public void setInitialOperation(LinkedOperation linkedOperation) {
        init = linkedOperation;
    }

    public void add(LinkedNode linkedNode) {
        linkedNodes.add(linkedNode);
    }

    public Map<LinkedNode, ArduinoProgram> generateNodePrograms() {
        Map<LinkedNode, ArduinoProgram> programs = new HashMap<>();
        for (LinkedNode node : linkedNodes) {
            ArduinoProgram program = new ArduinoProgram(node);
            programs.put(node, program);
        }
        return programs;
    }

    public void generateMasterProgram() throws InvalidKhiProcessException, UnavailableCommandException, InvalidStateException {
        // TODO
        StringBuilder code = new StringBuilder();
        code.append("void loop(){")
                .append("\n");
    }

    public static String defaultValueForType(String type) {
        switch (type) {
            case "boolean":
                return "false";
            case "int":
            case "long":
            case "double":
                return "0";
            case "string":
                return "\"\"";
            default:
                return "UNKNOWN!";
        }
    }
}
