package com.github.bric3.fireplace.ui;

import com.github.bric3.fireplace.DnDTarget;

import javax.swing.*;
import java.awt.*;

public class HudPanel {
    private final JPanel dndPanel;
    private final JPanel progressPanel;
    private final JPanel hudPanel;

    public HudPanel() {
        dndPanel = new GlassJPanel(new GridBagLayout());
        dndPanel.add(new JLabel("<html><font size=+4>Drag and drop JFR file here</font></html>"));
        dndPanel.setOpaque(false);
        dndPanel.setVisible(false);

        progressPanel = new GlassJPanel(new BorderLayout());
        var progress = new JProgressBar();
        progress.setIndeterminate(true);
        progressPanel.add(new JLabel("<html><font size=+4>Loading in progress</font></html>", SwingConstants.CENTER), BorderLayout.CENTER);
        progressPanel.add(progress, BorderLayout.SOUTH);

        hudPanel = new JPanel();
        hudPanel.setLayout(new BoxLayout(hudPanel, BoxLayout.Y_AXIS));
        hudPanel.add(dndPanel);
        hudPanel.add(progressPanel);
        hudPanel.setOpaque(false);
    }

    public JComponent getComponent() {
        return hudPanel;
    }

    public DnDTarget getDnDTarget() {
        return new DnDTarget() {
            @Override
            public JComponent getComponent() {
                return hudPanel;
            }

            @Override
            public void activate() {
                progressPanel.setVisible(false);
                dndPanel.setVisible(true);
            }

            @Override
            public void deactivate() {
                dndPanel.setVisible(false);
            }
        };
    }

    public void setProgressVisible(boolean visible) {
        if (visible) {
            dndPanel.setVisible(false);
        }
        progressPanel.setVisible(visible);
    }
}
