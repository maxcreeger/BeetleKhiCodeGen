package gui.graph;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

class ImagePanel extends JPanel {

    BufferedImage bufferedImage;

    public void setBufferedImage(BufferedImage img) {
        this.bufferedImage = img;
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        if (bufferedImage != null) {
            g.drawImage(bufferedImage, 0, 0, this);
        }
    }
}
