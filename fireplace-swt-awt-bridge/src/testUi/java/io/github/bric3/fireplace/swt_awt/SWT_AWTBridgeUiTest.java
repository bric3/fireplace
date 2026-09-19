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

import org.eclipse.swt.widgets.Display;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.awt.EventQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("ui")
@Timeout(value = 30, unit = TimeUnit.SECONDS, threadMode = Timeout.ThreadMode.SAME_THREAD)
class SWT_AWTBridgeUiTest {
    private Display display;

    @BeforeEach
    void setUp() {
        display = new Display();
    }

    @AfterEach
    void tearDown() {
        if (!display.isDisposed()) {
            display.dispose();
        }
    }

    @Test
    void computesOnTheEdtFromTheSwtThreadAndPropagatesFailures() {
        assertThat(SWT_AWTBridge.computeInEDT(EventQueue::isDispatchThread)).isTrue();

        var invokedOnEdt = new AtomicBoolean();
        SWT_AWTBridge.invokeInEDTAndWait(() -> invokedOnEdt.set(EventQueue.isDispatchThread()));
        assertThat(invokedOnEdt).isTrue();

        assertThatThrownBy(() -> SWT_AWTBridge.computeInEDT(() -> {
            throw new IllegalStateException("boom");
        })).isInstanceOf(RuntimeException.class)
          .hasRootCauseInstanceOf(IllegalStateException.class);
    }

    @Test
    void runsSwtQueueingAwayFromAwtAndSkipsDisposedDisplays() throws InterruptedException {
        var caller = Thread.currentThread();
        var worker = new AtomicReference<Thread>();
        var workerWasEdt = new AtomicBoolean(true);
        var completed = new CountDownLatch(1);

        SWT_AWTBridge.invokeSwtAwayFromAwt(display, () -> {
            worker.set(Thread.currentThread());
            workerWasEdt.set(EventQueue.isDispatchThread());
            completed.countDown();
        });

        assertThat(completed.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(worker.get()).isNotSameAs(caller);
        assertThat(workerWasEdt).isFalse();

        display.dispose();
        var invokedAfterDisposal = new AtomicBoolean();
        SWT_AWTBridge.invokeSwtAwayFromAwt(display, () -> invokedAfterDisposal.set(true));
        assertThat(invokedAfterDisposal).isFalse();
    }
}
