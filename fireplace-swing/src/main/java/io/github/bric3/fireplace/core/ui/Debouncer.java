package io.github.bric3.fireplace.core.ui;

import javax.swing.*;

/**
 * Debounce a task to prevent it from being executed too frequently.
 * The task will only be executed after a specified delay, and on the EDT.
 */
public class Debouncer {
    private final int defaultDelayMillis;
    private Timer timer;

    /**
     * Create a Debouncer with a default delay.
     *
     * @param defaultDelayMillis the default delay in milliseconds before executing the task
     */
    public Debouncer(int defaultDelayMillis) {
        this.defaultDelayMillis = defaultDelayMillis;
    }

    /**
     * Debounce a task to prevent it from being executed too frequently.
     * Uses the default delay.
     *
     * @param task the task to be executed
     */
    public void debounce(Runnable task) {
        debounce(defaultDelayMillis, task);
    }

    /**
     * Debounce a task to prevent it from being executed too frequently.
     *
     * @param delayMillis the delay in milliseconds before executing the task
     * @param task the task to be executed
     */
    public void debounce(int delayMillis, Runnable task) {
        if (timer != null && timer.isRunning()) {
            timer.stop();
        }

        timer = new Timer(delayMillis, __ -> task.run());
        timer.setRepeats(false);
        timer.start();
    }
}
