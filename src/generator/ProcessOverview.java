package generator;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import linker.LinkedNode;
import linker.LinkedOperation;
import test.beetlekhi.Khiprocess;
import exceptions.InvalidKhiProcessException;
import exceptions.InvalidStateException;
import exceptions.UnavailableCommandException;

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
