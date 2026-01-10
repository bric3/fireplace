/*
 * Fireplace
 *
 * Copyright (c) 2021, Today - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.github.bric3.fireplace.flamegraph;

import javax.swing.*;
import java.awt.*;

/**
 * Test utilities for Swing component testing.
 */
final class SwingTestUtil {

    private SwingTestUtil() {
        // utility class
    }

    /**
     * Recursively searches for a {@link JScrollPane} within the given container hierarchy.
     *
     * @param container the container to search within
     * @return the first JScrollPane found, or null if none exists
     */
    static JScrollPane findScrollPane(Container container) {
        for (var component : container.getComponents()) {
            if (component instanceof JScrollPane) {
                return (JScrollPane) component;
            }
            if (component instanceof Container) {
                var found = findScrollPane((Container) component);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}