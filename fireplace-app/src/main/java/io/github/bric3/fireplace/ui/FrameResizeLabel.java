package io.github.bric3.fireplace.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

public class FrameResizeLabel {
    private final JLabel dimensionLabel;
    private final JPanel dimensionOverlayPanel;
    private final Timer panelHider;

    public FrameResizeLabel() {
        dimensionLabel = new JLabel("hello");
        dimensionLabel.setVerticalAlignment(JLabel.CENTER);
        dimensionLabel.setHorizontalAlignment(JLabel.CENTER);
        dimensionLabel.setOpaque(true);
        dimensionLabel.setBorder(BorderFactory.createLineBorder(Color.black));
        dimensionOverlayPanel = new JPanel(new BorderLayout());
        dimensionOverlayPanel.add(dimensionLabel, BorderLayout.CENTER);
        dimensionOverlayPanel.setBackground(new Color(0, 0, 0, 0));
        dimensionOverlayPanel.setOpaque(false);
        dimensionOverlayPanel.setVisible(false);
        var textHeight = dimensionLabel.getFontMetrics(dimensionLabel.getFont()).getHeight();
        dimensionOverlayPanel.setMaximumSize(new Dimension(100, textHeight + 10));
        panelHider = new Timer(2_000, e -> dimensionOverlayPanel.setVisible(false));
        panelHider.setCoalesce(true);
    }

    public void installListener(JFrame frame) {
        frame.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                int height = frame.getHeight();
                int width = frame.getWidth();
                dimensionLabel.setText(height + " x " + width);
                dimensionOverlayPanel.setVisible(true);
                panelHider.restart();
            }
        });
    }

    public JComponent getComponent() {
        return dimensionOverlayPanel;
    }
}
