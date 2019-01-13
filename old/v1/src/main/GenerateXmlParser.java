package main;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import main.GenerateXmlParser.RuntimeOperation.CommandAttributes;
import test.beetlekhi.Attribute;
import test.beetlekhi.Command;
import test.beetlekhi.Error;
import test.beetlekhi.Event;
import test.beetlekhi.EventListener;
import test.beetlekhi.ExecuteCommand;
import test.beetlekhi.Khimodule;
import test.beetlekhi.Khiprocess;
import test.beetlekhi.Node;
import test.beetlekhi.Nodes;
import test.beetlekhi.Operation;
import test.beetlekhi.Sensor;
import test.beetlekhi.Triggers;
import exceptions.InvalidKhiModuleException;
import exceptions.InvalidKhiProcessException;
import exceptions.InvalidStateException;
import exceptions.MissingKhiModuleException;
import exceptions.UnavailableCommandException;

public class GenerateXmlParser {
	private final Khiprocess process;
	private final Map<String, Khimodule> moduleLookup;
	private final Map<String, Node> nodeLookup;
	private final Map<Node, Khimodule> node2moduleLookup;
	private final Map<Node, RuntimeNode> nodeStateLookup;

	public GenerateXmlParser(List<Khimodule> repository, Khiprocess process) throws JAXBException, MissingKhiModuleException, InvalidKhiProcessException,
			InvalidKhiModuleException, UnavailableCommandException {
		this.process = process;
		moduleLookup = buildModuleLookup(repository);
		nodeLookup = buildNodeLookup(process);
		node2moduleLookup = buildNode2moduleLookup(nodeLookup, moduleLookup);
		nodeStateLookup = buildRuntimeNodes(node2moduleLookup);
	}

	public static void main(String[] args) throws JAXBException, MissingKhiModuleException, InvalidKhiProcessException, InvalidKhiModuleException,
			UnavailableCommandException, InvalidStateException {
		Khimodule syringe = readModule(new File("C:\\Users\\Marmotte\\workspace\\BeetleKhiCodeGen\\resources\\xml\\mSyringeByAlexandre.xml"));
		Khimodule reactor = readModule(new File("C:\\Users\\Marmotte\\workspace\\BeetleKhiCodeGen\\resources\\xml\\mReactorByBernard.xml"));
		List<Khimodule> repository = new ArrayList<>();
		repository.add(syringe);
		repository.add(reactor);

		Khiprocess oxygenatedWater = readProcess(new File("C:\\Users\\Marmotte\\workspace\\BeetleKhiCodeGen\\resources\\xml\\pSaltedWaterByCharles.xml"));
		GenerateXmlParser executor = new GenerateXmlParser(repository, oxygenatedWater);
		Map<Node, ArduinoProgram> programs = executor.generateNodePrograms();
		System.out.println("Generated Programs:");
		for (Entry<Node, ArduinoProgram> prog : programs.entrySet()) {
			System.out.println("--+ " + prog.getKey()
											.getName() + " program+------------------------");
			System.out.println(prog.getValue());
		}
		// Simulate
		executor.executeMasterProgram();
	}

	private Map<Node, ArduinoProgram> generateNodePrograms() {
		Map<Node, ArduinoProgram> programs = new HashMap<>();
		for (Node node : nodeLookup.values()) {
			ArduinoProgram program = new ArduinoProgram(node);
			for (Operation op : process.getPlan()
										.getOperations()
										.getOperation()) {
				for (ExecuteCommand cmd : op.getExecuteCommand()) {
					if (node.getName()
							.equals(cmd.getNode())) {
						program.registerEvent(op);
					}
				}
			}
			programs.put(node, program);
		}
		return programs;
	}

	public void executeMasterProgram() throws InvalidKhiProcessException, UnavailableCommandException, InvalidStateException {
		Map<String, Operation> operationLookup = buildOperationLookup(process);
		String initialOperationName = process.getPlan()
												.getInitial();
		Operation initialOperation = operationLookup.get(initialOperationName);
		if (initialOperation == null) {
			throw new InvalidKhiProcessException("The initial phase of process '" + process.getName() + "' specifies an initial operation '"
					+ initialOperationName + "' which does not exist in the plan");
		}

		//Startup
		System.out.println("Example Execution:");
		Operation currentOperation = initialOperation;
		while (true) {
			RuntimeOperation op = new RuntimeOperation(currentOperation);
			op.execute(nodeLookup, nodeStateLookup);

			Optional<EventOccurrence> event = op.triggerAnEvent();
			if (event.isPresent()) {
				String nextOperation = event.get().listener.getValue();
				System.out.println("/!\\ Event triggered while performing operation '" + op.operation.getName() + "': " + event.get().event.getName()
						+ ", next Operation: " + nextOperation);
				currentOperation = operationLookup.get(nextOperation);
			} else {
				break;
			}
		}
	}

	private Map<Node, Khimodule> buildNode2moduleLookup(Map<String, Node> nodeLookup, Map<String, Khimodule> moduleLookup) throws MissingKhiModuleException {
		Map<Node, Khimodule> node2moduleLookup = new HashMap<>();
		System.out.println("Verifying that all required modules for the process are available:");
		for (Entry<String, Node> nodeEntry : nodeLookup.entrySet()) {
			Khimodule module = moduleLookup.get(nodeEntry.getValue()
															.getModule());
			if (module == null) {
				throw new MissingKhiModuleException("The Node '" + nodeEntry.getKey() + "' requires a module with name '" + nodeEntry.getValue()
																																		.getModule()
						+ "', but none was wound in the repository");
			} else {
				System.out.println("  + " + nodeEntry.getKey() + " --> " + nodeEntry.getValue()
																					.getModule());
				node2moduleLookup.put(nodeEntry.getValue(), module);
			}
		}
		return node2moduleLookup;
	}

	private Map<String, Node> buildNodeLookup(Khiprocess process) {
		Map<String, Node> moduleMap = new HashMap<>();
		Nodes nodes = process.getNodes();
		System.out.println("Nodes for the process:");
		for (Node node : nodes.getNode()) {
			moduleMap.put(node.getName(), node);
			System.out.println("  + " + node.getName());
		}
		return moduleMap;
	}

	private Map<Node, RuntimeNode> buildRuntimeNodes(Map<Node, Khimodule> node2moduleLookup) throws InvalidKhiModuleException {
		System.out.println("Initializing Process Nodes:");
		Map<Node, RuntimeNode> moduleStateLookup = new HashMap<>();
		for (Entry<Node, Khimodule> entry : node2moduleLookup.entrySet()) {
			moduleStateLookup.put(entry.getKey(), new RuntimeNode(entry.getKey(), entry.getValue()));
			System.out.println("  + " + entry.getKey()
												.getName() + " will use " + entry.getValue()
																					.getName());
		}
		return moduleStateLookup;
	}

	private static Map<String, Operation> buildOperationLookup(Khiprocess process) throws InvalidKhiProcessException {
		Map<String, Operation> operationMap = new HashMap<>();
		System.out.println("Operations found: ");
		for (Operation op : process.getPlan()
									.getOperations()
									.getOperation()) {
			operationMap.put(op.getName(), op);
			System.out.println("  + " + op.getName() + " with " + op.getExecuteCommand() + " commands");
		}
		return operationMap;
	}

	private static Map<String, Khimodule> buildModuleLookup(List<Khimodule> repository) throws JAXBException {
		Map<String, Khimodule> moduleLookup = new HashMap<>();
		System.out.println("Module repository retrieved: ");
		for (Khimodule khimodule : repository) {
			moduleLookup.put(khimodule.getName(), khimodule);
			System.out.println("  + " + khimodule.getName());
		}
		return moduleLookup;
	}

	private static Khimodule readModule(File file) throws JAXBException {
		JAXBContext jaxbContext = JAXBContext.newInstance(Khimodule.class);
		Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
		Khimodule module = (Khimodule) jaxbUnmarshaller.unmarshal(file);
		return module;
	}

	private static Khiprocess readProcess(File file) throws JAXBException {
		JAXBContext jaxbContext = JAXBContext.newInstance(Khiprocess.class);
		Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
		Khiprocess process = (Khiprocess) jaxbUnmarshaller.unmarshal(file);
		return process;
	}

	public class RuntimeNode {

		final Node node;
		final Khimodule khiModule;
		CommandAttributes receivedCommand;

		public RuntimeNode(Node node, Khimodule khiModule) {
			super();
			this.node = node;
			this.khiModule = khiModule;
			this.receivedCommand = null;
		}

		public void apply(CommandAttributes assignedCommand) {
			this.receivedCommand = assignedCommand;
		}
	}

	public class RuntimeOperation {
		final Operation operation;
		public boolean running;
		List<EventListener> expectedEvents = new ArrayList<>();

		public RuntimeOperation(Operation op) {
			this.operation = op;
			expectedEvents.clear();
			expectedEvents.add(op.getTriggers()
									.getEventListener());
		}

		public void execute(Map<String, Node> nodeLookup, Map<Node, RuntimeNode> nodeStateLookup) throws UnavailableCommandException, InvalidStateException {
			System.out.println("  + Applying Operation '" + operation.getName() + "' :");
			running = true;
			List<ExecuteCommand> cmds = operation.getExecuteCommand();
			for (ExecuteCommand command : cmds) {
				Node node = nodeLookup.get(command.getNode());
				RuntimeNode runtimeNode = nodeStateLookup.get(node);
				System.out.println("  +--> Executing command '" + command.getName() + "' on module '" + command.getNode() + "'");

				CommandAttributes assignedCommand = prepareCommand(runtimeNode, command);
				nodeStateLookup.get(node)
								.apply(assignedCommand);

				// Display available triggers
				Triggers triggers = operation.getTriggers();
				if (triggers.getError() != null) {
					Error err = triggers.getError();
					System.out.println("  |  +--> ERROR '" + err.getName() + "' triggered upon receiving '" + err.getEvent() + "' event");
				}
				if (triggers.getEventListener() != null) {
					EventListener event = triggers.getEventListener();
					System.out.println("  |  +--> EVENT '" + event.getName() + "' triggered upon receiving '" + event.getEvent() + "' event");
					System.out.println("  |       Will Execute operation '" + event.getValue() + "'");
				}
			}
		}

		CommandAttributes prepareCommand(RuntimeNode runtime, ExecuteCommand exec) throws UnavailableCommandException {
			Command moduleCommand = null;
			for (Command availableCommand : runtime.khiModule.getCommunication()
																.getCommands()
																.getCommand()) {
				if (exec.getName()
						.equals(availableCommand.getName())) {
					moduleCommand = availableCommand;
					break;
				}
			}
			if (moduleCommand == null) {
				throw new UnavailableCommandException("Operation '" + operation.getName() + "' requests command '" + exec.getName()
						+ "', but it is not provided by node '" + runtime.node.getName() + "' defined by module '" + runtime.khiModule.getName() + "'");
			}
			return new CommandAttributes(moduleCommand, exec);
		}

		public Optional<EventOccurrence> triggerAnEvent() {
			for (EventListener listener : expectedEvents) {
				//moduleLookup.get(listener.get)
				Node node = nodeLookup.get(listener.getNode());
				RuntimeNode nodeState = nodeStateLookup.get(node);
				Khimodule module = node2moduleLookup.get(node);
				for (Event event : module.getCommunication()
											.getEvents()
											.getEvent()) {
					if (event.getName()
								.equals(listener.getEvent())) {
						EventOccurrence occurrence = new EventOccurrence(nodeState, event, listener);
						return Optional.of(occurrence);
					}
				}
			}
			return Optional.empty();
		}

		public class CommandAttributes {
			public Command moduleCommand;
			public ExecuteCommand exec;
			public Map<String, String> assignments = new HashMap<>();

			public CommandAttributes(Command moduleCommand, ExecuteCommand exec) throws UnavailableCommandException {
				this.moduleCommand = moduleCommand;
				this.exec = exec;
				if (exec.getAttributes() != null) {
					exec: for (Attribute execAttribute : exec.getAttributes()
																.getAttribute()) {
						if (moduleCommand.getAttributes() != null) {
							for (Attribute availAttribute : moduleCommand.getAttributes()
																			.getAttribute()) {
								if (availAttribute.getName()
													.equals(execAttribute.getName())) {
									assignments.put(execAttribute.getName(), execAttribute.getValue());
									continue exec;
								}
							}
							throw new UnavailableCommandException("Operation '" + operation.getName() + " 'requests command '" + moduleCommand.getName()
									+ "' on module '" + exec.getNode() + "' but it does not accept an attribute named '" + execAttribute.getName() + "'");
						}
					}
				}
			}
		}
	}

	public static class EventOccurrence {
		RuntimeNode runtimeNode;
		Event event;
		EventListener listener;

		public EventOccurrence(RuntimeNode runtimeNode, Event event, EventListener listener) {
			this.runtimeNode = runtimeNode;
			this.event = event;
			this.listener = listener;
		}
	}

	public class ArduinoProgram {
		final Node node;
		final Khimodule khimodule;
		final StringBuilder include = new StringBuilder();
		final StringBuilder helpers = new StringBuilder();
		final StringBuilder setup = new StringBuilder();
		final StringBuilder loop = new StringBuilder();
		Map<Operation, StringBuilder> events = new HashMap<>();

		public ArduinoProgram(Node node) {
			this.node = node;
			this.khimodule = node2moduleLookup.get(node);
			this.include.append("#include <Wire.h>\n#include <stdio.h>");

			// simple checksum method:
			// Send a pointer to the data, and the size of the data.
			// The result of the method is the checksum.
			// Interestingly, if the expected checksum ispassed at the end of the data, then theresult *should* be zero.
			this.helpers.append("unsigned char checksum (unsigned char *ptr, size_t sz) {")
						.append("\n  unsigned char chk = 0;")
						.append("\n  while (sz-- != 0)")
						.append("\n    chk -= *ptr++;")
						.append("\n    return chk;")
						.append("\n  }")
						.append("\n}");
		}

		public void registerEvent(Operation op) {
			StringBuilder event = new StringBuilder();
			event.append("// Begin operation ")
					.append(op.getName());
			event.append("\n")
					.append(op.getExecuteCommand()
								.get(0)
								//TODO
								.getName());
			event.append("\n// End operation ")
					.append(op.getName());
			events.put(op, event);
		}

		@Override
		public String toString() {
			StringBuilder program = new StringBuilder();
			// Prepare includes
			program.append(include)
					.append("\n")
					.append(helpers)
					.append("\n");

			// Prepare state variables
			program.append("\n// State Variables: event and sensor values");
			program.append("\nchar receivedMessage[32] = \"\"");
			if (khimodule.getCommunication() != null && khimodule.getCommunication()
																	.getSensors() != null && khimodule.getCommunication()
																										.getSensors()
																										.getSensor() != null) {
				for (Sensor sensor : khimodule.getCommunication()
												.getSensors()
												.getSensor()) {
					for (Attribute attribute : sensor.getAttributes()
														.getAttribute()) {
						program.append("\n")
								.append(attribute.getType())
								.append(" ")
								.append(khimodule.getName())
								.append("_")
								.append(sensor.getName())
								.append("_")
								.append(attribute.getName())
								.append(" = ")
								.append(defaultValueForType(attribute.getType()))
								.append(";");
					}
				}

			}

			// Prepare setup()
			program.append("\n\n// Setup");
			program.append("\nvoid setup() {");
			program.append("\n  Wire.begin(")
					.append(node.getI2Caddress())
					.append("); // Join i2c bus");
			program.append("\n  Wire.onRequest(requestedI2cMesssage); // Register 'Master requests a message' event");
			program.append("\n  Wire.onReceive(receiveI2cMesssage);   // Register 'Master sends a message' event");
			program.append("\n  Serial.begin(9600);                   // start serial for output");
			program.append("\n}\n");

			//Begin loop()
			program.append("\nvoid loop() {");
			program.append("\n  delay(100);");
			program.append("\n}\n");

			//Begin receiveI2cMesssage event
			program.append("\nvoid receiveI2cMesssage(int size) {");
			program.append("\n  int i = 0;");
			program.append("\n  while (1 < Wire.available()) {        // loop through all but the last");
			program.append("\n    receivedMessage[i] = Wire.read();   // receive byte as a character");
			program.append("\n    i++;");
			program.append("\n  }");
			program.append("\n  char command[5]");
			program.append("\n  strncpy(command, receivedMessage, 5); // Extract command string");
			program.append("\n  target[10] = '\0'; // IMPORTANT!");
			program.append("\n}");

			// Begin respondI2cRequest
			program.append("\nvoid requestedI2cMesssage() {");
			program.append("\n  Wire.write(\"ACK\"); // Acknowledge");
			program.append("\n}");

			// End of file
			program.append("\n");
			return program.toString();
		}
	}

	public static String defaultValueForType(String type) {
		switch (type) {
		case "boolean":
			return "false";
		case "int":
			return "0";
		case "double":
			return "0.0";
		case "string":
			return "\"\"";
		default:
			return "UNKNOWN!";
		}
	}
}
