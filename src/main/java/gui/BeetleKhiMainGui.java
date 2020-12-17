package gui;

import exceptions.*;
import generator.ProcessOverview;
import linker.LinkedNode;
import linker.ProcessLinker;
import test.beetlekhi.module.Khimodule;
import test.beetlekhi.process.Khiprocess;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BeetleKhiMainGui extends JFrame {

    private final ProjectManager projectManager;

    public BeetleKhiMainGui(Path khiHome) {
        super("Beetle Khi");
        // Prepare Environment
        projectManager = new ProjectManager(khiHome);
        this.setPreferredSize(new Dimension(800, 600));

        // JMenu ------------------------------------------
        // Project management
        JMenuItem projectNew = new JMenuItem(new NewProjectAction(projectManager));
        JMenuItem projectOpen = new JMenuItem(new OpenProjectAction(projectManager));
        JMenuItem projectSave = new JMenuItem(new NotImplementedYet("Save")); // TODO
        JMenuItem projectSaveAs = new JMenuItem(new NotImplementedYet("Save As...")); // TODO
        JMenu project = new JMenu("Project");
        project.add(projectNew);
        project.add(projectOpen);
        project.add(projectSave);
        project.add(projectSaveAs);
        // Repositories
        JMenuItem repositoriesRemotes = new JMenuItem(new NotImplementedYet("Remotes..."));
        JMenuItem repositoriesLocals = new JMenuItem(new NotImplementedYet("Locals..."));
        JMenu repositories = new JMenu("Repositories");
        repositories.add(repositoriesRemotes);
        repositories.add(repositoriesLocals);
        // Simulation
        JMenuItem simulationStart = new JMenuItem(new NotImplementedYet("Start"));
        JMenu simulation = new JMenu("Simulation");
        simulation.add(simulationStart);
        // Settings
        JMenuItem settingsSystem = new JMenuItem(TreeTable.browsePreferencesAction("System Settings...", ProjectManager.SYSTEM_PREFERENCE_MODEL));
        JMenuItem settingsProject = new JMenuItem(TreeTable.browsePreferencesAction("User Settings...", ProjectManager.USER_PREFERENCE_MODEL));
        JMenu settings = new JMenu("Settings");
        settings.add(settingsSystem);
        settings.add(settingsProject);
        // Menu Bar
        JMenuBar bar = new JMenuBar();
        bar.add(project);
        bar.add(repositories);
        bar.add(simulation);
        bar.add(settings);
        this.setJMenuBar(bar);


        // Now Main Tabs ------------------------------------------
        JComponent centralPanel = new JPanel();
        centralPanel.setMinimumSize(new Dimension(60, 30));
        centralPanel.setPreferredSize(new Dimension(400, 400));
        JComponent console = border(new JPanel(), "Console");
        console.setMinimumSize(new Dimension(60, 30));
        console.setPreferredSize(new Dimension(400, 200));
        JComponent projectView = border(new JPanel(), "Project");
        projectView.setMinimumSize(new Dimension(60, 30));
        projectView.setPreferredSize(new Dimension(120, 400));
        JComponent palette = border(new JPanel(), "Palette");
        palette.setMinimumSize(new Dimension(60, 30));
        palette.setPreferredSize(new Dimension(120, 400));
        JSplitPane rightPanel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, projectView, centralPanel);
        rightPanel.setOneTouchExpandable(true);
        JSplitPane top = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, rightPanel, palette);
        top.setOneTouchExpandable(true);
        JSplitPane verticalSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, top, console);
        verticalSplit.setOneTouchExpandable(true);
        this.add(verticalSplit);

        // Make JFrame ------------------------------------------
        getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.PAGE_AXIS));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
        pack();
    }

    private static JComponent border(JComponent component, String title) {
        Border blackLine = BorderFactory.createTitledBorder(title);
        JPanel borderedPanel = new JPanel();
        borderedPanel.setBorder(blackLine);
        borderedPanel.add(component);
        return borderedPanel;
    }

    public void open(String projectName) {
        System.out.println("You chose to open this Project: " + projectName);
        //TODO
    }

    private class ModulesRepositoryPanel extends JPanel {

        private final Map<Khimodule, JComponent> moduleRepresentation = new HashMap<>();

        public ModulesRepositoryPanel(ProjectManager projectManager) {
            this.setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
            refreshRepositories(projectManager);
        }

        private void refreshRepositories(ProjectManager projectManager) {
            // Clean up
            moduleRepresentation.values().forEach(this::remove);
            moduleRepresentation.clear();
            // Scan
            List<Khimodule> modulesRepository = projectManager.getModulesRepository();
            if (modulesRepository.isEmpty()) {
                JLabel label = new JLabel("No Modules found in " + modulesRepository);
                moduleRepresentation.put(null, label);
                System.out.println(" + No modules found: " + modulesRepository);
                add(label);
            } else {
                modulesRepository.forEach(module -> {
                    JButton label = new JButton(module.getName());
                    moduleRepresentation.put(module, label);
                    add(label);
                });
            }
        }
    }

    private class ProcessRepositoryPanel extends JPanel {

        private final Map<Khiprocess, JComponent> processRepresentation = new HashMap<>();

        public ProcessRepositoryPanel(ProjectManager projectManager) {
            this.setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
            refreshRepositories(projectManager);
        }

        private void refreshRepositories(ProjectManager projectManager) {
            // Clean up
            processRepresentation.values().forEach(this::remove);
            processRepresentation.clear();
            // Scan
            List<Khiprocess> processRepository = projectManager.getProcessRepository();
            if (processRepository.isEmpty()) {
                JLabel label = new JLabel("No Processes found in " + processRepository);
                processRepresentation.put(null, label);
                System.out.println(" + No processes found: " + processRepository);
                add(label);
            } else {
                // Add
                processRepository.forEach(process -> {
                    ProcessPanel processPanel = new ProcessPanel(process);
                    processRepresentation.put(process, processPanel);
                    add(processPanel);
                });
            }
        }
    }

    private class ProcessPanel extends JPanel {

        private final Khiprocess process;

        public ProcessPanel(Khiprocess process) {
            this.process = process;
            // Prepare border
            this.setLayout(new FlowLayout());
            JComponent borderedPanel = border(new JPanel(), process.getName());
            this.add(borderedPanel);

            // Prepare contents
            ProcessLinker linker;
            List<Khimodule> modulesRepository = projectManager.getModulesRepository();
            try {
                linker = new ProcessLinker(modulesRepository, process);
            } catch (MissingKhiModuleException | InvalidKhiProcessException | InvalidCommandAttributeException | InvalidStateException | UnavailableCommandException e) {
                throw new RuntimeException(e);
            }
            ProcessOverview processOverview = linker.assemble();
            processOverview.getLinkedNodes()
                    .stream()
                    .map(LinkedNode::getKhiModule)
                    .map(Khimodule::getName)
                    .distinct()
                    .forEach(moduleName -> {
                        JLabel label = new JLabel(moduleName);
                        label.setOpaque(true);
                        if (modulesRepository.stream().map(Khimodule::getName).anyMatch(moduleName::equals)) {
                            label.setBackground(UIManager.getColor("controlShadow"));
                        }
                        borderedPanel.add(label);
                    });
        }
    }

    private class NewProjectAction extends AbstractAction {
        private final ProjectManager projectManager;

        public NewProjectAction(ProjectManager projectManager) {
            super("New...", Icons.FILE_VIEW_NEW_FOLDER_ICON);
            this.projectManager = projectManager;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Object selection = JOptionPane.showInputDialog(
                    BeetleKhiMainGui.this,
                    "Project Name:",
                    "New Project",
                    JOptionPane.PLAIN_MESSAGE,
                    Icons.FILE_VIEW_NEW_FOLDER_ICON,
                    null,
                    "MyProject");
            String rawProjectName = (String) selection;
            if (rawProjectName == null) {
                return; // Cancelled by user
            }
            String projectName = ProjectManager.sanitizeFileOrFolderName(rawProjectName);
            String errorMessage = null;
            if (!rawProjectName.equals(projectName)) {
                errorMessage = "Project \"" + projectName + "\" contains invalid character(s)";
            } else if (projectName.isEmpty()) {
                errorMessage = "Project Name was empty";
            } else if (projectManager.exists(projectName)) {
                errorMessage = "Project \"" + projectName + "\" already exists";
            }
            if (errorMessage == null) {
                projectManager.newProject(projectName);
                open(projectName);
            } else {
                JOptionPane.showMessageDialog(BeetleKhiMainGui.this, errorMessage, "Invalid Project Name", JOptionPane.ERROR_MESSAGE, Icons.FILE_VIEW_ERROR_ICON);
                actionPerformed(e);
            }
        }
    }

    private class OpenProjectAction extends AbstractAction {
        private final ProjectManager projectManager;

        public OpenProjectAction(ProjectManager projectManager) {
            super("Open...", Icons.FILE_VIEW_DIRECTORY_ICON);
            this.projectManager = projectManager;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            JFileChooser chooser = new JFileChooser(projectManager.getProjectsHome().toFile());
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int returnVal = chooser.showOpenDialog(BeetleKhiMainGui.this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                open(chooser.getSelectedFile().getName());
            }
        }
    }

    private class NotImplementedYet extends AbstractAction {
        public NotImplementedYet(String msg) {
            super(msg);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            JOptionPane.showMessageDialog(BeetleKhiMainGui.this, "Not implemented Yet...", "Not implemented Yet...", JOptionPane.ERROR_MESSAGE, Icons.FILE_VIEW_INFORMATION_ICON);
        }
    }
}
