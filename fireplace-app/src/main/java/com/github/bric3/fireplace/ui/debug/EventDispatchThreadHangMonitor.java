package com.github.bric3.fireplace.ui.debug;


import java.awt.*;
import java.awt.event.WindowEvent;
import java.util.LinkedList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Monitors the AWT event dispatch thread for events that take longer than
 * a certain time to be dispatched.
 * <p>
 * The principle is to record the time at which we start processing an event,
 * and have another thread check frequently to see if we're still processing.
 * If the other thread notices that we've been processing a single event for
 * too long, it prints a stack trace showing what the event dispatch thread
 * is doing, and continues to time it until it finally finishes.
 * <p>
 * This is useful in determining what code is causing your Java application's
 * GUI to be unresponsive.
 *
 * @author Elliott Hughes <enh@jessies.org>
 * <p>
 * Advice, bug fixes, and test cases from Alexander Potochkin and
 * Oleg Sukhodolsky.
 * <p>
 * Code extracted from the
 * <a href="https://github.com/swingexplorer/swingexplorer/blob/7539add39b8b6741f59baab87a4034778c782113/swingexplorer-core/src/main/java/org/swingexplorer/edt_monitor/EDTDebugQueue.java#L221">swing-explorer's variation</a>
 * and lightly modified by Brice Dutheil
 */
public final class EventDispatchThreadHangMonitor extends EventQueue {
    private static final EventDispatchThreadHangMonitor INSTANCE = new EventDispatchThreadHangMonitor();

    // Time to wait between checks that the event dispatch thread isn't hung.
    private static final long CHECK_INTERVAL_MS = 100;

    // Help distinguish multiple hangs in the log, and match start and end too.
    // Only access this via getNewHangNumber.
    private static int hangCount = 0;

    // Prevents us complaining about hangs during start-up, which are probably
    // the JVM vendor's fault.
    private boolean haveShownSomeComponent = false;

    // The currently outstanding event dispatches. The implementation of
    // modal dialogs is a common cause for multiple outstanding dispatches.
    private final LinkedList<DispatchInfo> dispatches = new LinkedList<>();

    // Maximum time we won't warn about. This used to be 500 ms, but 1.5 on
    // late-2004 hardware isn't really up to it; there are too many parts of
    // the JDK that can go away for that long (often code that has to be
    // called on the event dispatch thread, like font loading).
    private static long minimalMonitoredHangTime = 1000;
    private static Consumer<Problem> problemListener = System.out::println;

    // this flag is used for temporary switching off
    // events about hangs. It is useful fow SwingExplorer
    // when it performs image capturing that can be quite long
    // and occurs in the EDT
    // The flag is in force only on a single event and it always
    // reset to "false" after event is dispatched
    public static boolean disableHangEvents = false;

    static boolean monitorHangs = false;
    static boolean monitorExceptions = false;
    private ScheduledExecutorService scheduledExecutorService;

    public static class Problem {
        public final String description;
        public final StackTraceElement[] currentStack;

        public Problem(String description, StackTraceElement[] currentStack) {
            this.description = description;
            this.currentStack = currentStack;
        }

        @Override
        public String toString() {
            return description + "\n" + stackTraceToString(currentStack);
        }
    }

    static void setProblemListener(Consumer<Problem> problemListener) {
        EventDispatchThreadHangMonitor.problemListener = problemListener;
    }

    public static EventDispatchThreadHangMonitor getInstance() {
        return INSTANCE;
    }

    /**
     * Sets up hang detection for the event dispatch thread.
     */
    public static void initMonitoring() {
        Toolkit.getDefaultToolkit().getSystemEventQueue().push(INSTANCE);
        Runtime.getRuntime().addShutdownHook(new Thread(INSTANCE::dispose));
    }

    static long getMinimalMonitoredHangTime() {
        return minimalMonitoredHangTime;
    }

    static void setMinimalMonitoredHangTime(long _minimalMonitoredHangTime) {
        minimalMonitoredHangTime = _minimalMonitoredHangTime;
    }

    private static void fireProblem(Problem problem) {
        if (problemListener != null && !disableHangEvents) {
            problemListener.accept(problem);
        }
    }

    private EventDispatchThreadHangMonitor() {
        initTimer();
    }


    /**
     * Sets up a timer to check for hangs frequently.
     */
    private void initTimer() {
        final long initialDelayMs = 0;
        final boolean isDaemon = true;
        var tf = new ThreadFactory() {
            private final AtomicInteger threadNumber = new AtomicInteger(1);

            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(Thread.currentThread().getThreadGroup(),
                                      r,
                                      "EventDispatchThreadHangMonitor" + threadNumber.getAndIncrement(),
                                      0);
                t.setDaemon(true);
                if (t.getPriority() != Thread.NORM_PRIORITY)
                    t.setPriority(Thread.NORM_PRIORITY);
                return t;
            }
        };

        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(tf);
        scheduledExecutorService.scheduleAtFixedRate(() -> {
            synchronized (dispatches) {
                if (dispatches.isEmpty() || !haveShownSomeComponent) {
                    // Nothing to do.
                    // We don't destroy the timer when there's nothing happening
                    // because it would mean a lot more work on every single AWT
                    // event that gets dispatched.
                    return;
                }
                // Only the most recent dispatch can be hung; nested dispatches
                // by their nature cause the outer dispatch pump to be suspended.
                dispatches.getLast().checkForHang();
            }
        }, initialDelayMs, CHECK_INTERVAL_MS, TimeUnit.MILLISECONDS);
        //        Timer timer = new Timer("EventDispatchThreadHangMonitor", isDaemon);
        //        timer.schedule(new HangChecker(), initialDelayMs, CHECK_INTERVAL_MS);
    }

    //    private class HangChecker extends TimerTask {
    //        @Override
    //        public void run() {
    //            synchronized (dispatches) {
    //                if (dispatches.isEmpty() || !haveShownSomeComponent) {
    //                    // Nothing to do.
    //                    // We don't destroy the timer when there's nothing happening
    //                    // because it would mean a lot more work on every single AWT
    //                    // event that gets dispatched.
    //                    return;
    //                }
    //                // Only the most recent dispatch can be hung; nested dispatches
    //                // by their nature cause the outer dispatch pump to be suspended.
    //                dispatches.getLast().checkForHang();
    //            }
    //        }
    //    }

    private static class DispatchInfo {
        // The last-dumped hung stack trace for this dispatch.
        private StackTraceElement[] lastReportedStack;
        // If so; what was the identifying hang number?
        private int hangNumber;

        // The EDT for this dispatch (for the purpose of getting stack traces).
        // I don't know of any API for getting the event dispatch thread,
        // but we can assume that it's the current thread if we're in the
        // middle of dispatching an AWT event...
        // We can't cache this because the EDT can die and be replaced by a
        // new EDT if there's an uncaught exception.
        private final Thread eventDispatchThread = Thread.currentThread();

        // The last time in milliseconds at which we saw a dispatch on the above thread.
        private long lastDispatchTimeMillis = System.currentTimeMillis();

        DispatchInfo() {
            // All initialization is done by the field initializers.
        }

        void checkForHang() {
            if (timeSoFar() > getMinimalMonitoredHangTime()) {
                examineHang();
            }
        }

        // We can't use StackTraceElement.equals because that insists on checking the filename and line number.
        // That would be version-specific.
        private static boolean stackTraceElementIs(StackTraceElement e, String className, String methodName, boolean isNative) {
            return e.getClassName().equals(className)
                   && e.getMethodName().equals(methodName)
                   && e.isNativeMethod() == isNative;
        }

        // Checks whether the given stack looks like it's waiting for another event.
        // This relies on JDK implementation details.
        private boolean isWaitingForNextEvent(StackTraceElement[] currentStack) {
            return stackTraceElementIs(currentStack[0],
                                       "java.lang.Object",
                                       "wait",
                                       true)
                   && stackTraceElementIs(currentStack[1],
                                          "java.lang.Object",
                                          "wait",
                                          false)
                   && stackTraceElementIs(currentStack[2],
                                          "java.awt.EventQueue",
                                          "getNextEvent",
                                          false);
        }

        private void examineHang() {
            var currentStack = eventDispatchThread.getStackTrace();

            if (isWaitingForNextEvent(currentStack)) {
                // Don't be fooled by a modal dialog if it's waiting for its next event.
                // As long as the modal dialog's event pump doesn't get stuck, it's okay for the outer pump to be suspended.
                return;
            }

            if (stacksEqual(lastReportedStack, currentStack)) {
                // Don't keep reporting the same hang every time the timer goes off.
                return;
            }

            hangNumber = getNewHangNumber();
            lastReportedStack = currentStack;

            if (monitorHangs) {
                fireProblem(new Problem("EDT hang end after " + timeSoFar() + "ms", currentStack));
            }
        }

        private static boolean stacksEqual(StackTraceElement[] a, StackTraceElement[] b) {
            if (a == null) {
                return false;
            }
            if (a.length != b.length) {
                return false;
            }
            for (int i = 0; i < a.length; ++i) {
                if (!a[i].equals(b[i])) {
                    return false;
                }
            }
            return true;
        }

        /**
         * Returns how long this dispatch has been going on (in milliseconds).
         */
        private long timeSoFar() {
            return (System.currentTimeMillis() - lastDispatchTimeMillis);
        }

        public void dispose() {
            // reset
            if (disableHangEvents) {
                disableHangEvents = false;
                return;
            }

            if (lastReportedStack != null) {
                if (monitorHangs) {
                    fireProblem(new Problem("EDT hang end after " + timeSoFar() + "ms", lastReportedStack));
                }
            }
        }
    }


    /**
     * Overrides EventQueue.dispatchEvent to call our pre and post hooks either
     * side of the system's event dispatch code.
     */
    @Override
    protected void dispatchEvent(AWTEvent event) {
        try {
            preDispatchEvent();
            super.dispatchEvent(event);
        } catch (RuntimeException ex) {
            if (monitorExceptions) {
                fireProblem(new Problem("Exception caught in the Event Dispatch thread: " + ex.getMessage(), ex.getStackTrace()));
            } else {
                throw ex;
            }
        } finally {
            postDispatchEvent();
            if (!haveShownSomeComponent &&
                event instanceof WindowEvent && event.getID() == WindowEvent.WINDOW_OPENED) {
                haveShownSomeComponent = true;
            }
        }
    }

    @SuppressWarnings("unused")
    private void debug(String which) {
        if (false) {
            for (int i = dispatches.size(); i >= 0; --i) {
                System.out.print(' ');
            }
            System.out.println(which);
        }
    }

    /**
     * Starts tracking a dispatch.
     */
    private synchronized void preDispatchEvent() {
        debug("pre");
        synchronized (dispatches) {
            dispatches.addLast(new DispatchInfo());
        }
    }

    /**
     * Stops tracking a dispatch.
     */
    private synchronized void postDispatchEvent() {
        synchronized (dispatches) {
            // We've finished the most nested dispatch, and don't need it any longer.
            var justFinishedDispatch = dispatches.removeLast();
            justFinishedDispatch.dispose();

            // The other dispatches, which have been waiting, need to be credited extra time.
            // We do this rather simplistically by pretending they've just been redispatched.
            var currentEventDispatchThread = Thread.currentThread();
            for (var dispatchInfo : dispatches) {
                if (dispatchInfo.eventDispatchThread == currentEventDispatchThread) {
                    dispatchInfo.lastDispatchTimeMillis = System.currentTimeMillis();
                }
            }
        }
        debug("post");
    }

    private void dispose() {
        scheduledExecutorService.shutdownNow();
    }

    private static String stackTraceToString(StackTraceElement[] stackTrace) {
        var result = new StringBuilder();
        // We used to avoid showing any code above where this class gets
        // involved in event dispatch, but that hides potentially useful
        // information when dealing with modal dialogs. Maybe we should
        // reinstate that, but search from the other end of the stack?
        for (StackTraceElement stackTraceElement : stackTrace) {
            result.append("\n    at " + stackTraceElement);
        }
        return result.toString();
    }

    private synchronized static int getNewHangNumber() {
        return ++hangCount;
    }
}