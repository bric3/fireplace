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

import org.junit.jupiter.api.Test;

import java.awt.EventQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class SWT_AWTBridgeTest {

    @Test
    void convertsPointsBetweenSwtAndAwt() {
        var swtPoint = new org.eclipse.swt.graphics.Point(12, 34);
        var awtPoint = new java.awt.Point(12, 34);

        assertThat(SWT_AWTBridge.toAWTPoint(swtPoint)).isEqualTo(awtPoint);
        assertThat(SWT_AWTBridge.toSWTPoint(awtPoint)).isEqualTo(swtPoint);
    }

    @Test
    void runsTasksDirectlyWhenAlreadyOnEdt() throws Exception {
        var invokedOnEdt = new AtomicBoolean();

        EventQueue.invokeAndWait(() -> {
            assertThat(SWT_AWTBridge.computeInEDT(EventQueue::isDispatchThread)).isTrue();
            SWT_AWTBridge.invokeInEDTAndWait(() -> invokedOnEdt.set(EventQueue.isDispatchThread()));
        });

        assertThat(invokedOnEdt).isTrue();
    }
}
