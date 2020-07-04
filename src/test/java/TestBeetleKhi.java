
import generator.MasterProgram;
import generator.SlaveProgram;
import generator.ProcessOverview;
import gui.ModuleMonitor;
import linker.LinkedNode;
import org.junit.Test;
import parsing.Parser;
import parsing.ProcessLinker;
import test.beetlekhi.module.Khimodule;
import test.beetlekhi.process.Khiprocess;

import javax.swing.*;
import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class TestBeetleKhi {

    public static void main(String[] arg) throws JAXBException, exceptions.MissingKhiModuleException, exceptions.InvalidKhiProcessException, exceptions.InvalidKhiModuleException,
            exceptions.UnavailableCommandException, exceptions.InvalidStateException, IOException {
        new TestBeetleKhi().main();
    }

    @Test
    public void main() throws JAXBException, exceptions.MissingKhiModuleException, exceptions.InvalidKhiProcessException, exceptions.InvalidKhiModuleException,
            exceptions.UnavailableCommandException, exceptions.InvalidStateException, IOException {
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
        for (Entry<LinkedNode, SlaveProgram> prog : programs.entrySet()) {
            String nodeName = prog.getKey().node.getName();
            SlaveProgram slaveSourceCode = prog.getValue();
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
        JFrame frame = newFrame(saltedWater.getName());
        for (LinkedNode node : programs.keySet()) {
            frame.add(new ModuleMonitor(node).getPanel());
        }
        frame.pack();

        process.begin("COM1");
    }

    public JFrame newFrame(String title) {
        JFrame frame = new JFrame(title);
        frame.getContentPane().setLayout(new BoxLayout(frame.getContentPane(), BoxLayout.PAGE_AXIS));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
        return frame;

    }

}
