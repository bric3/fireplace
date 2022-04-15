/*
 * Fireplace
 *
 * Copyright (c) 2021, Today - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.github.bric3.fireplace.ui;

import com.github.weisj.darklaf.platform.SystemInfo;
import com.github.weisj.darklaf.platform.decorations.ExternalLafDecorator;

import javax.swing.*;
import java.awt.*;

public class TitleBar extends JPanel {
    public TitleBar(JComponent component) {
        setLayout(new BorderLayout());
        add(component, BorderLayout.CENTER);
    }

    @Override
    public void doLayout() {
        if (SystemInfo.isMac) {
            add(WindowButtonSpace.INSTANCE, BorderLayout.WEST);
        }
        if (SystemInfo.isWindows || SystemInfo.isLinux) {
            add(WindowButtonSpace.INSTANCE);
        }
        super.doLayout();
    }

    private static class WindowButtonSpace extends JComponent {
        private static final long serialVersionUID = 1L;
        private static final WindowButtonSpace INSTANCE = new WindowButtonSpace();

        private WindowButtonSpace() {}

        private Rectangle _windowButtonRect = null;

        private Rectangle windowButtonRect() {
            if (_windowButtonRect == null) {
                _windowButtonRect = ExternalLafDecorator.instance()
                                                        .decorationsManager()
                                                        .titlePaneLayoutInfo(getRootPane())
                                                        .windowButtonRect();
            }
            return _windowButtonRect;
        }

        public Dimension getPreferredSize() {
            var rectangle = windowButtonRect();
            var size = rectangle.getSize();
            if (SystemInfo.isMac) {
                size.width = size.width + rectangle.x;
            }
            if (SystemInfo.isWindows || SystemInfo.isLinux) {
                var rightAdjustment = getRootPane().getWidth() - windowButtonRect().x - windowButtonRect().width;
                size.width = size.width + rightAdjustment;
            }
            return size;
        }

        public Dimension getMinimumSize() {
            return getPreferredSize();
        }

        public Dimension getMaximumSize() {
            return getPreferredSize();
        }
    }
}
