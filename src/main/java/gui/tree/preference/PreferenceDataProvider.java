package gui.tree.preference;

import org.netbeans.swing.outline.RenderDataProvider;

import javax.swing.*;
import java.util.prefs.Preferences;

public class PreferenceDataProvider implements RenderDataProvider {

    @Override
    public java.awt.Color getBackground(Object o) {
        Preferences f = (Preferences) o;
        if (f.get("editable", "false").equals("false")) {
            return UIManager.getColor("controlShadow");
        }
        return null;
    }

    @Override
    public String getDisplayName(Object o) {
        return ((Preferences) o).name();
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
        return ((Preferences) o).get("key", "undefined");
    }

    @Override
    public boolean isHtmlDisplayName(Object o) {
        return false;
    }
}
