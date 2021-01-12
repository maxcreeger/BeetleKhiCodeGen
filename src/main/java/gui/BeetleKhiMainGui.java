package gui;

import exceptions.*;
import generator.ProcessOverview;
import gui.actions.NewProjectAction;
import gui.actions.OpenProjectAction;
import gui.tree.file.FileRenderDataProvider;
import gui.tree.file.FileRowModel;
import gui.tree.file.FileTreeModel;
import gui.views.ModulesRepositoryPanel;
import gui.views.TabbedCentralPanel;
import linker.LinkedNode;
import linker.ProcessLinker;
import org.netbeans.swing.outline.Outline;
import test.beetlekhi.module.Khimodule;
import test.beetlekhi.process.Khiprocess;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BeetleKhiMainGui extends JFrame {

    private final ProjectManager projectManager;
    private final JPanel fileBrowser;
    private final TabbedCentralPanel centralPanel = new TabbedCentralPanel(this);
    private final JComponent projectView;

    public BeetleKhiMainGui(Path khiHome) {
        super("Beetle Khi");
        // Prepare Environment
        projectManager = new ProjectManager(khiHome);
        this.setPreferredSize(new Dimension(800, 600));

        // JMenu ------------------------------------------
        // Project management
        JMenuItem projectNew = new JMenuItem(new NewProjectAction(this, projectManager));
        JMenuItem projectOpen = new JMenuItem(new OpenProjectAction(this, projectManager));
        JMenuItem projectSave = new JMenuItem(new NotImplementedYet("Save")); // TODO
        JMenuItem projectSaveAs = new JMenuItem(new NotImplementedYet("Save As...")); // TODO
        JMenu project = new JMenu("Project");
        project.add(projectNew);
        project.add(projectOpen);
        project.add(projectSave);
        project.add(projectSaveAs);
        // Repositories
        JMenuItem repositoriesRemotes = new JMenuItem(new NotImplementedYet("Remotes...")); // TODO
        JMenuItem repositoriesLocals = new JMenuItem(new NotImplementedYet("Locals...")); // TODO
        JMenu repositories = new JMenu("Repositories");
        repositories.add(repositoriesRemotes);
        repositories.add(repositoriesLocals);
        // Simulation
        JMenuItem simulationStart = new JMenuItem(new NotImplementedYet("Start")); // TODO
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

        // Main JPanel ------------------------------------------
        centralPanel.setMinimumSize(new Dimension(60, 30));
        centralPanel.setPreferredSize(new Dimension(400, 400));
        JComponent console = border(new JPanel(), "Console");
        console.setMinimumSize(new Dimension(60, 30));
        console.setPreferredSize(new Dimension(400, 200));
        fileBrowser = new JPanel();
        // fileBrowser.setBorder(BorderFactory.createLineBorder(Color.red));
        JScrollPane scrollableFileBrowser = new JScrollPane(fileBrowser);
        // scrollableFileBrowser.setBorder(BorderFactory.createLineBorder(Color.green));
        projectView = border(scrollableFileBrowser, "Project");
        FlowLayout projectViewLayout = new FlowLayout();
        projectViewLayout.setAlignment(FlowLayout.LEFT);
        projectView.setLayout(projectViewLayout);
        JComponent palette = makePalette();
        JSplitPane leftPanel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, projectView, centralPanel);
        leftPanel.setOneTouchExpandable(true);
        leftPanel.setContinuousLayout(true);
        leftPanel.setResizeWeight(1.); // All size changes given to the central tabbed panel (project view is slave)
        JSplitPane top = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, palette);
        top.setOneTouchExpandable(true);
        top.setContinuousLayout(true);
        leftPanel.setResizeWeight(0.); // All size changes given to the left panels (palette is slave)
        JSplitPane verticalSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, top, console);
        verticalSplit.setOneTouchExpandable(true);
        verticalSplit.setContinuousLayout(true);
        verticalSplit.setResizeWeight(1.);
        this.add(verticalSplit);

        // Make JFrame ------------------------------------------
        getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.PAGE_AXIS));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
        pack();
    }

    private JComponent makePalette() {
        JComponent modulesView = border(new ModulesRepositoryPanel(projectManager), "Modules");
        JComponent processesView = border(new ProcessRepositoryPanel(projectManager), "Processes");
        JComponent splitPalette = new JSplitPane(JSplitPane.VERTICAL_SPLIT, modulesView, processesView);
        JComponent palette = new JScrollPane(border(splitPalette, "Palette"));
        palette.setMinimumSize(new Dimension(60, 30));
        palette.setPreferredSize(new Dimension(120, 400));
        return palette;
    }

    public static JComponent border(JComponent component, String title) {
        Border blackLine = BorderFactory.createTitledBorder(title);
        JPanel borderedPanel = new JPanel();
        borderedPanel.setBorder(blackLine);
        borderedPanel.add(component);
        return borderedPanel;
    }

    public void open(String projectName) {
        fileBrowser.removeAll();
        Path projectHome = projectManager.getProjectHome(projectName);
        FileTreeModel model = new FileTreeModel(projectHome.toFile());
        Outline fileOutline = TreeTable.browseInJPanel(model, new FileRowModel(), new FileRenderDataProvider());
        fileOutline.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent mouseEvent) {
                Point point = mouseEvent.getPoint();
                int row = fileOutline.rowAtPoint(point);
                if (mouseEvent.getClickCount() == 2 && fileOutline.getSelectedRow() != -1) {
                    File file = (File) fileOutline.getValueAt(row, 0);
                    if(file.isDirectory()) {
                        return; // Nothing to do, just navigating
                    }
                    System.out.println("Double clicked: " + file);
                    centralPanel.open(file);
                }
            }
        });

        //fileOutline.getSelectionModel().addListSelectionListener(selectionListener);
        fileBrowser.add(fileOutline);
        projectView.revalidate();
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
