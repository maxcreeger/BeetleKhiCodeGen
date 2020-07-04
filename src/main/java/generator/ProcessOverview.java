package generator;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;
import linker.LinkedNode;
import linker.LinkedOperation;
import test.beetlekhi.command.Attribute;
import test.beetlekhi.process.Khiprocess;

import java.util.*;
import java.util.function.Supplier;

public class ProcessOverview {

	private final Khiprocess process;
	private LinkedOperation init;
	private final Set<LinkedOperation> linkedOperations = new HashSet<>();
	private final Set<LinkedNode> linkedNodes = new HashSet<>();

	public ProcessOverview(Khiprocess root) {
		super();
		this.process = root;
	}

	public Khiprocess getProcess() {
		return process;
	}

	public LinkedOperation getInit() {
		return init;
	}

	public Set<LinkedOperation> getLinkedOperations() {
		return linkedOperations;
	}

	public Set<LinkedNode> getLinkedNodes() {
		return linkedNodes;
	}

	public void setInitialOperation(LinkedOperation linkedOperation) {
		init = linkedOperation;
	}

	public void add(LinkedNode linkedNode) {
		linkedNodes.add(linkedNode);
	}

	public Map<LinkedNode, SlaveProgram> generateNodePrograms() {
		Map<LinkedNode, SlaveProgram> programs = new HashMap<>();
		for (LinkedNode node : linkedNodes) {
			SlaveProgram program = new SlaveProgram(node);
			programs.put(node, program);
		}
		return programs;
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

	public void begin(String portDescriptor) {
		// Sensor state storage
		Map<Attribute, Supplier<Object>> sensorValue = new HashMap<>();
		// All com ports
		SerialPort[] ports = SerialPort.getCommPorts();
		System.out.println("Ports:");
		for(SerialPort port: ports){
			System.out.println(" + " + port.getDescriptivePortName());
		}
		// Listen to port
		SerialPort port = SerialPort.getCommPort(portDescriptor);
		port.openPort();
		port.addDataListener(new SerialPortDataListener() {
			@Override
			public int getListeningEvents() {
				return SerialPort.LISTENING_EVENT_DATA_AVAILABLE;
			}

			@Override
			public void serialEvent(SerialPortEvent serialPortEvent) {
				if(serialPortEvent.getEventType() != SerialPort.LISTENING_EVENT_DATA_AVAILABLE) {
					return;
				}
				byte[] newData = new byte[port.bytesAvailable()];
				int numRead = port.readBytes(newData, newData.length);
				System.out.println("/!\\ Read " + numRead + " bytes.");
				String message = new String(newData);
				System.out.println("     + Message received: " + message);
				String[] split = message.split("\\|");
				Optional<Attribute> attribute = sensorValue.keySet()
						.stream()
						.filter(attr -> attr.getName() .equals(split[0]))
						.findFirst();
				attribute.ifPresent(attr -> sensorValue.put(attr, () -> split[1]));
			}
		});
		// Prepare suppliers
		for(LinkedNode node: linkedNodes) {
			node.addSensorCallbacks(sensorValue);
		}
	}
}
