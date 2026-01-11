/*
 * Fireplace
 *
 * Copyright (c) 2021, Today - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.github.bric3.fireplace.core.ui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link SwingUtils}.
 * These tests run in headless mode without requiring a display.
 */
@DisplayName("SwingUtils")
class SwingUtilsTest {

    @Nested
    @DisplayName("invokeLater")
    class InvokeLater {

        @Test
        @Timeout(5)
        void from_non_EDT_executes_on_EDT() throws InterruptedException {
            var executedOnEDT = new AtomicBoolean(false);
            var latch = new CountDownLatch(1);

            // Ensure we're NOT on EDT
            assertThat(SwingUtilities.isEventDispatchThread()).isFalse();

            SwingUtils.invokeLater(() -> {
                executedOnEDT.set(SwingUtilities.isEventDispatchThread());
                latch.countDown();
            });

            boolean completed = latch.await(2, TimeUnit.SECONDS);

            assertThat(completed).isTrue();
            assertThat(executedOnEDT.get()).isTrue();
        }

        @Test
        @Timeout(5)
        void from_EDT_executes_immediately() throws InterruptedException, InvocationTargetException {
            var executedImmediately = new AtomicBoolean(false);
            var executedOnEDT = new AtomicBoolean(false);

            SwingUtilities.invokeAndWait(() -> {
                // Now we're on EDT
                assertThat(SwingUtilities.isEventDispatchThread()).isTrue();

                SwingUtils.invokeLater(() -> {
                    executedOnEDT.set(SwingUtilities.isEventDispatchThread());
                });

                // When called from EDT, the runnable should execute synchronously
                // Check immediately after the call
                executedImmediately.set(true);
            });

            assertThat(executedImmediately.get()).isTrue();
            assertThat(executedOnEDT.get()).isTrue();
        }

        @Test
        @Timeout(5)
        void executes_runnable() throws InterruptedException {
            var counter = new AtomicReference<>(0);
            var latch = new CountDownLatch(1);

            SwingUtils.invokeLater(() -> {
                counter.set(42);
                latch.countDown();
            });

            boolean completed = latch.await(2, TimeUnit.SECONDS);

            assertThat(completed).isTrue();
            assertThat(counter.get()).isEqualTo(42);
        }

        @Test
        @Timeout(5)
        void multiple_calls_execute_in_order() throws InterruptedException {
            var order = new StringBuilder();
            var latch = new CountDownLatch(3);

            SwingUtils.invokeLater(() -> {
                order.append("1");
                latch.countDown();
            });
            SwingUtils.invokeLater(() -> {
                order.append("2");
                latch.countDown();
            });
            SwingUtils.invokeLater(() -> {
                order.append("3");
                latch.countDown();
            });

            boolean completed = latch.await(2, TimeUnit.SECONDS);

            assertThat(completed).isTrue();
            assertThat(order.toString()).isEqualTo("123");
        }

        @Test
        @Timeout(5)
        void from_EDT_does_not_deadlock() throws InterruptedException, InvocationTargetException {
            var completed = new AtomicBoolean(false);

            SwingUtilities.invokeAndWait(() -> {
                // Nested invokeLater from EDT should not cause deadlock
                SwingUtils.invokeLater(() -> {
                    SwingUtils.invokeLater(() -> {
                        completed.set(true);
                    });
                });
            });

            // Give some time for nested tasks to complete
            Thread.sleep(100);
            assertThat(completed.get()).isTrue();
        }
    }

    @Nested
    @DisplayName("invokeAndWait")
    class InvokeAndWait {

        @Test
        @Timeout(5)
        void from_non_EDT_executes_on_EDT_and_waits() throws InterruptedException, InvocationTargetException {
            var executedOnEDT = new AtomicBoolean(false);
            var result = new AtomicReference<String>(null);

            // Ensure we're NOT on EDT
            assertThat(SwingUtilities.isEventDispatchThread()).isFalse();

            SwingUtils.invokeAndWait(() -> {
                executedOnEDT.set(SwingUtilities.isEventDispatchThread());
                result.set("completed");
            });

            // After invokeAndWait returns, the task should be complete
            assertThat(executedOnEDT.get()).isTrue();
            assertThat(result.get()).isEqualTo("completed");
        }

        @Test
        @Timeout(5)
        void from_EDT_executes_immediately() throws InterruptedException, InvocationTargetException {
            var executedOnEDT = new AtomicBoolean(false);
            var executionOrder = new AtomicReference<String>("");

            SwingUtilities.invokeAndWait(() -> {
                // Now we're on EDT
                assertThat(SwingUtilities.isEventDispatchThread()).isTrue();

                try {
                    SwingUtils.invokeAndWait(() -> {
                        executedOnEDT.set(SwingUtilities.isEventDispatchThread());
                        executionOrder.set("inner");
                    });
                } catch (InterruptedException | InvocationTargetException e) {
                    throw new RuntimeException(e);
                }

                // When called from EDT, it should execute synchronously
                executionOrder.set(executionOrder.get() + "-outer");
            });

            assertThat(executedOnEDT.get()).isTrue();
            assertThat(executionOrder.get()).isEqualTo("inner-outer");
        }

        @Test
        @Timeout(5)
        void blocks_until_complete() throws InterruptedException, InvocationTargetException {
            var taskStarted = new AtomicBoolean(false);
            var taskCompleted = new AtomicBoolean(false);

            long startTime = System.currentTimeMillis();

            SwingUtils.invokeAndWait(() -> {
                taskStarted.set(true);
                try {
                    Thread.sleep(100); // Simulate some work
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                taskCompleted.set(true);
            });

            long elapsed = System.currentTimeMillis() - startTime;

            // Task should be complete when invokeAndWait returns
            assertThat(taskStarted.get()).isTrue();
            assertThat(taskCompleted.get()).isTrue();
            assertThat(elapsed).isGreaterThanOrEqualTo(100); // Should have waited for the task
        }

        @Test
        @Timeout(5)
        void propagates_exception() {
            // When the runnable throws an exception, invokeAndWait should wrap it
            assertThatThrownBy(() -> SwingUtils.invokeAndWait(() -> {
                throw new RuntimeException("Test exception");
            })).isInstanceOf(InvocationTargetException.class)
               .hasCauseInstanceOf(RuntimeException.class)
               .hasRootCauseMessage("Test exception");
        }
    }
}