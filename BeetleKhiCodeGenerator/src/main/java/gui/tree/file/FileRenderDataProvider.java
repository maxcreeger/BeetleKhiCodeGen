package gui.tree.file;

import gui.TreeTable;
import org.netbeans.swing.outline.RenderDataProvider;

import javax.swing.*;
import java.io.File;

public class FileRenderDataProvider implements RenderDataProvider {

    @Override
    public java.awt.Color getBackground(Object o) {
        File f = (File) o;
        if (!f.isFile()) {
            return UIManager.getColor("controlShadow");
        }
        return null;
    }

    @Override
    public String getDisplayName(Object o) {
        return ((File) o).getName();
    }

    @Override
    public java.awt.Color getForeground(Object o) {
        return null;
    }

    @Override
    public Icon getIcon(Object o) {
        return null;
    }

    @Override
    public String getTooltipText(Object o) {
        return TreeTable.getFileExtension((File) o).orElse("Unknown");
    }

    @Override
    public boolean isHtmlDisplayName(Object o) {
        return false;
    }
}
