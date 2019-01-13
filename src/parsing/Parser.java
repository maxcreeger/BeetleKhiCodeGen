package parsing;

import java.io.File;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import test.beetlekhi.Khimodule;
import test.beetlekhi.Khiprocess;

public class Parser {

	public static Khimodule readModule(File file) throws JAXBException {
		System.out.println("[Opening] Module XML (" + file.getName() + ")...");
		JAXBContext jaxbContext = JAXBContext.newInstance(Khimodule.class);
		Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
		Khimodule module = (Khimodule) jaxbUnmarshaller.unmarshal(file);
		System.out.println("          Module XML (" + file.getName() + ") OK");
		return module;
	}

	public static Khiprocess readProcess(File file) throws JAXBException {
		System.out.println("[Opening] Process XML (" + file.getName() + ")...");
		JAXBContext jaxbContext = JAXBContext.newInstance(Khiprocess.class);
		Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
		Khiprocess process = (Khiprocess) jaxbUnmarshaller.unmarshal(file);
		System.out.println("          Process XML (" + file.getName() + ") OK");
		return process;
	}
}
