package io.github.bric3.fireplace.ui;

import io.github.bric3.fireplace.core.ui.Colors;
import io.github.bric3.fireplace.core.ui.LightDarkColor;

import javax.swing.*;
import java.awt.*;

public class GlassJPanel extends JPanel {
    private static final Color TRANSLUCENT_BACKGROUND = new LightDarkColor(
            Colors.translucent_white_D0,
            Colors.translucent_black_80
    );

    public GlassJPanel() {
        this(new GridBagLayout());
    }

    public GlassJPanel(LayoutManager layout) {
        super(layout);
    }

    @Override
    protected void paintComponent(Graphics g) {
        var g2 = (Graphics2D) g;
        g2.setColor(TRANSLUCENT_BACKGROUND);
        g2.fillRect(0, 0, getWidth(), getHeight());
    }
}
