package gui.views;

import gui.BeetleKhiMainGui;
import gui.Icons;
import gui.graph.FileGraphViz;
import gui.graph.FileGraphX;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public class TabbedCentralPanel extends JTabbedPane {

    private final BeetleKhiMainGui beetleKhiMainGui;
    private final Map<File, JComponent> panels = new HashMap<>();

    public TabbedCentralPanel(BeetleKhiMainGui beetleKhiMainGui) {
        this.beetleKhiMainGui = beetleKhiMainGui;
    }

    public void open(File file) {
        JComponent existing = panels.get(file);
        if (existing == null) {
            JComponent newPanel;
            switch (getFileExtension(file)) {
                case ".c":
                    newPanel = openSourceCode(file);
                    break;
                case ".dot":
                    newPanel = new FileGraphViz(file);
                    break;
                case ".grafcet":
                    newPanel = new FileGraphX(file);
                    break;
                default:
                    JOptionPane.showMessageDialog(beetleKhiMainGui, "Unsupported File Extension: " + file.getAbsolutePath(), "Unsupported File Extension", JOptionPane.ERROR_MESSAGE, Icons.FILE_VIEW_INFORMATION_ICON);
                    throw new UnsupportedOperationException("Unknown file type " + getFileExtension(file));
            }
            panels.put(file, newPanel);
            add(newPanel, file.getName());
            int index = indexOfComponent(newPanel);
            JComponent titlePanel = new JPanel();
            titlePanel.setOpaque(false);
            titlePanel.setLayout(new FlowLayout());
            JLabel titleText = new JLabel(file.getName());
            titleText.setOpaque(true);
            titleText.setBackground(new Color(0, 0, 0, 0));
            titlePanel.add(titleText);
            JButton closeButton = new JButton("X");
            closeButton.addActionListener(e -> {
                closeTab(file);
            });
            closeButton.setOpaque(false);
            closeButton.setContentAreaFilled(false);
            closeButton.setForeground(Color.RED);
            closeButton.setBorderPainted(false);
            closeButton.setMargin(new Insets(1, 1, 1, 1));
            closeButton.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseEntered(java.awt.event.MouseEvent evt) {
                    closeButton.setContentAreaFilled(true);
                    closeButton.setBackground(Color.gray);
                }

                public void mouseExited(java.awt.event.MouseEvent evt) {
                    closeButton.setContentAreaFilled(false);
                    closeButton.setBackground(UIManager.getColor("control"));
                }
            });

            titlePanel.add(closeButton);
            setTabComponentAt(index, titlePanel);
            setSelectedComponent(newPanel);
            revalidate();
        } else {
            setSelectedComponent(existing);
        }
    }

    private String getFileExtension(File file) {
        String name = file.getName();
        int lastIndexOf = name.lastIndexOf(".");
        if (lastIndexOf == -1) {
            return ""; // empty extension
        }
        return name.substring(lastIndexOf);
    }


    public static RTextScrollPane openSourceCode(File file) {
        try {
            RSyntaxTextArea textArea = new RSyntaxTextArea(Files.readString(file.toPath()));
            textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_C);
            textArea.setCodeFoldingEnabled(true);
            return new RTextScrollPane(textArea);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void closeTab(File file) {
        JComponent panel = panels.remove(file);
        if (panel != null) {
            remove(panel);
        }
    }
}
