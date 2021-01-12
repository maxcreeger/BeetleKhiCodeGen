package gui.graph;

import com.beetlekhi.grafcet.model.*;
import com.mxgraph.model.mxCell;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.util.mxConstants;
import com.mxgraph.view.mxGraph;

import javax.swing.*;
import javax.xml.bind.JAXBException;
import java.awt.*;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class FileGraphX extends JPanel {

    private final File file;
    private JComponent activeComponent;

    public FileGraphX(File file) {
        this.file = file;
        setLayout(new FlowLayout());
        refresh();
    }

    public void refresh() {
        mxGraph graph = new mxGraph() {
            @Override
            public String convertValueToString(Object cell) {
                if (cell instanceof mxCell) {
                    Object value = ((mxCell) cell).getValue();
                    if (value instanceof Step) {
                        Step step = (Step) value;
                        return "Step N°" + step.getNum();
                    } else if (value instanceof Transition) {
                        Transition transition = (Transition) value;
                        return "Transition N°" + transition.getNum();
                    }
                }
                return super.convertValueToString(cell);
            }
        };
        graph.getModel().beginUpdate();
        Object parent = graph.getDefaultParent();

        // Build
        Grafcet grafcetDAO = null;
        try {
            grafcetDAO = GrafcetUtils.readGrafcetFromXML(file);
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
        Map<Integer, Object> graphSteps = new HashMap<>();
        Map<Integer, Step> daoSteps = new HashMap<>();
        for (Step stepDAO : grafcetDAO.getSteps().getStep()) {
            Object step = graph.insertVertex(parent, null, stepDAO, stepDAO.getX() * 100, stepDAO.getY() * 100 - 20, 80, 60, "ROUNDED");
            graphSteps.put(stepDAO.getNum(), step);
            daoSteps.put(stepDAO.getNum(), stepDAO);
            // TODO: do something wit actions!
            Actions actions = stepDAO.getActions();
            if (actions != null) {
                for (String action : actions.getAction()) {
                    // TODO Do something?
                }
            }
        }

        graph.getModel().endUpdate();
        graph.getStylesheet().getDefaultEdgeStyle().put(mxConstants.STYLE_EDGE,
                mxConstants.EDGESTYLE_ELBOW /*your desired style e.g. mxConstants.EDGESTYLE_ELBOW*/);
        System.out.println(graph.toString());

        mxGraphComponent graphComponent = new mxGraphComponent(graph);
        graphComponent.setConnectable(false);
        //graph.setAllowDanglingEdges(false);
        graph.setDisconnectOnMove(false);
        //graphComponent.setEnabled(false); disables all edition
        graph.setCellsEditable(false);


        // Replace the content & redraw
        if(activeComponent != null) {
            remove(activeComponent);
        }
        activeComponent = graphComponent;
        add(activeComponent);
        invalidate();
    }

}
