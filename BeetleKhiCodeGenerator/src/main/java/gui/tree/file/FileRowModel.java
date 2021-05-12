package gui.tree.file;

import org.netbeans.swing.outline.RowModel;

import java.io.File;

public class FileRowModel implements RowModel {

    @Override
    public Class<?> getColumnClass(int column) {
        if (column == 0) {
            return String.class;
        } else {
            assert false;
        }
        return null;
    }

    @Override
    public int getColumnCount() {
        return 0;
    }

    @Override
    public String getColumnName(int column) {
        return "Files";
    }

    @Override
    public String getValueFor(Object node, int column) {
        File file = (File) node;
        return file.getName();
    }

    @Override
    public boolean isCellEditable(Object node, int column) {
        return false;
    }

    @Override
    public void setValueFor(Object node, int column, Object value) {
    }
}
