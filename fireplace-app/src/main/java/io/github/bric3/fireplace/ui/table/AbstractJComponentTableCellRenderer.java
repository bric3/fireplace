/*
 * Fireplace
 *
 * Copyright (c) 2021, Today - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.github.bric3.fireplace.ui.table;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

public abstract class AbstractJComponentTableCellRenderer<C extends JComponent> extends JComponent implements TableCellRenderer {

    /**
     * The renderer that should get the updateUI propagation. Nullable.
     */
    protected C renderComponent;

    /**
     * Constructs an {@code AbstractJComponentTableCellRenderer} that uses a given renderer component.
     * @param renderComponent the component to be used as renderer, <code>null</code> accepted.
     */
    public AbstractJComponentTableCellRenderer(C renderComponent) {
        this.renderComponent = renderComponent;
    }

    /**
     * Notification from the `UIManager` that the look and feel
     * [L&amp;F] has changed.
     * Replaces the current UI object with the latest version from the
     * `UIManager`.
     *
     * @see JComponent#updateUI
     */
    @Override
    public void updateUI() {
        if (renderComponent != null) {
            SwingUtilities.updateComponentTreeUI(renderComponent);
        }
    }

    //// Class field method order not respected to mark the copy/paste of DefaultTableCellRenderer methods.
    //// The rest of this class is copy/paste of DefaultTableCellRenderer
    //// except the firePropertyChange methods

    /**
     * Overridden for performance reasons.
     * See the <a href="#override">Implementation Note</a>
     * for more information.
     *
     * @since 1.5
     */
    public void invalidate() {}

    /**
     * Overridden for performance reasons.
     * See the <a href="#override">Implementation Note</a>
     * for more information.
     */
    public void validate() {}

    /**
     * Overridden for performance reasons.
     * See the <a href="#override">Implementation Note</a>
     * for more information.
     */
    public void revalidate() {}

    /**
     * Overridden for performance reasons.
     * See the <a href="#override">Implementation Note</a>
     * for more information.
     */
    public void repaint(long tm, int x, int y, int width, int height) {}

    /**
     * Overridden for performance reasons.
     * See the <a href="#override">Implementation Note</a>
     * for more information.
     */
    public void repaint(Rectangle r) { }

    /**
     * Overridden for performance reasons.
     * See the <a href="#override">Implementation Note</a>
     * for more information.
     *
     * @since 1.5
     */
    public void repaint() {
    }
}
