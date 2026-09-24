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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.Timeout;

import javax.swing.JComponent;
import javax.swing.KeyStroke;
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
import java.awt.Toolkit;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

@Tag("ui")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Timeout(value = 30, unit = TimeUnit.SECONDS, threadMode = Timeout.ThreadMode.SAME_THREAD)
class EmbeddingCompositeUiTest {
    private KeyboardFocusManager originalFocusManager;
    private RecordingKeyboardFocusManager focusManager;
    private Display display;
    private Shell shell;

    @BeforeAll
    void createDisplay() {
        // GTK/AWT retain native state across tests. Match an RCP application:
        // one Display for this class, with a fresh Shell for each test.
        display = new Display();
    }

    @AfterAll
    void disposeDisplay() {
        display.dispose();
    }

    @BeforeEach
    void setUp() {
        originalFocusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        focusManager = new RecordingKeyboardFocusManager();
        KeyboardFocusManager.setCurrentKeyboardFocusManager(focusManager);

        shell = new Shell(display);
        shell.setLayout(new GridLayout(1, false));
    }

    @AfterEach
    void tearDown() {
        if (!shell.isDisposed()) {
            shell.dispose();
        }
        // Drain queued disposal/dispatcher cleanup before restoring the global focus manager.
        SWT_AWTBridge.invokeInEDTAndWait(() ->
                KeyboardFocusManager.setCurrentKeyboardFocusManager(originalFocusManager)
        );
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
    void disposesWithoutWaitingForTheEdt() throws InterruptedException {
        var embedding = new EmbeddingComposite(shell);
        embedding.init(JPanel::new);
        var edtBlocked = new CountDownLatch(1);
        var releaseEdt = new CompletableFuture<Void>();
        EventQueue.invokeLater(() -> {
            edtBlocked.countDown();
            // Bound a broken synchronous dispose so it reports a failure, not a deadlock.
            releaseEdt.completeOnTimeout(null, 5, TimeUnit.SECONDS).join();
        });

        try {
            waitUntil(() -> edtBlocked.getCount() == 0);
            embedding.dispose();
            assertThat(releaseEdt.isDone())
                    .as("SWT disposal must return while the EDT is still blocked")
                    .isFalse();
        } finally {
            releaseEdt.complete(null);
        }
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

        assertThat(SWT_AWTBridge.computeInEDT(() -> focusManager.removedDispatchers)).contains(dispatcher);
    }

    @Test
    void reentersTheSameSwingFrameAndTraversesBackToSwt() throws InterruptedException {
        var transitions = new CopyOnWriteArrayList<String>();
        var beforeSwing = new Text(shell, SWT.NONE);
        var embedding = new EmbeddingComposite(shell);
        var afterSwing = new Text(shell, SWT.NONE);
        shell.setTabList(new org.eclipse.swt.widgets.Control[]{beforeSwing, embedding, afterSwing});
        embedding.setFocusTraversalEnabled(true);
        embedding.addTraverseListener(event -> {
            transitions.add("SWT traverse: " + event.detail);
            if (event.detail == SWT.TRAVERSE_TAB_NEXT) {
                afterSwing.setFocus();
                event.doit = false;
            } else if (event.detail == SWT.TRAVERSE_TAB_PREVIOUS) {
                beforeSwing.setFocus();
                event.doit = false;
            }
        });
        var first = new AtomicReference<Component>();
        var last = new AtomicReference<Component>();
        embedding.init(() -> {
            var mainPanel = new JPanel();
            mainPanel.setFocusable(true);
            mainPanel.setName("first");
            var searchField = new JTextField();
            var flamegraph = new JPanel();
            flamegraph.setFocusable(true);
            flamegraph.setName("last");
            var root = new JPanel();
            root.add(mainPanel);
            root.add(searchField);
            root.add(flamegraph);
            first.set(mainPanel);
            last.set(flamegraph);
            return root;
        });
        var frame = SWT_AWT.getFrame(embedding);
        var gained = new AtomicInteger();
        var lost = new AtomicInteger();
        var windowFocus = new WindowAdapter() {
            @Override
            public void windowGainedFocus(WindowEvent event) {
                transitions.add("AWT window gained focus");
                gained.incrementAndGet();
            }

            @Override
            public void windowLostFocus(WindowEvent event) {
                transitions.add("AWT window lost focus");
                lost.incrementAndGet();
            }
        };
        PropertyChangeListener ownerChanges = event -> {
            var owner = (Component) event.getNewValue();
            transitions.add("AWT owner: " + (owner == null ? "none" : owner.getName()));
        };
        beforeSwing.addListener(SWT.FocusIn, event -> transitions.add("SWT before gained focus"));
        afterSwing.addListener(SWT.FocusIn, event -> transitions.add("SWT after gained focus"));
        SWT_AWTBridge.invokeInEDTAndWait(() -> {
            frame.addWindowFocusListener(windowFocus);
            focusManager.addPropertyChangeListener("focusOwner", ownerChanges);
        });
        try {
            openShell();
            assertSoftly(softly -> {
                softly.assertThat(SWT_AWTBridge.computeInEDT(
                        () -> frame.getFocusTraversalPolicy().getFirstComponent(frame)
                )).isSameAs(first.get());
                softly.assertThat(SWT_AWTBridge.computeInEDT(
                        () -> frame.getFocusTraversalPolicy().getLastComponent(frame)
                )).isSameAs(last.get());
            });
            assertThat(embedding.setFocus()).isTrue();
            focus(last.get());
            int lossesBeforeExit = lost.get();
            SWT_AWTBridge.invokeInEDTAndWait(() -> {
                assertThat(last.get().isFocusOwner()).as("forward traversal starts at the last Swing component").isTrue();
                assertThat(dispatchThroughFocusManager(last.get(), KeyEvent.VK_TAB, 0)).containsExactly(true, true);
            });
            waitUntil(afterSwing::isFocusControl);

            // SWT focus can arrive before AWT processes its native focus-loss notification.
            waitUntil(() -> lost.get() > lossesBeforeExit && SWT_AWTBridge.computeInEDT(() -> !frame.isFocused()));
            int gainsBeforeReturn = gained.get();
            assertThat(embedding.setFocus()).isTrue();
            waitUntil(() -> gained.get() > gainsBeforeReturn && SWT_AWTBridge.computeInEDT(() -> {
                var owner = focusManager.getFocusOwner();
                return owner != null && frame.isAncestorOf(owner);
            }));
            focus(first.get());
            SWT_AWTBridge.invokeInEDTAndWait(() -> {
                assertThat(first.get().isFocusOwner()).as("backward traversal starts at the first Swing component").isTrue();
                assertThat(dispatchThroughFocusManager(first.get(), KeyEvent.VK_TAB, KeyEvent.SHIFT_DOWN_MASK))
                        .containsExactly(true, true);
            });
            waitUntil(beforeSwing::isFocusControl);
            assertThat(transitions).containsSubsequence("AWT owner: last", "SWT after gained focus",
                                                       "AWT owner: first", "SWT before gained focus");
        } catch (RuntimeException | AssertionError failure) {
            failure.addSuppressed(new AssertionError("Focus transitions: " + transitions));
            throw failure;
        } finally {
            SWT_AWTBridge.invokeInEDTAndWait(() -> {
                frame.removeWindowFocusListener(windowFocus);
                focusManager.removePropertyChangeListener("focusOwner", ownerChanges);
            });
        }
    }

    @Test
    void ignoresAQueuedTraversalAfterDisposal() throws InterruptedException {
        var traversals = new AtomicInteger();
        var embedding = new EmbeddingComposite(shell);
        embedding.addTraverseListener(event -> traversals.incrementAndGet());
        var boundary = new AtomicReference<Component>();
        embedding.setFocusTraversalEnabled(true);
        embedding.init(() -> {
            var component = new JPanel();
            component.setFocusable(true);
            boundary.set(component);
            return component;
        });
        setBoundaryPolicy(embedding, boundary.get(), boundary.get());

        var blockerEntered = new CountDownLatch(1);
        var releaseBlocker = new CountDownLatch(1);
        SWT_AWTBridge.invokeSwtAwayFromAwt(display, () -> {
            blockerEntered.countDown();
            try {
                releaseBlocker.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        try {
            assertThat(blockerEntered.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(dispatchTab(traversalDispatcher(), boundary.get(), 0)).containsExactly(true, true);
            embedding.dispose();
        } finally {
            releaseBlocker.countDown();
        }

        var sideQueueDrained = new CountDownLatch(1);
        SWT_AWTBridge.invokeSwtAwayFromAwt(display, sideQueueDrained::countDown);
        assertThat(sideQueueDrained.await(5, TimeUnit.SECONDS)).isTrue();
        while (display.readAndDispatch()) {
            // Drain the traversal callback queued after the composite was disposed.
        }
        assertThat(traversals).hasValue(0);
    }

    @Test
    void scopesTraversalAndCleanupToEachEmbeddedFrame() {
        var firstEmbedding = new EmbeddingComposite(shell);
        var firstBoundary = new JPanel();
        firstBoundary.setFocusable(true);
        firstEmbedding.setFocusTraversalEnabled(true);
        firstEmbedding.init(() -> firstBoundary);
        setBoundaryPolicy(firstEmbedding, firstBoundary, firstBoundary);
        var firstDispatcher = traversalDispatcher();

        var secondEmbedding = new EmbeddingComposite(shell);
        var secondBoundary = new JPanel();
        secondBoundary.setFocusable(true);
        secondEmbedding.setFocusTraversalEnabled(true);
        secondEmbedding.init(() -> secondBoundary);
        setBoundaryPolicy(secondEmbedding, secondBoundary, secondBoundary);
        var secondDispatcher = traversalDispatchers().get(1);

        assertThat(dispatchTab(firstDispatcher, secondBoundary, 0)).containsExactly(false, false);
        assertThat(dispatchTab(secondDispatcher, secondBoundary, 0)).containsExactly(true, true);

        firstEmbedding.dispose();
        SWT_AWTBridge.invokeInEDTAndWait(() -> {});
        assertSoftly(softly -> {
            softly.assertThat(focusManager.removedDispatchers).contains(firstDispatcher);
            softly.assertThat(focusManager.removedDispatchers).doesNotContain(secondDispatcher);
        });
        assertThat(dispatchTab(secondDispatcher, secondBoundary, 0)).containsExactly(true, true);

        secondEmbedding.dispose();
        assertThat(SWT_AWTBridge.computeInEDT(() -> focusManager.removedDispatchers)).contains(secondDispatcher);
    }

    @Test
    void leavesJmcStyleKeyListenersAloneWhenTraversalIsDisabled() throws InterruptedException {
        var tabPresses = new AtomicInteger();
        var component = new JPanel();
        component.setFocusable(true);
        // JMC lets its KeyListener handle Tab by removing AWT's traversal keys.
        component.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, Set.of());
        component.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, Set.of());
        component.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent event) {
                if (event.getKeyCode() == KeyEvent.VK_TAB) {
                    tabPresses.incrementAndGet();
                }
            }
        });
        var embedding = new EmbeddingComposite(shell);
        embedding.init(() -> component);
        var frame = SWT_AWT.getFrame(embedding);
        openShell();
        focus(component);

        var dispatch = dispatchThroughFocusManager(component, KeyEvent.VK_TAB, 0);

        assertSoftly(softly -> {
            softly.assertThat(traversalDispatchers()).isEmpty();
            softly.assertThat(SWT_AWTBridge.computeInEDT(frame::getFocusTraversalKeysEnabled)).isFalse();
            softly.assertThat(dispatch).containsExactly(true, false);
            softly.assertThat(tabPresses).hasValue(1);
        });
    }

    @Test
    void supportsWindowLevelSwingShortcuts() throws InterruptedException {
        var shortcutInvocations = new AtomicInteger();
        var focusedComponent = new AtomicReference<JTextField>();
        var embedding = new EmbeddingComposite(shell);
        embedding.init(() -> {
            var root = new JPanel();
            var textField = new JTextField();
            root.add(textField);
            root.registerKeyboardAction(
                    event -> shortcutInvocations.incrementAndGet(),
                    KeyStroke.getKeyStroke(
                            KeyEvent.VK_Q,
                            Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()
                    ),
                    JComponent.WHEN_IN_FOCUSED_WINDOW
            );
            focusedComponent.set(textField);
            return root;
        });
        openShell();
        focus(focusedComponent.get());

        dispatchThroughFocusManager(
                focusedComponent.get(),
                KeyEvent.VK_Q,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()
        );

        assertThat(shortcutInvocations).hasValue(1);
    }

    private boolean[] dispatchTab(KeyEventDispatcher dispatcher, Component focusOwner, int modifiers) {
        focusManager.focusOwner = focusOwner;
        return dispatchFocusedTab(dispatcher, focusOwner, modifiers);
    }

    private boolean[] dispatchFocusedTab(KeyEventDispatcher dispatcher, Component focusOwner, int modifiers) {
        return SWT_AWTBridge.computeInEDT(() -> {
            var event = new KeyEvent(focusOwner, KeyEvent.KEY_PRESSED, 0, modifiers, KeyEvent.VK_TAB, '\t');
            var dispatched = dispatcher.dispatchKeyEvent(event);
            return new boolean[]{dispatched, event.isConsumed()};
        });
    }

    private boolean[] dispatchThroughFocusManager(Component focusOwner, int keyCode, int modifiers) {
        focusManager.focusOwner = null;
        return SWT_AWTBridge.computeInEDT(() -> {
            var event = new KeyEvent(
                    focusOwner,
                    KeyEvent.KEY_PRESSED,
                    System.currentTimeMillis(),
                    modifiers,
                    keyCode,
                    KeyEvent.CHAR_UNDEFINED
            );
            var dispatched = focusManager.dispatchEvent(event);
            return new boolean[]{dispatched, event.isConsumed()};
        });
    }

    private List<KeyEventDispatcher> traversalDispatchers() {
        return focusManager.addedDispatchers.stream()
                           .filter(candidate -> !(candidate instanceof Frame))
                           .collect(Collectors.toList());
    }

    private KeyEventDispatcher traversalDispatcher() {
        return traversalDispatchers().stream().findFirst().orElseThrow();
    }

    private void setBoundaryPolicy(EmbeddingComposite embedding, Component first, Component last) {
        var frame = SWT_AWT.getFrame(embedding);
        SWT_AWTBridge.invokeInEDTAndWait(() -> frame.setFocusTraversalPolicy(new ContainerOrderFocusTraversalPolicy() {
            @Override
            public Component getFirstComponent(Container container) {
                return first;
            }

            @Override
            public Component getLastComponent(Container container) {
                return last;
            }
        }));
    }

    private void openShell() {
        shell.setSize(600, 300);
        shell.open();
        shell.forceActive();
        while (display.readAndDispatch()) {
            // Allow both toolkits to realize their native peers.
        }
    }

    private void focus(Component component) throws InterruptedException {
        focusManager.focusOwner = null;
        SWT_AWTBridge.invokeInEDTAndWait(() -> {
            if (!component.requestFocusInWindow()) {
                component.requestFocus();
            }
        });
        waitUntil(() -> SWT_AWTBridge.computeInEDT(component::isFocusOwner));
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
            return focusOwner != null ? focusOwner : super.getFocusOwner();
        }

        @Override
        public void addKeyEventDispatcher(KeyEventDispatcher dispatcher) {
            addedDispatchers.add(dispatcher);
            super.addKeyEventDispatcher(dispatcher);
        }

        @Override
        public void removeKeyEventDispatcher(KeyEventDispatcher dispatcher) {
            removedDispatchers.add(dispatcher);
            super.removeKeyEventDispatcher(dispatcher);
        }
    }
}
