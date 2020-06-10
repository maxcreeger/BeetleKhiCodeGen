
import generator.ArduinoProgram;
import generator.ProcessOverview;
import gui.ModuleMonitor;
import linker.LinkedNode;
import org.junit.Test;
import parsing.Parser;
import parsing.ProcessLinker;
import test.beetlekhi.module.Khimodule;
import test.beetlekhi.process.Khiprocess;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class TestBeetleKhi {

	@Test
	public void main() throws JAXBException, exceptions.MissingKhiModuleException, exceptions.InvalidKhiProcessException, exceptions.InvalidKhiModuleException,
			exceptions.UnavailableCommandException, exceptions.InvalidStateException {
		// Read Modules
		Khimodule syringe = Parser.readModule(new File("C:\\Users\\Marmotte\\IdeaProjects\\BeetleKhiCodeGen\\src\\test\\resources\\xml\\mSyringeByAlexandre.xml"));
		Khimodule reactor = Parser.readModule(new File("C:\\Users\\Marmotte\\IdeaProjects\\BeetleKhiCodeGen\\src\\test\\resources\\xml\\mReactorByBernard.xml"));
		List<Khimodule> repository = new ArrayList<>();
		repository.add(syringe);
		repository.add(reactor);

		// Read process
		Khiprocess oxygenatedWater = Parser.readProcess(new File("C:\\Users\\Marmotte\\IdeaProjects\\BeetleKhiCodeGen\\src\\test\\resources\\xml\\pSaltedWaterByCharles.xml"));

		// Build process and link to modules from the repo
		ProcessLinker linker = new ProcessLinker(repository, oxygenatedWater);
		ProcessOverview process = linker.assemble();

		Map<LinkedNode, ArduinoProgram> programs = process.generateNodePrograms();
		System.out.println();
		System.out.println();
		System.out.println("Generated Programs:");
		for (Entry<LinkedNode, ArduinoProgram> prog : programs.entrySet()) {
			String nodeName = prog.getKey().node.getName();
			ArduinoProgram sourceCode = prog.getValue();
			File file = new File("./resources/generated/" + nodeName + ".c");
			file.getParentFile()
				.mkdirs();
			try (FileOutputStream fos = new FileOutputStream(file)) {
				fos.write(sourceCode.toString()
									.getBytes());
			} catch (IOException e) {
				e.printStackTrace();
			}
			System.out.println("-----------+ " + nodeName + " program +------------------------");
			System.out.println(sourceCode);
		}
		// Simulate
		System.out.println("-----------+ Master program +------------------------");
		process.generateMasterProgram();
		// TODO add library loading
		// TODO read sensors tag
		// TODO link sensor variableReference to code/variales
		for (LinkedNode node : programs.keySet()) {
			new ModuleMonitor(node).display();
		}
	}

}
