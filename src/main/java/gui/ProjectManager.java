package gui;

import gui.tree.preference.PreferenceTreeModel;
import parsing.Parser;
import test.beetlekhi.module.Khimodule;
import test.beetlekhi.process.Khiprocess;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.InvalidPreferencesFormatException;
import java.util.prefs.Preferences;

public class ProjectManager {

    // System data
    private final Path KHI_INSTALL;

    // User data
    private static final Path USER_HOME = Path.of(System.getProperty("user.home"));
    private static final Path KHI_HOME = USER_HOME.resolve("BeetleKhi");

    private static final Path KHI_PROJECTS = KHI_HOME.resolve("projects");
    private static final Path KHI_SETTINGS = KHI_HOME.resolve("settings");
    private static final Preferences SYSTEM_PREFERENCES = Preferences.systemRoot().node("org").node("marmotte").node("BeetleKhi");
    private static final Preferences USER_PREFERENCES = Preferences.userRoot().node("org").node("marmotte").node("BeetleKhi");

    public static final PreferenceTreeModel SYSTEM_PREFERENCE_MODEL = new PreferenceTreeModel(SYSTEM_PREFERENCES);
    public static final PreferenceTreeModel USER_PREFERENCE_MODEL = new PreferenceTreeModel(USER_PREFERENCES);


    // Session data
    private final List<Khimodule> modulesRepository;
    private final List<Khiprocess> processRepository;


    // /!\ Careful: https://groups.google.com/g/google-web-toolkit/c/LQfsBSwD_ug?pli=1
    // Java Bug may prevent users from writing to System Prefs, need to start as admin first time

    public ProjectManager(Path khiInstall) {
        KHI_INSTALL = khiInstall;
        System.out.println("KHI_INSTALL  : " + KHI_INSTALL.toAbsolutePath().toString());
        System.out.println("USER_HOME    : " + USER_HOME.toAbsolutePath().toString());
        System.out.println("KHI_PROJECTS : " + KHI_PROJECTS.toAbsolutePath().toString());
        System.out.println("KHI_SETTINGS : " + KHI_SETTINGS.toAbsolutePath().toString());
        try {
            // TODO check KHI_HOME exists & looks OK
            // Check KHI_PROJECTS
            if (!Files.exists(KHI_PROJECTS)) {
                System.out.println("Could not find KHI_PROJECTS folder at " + KHI_PROJECTS.toAbsolutePath().toString() + ", creating it...");
                Files.createDirectories(KHI_PROJECTS);
            }
            if (!Files.exists(KHI_SETTINGS)) {
                System.out.println("Could not find KHI_SETTINGS folder at " + KHI_SETTINGS.toAbsolutePath().toString() + ", creating it...");
                Files.createDirectories(KHI_SETTINGS);
            }
            // init settings
            // USER_PREFERENCES.removeNode();
            loadUserPreferences("test.xml");
            // saveUserPreferences("test.xml", USER_PREFERENCES);


            Preferences test = USER_PREFERENCES.node("test");
            test.put("key", "testKey");
            if(test.get("value", null) == null) {
                test.put("value", "testValue");
                test.flush();
            }
            Preferences testProperties = test.node("properties");
            testProperties.put("key", "someKey");
            if(testProperties.get("value", null) == null) {
                System.out.println("no value for test.properties, reverting to default");
                testProperties.put("value", "someValue");
                testProperties.flush();
            }
        } catch (IOException | BackingStoreException ioe) {
            throw new RuntimeException("Could not modify files", ioe);
        }
        this.modulesRepository = scanModules();
        this.processRepository = scanProcesses();
    }

    public List<Khimodule> getModulesRepository(){
        return modulesRepository;
    }

    public List<Khiprocess> getProcessRepository(){
        return processRepository;
    }

    List<Khimodule> scanModules() {
        String modulesRepositoryLocation = getUserSetting("repositories", "Modules", "Local").get("value", "????");
        System.out.println("Scanning for modules at: " + modulesRepositoryLocation);
        File actual = new File(modulesRepositoryLocation);
        List<Khimodule> repository = new ArrayList<>();
        File[] files = actual.listFiles();
        if (files != null) {
            for (File f : files) {
                System.out.println(" + Found Module file: " + f.getName());
                try {
                    repository.add(Parser.readModule(f));
                } catch (JAXBException e) {
                    throw new RuntimeException("invalid Module file: " + f.getAbsolutePath(), e);
                }
            }
        } else {
            System.out.println(" + No modules found at: " + modulesRepositoryLocation);
        }
        return repository;
    }

    List<Khiprocess> scanProcesses() {
        String processRepositoryLocation = getUserSetting("repositories", "Processes", "Local").get("value", "????");
        System.out.println("Scanning for processes at: " + processRepositoryLocation);
        File actual = new File(processRepositoryLocation);
        List<Khiprocess> repository = new ArrayList<>();
        File[] files = actual.listFiles();
        if (files != null) {
            for (File f : files) {
                System.out.println(" + Found Process file: " + f.getName());
                try {
                    repository.add(Parser.readProcess(f));
                } catch (JAXBException e) {
                    throw new RuntimeException("invalid Process file: " + f.getAbsolutePath(), e);
                }
            }
        } else {
            System.out.println(" + No processes found at: " + processRepositoryLocation);
        }
        return repository;
    }

    public static String sanitizeFileOrFolderName(String name) {
        StringBuilder filename = new StringBuilder();
        for (char c : name.toCharArray()) {
            if (c == '.' || Character.isJavaIdentifierPart(c)) {
                filename.append(c);
            }
        }
        return filename.toString();
    }

    public boolean exists(String projectName) {
        return Files.exists(projectHome(projectName));
    }

    public Path getProjectsHome() {
        return KHI_PROJECTS;
    }

    public Path khiHome() {
        return KHI_HOME;
    }

    public Path projectHome(String projectName) {
        return KHI_PROJECTS.resolve(projectName);
    }

    public Preferences getSystemSetting(String... path) {
        Preferences pref = SYSTEM_PREFERENCES;
        for (String elem : path) {
            pref = pref.node(elem);
        }
        return pref;
    }

    public Preferences getUserSetting(String... path) {
        Preferences pref = USER_PREFERENCES;
        for (String elem : path) {
            pref = pref.node(elem);
        }
        return pref;
    }

    public Preferences getProjectSetting(String projectName, String... path) {
        Preferences pref = SYSTEM_PREFERENCES.node("projects").node(projectName);
        for (String elem : path) {
            pref = pref.node(elem);
        }
        return pref;
    }

    public void setUserPreference(String key, String value, String... path) {
        Preferences pref = USER_PREFERENCES;
        for (String elem : path) {
            pref = pref.node(elem);
        }
        pref.put("key", key);
        pref.put("value", value);
        USER_PREFERENCE_MODEL.warnListeners(path);
    }

    public void newProject(String projectName) {
        try {
            Path projectDirectory = Files.createDirectory(KHI_PROJECTS.resolve(projectName));
            Files.createFile(projectDirectory.resolve(projectName + ".khiProject"));
        } catch (IOException ioe) {
            throw new RuntimeException("Could not initialize project " + projectName, ioe);
        }
    }



    /**
     * Loads user preferences from the given file.
     *
     * @param filename filename of the file storing the preferences
     */
    public static void loadUserPreferences(String filename) {
        try {
            FileInputStream fis = new FileInputStream(filename);
            Preferences.importPreferences(fis);
        } catch (IOException | InvalidPreferencesFormatException e){
            throw new RuntimeException(e);
        }
    }

    /**
     * Saves a given preference to file.
     *
     * @param preferencesFilename the file where the preferences are stored
     * @param preferences the preferences to be stored
     */
    public static void saveUserPreferences(String preferencesFilename,
                                           Preferences preferences)  {
        try (FileOutputStream fos = new FileOutputStream(preferencesFilename)) {
            preferences.exportSubtree(fos);
        } catch (IOException | BackingStoreException e){
            throw new RuntimeException(e);
        }
    }

    public Path getProjectHome(String projectName) {
        return KHI_PROJECTS.resolve(projectName);
    }
}
