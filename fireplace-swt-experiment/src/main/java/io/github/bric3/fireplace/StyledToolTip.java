/*
 * Fireplace
 *
 * Copyright (c) 2021, Today - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.github.bric3.fireplace;

import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.window.DefaultToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.forms.widgets.FormText;

/**
 * This tool tip extends the Jface implementation and relies on the {@link FormText} control
 * to render the text.
 *
 * @author brice.dutheil
 * @see FormText
 */
public class StyledToolTip extends DefaultToolTip {
    public StyledToolTip(Control control) {
        super(control);
    }

    public StyledToolTip(Control control, int style, boolean manualActivation) {
        super(control, style, manualActivation);
    }

    @Override
    protected Composite createToolTipContentArea(Event event, Composite parent) {
        final Composite container = setColorsAndFont(new Composite(parent, SWT.NULL), event);
        GridLayoutFactory.fillDefaults().margins(2, 2).generateLayout(container);
        var formText = setColorsAndFont(new FormText(container, SWT.NONE), event);

        String pseudoHtml = getText(event);

        formText.setText(pseudoHtml, true, false);
        return parent;
    }

    private <T extends Control> T setColorsAndFont(T control, Event event) {
        control.setBackground(getBackgroundColor(event));
        control.setForeground(getForegroundColor(event));
        control.setFont(getFont(event));
        return control;
    }
} 