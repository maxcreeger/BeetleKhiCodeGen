
import exceptions.InvalidCommandAttributeException;
import exceptions.UnavailableCommandException;
import generator.MasterProgram;
import generator.ProcessOverview;
import generator.SlaveProgram;
import gui.BeetleKhiMainGui;
import gui.ModuleMonitor;
import linker.LinkedNode;
import org.junit.Test;
import parsing.Parser;
import linker.ProcessLinker;
import test.beetlekhi.module.Khimodule;
import test.beetlekhi.process.Khiprocess;

import javax.swing.*;
import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

public class TestBeetleKhi {

    public static void main(String[] arg) throws JAXBException, exceptions.MissingKhiModuleException, exceptions.InvalidKhiProcessException, exceptions.InvalidStateException, IOException, InvalidCommandAttributeException, UnavailableCommandException {
        new TestBeetleKhi().moduleMonitorDisplay();
    }

    @Test
    public void mainGuiDisplay() throws InterruptedException {
        new BeetleKhiMainGui(Paths.get(""));
        Thread.sleep(1000000);
    }

    @Test
    public void moduleMonitorDisplay() throws JAXBException, exceptions.MissingKhiModuleException, exceptions.InvalidKhiProcessException,
            exceptions.InvalidStateException, IOException, InvalidCommandAttributeException, UnavailableCommandException {
        // Read Modules
        File syringe = new File("./src/test/resources/xml/mSyringeByAlexandre.xml");
        File reactor = new File("./src/test/resources/xml/mReactorByBernard.xml");
        List<Khimodule> repository = Parser.readModules(syringe, reactor);

        // Read process
        Khiprocess saltedWater = Parser.readProcess(new File("./src/test/resources/xml/pSaltedWaterByCharles.xml"));

        // Build process and link to modules from the repo
        ProcessLinker linker = new ProcessLinker(repository, saltedWater);
        ProcessOverview process = linker.assemble();

        Map<LinkedNode, SlaveProgram> programs = process.generateNodePrograms();
        System.out.println();
        System.out.println();
        System.out.println("Generated Programs:");
        for (Entry<LinkedNode, SlaveProgram> program : programs.entrySet()) {
            String nodeName = program.getKey().node.getName();
            SlaveProgram slaveSourceCode = program.getValue();
            File file = new File("./target/generated-test-sources/arduino-programs/" + nodeName + ".c");
            slaveSourceCode.saveToFile(file);
            System.out.println(" + " + nodeName + ".c program generated");
        }
        // Simulate
        System.out.println("-----------+ Master program +------------------------");
        MasterProgram masterProgram = new MasterProgram(process);
        File masterFile = new File("./target/generated-test-sources/arduino-programs/master.c");
        masterProgram.saveToFile(masterFile);
        // TODO add C library loading
        // TODO read sensors tag
        // TODO link sensor variableReference to code/variables
        List<JComponent> panels = programs.keySet()
                .stream()
                .map(
                linkedNode -> new ModuleMonitor(linkedNode).getPanel()
        ).collect(Collectors.toList());
        newFrame(saltedWater.getName(), panels);
        process.begin("COM1");
    }

    public JFrame newFrame(String title, List<JComponent> components) {
        JFrame frame = newFrame(title);
        components.forEach(frame::add);
        frame.pack();
        return frame;
    }

    public JFrame newFrame(String title, JComponent... components) {
        return newFrame(title, Arrays.asList(components));
    }

    public JFrame newFrame(String title) {
        JFrame frame = new JFrame(title);
        frame.getContentPane().setLayout(new BoxLayout(frame.getContentPane(), BoxLayout.PAGE_AXIS));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
        return frame;

    }

}
