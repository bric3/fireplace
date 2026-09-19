/*
 * Fireplace
 *
 * Copyright (c) 2021, Today - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.github.bric3.fireplace.swt_awt;

import org.eclipse.swt.SWT;
import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.ContainerOrderFocusTraversalPolicy;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

class EmbeddingCompositeTest {

    @Test
    void providesHeavyweightSwingRootPaneContainer() {
        var content = new JPanel();
        var container = new EmbeddingComposite.SwingRootPaneContainer(content);

        assertSoftly(softly -> {
            softly.assertThat(container.isLightweight()).isFalse();
            softly.assertThat(container.getLayout()).isInstanceOf(BorderLayout.class);
            softly.assertThat(container.getComponents()).containsExactly(container.getRootPane());
            softly.assertThat(container.getRootPane().isOpaque()).isTrue();
            softly.assertThat(container.getContentPane().getComponents()).containsExactly(content);
            softly.assertThat(content.getParent()).isSameAs(container.getContentPane());
            softly.assertThat(container.getInputContext()).isNull();
        });
    }

    @Test
    void delegatesRootPaneContainerProperties() {
        var container = new EmbeddingComposite.SwingRootPaneContainer(new JPanel());
        var contentPane = new JPanel();
        var layeredPane = new JLayeredPane();
        var glassPane = new JPanel();

        container.setContentPane(contentPane);
        container.setLayeredPane(layeredPane);
        container.setGlassPane(glassPane);

        assertSoftly(softly -> {
            softly.assertThat(container.getContentPane()).isSameAs(contentPane);
            softly.assertThat(container.getLayeredPane()).isSameAs(layeredPane);
            softly.assertThat(container.getGlassPane()).isSameAs(glassPane);
        });
    }

    @Test
    void updatesWithoutClearingTheHeavyweightBackground() {
        var container = new EmbeddingComposite.SwingRootPaneContainer(new JPanel());
        container.setSize(4, 4);
        container.setBackground(Color.RED);
        var image = new BufferedImage(4, 4, BufferedImage.TYPE_INT_ARGB);
        var graphics = image.createGraphics();
        graphics.setColor(Color.BLUE);
        graphics.fillRect(0, 0, image.getWidth(), image.getHeight());

        container.update(graphics);
        graphics.dispose();

        assertThat(new Color(image.getRGB(1, 1), true)).isEqualTo(Color.BLUE);
    }

    @Test
    void mapsOnlyBoundaryTabPressesToSwtTraversal() {
        var focusCycleRoot = new JPanel();
        var first = new JButton("first");
        var middle = new JButton("middle");
        var last = new JButton("last");
        focusCycleRoot.add(first);
        focusCycleRoot.add(middle);
        focusCycleRoot.add(last);
        focusCycleRoot.setFocusCycleRoot(true);
        focusCycleRoot.setFocusTraversalPolicy(new ContainerOrderFocusTraversalPolicy() {
            @Override
            public java.awt.Component getFirstComponent(Container container) {
                return first;
            }

            @Override
            public java.awt.Component getLastComponent(Container container) {
                return last;
            }
        });

        assertSoftly(softly -> {
            softly.assertThat(direction(tabPressed(last, 0), last, focusCycleRoot))
                  .isEqualTo(SWT.TRAVERSE_TAB_NEXT);
            softly.assertThat(direction(tabPressed(first, KeyEvent.SHIFT_DOWN_MASK), first, focusCycleRoot))
                  .isEqualTo(SWT.TRAVERSE_TAB_PREVIOUS);
            softly.assertThat(direction(tabPressed(first, 0), first, focusCycleRoot))
                  .isEqualTo(SWT.TRAVERSE_NONE);
            softly.assertThat(direction(tabPressed(last, KeyEvent.SHIFT_DOWN_MASK), last, focusCycleRoot))
                  .isEqualTo(SWT.TRAVERSE_NONE);
            softly.assertThat(direction(tabPressed(middle, 0), middle, focusCycleRoot))
                  .isEqualTo(SWT.TRAVERSE_NONE);
            softly.assertThat(direction(tabReleased(last), last, focusCycleRoot))
                  .isEqualTo(SWT.TRAVERSE_NONE);
            softly.assertThat(direction(keyPressed(last, KeyEvent.VK_ENTER), last, focusCycleRoot))
                  .isEqualTo(SWT.TRAVERSE_NONE);
            softly.assertThat(direction(tabPressed(last, 0), new JButton("outside"), focusCycleRoot))
                  .isEqualTo(SWT.TRAVERSE_NONE);
        });
    }

    private static int direction(KeyEvent event, java.awt.Component focusOwner, Container focusCycleRoot) {
        return EmbeddingComposite.swtTraversalDirection(event, focusOwner, focusCycleRoot);
    }

    private static KeyEvent tabPressed(java.awt.Component source, int modifiers) {
        return new KeyEvent(source, KeyEvent.KEY_PRESSED, 0, modifiers, KeyEvent.VK_TAB, '\t');
    }

    private static KeyEvent tabReleased(java.awt.Component source) {
        return new KeyEvent(source, KeyEvent.KEY_RELEASED, 0, 0, KeyEvent.VK_TAB, '\t');
    }

    private static KeyEvent keyPressed(java.awt.Component source, int keyCode) {
        return new KeyEvent(source, KeyEvent.KEY_PRESSED, 0, 0, keyCode, KeyEvent.CHAR_UNDEFINED);
    }
}
