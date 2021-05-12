package gui.actions;

import gui.BeetleKhiMainGui;
import gui.Icons;
import gui.ProjectManager;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class OpenProjectAction extends AbstractAction {
    private final BeetleKhiMainGui beetleKhiMainGui;
    private final ProjectManager projectManager;

    public OpenProjectAction(BeetleKhiMainGui beetleKhiMainGui, ProjectManager projectManager) {
        super("Open...", Icons.FILE_VIEW_DIRECTORY_ICON);
        this.beetleKhiMainGui = beetleKhiMainGui;
        this.projectManager = projectManager;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        JFileChooser chooser = new JFileChooser(projectManager.getProjectsHome().toFile());
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int returnVal = chooser.showOpenDialog(beetleKhiMainGui);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            beetleKhiMainGui.open(chooser.getSelectedFile().getName());
        }
    }
}
