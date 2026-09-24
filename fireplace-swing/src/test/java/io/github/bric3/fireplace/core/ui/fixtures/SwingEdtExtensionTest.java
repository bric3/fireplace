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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor.Invocation;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import javax.swing.SwingUtilities;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class SwingEdtExtensionTest {
    @Nested
    @ExtendWith(SwingEdtExtension.class)
    class RegisteredExtension {
        private Thread setupThread;
        private int setupCalls;

        @BeforeEach
        void setup() {
            assertTrue(SwingUtilities.isEventDispatchThread());
            setupThread = Thread.currentThread();
            setupCalls++;
        }

        @Test
        void setup_and_test_run_on_the_edt_in_order() {
            assertTrue(SwingUtilities.isEventDispatchThread());
            assertSame(setupThread, Thread.currentThread());
            assertEquals(1, setupCalls);
        }
    }

    @ParameterizedTest(name = "propagates EDT failures from beforeEach={0}")
    @ValueSource(booleans = {true, false})
    void propagates_original_errors_and_checked_exceptions_to_the_calling_thread(boolean beforeEach) {
        assertFalse(SwingUtilities.isEventDispatchThread());
        var extension = new SwingEdtExtension();
        var context = mock(ExtensionContext.class);
        @SuppressWarnings("unchecked")
        ReflectiveInvocationContext<Method> invocationContext = mock(ReflectiveInvocationContext.class);

        for (Throwable expected : List.of(new AssertionError("assertion on EDT"), new IOException("checked on EDT"))) {
            Invocation<Void> invocation = () -> {
                assertTrue(SwingUtilities.isEventDispatchThread());
                throw expected;
            };
            var actual = assertThrows(Throwable.class, () -> {
                if (beforeEach) {
                    extension.interceptBeforeEachMethod(invocation, invocationContext, context);
                } else {
                    extension.interceptTestMethod(invocation, invocationContext, context);
                }
            });
            assertSame(expected, actual, "failure must reach the caller without being wrapped or swallowed");
        }
    }
}
