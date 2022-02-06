/*
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * Copyright 2012-2018 the original author or authors.
 */
// https://github.com/assertj/assertj-swing/blob/9dae1fa4857fb9fc02ed77bf72f12b3ded75c8d2/assertj-swing/src/main/java/org/assertj/swing/edt/CheckThreadViolationRepaintManager.java

// idea from Scott Delap
// part 1 : https://web.archive.org/web/20071213073907/http://www.clientjava.com/blog/2004/08/20/1093059428000.html
// part 2 : https://web.archive.org/web/20071213073907/http://www.clientjava.com/blog/2004/08/24/1093363375000.html
// part 3 : https://web.archive.org/web/20071213073907/http://www.clientjava.com/blog/2004/08/31/1093972473000.html

package com.github.bric3.fireplace.ui.debug;

import javax.swing.*;
import java.lang.ref.WeakReference;
import java.util.Objects;

import static javax.swing.SwingUtilities.isEventDispatchThread;

/**
 * <p>
 * This class is used to detect Event Dispatch Thread rule violations<br>
 * See <a href="http://java.sun.com/docs/books/tutorial/uiswing/misc/threads.html">How to Use Threads</a> for more info
 * </p>
 *
 * <p>
 * This is a modification of original idea of Scott Delap.<br>
 * </p>
 *
 * @author Scott Delap
 * @author Alexander Potochkin
 * <p>
 * https://swinghelper.dev.java.net/
 */
public class CheckThreadViolationRepaintManager extends RepaintManager {
    // Should always be turned on because it shouldn't matter
    // whether the component is showing (realized) or not.
    // This flag exists only for historical reasons, see
    // https://stackoverflow.com/questions/491323/is-it-safe-to-construct-swing-awt-widgets-not-on-the-event-dispatch-thread
    private final boolean completeCheck;

    private WeakReference<JComponent> lastComponent;

    CheckThreadViolationRepaintManager() {
        // it is recommended to pass the complete check
        this(true);
    }

    CheckThreadViolationRepaintManager(boolean completeCheck) {
        this.completeCheck = completeCheck;
    }

    public static void install() {
        RepaintManager.setCurrentManager(new CheckThreadViolationRepaintManager());
    }

    @Override
    public synchronized void addInvalidComponent(JComponent component) {
        checkThreadViolations(Objects.requireNonNull(component));
        super.addInvalidComponent(component);
    }

    @Override
    public void addDirtyRegion(JComponent component, int x, int y, int w, int h) {
        checkThreadViolations(Objects.requireNonNull(component));
        super.addDirtyRegion(component, x, y, w, h);
    }

    /**
     * Rules enforced by this method:
     * (1) it is always OK to reach this method on the Event Dispatch Thread.
     * (2) it is generally not OK to reach this method outside the Event Dispatch Thread.
     * (3) (exception form rule 2) except when we get here from a repaint() call, because repaint() is thread-safe
     * (4) (exception from rule 3) it is not OK if swing code calls repaint() outside the EDT, because swing code should be called on the EDT.
     * (5) (exception from rule 4) using SwingWorker subclasses should not be considered swing code.
     */
    private void checkThreadViolations(JComponent c) {
        if (!isEventDispatchThread() && (completeCheck || c.isShowing())) {
            boolean imageUpdate = false;
            boolean repaint = false;
            boolean fromSwing = false; // whether we were in a swing method before the repaint() call
            var stackTrace = Thread.currentThread().getStackTrace();
            for (var st : stackTrace) {
                if (repaint
                    && st.getClassName().startsWith("javax.swing.")
                    && !st.getClassName().startsWith("javax.swing.SwingWorker")) {
                    fromSwing = true;
                }
                if (repaint && "imageUpdate".equals(st.getMethodName())) {
                    imageUpdate = true;
                }
                if ("repaint".equals(st.getMethodName())) {
                    repaint = true;
                    fromSwing = false;
                }
            }
            if (imageUpdate) {
                // assuming it is java.awt.image.ImageObserver.imageUpdate(...)
                // image was asynchronously updated, that's ok
                return;
            }
            if (repaint && !fromSwing) {
                // no problems here, since repaint() is thread safe
                return;
            }
            // ignore the last processed component
            if (lastComponent != null && c == lastComponent.get()) {
                return;
            }
            lastComponent = new WeakReference<>(c);
            violationFound(c, stackTrace);
        }
    }

    void violationFound(JComponent c, StackTraceElement[] stackTrace) {
        System.out.println();
        System.out.println("EDT violation detected on component " + c);

        // don't print java.lang.Thread.getStackTrace
        // don't print this class
        // skip java frames
        for (int i = 1; i < stackTrace.length; i++) {
            if (stackTrace[i].getClassName().startsWith(getClass().getName())
                //                || Objects.requireNonNullElse(stackTrace[i].getModuleName(), "").startsWith("java")
                //                || stackTrace[i].getClassName().startsWith("java")
            ) {
                continue;
            }
            StackTraceElement st = stackTrace[i];
            System.out.println("    at " + st);
        }
    }
}