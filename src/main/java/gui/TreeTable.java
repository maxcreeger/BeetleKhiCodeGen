package gui;

import org.netbeans.swing.outline.*;

import javax.swing.*;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public class TreeTable {

    public static Action browsePreferencesAction(String msg, PreferenceTreeModel model) {
        return new AbstractAction(msg, Icons.FILE_VIEW_DIRECTORY_ICON) {
            @Override
            public void actionPerformed(ActionEvent e) {
                browsePreferences(model);
            }
        };
    }

    public static void browsePreferences(PreferenceTreeModel model) {
        OutlineModel mdl = DefaultOutlineModel.createOutlineModel(model, new FileRowModel(), false);
        Outline outline = new Outline();
        outline.setRenderDataProvider(new FileDataProvider());
        outline.setRootVisible(true);
        outline.setModel(mdl);
        outline.setPreferredSize(new Dimension(800, 600));

        JFrame table = new JFrame("Settings");
        table.getContentPane().setLayout(new BoxLayout(table.getContentPane(), BoxLayout.PAGE_AXIS));
        table.add(new JScrollPane(outline));
        table.pack();
        table.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        table.setVisible(true);
    }

    public static class PreferenceTreeModel implements TreeModel {

        private final Preferences root;
        private final List<TreeModelListener> preferenceListeners = new ArrayList<>();

        public PreferenceTreeModel(Preferences pref) {
            this.root = pref;
        }

        @Override
        public Object getRoot() {
            return root;
        }

        @Override
        public Object getChild(Object parent, int index) {
            Preferences parentPref = (Preferences) parent;
            try {
                String[] childrenNames = parentPref.childrenNames();
                return parentPref.node(childrenNames[index]);
            } catch (BackingStoreException e) {
                throw new RuntimeException("Can find Preferences", e);
            }
        }

        @Override
        public int getChildCount(Object parent) {
            Preferences parentPref = (Preferences) parent;
            try {
                return parentPref.childrenNames().length;
            } catch (BackingStoreException e) {
                throw new RuntimeException("Can find Preferences", e);
            }
        }

        @Override
        public boolean isLeaf(Object node) {
            return getChildCount(node) == 0;
        }

        @Override
        public void valueForPathChanged(TreePath path, Object newValue) {
            Preferences pref = root;
            Object[] pathObjects = path.getPath();
            for (int i = 0; i < pathObjects.length - 1; i++) {
                String elem = (String) pathObjects[i];
                pref = pref.node(elem);
            }
            String lastKey = (String) pathObjects[pathObjects.length - 1];
            pref.put("key", lastKey);/// TODO or not ?
            pref.put("value", newValue.toString());
        }

        @Override
        public int getIndexOfChild(Object parent, Object child) {
            Preferences parentPref = (Preferences) parent;
            try {
                Object[] children = parentPref.childrenNames();
                for (int i = 0; i < children.length; i++) {
                    if (children[i] == child) {
                        return i;
                    }
                }
                return -1;
            } catch (BackingStoreException e) {
                throw new RuntimeException("Can find Preferences", e);
            }
        }

        @Override
        public void addTreeModelListener(TreeModelListener l) {
            preferenceListeners.add(l);
        }

        @Override
        public void removeTreeModelListener(TreeModelListener l) {
            preferenceListeners.remove(l);
        }

        public void warnListeners(String... path) {
            TreeModelEvent event = new TreeModelEvent(root, path);
            preferenceListeners.forEach(
                    listener -> listener.treeNodesChanged(event)
            );
        }
    }

    private static class FileRowModel implements RowModel {
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

        public int getColumnCount() {
            return 2;
        }

        public String getColumnName(int column) {
            return column == 0 ? "Key" : "Value";
        }

        public Object getValueFor(Object node, int column) {
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

        public boolean isCellEditable(Object node, int column) {
            return column == 1 && ((Preferences)node).get("editable", "false").equals("true");
        }

        public void setValueFor(Object node, int column, Object value) {
            if (column == 1) {
                Preferences pref = (Preferences) node;
                pref.put("value", value.toString());
            }
        }
    }

    private static class FileDataProvider implements RenderDataProvider {
        public java.awt.Color getBackground(Object o) {
            Preferences f = (Preferences) o;
            if (f.get("editable", "false") .equals("false")) {
                return UIManager.getColor("controlShadow");
            }
            return null;
        }

        public String getDisplayName(Object o) {
            return ((Preferences) o).name();
        }

        public java.awt.Color getForeground(Object o) {
            return null;
        }

        public javax.swing.Icon getIcon(Object o) {
            return null;
        }

        public String getTooltipText(Object o) {
            return ((Preferences) o).get("key", "undefined");
        }

        public boolean isHtmlDisplayName(Object o) {
            return false;
        }
    }


}
