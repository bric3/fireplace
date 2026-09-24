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

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;

import javax.swing.SwingUtilities;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;

/** Runs synchronous, headless Swing API checks on the EDT. Mounted tests use the window fixture. */
public class SwingEdtExtension implements InvocationInterceptor {
    @Override
    public void interceptBeforeEachMethod(Invocation<Void> invocation,
                                          ReflectiveInvocationContext<Method> context,
                                          ExtensionContext extensionContext) throws Throwable {
        onEdt(invocation);
    }

    @Override
    public void interceptTestMethod(Invocation<Void> invocation,
                                    ReflectiveInvocationContext<Method> context,
                                    ExtensionContext extensionContext) throws Throwable {
        onEdt(invocation);
    }

    private static void onEdt(Invocation<Void> invocation) throws Throwable {
        var failure = new AtomicReference<Throwable>();
        SwingUtilities.invokeAndWait(() -> {
            try {
                invocation.proceed();
            } catch (Throwable thrown) {
                failure.set(thrown);
            }
        });
        if (failure.get() != null) {
            throw failure.get();
        }
    }
}
