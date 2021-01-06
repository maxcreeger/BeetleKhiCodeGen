package gui;

import org.netbeans.swing.outline.*;

import javax.swing.*;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public class TreeTable {

    public static Action browsePreferencesAction(String msg, PreferenceTreeModel model) {
        return new AbstractAction(msg, Icons.FILE_VIEW_DIRECTORY_ICON) {
            @Override
            public void actionPerformed(ActionEvent e) {
                browsePreferencesInJFrame(msg, model, new PreferenceRowModel(), new PreferenceDataProvider());
            }
        };
    }

    public static void browsePreferencesInJFrame(String title, TreeModel treeModel, RowModel rowModel, RenderDataProvider renderDataProvider) {
        JScrollPane scrollableOutline = new JScrollPane(browseInJPanel(treeModel, rowModel, renderDataProvider));
        JFrame table = new JFrame(title);
        table.getContentPane().setLayout(new BoxLayout(table.getContentPane(), BoxLayout.PAGE_AXIS));
        table.add(scrollableOutline);
        table.pack();
        table.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        table.setVisible(true);
    }

    public static JComponent browseInJPanel(TreeModel model, RowModel rowModel, RenderDataProvider renderDataProvider) {
        OutlineModel mdl = DefaultOutlineModel.createOutlineModel(model, rowModel, false);
        Outline outline = new Outline();
        outline.setRenderDataProvider(renderDataProvider);
        outline.setRootVisible(true);
        outline.setModel(mdl);
        outline.setPreferredSize(new Dimension(800, 600));
        return outline;
    }

    /**
     * The methods in this class allow the JTree component to traverse
     * the file system tree, and display the files and directories.
     **/
    public static class FileTreeModel implements TreeModel {
        // We specify the root directory when we create the model.
        protected File root;

        public FileTreeModel(File root) {
            this.root = root;
        }

        // The model knows how to return the root object of the tree
        public Object getRoot() {
            return root;
        }

        // Tell JTree whether an object in the tree is a leaf or not
        public boolean isLeaf(Object node) {
            return ((File) node).isFile();
        }

        // Tell JTree how many children a node has
        public int getChildCount(Object parent) {
            String[] children = ((File) parent).list();
            if (children == null) return 0;
            return children.length;
        }

        // Fetch any numbered child of a node for the JTree.
        // Our model returns File objects for all nodes in the tree.  The
        // JTree displays these by calling the File.toString() method.
        public Object getChild(Object parent, int index) {
            String[] children = ((File) parent).list();
            if ((children == null) || (index >= children.length)) return null;
            return new File((File) parent, children[index]);
        }

        // Figure out a child's position in its parent node.
        public int getIndexOfChild(Object parent, Object child) {
            String[] children = ((File) parent).list();
            if (children == null) return -1;
            String childName = ((File) parent).getName();
            for (int i = 0; i < children.length; i++) {
                if (childName.equals(children[i])) return i;
            }
            return -1;
        }

        // This method is only invoked by the JTree for editable trees.
        // This TreeModel does not allow editing, so we do not implement
        // this method.  The JTree editable property is false by default.
        public void valueForPathChanged(TreePath path, Object newValue) {
        }

        // Since this is not an editable tree model, we never fire any events,
        // so we don't actually have to keep track of interested listeners.
        public void addTreeModelListener(TreeModelListener l) {
        }

        public void removeTreeModelListener(TreeModelListener l) {
        }
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

    static class PreferenceRowModel implements RowModel {
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

        public boolean isCellEditable(Object node, int column) {
            return column == 1 && ((Preferences) node).get("editable", "false").equals("true");
        }

        public void setValueFor(Object node, int column, Object value) {
            if (column == 1) {
                Preferences pref = (Preferences) node;
                pref.put("value", value.toString());
            }
        }
    }

    static class FileRowModel implements RowModel {
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
            return 1;
        }

        public String getColumnName(int column) {
            return "Files";
        }

        public String getValueFor(Object node, int column) {
            File file = (File) node;
            return file.getName();
        }

        public boolean isCellEditable(Object node, int column) {
            return column == 1 && ((Preferences) node).get("editable", "false").equals("true");
        }

        public void setValueFor(Object node, int column, Object value) {
            if (column == 1) {
                Preferences pref = (Preferences) node;
                pref.put("value", value.toString());
            }
        }
    }

    static class PreferenceDataProvider implements RenderDataProvider {
        public java.awt.Color getBackground(Object o) {
            Preferences f = (Preferences) o;
            if (f.get("editable", "false").equals("false")) {
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

    static class FileDataProvider implements RenderDataProvider {

        public java.awt.Color getBackground(Object o) {
            File f = (File) o;
            if (!f.isFile()) {
                return UIManager.getColor("controlShadow");
            }
            return null;
        }

        public String getDisplayName(Object o) {
            return ((File) o).getName();
        }

        public java.awt.Color getForeground(Object o) {
            return null;
        }

        public javax.swing.Icon getIcon(Object o) {
            return null;
        }

        public String getTooltipText(Object o) {
            return getFileExtension((File) o).orElse("Unknown");
        }

        public boolean isHtmlDisplayName(Object o) {
            return false;
        }
    }

    public static Optional<String> getFileExtension(File file) {
        String fileName = file.getName();
        int i = fileName.lastIndexOf('.');
        if (i > 0) {
            return Optional.of(fileName.substring(i + 1));
        } else {
            return Optional.empty();
        }
    }


}
