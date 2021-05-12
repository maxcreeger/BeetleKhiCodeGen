package parsing;

import test.beetlekhi.module.Khimodule;
import test.beetlekhi.process.Khiprocess;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Parser {

    public static List<Khimodule> readModules(File... files) throws JAXBException {
        System.out.println("[Opening] " + files.length + " Module XMLs");
        List<Khimodule> modules = new ArrayList<>();
        for(File file : files) {
            modules.add(readModule(file));
        }
        System.out.println();
        return modules;
    }

    public static Khimodule readModule(File file) throws JAXBException {
        System.out.print(" + " + file.getName() + "...");
        JAXBContext jaxbContext = JAXBContext.newInstance(Khimodule.class);
        Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
        Khimodule module = (Khimodule) jaxbUnmarshaller.unmarshal(file);
        System.out.println(" [OK]");
        return module;
    }

    public static Khiprocess readProcess(File file) throws JAXBException {
        System.out.println("[Opening] Process XMLs");
        System.out.print(" + " + file.getName() + "...");
        JAXBContext jaxbContext = JAXBContext.newInstance(Khiprocess.class);
        Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
        Khiprocess process = (Khiprocess) jaxbUnmarshaller.unmarshal(file);
        System.out.println(" [OK]");
        System.out.println();
        return process;
    }
}
