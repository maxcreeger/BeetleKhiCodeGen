package linker;

import beetlkhi.utils.xsd.ElementFilter;
import test.beetlekhi.command.Attribute;
import test.beetlekhi.command.Attributes;
import test.beetlekhi.module.Communication;
import test.beetlekhi.module.Khimodule;
import test.beetlekhi.module.Sensor;
import test.beetlekhi.module.Sensors;
import test.beetlekhi.process.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Represents a resolved Node that can be driven by a process.
 */
public class LinkedNode {

    public final Node node;
    private final Khimodule khiModule;
    public final List<LinkedCommand> availableCommands = new ArrayList<>();
    public final List<LinkedSensor> availableSensors = new ArrayList<>();

    public LinkedNode(Node node, Khimodule khiModule) {
        super();
        this.node = node;
        this.khiModule = khiModule;
    }

    public String getName(){
        return node.getName();
    }

    public List<LinkedCommand> getAvailableCommands() {
        return availableCommands;
    }

    public List<LinkedSensor> getAvailableSensors() {
        return availableSensors;
    }

    public interface SensorCallback {
        public Object getValue();
    }

    public void addSensorCallbacks(Map<Attribute, Supplier<Object>> sensorValueSupplier) {
        Optional<Communication> communication = ElementFilter.getClass(khiModule.getCodeOrCommunicationOrHardware(), Communication.class);
        if(communication.isPresent()){
            Optional<Sensors> sensors = ElementFilter.getClass(communication.get().getCommandsOrSensorsOrEvents(), Sensors.class);
            if(sensors.isPresent()) {
                for (Sensor sensor : sensors.get().getSensor()) {
                    Optional<Attributes> attributes = ElementFilter.getClass(sensor.getAttributesOrVariableReference(), Attributes.class);
                    if (attributes.isPresent()) {
                        for (Attribute attribute : attributes.get().getAttribute()) {
                            Supplier<Object> supplier = sensorValueSupplier.get(attribute);
                            LinkedSensor linkedSensor = new LinkedSensor(supplier, attribute);
                            availableSensors.add(linkedSensor);
                        }
                    }
                }
            }
        }
    }

    public void add(LinkedCommand linkedCommand) {
        availableCommands.add(linkedCommand);
    }

    public Khimodule getKhiModule() {
        return khiModule;
    }
}