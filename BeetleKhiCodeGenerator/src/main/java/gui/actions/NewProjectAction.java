package gui.actions;

import gui.BeetleKhiMainGui;
import gui.Icons;
import gui.ProjectManager;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class NewProjectAction extends AbstractAction {
    private final BeetleKhiMainGui beetleKhiMainGui;
    private final ProjectManager projectManager;

    public NewProjectAction(BeetleKhiMainGui beetleKhiMainGui, ProjectManager projectManager) {
        super("New...", Icons.FILE_VIEW_NEW_FOLDER_ICON);
        this.beetleKhiMainGui = beetleKhiMainGui;
        this.projectManager = projectManager;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object selection = JOptionPane.showInputDialog(
                beetleKhiMainGui,
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
            beetleKhiMainGui.open(projectName);
        } else {
            JOptionPane.showMessageDialog(beetleKhiMainGui, errorMessage, "Invalid Project Name", JOptionPane.ERROR_MESSAGE, Icons.FILE_VIEW_ERROR_ICON);
            actionPerformed(e);
        }
    }
}
