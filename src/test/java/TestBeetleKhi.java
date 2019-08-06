import com.beetlekhi.exceptions.*;
import com.beetlekhi.generator.ArduinoProgram;
import com.beetlekhi.generator.ProcessOverview;
import com.beetlekhi.gui.ModuleMonitor;
import com.beetlekhi.linker.LinkedNode;
import com.beetlekhi.module.Khimodule;
import com.beetlekhi.parsing.Parser;
import com.beetlekhi.parsing.ProcessLinker;
import com.beetlekhi.process.Khiprocess;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class TestBeetleKhi {

	public static void main(String[] args) throws JAXBException, MissingKhiModuleException, InvalidKhiProcessException, InvalidKhiModuleException,
			UnavailableCommandException, InvalidStateException {
		// Read Modules
		Khimodule syringe = Parser.readModule(new File(TestBeetleKhi.class.getClassLoader().getResource("samples/modules/mSyringeByAlexandre.xml").getFile()));
		Khimodule reactor = Parser.readModule(new File(TestBeetleKhi.class.getClassLoader().getResource("samples/modules/mReactorByBernard.xml").getFile()));
		List<Khimodule> repository = new ArrayList<>();
		repository.add(syringe);
		repository.add(reactor);

		// Read process
		Khiprocess oxygenatedWater = Parser.readProcess(new File(TestBeetleKhi.class.getClassLoader().getResource("samples/processes/pSaltedWaterByCharles.xml").getFile()));

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
