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
import org.eclipse.swt.SWTException;
import org.eclipse.swt.widgets.Display;

import java.awt.*;
import java.util.concurrent.*;
import java.util.function.Supplier;

/**
 * Various utility method to bridge the gap between SWT and AWT.
 *
 * <p>
 * <strong>CAUTION:</strong> This code is barely in its infancy
 * and is likely to have bugs. Use at your own risk.
 * </p>
 */
public abstract class SWT_AWTBridge {

    /**
     * Allow to compute / get a value from AWT's Event Dispatch Thread.
     *
     * <p>This is particularly useful if a code needs to wait for
     * something to happen in AWT world that needs to be used in the SWT world.
     * Indeed, blocking the SWT thread could cause deadlocks.
     * </p>
     *
     * @param <T> the type of the value to compute
     * @param task The task that produce the value to run.
     * @return the value computed in AWT's Event Dispatch Thread.
     */
    public static <T> T computeInEDT(Supplier<T> task) {
        if (EventQueue.isDispatchThread()) {
            return task.get();
        } else {
            try {
                // Never block the main SWT thread
                var currentDisplay = Display.getCurrent();
                if (currentDisplay == null) {
                    throw new IllegalStateException("No current display");
                }

                var completion = new CompletableFuture<T>();
                EventQueue.invokeLater(() -> {
                    try {
                        completion.complete(task.get());
                    } catch (Throwable t) {
                        completion.completeExceptionally(t);
                    }
                });
                // poll the result until it is finished
                while (!completion.isDone()) {
                    if (!currentDisplay.readAndDispatch()) {
                        currentDisplay.sleep();
                    }
                }

                return completion.get();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Equivalent to {@link java.awt.EventQueue#invokeAndWait(Runnable)} but never blocks the main SWT thread.
     *
     * <p>This is particularly useful if a code needs to wait for
     * something in AWT world to be finished before continuing in SWT world.
     * Indeed, blocking the SWT thread could cause deadlocks, and
     * using {@link java.awt.EventQueue#invokeLater(Runnable)} may involves races.
     * </p>
     *
     * @param runnable The task to run.
     */
    public static void invokeInEDTAndWait(Runnable runnable) {
        computeInEDT(() -> {
            runnable.run();
            return null;
        });
    }

    /**
     * Convert an SWT Color to an AWT Color (RGBa).
     *
     * <p>
     * Note that the color has to be acquired on the SWT thread.
     * </p>
     *
     * @param swtColor The SWT color to convert.
     * @return The AWT color equivalent.
     */
    public static Color toAWTColor(org.eclipse.swt.graphics.Color swtColor) {
        return new Color(
                swtColor.getRed(),
                swtColor.getGreen(),
                swtColor.getBlue(),
                swtColor.getAlpha()
        );
    }

    /**
     * Convert an AWT Color to an SWT Color (RGBA).
     *
     * @param awtColor The AWT color to convert.
     * @return The SWT color equivalent.
     */
    public static org.eclipse.swt.graphics.Color toSWTColor(Color awtColor) {
        return new org.eclipse.swt.graphics.Color(
                awtColor.getRed(),
                awtColor.getGreen(),
                awtColor.getBlue(),
                awtColor.getAlpha()
        );
    }

    public static Point toAWTPoint(org.eclipse.swt.graphics.Point swtPoint) {
        return new Point(swtPoint.x, swtPoint.y);
    }

    public static org.eclipse.swt.graphics.Point toSWTPoint(Point awtPoint) {
        return new org.eclipse.swt.graphics.Point(awtPoint.x, awtPoint.y);
    }

    /**
     * Queue an SWT task on a different thread to avoid deadlock with AWT, in particular different that AWT Event Dispatch Thread.
     *
     * <p>
     *     The issue is that invoking {@link Display#asyncExec(Runnable)} from AWT Event Dispatch Thread
     *     may dead lock the application (as it synchronizes on the {@link org.eclipse.swt.graphics.Device} class).
     *     Typically this happens when SWT is trying to dispose the shell/display, but there is still pending tasks
     *     that want to {@code asyncExec}.
     * </p>
     *
     * @param display The contextual display
     * @param task The task to run
     */
    public static void invokeSwtAwayFromAwt(Display display, Runnable task) {
        if (display.isDisposed()) {
            return;
        }
        SideExecutorHolder.es.execute(() -> {
            try {
                if (display.isDisposed()) {
                    return;
                }
                task.run();
            } catch (SWTException e) {
                if (e.code != SWT.ERROR_DEVICE_DISPOSED) {
                    throw e;
                }
            }
        });
    }

    private static abstract class SideExecutorHolder {
        static final ExecutorService es = Executors.newSingleThreadExecutor(targetRunnable -> {
            var thread = new Thread(targetRunnable, "Side-SWT-Queue");
            thread.setDaemon(true);
            return thread;
        });
    }
}
