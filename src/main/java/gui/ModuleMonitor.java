package gui;

import beetlkhi.utils.xsd.ElementFilter;
import linker.LinkedNode;
import linker.LinkedSensor;
import test.beetlekhi.command.Attribute;
import test.beetlekhi.command.Attributes;
import test.beetlekhi.module.Communication;
import test.beetlekhi.module.Sensor;
import test.beetlekhi.module.Sensors;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ModuleMonitor {

    private final LinkedNode myNode;
    private final JPanel panel;
    private final String moduleTitle;

    public ModuleMonitor(LinkedNode myNode) {
        this.myNode = myNode;
        String nodeName = myNode.node.getName();
        String moduleName = myNode.getKhiModule().getName();
        moduleTitle = nodeName + " (" + moduleName + ")";
        displayStatus();
        Optional<Communication> communication = ElementFilter.getClass(myNode.getKhiModule().getCodeOrCommunicationOrHardware(), Communication.class);
        panel = communication.map(this::displayCommunication).orElse(new JPanel());
    }

    public void displayStatus() {

    }

    public JPanel displayCommunication(Communication communication) {
        Optional<Sensors> sensors = ElementFilter.getClass(communication.getCommandsOrSensorsOrEvents(), Sensors.class);
        return sensors.map(this::displaySensors).orElse(new JPanel());
    }

    public JPanel displaySensors(Sensors sensors) {
        List<List<Object>> sensorsData = new ArrayList<>();
        int maxAttributeCount = 0;
        List<Object> sensorHeader = new ArrayList<>();
        sensorHeader.add("Sensor Name");
        for (Sensor sensor : sensors.getSensor()) {
            List<Object> sensorData = new ArrayList<>();
            String sensorName = sensor.getName();
            sensorData.add(sensorName);
            Optional<Attributes> attributes = ElementFilter.getClass(sensor.getAttributesOrVariableReference(), Attributes.class);
            if (attributes.isPresent()) {
                for (Attribute attribute : attributes.get().getAttribute()) {
                    sensorData.add(attribute.getName());
                    Optional<LinkedSensor> linkedSensor = myNode.availableSensors.stream()
                            .filter(s -> s.attribute == attribute)
                            .findFirst();
                    if (linkedSensor.isPresent()) {
                        sensorData.add(linkedSensor.get().getCurrentValue());
                    } else {
                        sensorData.add("No Value can be found");
                    }
                }
                maxAttributeCount = Math.max(maxAttributeCount, attributes.get().getAttribute().size());
            }
            sensorsData.add(sensorData);
        }
        for (int i = 0; i < maxAttributeCount; i++) {
            sensorHeader.add("Attribute #" + (i + 1));
            sensorHeader.add("Attribute #" + (i + 1) + " Value");
        }
        Object[][] dataArray = sensorsData
                .stream()
                .map(List::toArray)
                .collect(Collectors.toList())
                .toArray(new Object[0][0]);
        JTable table = new JTable(dataArray, sensorHeader.toArray());
        JPanel tablePanel = new JPanel(new GridLayout());
        tablePanel.add(new JScrollPane(table));
        tablePanel.setBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createEtchedBorder(),
                        moduleTitle,
                        TitledBorder.CENTER,
                        TitledBorder.TOP));
        tablePanel.setPreferredSize(table.getPreferredSize());
        return tablePanel;
    }

    public JFrame newFrame() {
        JFrame frame = new JFrame(moduleTitle);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(panel);
        frame.pack();
        frame.setVisible(true);
        return frame;
    }

    public JPanel getPanel() {
        return panel;
    }

}
