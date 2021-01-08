package gui;

import gui.tree.preference.PreferenceDataProvider;
import gui.tree.preference.PreferenceRowModel;
import gui.tree.preference.PreferenceTreeModel;
import org.netbeans.swing.outline.*;

import javax.swing.*;
import javax.swing.tree.TreeModel;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.Optional;

public class TreeTable {

    public static Action browsePreferencesAction(String msg, PreferenceTreeModel model) {
        return new AbstractAction(msg, Icons.FILE_VIEW_DIRECTORY_ICON) {
            @Override
            public void actionPerformed(ActionEvent e) {
                browseInJFrame(msg, model, new PreferenceRowModel(), new PreferenceDataProvider());
            }
        };
    }

    public static void browseInJFrame(String title, TreeModel treeModel, RowModel rowModel, RenderDataProvider renderDataProvider) {
        JScrollPane scrollableOutline = new JScrollPane(browseInJPanel(treeModel, rowModel, renderDataProvider));
        JFrame table = new JFrame(title);
        table.getContentPane().setLayout(new BoxLayout(table.getContentPane(), BoxLayout.PAGE_AXIS));
        table.add(scrollableOutline);
        table.pack();
        table.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        table.setVisible(true);
    }

    public static Outline browseInJPanel(TreeModel model, RowModel rowModel, RenderDataProvider renderDataProvider) {
        OutlineModel mdl = DefaultOutlineModel.createOutlineModel(model, rowModel, false);
        Outline outline = new Outline();
        outline.setRenderDataProvider(renderDataProvider);
        outline.setRootVisible(true);
        outline.setModel(mdl);
        return outline;
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
