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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import javax.swing.SwingUtilities;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static io.github.bric3.fireplace.core.ui.fixtures.SwingWindowFixture.runOnEdt;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Swing timers run headlessly; schedule on the EDT and await callbacks off it. */
@Timeout(10)
class DebouncerTest {
    @ParameterizedTest(name = "default {0}ms, explicit {1}ms")
    @CsvSource({"250, -1, 200", "10000, 25, 0", "0, 250, 200"})
    void honors_default_and_explicit_delays(int defaultDelay, int explicitDelay, int minimumDelay) throws Exception {
        var elapsed = new CompletableFuture<Long>();
        runOnEdt(() -> {
            var debouncer = new Debouncer(defaultDelay);
            long started = System.nanoTime();
            Runnable callback = () -> elapsed.complete(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started));
            if (explicitDelay < 0) {
                debouncer.debounce(callback);
            } else {
                debouncer.debounce(explicitDelay, callback);
            }
            return null;
        });

        // The deadline tolerates scheduling load but rejects using the 10s default for a 25ms override.
        assertThat(elapsed.get(2, TimeUnit.SECONDS)).isGreaterThanOrEqualTo(minimumDelay);
    }

    @ParameterizedTest(name = "burst of {0} calls")
    @ValueSource(ints = {2, 100})
    void a_burst_executes_only_the_last_callback(int calls) throws Exception {
        var completed = new CompletableFuture<Integer>();
        var executions = new AtomicInteger();
        runOnEdt(() -> {
            var debouncer = new Debouncer(25);
            // One EDT turn guarantees no timer callback can interleave with scheduling the burst.
            for (int value = 0; value < calls; value++) {
                int captured = value;
                debouncer.debounce(() -> {
                    executions.incrementAndGet();
                    completed.complete(captured);
                });
            }
            return null;
        });

        assertThat(completed.get(2, TimeUnit.SECONDS)).isEqualTo(calls - 1);
        assertThat(runOnEdt(executions::get)).isEqualTo(1);
    }

    @Test
    void can_schedule_again_after_a_callback_completes() throws Exception {
        var executions = new AtomicInteger();
        var debouncer = new Debouncer(25);
        var first = new CompletableFuture<Integer>();
        runOnEdt(() -> {
            debouncer.debounce(() -> first.complete(executions.incrementAndGet()));
            return null;
        });
        assertThat(first.get(2, TimeUnit.SECONDS)).isEqualTo(1);

        var second = new CompletableFuture<Integer>();
        runOnEdt(() -> {
            debouncer.debounce(() -> second.complete(executions.incrementAndGet()));
            return null;
        });
        assertThat(second.get(2, TimeUnit.SECONDS)).isEqualTo(2);
    }

    @Test
    void callback_runs_once_without_repeating() throws Exception {
        var first = new CompletableFuture<Void>();
        var repeated = new CompletableFuture<Void>();
        var executions = new AtomicInteger();
        runOnEdt(() -> {
            new Debouncer(25).debounce(() -> {
                if (executions.incrementAndGet() == 1) {
                    first.complete(null);
                } else {
                    repeated.complete(null);
                }
            });
            return null;
        });
        first.get(2, TimeUnit.SECONDS);

        // Absence needs a bounded observation window, spanning several possible timer repetitions.
        assertThatThrownBy(() -> repeated.get(200, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
        assertThat(runOnEdt(executions::get)).isEqualTo(1);
    }

    @Test
    void zero_delay_still_queues_the_callback_on_the_edt() throws Exception {
        var onEdt = new CompletableFuture<Boolean>();
        runOnEdt(() -> {
            new Debouncer(0).debounce(() -> onEdt.complete(SwingUtilities.isEventDispatchThread()));
            assertThat(onEdt).isNotDone();
            return null;
        });

        assertThat(onEdt.get(2, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    void can_schedule_again_after_a_callback_throws() throws Exception {
        var debouncer = new Debouncer(25);
        var expected = new RuntimeException("deliberate timer callback failure");
        var failure = new CompletableFuture<Throwable>();
        var failures = new ConcurrentLinkedQueue<Throwable>();
        var executions = new AtomicInteger();
        var active = new AtomicBoolean(true);
        var edt = runOnEdt(Thread::currentThread);
        var previousHandler = runOnEdt(edt::getUncaughtExceptionHandler);
        try {
            runOnEdt(() -> {
                edt.setUncaughtExceptionHandler((thread, thrown) -> {
                    failures.add(thrown);
                    failure.complete(thrown);
                });
                debouncer.debounce(() -> {
                    if (active.get()) {
                        executions.incrementAndGet();
                        throw expected;
                    }
                });
                return null;
            });
            // Observe the thrown exception before scheduling again, so the first task cannot be cancelled.
            assertThat(failure.get(2, TimeUnit.SECONDS)).isSameAs(expected);

            var recovered = new CompletableFuture<Integer>();
            runOnEdt(() -> {
                debouncer.debounce(() -> recovered.complete(executions.incrementAndGet()));
                return null;
            });
            assertThat(recovered.get(2, TimeUnit.SECONDS)).isEqualTo(2);
        } finally {
            // A timed-out deliberately failing callback must not throw into a later test's handler.
            active.set(false);
            runOnEdt(() -> {
                edt.setUncaughtExceptionHandler(previousHandler);
                return null;
            });
        }
        assertThat(failures).containsExactly(expected);
    }
}
