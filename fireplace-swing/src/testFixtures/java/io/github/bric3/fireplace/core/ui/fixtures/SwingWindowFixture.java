/*
 * Fireplace
 *
 * Copyright (c) 2021, Today - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.github.bric3.fireplace.core.ui.fixtures;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/** Window lifecycle and EDT failure ownership shared by mounted component fixtures. */
public final class SwingWindowFixture implements AutoCloseable {
    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    private static final AtomicBoolean ACTIVE = new AtomicBoolean();
    private static final AtomicReference<Throwable> UNUSABLE = new AtomicReference<>();

    private final FailureQueue queue = new FailureQueue();
    private final JFrame window;
    private boolean closed;

    private SwingWindowFixture(int width, int height) {
        window = runOnEdt(() -> {
            Toolkit.getDefaultToolkit().getSystemEventQueue().push(queue);
            var frame = new JFrame("Fireplace UI test");
            frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            frame.setSize(width, height);
            frame.setLocation(40, 40);
            queue.window = frame;
            return frame;
        });
    }

    public static SwingWindowFixture open(int width, int height) {
        if (UNUSABLE.get() != null) {
            throw new AssertionError("Previous UI scope failed; use a fresh test JVM", UNUSABLE.get());
        }
        if (!ACTIVE.compareAndSet(false, true)) {
            throw new IllegalStateException("Mounted UI scenarios must run serially");
        }
        try {
            return new SwingWindowFixture(width, height);
        } catch (RuntimeException | Error failure) {
            ACTIVE.set(false);
            UNUSABLE.compareAndSet(null, failure);
            throw failure;
        }
    }

    public void mount(JComponent component) {
        onEdt(() -> {
            window.setContentPane(component);
            window.setVisible(true);
        });
        await("window layout", () -> component.isShowing() && component.getWidth() > 0);
    }

    public JFrame window() {
        requireEdt();
        return window;
    }

    public static void requireEdt() {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("Inspect or change Swing state through onEdt");
        }
    }

    public <T> T onEdt(Supplier<T> action) {
        checkFailures();
        T result = runOnEdt(action);
        checkFailures();
        return result;
    }

    public void onEdt(Runnable action) {
        onEdt(() -> {
            action.run();
            return null;
        });
    }

    public void await(String description, BooleanSupplier condition) {
        await(description, TIMEOUT, condition);
    }

    void await(String description, Duration timeout, BooleanSupplier condition) {
        await(description, timeout, condition, true);
    }

    /** Drain owned work even after an asynchronous failure; close still reports that failure. */
    public void awaitCleanup(String description, BooleanSupplier condition) {
        await(description, TIMEOUT, condition, false);
    }

    private void await(String description, Duration timeout, BooleanSupplier condition, boolean checkFailure) {
        if (SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("Await completion off the EDT");
        }
        long deadline = System.nanoTime() + timeout.toNanos();
        do {
            if (checkFailure) {
                checkFailures();
            }
            boolean complete = runOnEdt(condition::getAsBoolean);
            if (checkFailure) {
                checkFailures();
            }
            if (complete) {
                return;
            }
            try {
                Thread.sleep(5);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                UNUSABLE.compareAndSet(null, interrupted);
                throw new AssertionError("Interrupted while awaiting " + description, interrupted);
            }
        } while (System.nanoTime() < deadline);
        var failure = new AssertionError("Timed out awaiting " + description);
        UNUSABLE.compareAndSet(null, failure);
        throw failure;
    }

    public void retireMouseInput() {
        requireEdt();
        queue.retiring = true;
    }

    private void checkFailures() {
        Throwable failure = queue.failure.get();
        if (failure != null) {
            throw new AssertionError("Asynchronous EDT callback failed in this UI scope", failure);
        }
    }

    // Cleanup must still run after the recorder has observed a failure.
    public static <T> T runOnEdt(Supplier<T> action) {
        if (SwingUtilities.isEventDispatchThread()) {
            return action.get();
        }
        var invocation = new FutureTask<>(action::get);
        SwingUtilities.invokeLater(invocation);
        try {
            return invocation.get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        } catch (ExecutionException failure) {
            Throwable cause = failure.getCause();
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw new AssertionError(cause);
        } catch (InterruptedException failure) {
            Thread.currentThread().interrupt();
            UNUSABLE.compareAndSet(null, failure);
            throw new AssertionError("Interrupted awaiting EDT", failure);
        } catch (TimeoutException failure) {
            UNUSABLE.compareAndSet(null, failure);
            throw new AssertionError("EDT did not complete within " + TIMEOUT, failure);
        }
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        try {
            runOnEdt(() -> {
                queue.retiring = true;
                window.dispose();
                return null;
            });
            runOnEdt(() -> {
                queue.remove();
                return null;
            });
            checkFailures();
        } catch (RuntimeException | Error failure) {
            UNUSABLE.compareAndSet(null, failure);
            throw failure;
        } finally {
            ACTIVE.set(false);
        }
    }

    private static final class FailureQueue extends EventQueue {
        private final AtomicReference<Throwable> failure = new AtomicReference<>();
        private JFrame window;
        private boolean retiring;

        @Override
        protected void dispatchEvent(AWTEvent event) {
            // Stop new native input only while draining and disposing this window.
            if (retiring && event instanceof MouseEvent
                && SwingUtilities.isDescendingFrom(((MouseEvent) event).getComponent(), window)) {
                return;
            }
            try {
                super.dispatchEvent(event);
            } catch (Throwable thrown) {
                failure.compareAndSet(null, thrown);
                UNUSABLE.compareAndSet(null, thrown);
            }
        }

        private void remove() {
            pop();
        }
    }
}
