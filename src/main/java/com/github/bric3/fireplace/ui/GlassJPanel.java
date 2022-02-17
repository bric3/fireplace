package com.github.bric3.fireplace.ui;

import javax.swing.*;
import java.awt.*;

public class GlassJPanel extends JPanel {
    public GlassJPanel() {
        this(new GridBagLayout());
    }

    public GlassJPanel(LayoutManager layout) {
        super(layout);
    }

    @Override
    protected void paintComponent(Graphics g) {
        var g2 = (Graphics2D) g;
        g2.setColor(Colors.darkMode ? Colors.translucent_black_80 : Colors.translucent_white_D0);
        g2.fillRect(0, 0, getWidth(), getHeight());
    }
}
