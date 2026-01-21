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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link Debouncer}.
 * Uses Swing Timer which works in headless mode.
 */
@DisplayName("Debouncer")
class DebouncerTest {

    @Nested
    @DisplayName("Basic execution")
    class BasicExecution {

        @Test
        @Timeout(5)
        void executes_task_after_delay() throws InterruptedException {
            var latch = new CountDownLatch(1);
            var debouncer = new Debouncer(50);

            debouncer.debounce(latch::countDown);

            boolean completed = latch.await(2, TimeUnit.SECONDS);
            assertThat(completed).isTrue();
        }

        @Test
        @Timeout(5)
        void with_explicit_delay_executes_after_specified_delay() throws InterruptedException {
            var latch = new CountDownLatch(1);
            var debouncer = new Debouncer(1000); // Default is 1 second

            debouncer.debounce(50, latch::countDown); // But we use 50ms

            boolean completed = latch.await(2, TimeUnit.SECONDS);
            assertThat(completed).isTrue();
        }

        @Test
        @Timeout(5)
        void executes_runnable() throws InterruptedException {
            var counter = new AtomicInteger(0);
            var latch = new CountDownLatch(1);
            var debouncer = new Debouncer(50);

            debouncer.debounce(() -> {
                counter.set(42);
                latch.countDown();
            });

            boolean completed = latch.await(2, TimeUnit.SECONDS);

            assertThat(completed).isTrue();
            assertThat(counter.get()).isEqualTo(42);
        }
    }

    @Nested
    @DisplayName("Debounce behavior")
    class DebounceBehavior {

        @Test
        @Timeout(5)
        void multiple_rapid_calls_executes_only_last() throws InterruptedException {
            var counter = new AtomicInteger(0);
            var latch = new CountDownLatch(1);
            var debouncer = new Debouncer(100);

            // Rapidly call debounce multiple times
            for (int i = 1; i <= 5; i++) {
                final int value = i;
                debouncer.debounce(() -> {
                    counter.set(value);
                    latch.countDown();
                });
                Thread.sleep(20); // Less than debounce delay
            }

            boolean completed = latch.await(2, TimeUnit.SECONDS);
            assertThat(completed).isTrue();
            // Should have the value from the last call
            assertThat(counter.get()).isEqualTo(5);
        }

        @Test
        @Timeout(5)
        void cancels_previous_task() throws InterruptedException {
            var firstTaskExecuted = new AtomicInteger(0);
            var secondTaskLatch = new CountDownLatch(1);
            var debouncer = new Debouncer(100);

            // First task
            debouncer.debounce(firstTaskExecuted::incrementAndGet);

            // Immediately override with second task
            debouncer.debounce(() -> secondTaskLatch.countDown());

            // Wait for second task to complete
            boolean completed = secondTaskLatch.await(2, TimeUnit.SECONDS);
            assertThat(completed).isTrue();

            // First task should not have executed
            assertThat(firstTaskExecuted.get()).isZero();
        }

        @Test
        @Timeout(5)
        void separate_calls_executes_separately() throws InterruptedException {
            var counter = new AtomicInteger(0);
            var latch = new CountDownLatch(2);
            var debouncer = new Debouncer(50);

            // First call
            debouncer.debounce(() -> {
                counter.incrementAndGet();
                latch.countDown();
            });

            // Wait for first to complete
            Thread.sleep(150);

            // Second call after delay
            debouncer.debounce(() -> {
                counter.incrementAndGet();
                latch.countDown();
            });

            boolean completed = latch.await(2, TimeUnit.SECONDS);
            assertThat(completed).isTrue();
            assertThat(counter.get()).isEqualTo(2);
        }

        @Test
        @Timeout(5)
        void does_not_repeat() throws InterruptedException {
            var counter = new AtomicInteger(0);
            var debouncer = new Debouncer(50);

            debouncer.debounce(counter::incrementAndGet);

            // Wait long enough for multiple potential executions
            Thread.sleep(300);

            // Should only have executed once
            assertThat(counter.get()).isEqualTo(1);
        }

        @Test
        @Timeout(5)
        void stress_test() throws InterruptedException {
            var lastValue = new AtomicInteger(-1);
            var latch = new CountDownLatch(1);
            var debouncer = new Debouncer(50);

            // Rapidly fire many events
            int totalCalls = 100;
            for (int i = 0; i < totalCalls; i++) {
                final int value = i;
                debouncer.debounce(() -> {
                    lastValue.set(value);
                    latch.countDown();
                });
            }

            boolean completed = latch.await(2, TimeUnit.SECONDS);
            assertThat(completed).isTrue();

            // Should have the last value (or close to it, due to timing)
            assertThat(lastValue.get()).isGreaterThanOrEqualTo(totalCalls - 10);
        }
    }

    @Nested
    @DisplayName("Delay configuration")
    class DelayConfiguration {

        @Test
        @Timeout(5)
        void zero_delay_executes_immediately() throws InterruptedException {
            var latch = new CountDownLatch(1);
            var debouncer = new Debouncer(0);

            long startTime = System.currentTimeMillis();
            debouncer.debounce(latch::countDown);

            boolean completed = latch.await(1, TimeUnit.SECONDS);
            long elapsed = System.currentTimeMillis() - startTime;

            assertThat(completed).isTrue();
            // Should execute almost immediately (allowing for timer overhead)
            assertThat(elapsed).isLessThan(500);
        }

        @Test
        @Timeout(5)
        void different_delays_respects_explicit_delay() throws InterruptedException {
            var counter = new AtomicInteger(0);
            var latch = new CountDownLatch(1);
            var debouncer = new Debouncer(500); // Default is 500ms

            // First call with short delay
            debouncer.debounce(20, () -> {
                counter.set(1);
                latch.countDown();
            });

            boolean completed = latch.await(2, TimeUnit.SECONDS);
            assertThat(completed).isTrue();
            assertThat(counter.get()).isEqualTo(1);
        }

        @Test
        void constructor_sets_default_delay() {
            var debouncer = new Debouncer(123);

            // The default delay is stored internally
            // We verify it works by testing that default is used
            var counter = new AtomicInteger(0);
            var latch = new CountDownLatch(1);

            debouncer.debounce(() -> {
                counter.incrementAndGet();
                latch.countDown();
            });

            try {
                boolean completed = latch.await(2, TimeUnit.SECONDS);
                assertThat(completed).isTrue();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Nested
    @DisplayName("Exception handling")
    class ExceptionHandling {

        @Test
        @Timeout(5)
        void task_exception_does_not_break_debouncer() throws InterruptedException {
            var latch = new CountDownLatch(1);
            var debouncer = new Debouncer(50);

            // First task throws exception
            debouncer.debounce(() -> {
                throw new RuntimeException("Test exception");
            });

            // Wait a bit for exception
            Thread.sleep(100);

            // Second task should still work
            debouncer.debounce(latch::countDown);

            boolean completed = latch.await(2, TimeUnit.SECONDS);
            assertThat(completed).isTrue();
        }
    }

    @Nested
    @DisplayName("EDT execution")
    class EdtExecution {

        @Test
        @Timeout(5)
        void executes_on_EDT() throws InterruptedException {
            var executedOnEDT = new AtomicInteger(0); // 0 = not set, 1 = EDT, -1 = not EDT
            var latch = new CountDownLatch(1);
            var debouncer = new Debouncer(50);

            debouncer.debounce(() -> {
                if (javax.swing.SwingUtilities.isEventDispatchThread()) {
                    executedOnEDT.set(1);
                } else {
                    executedOnEDT.set(-1);
                }
                latch.countDown();
            });

            boolean completed = latch.await(2, TimeUnit.SECONDS);
            assertThat(completed).isTrue();
            assertThat(executedOnEDT.get()).isEqualTo(1);
        }
    }
}