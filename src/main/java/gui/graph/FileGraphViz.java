package gui.graph;

import gui.views.TabbedCentralPanel;
import guru.nidi.graphviz.attribute.Color;
import guru.nidi.graphviz.attribute.Font;
import guru.nidi.graphviz.attribute.*;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.Graph;
import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.parse.Parser;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import static guru.nidi.graphviz.model.Factory.*;

public class FileGraphViz extends JSplitPane {

    private final File file;
    private final JComponent textPanel;
    private final ImagePanel graphPanel;

    public static void main(String[] argc) throws IOException {
        FileGraphViz fileGraphPanel = new FileGraphViz(new File("example/test.dot"));
        JFrame jframe = new JFrame("test Graph");
        jframe.getContentPane().setLayout(new BoxLayout(jframe.getContentPane(), BoxLayout.PAGE_AXIS));
        jframe.add(fileGraphPanel);
        jframe.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        jframe.setVisible(true);
        jframe.pack();
    }

    public FileGraphViz(File file) {
        super();
        this.file = file;
        textPanel = TabbedCentralPanel.openSourceCode(file);
        graphPanel = new ImagePanel();
        this.setLeftComponent(textPanel);
        this.setRightComponent(graphPanel);
        refresh();
    }

    private static Graph sampleApi() {
        return graph("example1").directed()
                .graphAttr().with(Rank.dir(Rank.RankDir.LEFT_TO_RIGHT))
                .nodeAttr().with(Font.name("arial"))
                .linkAttr().with("class", "link-class")
                .with(
                        node("a").with(Color.RED).link(node("b")),
                        node("b").link(
                                to(node("c")).with(Attributes.attr("weight", 5), Style.DASHED)
                        )
                );
    }

    private static MutableGraph readGraph(File file) {
        try (InputStream dot = new FileInputStream(file)) {
            return new Parser().read(dot);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static BufferedImage generateImage(MutableGraph g) {
        return Graphviz.fromGraph(g).scale(1.0).render(Format.PNG).toImage();
    }

    public void refresh() {
        BufferedImage bufferedImage = generateImage(readGraph(file));
        graphPanel.setBufferedImage(bufferedImage);
    }

}
