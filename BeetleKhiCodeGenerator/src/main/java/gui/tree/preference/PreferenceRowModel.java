package gui.tree.preference;

import org.netbeans.swing.outline.RowModel;

import java.util.prefs.Preferences;

public class PreferenceRowModel implements RowModel {
    @Override
    public Class<?> getColumnClass(int column) {
        switch (column) {
            case 0:
            case 1:
                return String.class;
            default:
                assert false;
        }
        return null;
    }

    @Override
    public int getColumnCount() {
        return 2;
    }

    @Override
    public String getColumnName(int column) {
        return column == 0 ? "Key" : "Value";
    }

    @Override
    public String getValueFor(Object node, int column) {
        Preferences pref = (Preferences) node;
        switch (column) {
            case 0:
                return pref.get("key", "undefined");
            case 1:
                return pref.get("value", "undefined");
            default:
                assert false;
        }
        return null;
    }

    @Override
    public boolean isCellEditable(Object node, int column) {
        return column == 1 && ((Preferences) node).get("editable", "false").equals("true");
    }

    @Override
    public void setValueFor(Object node, int column, Object value) {
        if (column == 1) {
            Preferences pref = (Preferences) node;
            pref.put("value", value.toString());
        }
    }
}
