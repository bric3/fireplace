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
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static io.github.bric3.fireplace.core.ui.fixtures.SwingWindowFixture.runOnEdt;
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
        void from_EDT_executes_immediately() {
            runOnEdt(() -> {
                assertThat(SwingUtilities.isEventDispatchThread()).isTrue();
                var order = new StringBuilder("before-");
                SwingUtils.invokeLater(() -> {
                    order.append("inner-");
                    SwingUtils.invokeLater(() -> order.append("nested-"));
                });
                order.append("after");
                // Assert before yielding the EDT; an incorrectly queued callback cannot catch up.
                assertThat(order.toString()).isEqualTo("before-inner-nested-after");
                return null;
            });
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
        @Timeout(10)
        void blocks_until_complete() throws Exception {
            var taskStarted = new CountDownLatch(1);
            var releaseTask = new CountDownLatch(1);
            var taskCompleted = new AtomicBoolean(false);
            var call = new FutureTask<>(() -> {
                SwingUtils.invokeAndWait(() -> {
                    taskStarted.countDown();
                    try {
                        assertThat(releaseTask.await(2, TimeUnit.SECONDS)).as("test releases the EDT task").isTrue();
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        throw new AssertionError(interrupted);
                    }
                    taskCompleted.set(true);
                });
                return taskCompleted.get();
            });
            var caller = new Thread(call, "SwingUtilsTest-invokeAndWait");
            try {
                caller.start();
                assertThat(taskStarted.await(2, TimeUnit.SECONDS)).isTrue();
                assertThatThrownBy(() -> call.get(150, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
                releaseTask.countDown();
                assertThat(call.get(2, TimeUnit.SECONDS)).isTrue();
            } finally {
                releaseTask.countDown();
                caller.join(2000);
                assertThat(caller.isAlive()).isFalse();
                runOnEdt(() -> null);
            }
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
