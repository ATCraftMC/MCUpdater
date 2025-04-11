package org.atcraftmc.updater.client.ui.component;

import javax.swing.*;
import java.awt.*;

public final class ImagePanel extends JPanel {
    private Image image = null;

    public void paintImage(Image image) {
        this.image = image;
        this.repaint();
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(this.image, 0, 0, this.getWidth(), this.getHeight(), null);
    }
}
