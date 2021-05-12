package gui.views;

import gui.ProjectManager;
import test.beetlekhi.module.Khimodule;

import javax.swing.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModulesRepositoryPanel extends JPanel {

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
