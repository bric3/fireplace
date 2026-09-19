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
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.Component;
import java.awt.Container;
import java.awt.ContainerOrderFocusTraversalPolicy;
import java.awt.DefaultKeyboardFocusManager;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

@Tag("ui")
class EmbeddingCompositeUiTest {
    private KeyboardFocusManager originalFocusManager;
    private RecordingKeyboardFocusManager focusManager;
    private Display display;
    private Shell shell;

    @BeforeEach
    void setUp() {
        originalFocusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        focusManager = new RecordingKeyboardFocusManager();
        KeyboardFocusManager.setCurrentKeyboardFocusManager(focusManager);

        display = new Display();
        shell = new Shell(display);
        shell.setLayout(new GridLayout(1, false));
    }

    @AfterEach
    void tearDown() {
        if (!shell.isDisposed()) {
            shell.dispose();
        }
        if (!display.isDisposed()) {
            display.dispose();
        }
        KeyboardFocusManager.setCurrentKeyboardFocusManager(originalFocusManager);
    }

    @Test
    void createsAndDisposesTheEmbeddedFrameOnTheCorrectThreads() {
        assertThatThrownBy(() -> new EmbeddingComposite(shell, SWT.BORDER))
                .isInstanceOf(IllegalArgumentException.class);

        var suppliedOnEdt = new AtomicBoolean();
        var content = new AtomicReference<JPanel>();
        var embedding = new EmbeddingComposite(shell);
        embedding.init(() -> {
            suppliedOnEdt.set(EventQueue.isDispatchThread());
            var panel = new JPanel();
            panel.setPreferredSize(new java.awt.Dimension(320, 180));
            content.set(panel);
            return panel;
        });

        var frame = SWT_AWT.getFrame(embedding);
        var root = SWT_AWTBridge.computeInEDT(() -> frame.getComponents()[0]);

        assertSoftly(softly -> {
            softly.assertThat(suppliedOnEdt).isTrue();
            softly.assertThat(frame).isNotNull();
            softly.assertThat(root).isInstanceOf(EmbeddingComposite.SwingRootPaneContainer.class);
            softly.assertThat(((EmbeddingComposite.SwingRootPaneContainer) root).getContentPane().getComponents())
                  .containsExactly(content.get());
            softly.assertThat(embedding.getSize()).isEqualTo(new org.eclipse.swt.graphics.Point(320, 180));
            softly.assertThat(focusManager.addedDispatchers).allMatch(Frame.class::isInstance);
        });

        embedding.dispose();

        assertThat(SWT_AWTBridge.computeInEDT(frame::isDisplayable)).isFalse();
    }

    @Test
    void handsOnlyJmcStyleFocusCycleBoundariesBackToSwt() throws InterruptedException {
        var beforeSwing = new Text(shell, SWT.NONE);
        var embedding = new EmbeddingComposite(shell);
        var afterSwing = new Text(shell, SWT.NONE);
        var traversals = new ArrayList<Integer>();
        embedding.addTraverseListener(event -> {
            traversals.add(event.detail);
            if (event.detail == SWT.TRAVERSE_TAB_NEXT) {
                afterSwing.setFocus();
            } else if (event.detail == SWT.TRAVERSE_TAB_PREVIOUS) {
                beforeSwing.setFocus();
            }
            // The application selected the RCP-specific target, so SWT must not traverse again.
            event.doit = false;
        });

        var first = new AtomicReference<Component>();
        var middle = new AtomicReference<Component>();
        var last = new AtomicReference<Component>();
        embedding.setFocusTraversalEnabled(true);
        embedding.init(() -> {
            // Mirrors JMC: main panel -> search field -> flame graph.
            var mainPanel = new JPanel();
            mainPanel.setFocusable(true);
            var searchField = new JTextField();
            var flamegraph = new JPanel();
            flamegraph.setFocusable(true);

            var root = new JPanel();
            root.add(mainPanel);
            root.add(searchField);
            root.add(flamegraph);
            first.set(mainPanel);
            middle.set(searchField);
            last.set(flamegraph);
            return root;
        });

        var frame = SWT_AWT.getFrame(embedding);
        SWT_AWTBridge.invokeInEDTAndWait(() -> frame.setFocusTraversalPolicy(new ContainerOrderFocusTraversalPolicy() {
            @Override
            public Component getFirstComponent(Container container) {
                return first.get();
            }

            @Override
            public Component getLastComponent(Container container) {
                return last.get();
            }
        }));
        var dispatcher = focusManager.addedDispatchers.stream()
                                     .filter(candidate -> !(candidate instanceof Frame))
                                     .findFirst()
                                     .orElseThrow();

        assertThat(dispatchTab(dispatcher, middle.get(), 0)).containsExactly(false, false);
        assertThat(traversals).isEmpty();

        assertThat(dispatchTab(dispatcher, last.get(), 0)).containsExactly(true, true);
        waitUntil(() -> traversals.size() == 1);
        assertThat(traversals).containsExactly(SWT.TRAVERSE_TAB_NEXT);

        assertThat(dispatchTab(dispatcher, first.get(), KeyEvent.SHIFT_DOWN_MASK))
                .containsExactly(true, true);
        waitUntil(() -> traversals.size() == 2);
        assertThat(traversals).containsExactly(SWT.TRAVERSE_TAB_NEXT, SWT.TRAVERSE_TAB_PREVIOUS);

        // A second cycle must work without the mutable traversal flag used by JMC's old listener.
        assertThat(dispatchTab(dispatcher, last.get(), 0)).containsExactly(true, true);
        waitUntil(() -> traversals.size() == 3);
        assertThat(traversals).containsExactly(
                SWT.TRAVERSE_TAB_NEXT,
                SWT.TRAVERSE_TAB_PREVIOUS,
                SWT.TRAVERSE_TAB_NEXT
        );

        embedding.dispose();

        assertThat(focusManager.removedDispatchers).contains(dispatcher);
    }

    private boolean[] dispatchTab(KeyEventDispatcher dispatcher, Component focusOwner, int modifiers) {
        focusManager.focusOwner = focusOwner;
        return SWT_AWTBridge.computeInEDT(() -> {
            var event = new KeyEvent(focusOwner, KeyEvent.KEY_PRESSED, 0, modifiers, KeyEvent.VK_TAB, '\t');
            var dispatched = dispatcher.dispatchKeyEvent(event);
            return new boolean[]{dispatched, event.isConsumed()};
        });
    }

    private void waitUntil(BooleanSupplier condition) throws InterruptedException {
        var deadline = System.nanoTime() + 5_000_000_000L;
        while (!condition.getAsBoolean() && System.nanoTime() < deadline) {
            if (!display.readAndDispatch()) {
                Thread.sleep(10);
            }
        }
        assertThat(condition.getAsBoolean()).isTrue();
    }

    private static final class RecordingKeyboardFocusManager extends DefaultKeyboardFocusManager {
        private volatile Component focusOwner;
        private final List<KeyEventDispatcher> addedDispatchers = new CopyOnWriteArrayList<>();
        private final List<KeyEventDispatcher> removedDispatchers = new CopyOnWriteArrayList<>();

        @Override
        public Component getFocusOwner() {
            return focusOwner;
        }

        @Override
        public void addKeyEventDispatcher(KeyEventDispatcher dispatcher) {
            addedDispatchers.add(dispatcher);
        }

        @Override
        public void removeKeyEventDispatcher(KeyEventDispatcher dispatcher) {
            removedDispatchers.add(dispatcher);
        }
    }
}
