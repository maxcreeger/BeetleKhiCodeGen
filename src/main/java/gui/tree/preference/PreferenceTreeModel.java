package gui.tree.preference;

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public class PreferenceTreeModel implements TreeModel {

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
