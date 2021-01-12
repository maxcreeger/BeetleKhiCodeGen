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
import java.util.List;
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
        Grafcet grafcetDAO;
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


        // Build transitions
        Map<Integer, Object> graphTransitions = new HashMap<>();
        Map<Integer, Transition> daoTransitions = new HashMap<>();
        for (Transition transitionDAO : grafcetDAO.getTransitions().getTransition()) {
            Object tran = graph.insertVertex(parent, null, transitionDAO, transitionDAO.getX() * 100, transitionDAO.getY() * 100, 80, 12, "fillColor=black");
            graphTransitions.put(transitionDAO.getNum(), tran);
            daoTransitions.put(transitionDAO.getNum(), transitionDAO);

            // Setup transition upstream step links
            if (transitionDAO.getRequiredSteps() != null) {
                java.util.List<Required> requiredStepsDAO = transitionDAO.getRequiredSteps().getRequired();
                if (requiredStepsDAO.size() == 1) {
                    Object step = graphSteps.get(requiredStepsDAO.get(0).getStep());
                    graph.insertEdge(parent, null, null, step, tran);
                } else if (requiredStepsDAO.size() > 1) {
                    // Compute concentrator span
                    int xMin = transitionDAO.getX();
                    int xMax = transitionDAO.getX();
                    for (Required required : requiredStepsDAO) {
                        xMin = Math.min(xMin, daoSteps.get(required.getStep()).getX());
                        xMax = Math.max(xMax, daoSteps.get(required.getStep()).getX());
                    }
                    // Create concentrator
                    Object concentrator = graph.insertVertex(parent, null, null, xMin * 100, transitionDAO.getY() * 100 - 30, (xMax - xMin) * 100, 1);
                    graph.insertEdge(parent, null, "", concentrator, tran);
                    // Link to each Tran
                    for (Required executeDAO : requiredStepsDAO) {
                        Object graphStep = graphSteps.get(executeDAO.getStep());
                        graph.insertEdge(parent, null, executeDAO, graphStep, concentrator);
                    }
                }
            }

            // Setup transition downstream step links
            if (transitionDAO.getExecutedSteps() != null) {
                List<Executed> executedStepsDAO = transitionDAO.getExecutedSteps().getExecuted();
                if (executedStepsDAO.size() == 1) {
                    Object step = graphSteps.get(executedStepsDAO.get(0).getStep());
                    graph.insertEdge(parent, null, null, tran, step);
                } else if (executedStepsDAO.size() > 1) {
                    // Compute concentrator span
                    int xMin = transitionDAO.getX();
                    int xMax = transitionDAO.getX();
                    for (Executed executeDAO : executedStepsDAO) {
                        xMin = Math.min(xMin, daoSteps.get(executeDAO.getStep()).getX());
                        xMax = Math.max(xMax, daoSteps.get(executeDAO.getStep()).getX());
                    }
                    // Create concentrator
                    Object concentrator = graph.insertVertex(parent, null, null, xMin * 100 + 10, transitionDAO.getY() * 100 + 50, (xMax - xMin) * 100 + 60, 1);
                    graph.insertEdge(parent, null, null, tran, concentrator);
                    // Link to each step
                    for (Executed executeDAO : executedStepsDAO) {
                        Object step = graphSteps.get(executeDAO.getStep());
                        graph.insertEdge(parent, null, executeDAO, concentrator, step);
                    }
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
