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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.JRootPane;
import javax.swing.RootPaneContainer;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Panel;
import java.awt.event.KeyEvent;
import java.awt.im.InputContext;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Center the logic of creating adding a Swing component to SWT components.
 *
 * <p>
 * How to use:
 * <pre><code>
 *  var embed = new EmbeddingComposite(parent);
 *
 *  // possibly declare childs to the composite, e.g.
 *  var tooltip = new DefaultToolTip(embed);
 *
 *  // run the initialization by providing a swing component supplier
 *  // this will be called in the AWT Event Dispatch Thread.
 *  // This supplier may use the embed children like the tooltip, however beware of SWT deadlocks.
 *  embed.init(() -&gt; new JLabel("Hello World"));
 * </code></pre>
 *
 * <p>
 * Remember to use {@link SWT_AWTBridge} methods to dispatch events from SWT to AWT.
 * </p>
 *
 * @see SWT_AWTBridge
 */
@SuppressWarnings("unused")
public class EmbeddingComposite extends Composite {
    private boolean focusTraversalEnabled;

    /**
     * Create the embedded composite with {@link SWT#EMBEDDED} and {@link SWT#NO_BACKGROUND} styles.
     *
     * @param parent the parent composite
     */
    public EmbeddingComposite(Composite parent) {
        this(parent, SWT.NONE);
    }

    /**
     * Create the embedded composite with {@link SWT#EMBEDDED} and {@link SWT#NO_BACKGROUND} styles with additional passed styles.
     * <p>
     * Note that {@link SWT#BORDER} is not supported.
     * </p>
     *
     * @param parent the parent composite
     * @param style  the additional style
     */
    public EmbeddingComposite(Composite parent, int style) {
        super(parent, checkNotBorder(style) | SWT.EMBEDDED | SWT.NO_BACKGROUND);
        setLayoutData(new GridData(GridData.FILL_BOTH));
        setLayout(new GridLayout(1, true));
    }

    private static int checkNotBorder(int style) {
        if ((style & SWT.BORDER) != 0) {
            throw new IllegalArgumentException("Border style is known to cause problem with the integration, apply the border to a wrapping SWT component");
        }
        return style;
    }

    /**
     * Configure whether <kbd>Tab</kbd> at the last Swing component, and <kbd>Shift</kbd>+<kbd>Tab</kbd>
     * at the first one, transfer focus to the neighboring SWT control. Call this before {@link #init(Supplier)}.
     *
     * <p>The dispatcher remains internal, so it can be limited to this embedded AWT frame and
     * removed when this composite is disposed of.</p>
     *
     * @param focusTraversalEnabled whether focus may leave Swing through Tab traversal
     */
    public void setFocusTraversalEnabled(boolean focusTraversalEnabled) {
        checkWidget();
        this.focusTraversalEnabled = focusTraversalEnabled;
    }

    /**
     * Initialize the AWT frame that will host the Swing component.
     *
     * <p>
     * Takes care of initializing the AWT frame and the Swing component on the AWT Event Dispatch Thread.
     * Note this method is blocking until the Swing component is initialized. But don't block the SWT Event thread.
     * </p>
     *
     * @param jComponentSupplier The jComponent supplier to be called on the AWT Event Dispatch Thread.
     * @see SWT_AWTBridge
     */
    public void init(Supplier<JComponent> jComponentSupplier) {
        var frame = SWT_AWT.new_Frame(this);
        frame.getInputContext(); // get the input context first to avoid deadlock
        var display = getDisplay();
        // KeyboardFocusManager keeps global dispatchers until explicitly removed.
        var traversalDispatcherRef = new AtomicReference<KeyEventDispatcher>();

        // SWT_AWT.new_Frame already queues Frame.dispose() when this Composite is disposed.
        // This EDT barrier lets that finish; removing the AWT peer again corrupts GTK state.
        addDisposeListener(e -> {
            SWT_AWTBridge.invokeInEDTAndWait(() -> {
                var traversalDispatcher = traversalDispatcherRef.get();
                if (traversalDispatcher != null) {
                    KeyboardFocusManager.getCurrentKeyboardFocusManager()
                                        .removeKeyEventDispatcher(traversalDispatcher);
                }
            });
        });

        var componentRef = new AtomicReference<JComponent>(null);
        SWT_AWTBridge.invokeInEDTAndWait(() -> {
            var jComponent = jComponentSupplier.get();
            componentRef.set(jComponent);

            /*
             * Bug 228221 - SWT no longer receives key events when using an SWT_AWT.new_Frame AWT frame.
             * Use a heavyweight RootPaneContainer to embed the Swing panel in the SWT part.
             * https://bugs.eclipse.org/bugs/show_bug.cgi?id=228221
             * http://www.eclipse.org/articles/article.php?file=Article-Swing-SWT-Integration/index.html
             * JApplet used to provide both pieces, but was removed in Java 26.
             *
             * Possible workaround for SWT hanging on some swing events
             * https://bugs.eclipse.org/bugs/show_bug.cgi?id=291326
             * https://bugs.eclipse.org/bugs/show_bug.cgi?id=376561
             */
            frame.add(new SwingRootPaneContainer(jComponent));

            if (focusTraversalEnabled) {
                /*
                 * AWT normally wraps Tab traversal inside its embedded Frame. Intercept only a Tab
                 * leaving the first or last Swing focus owner and hand it back to SWT instead. The
                 * dispatcher is scoped to this Frame and removed above when the composite is disposed.
                 */
                KeyEventDispatcher traversalDispatcher = event -> {
                    var focusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
                    var focusOwner = focusManager.getFocusOwner();
                    var traversal = swtTraversalDirection(event, focusOwner, frame);
                    if (traversal == SWT.TRAVERSE_NONE) {
                        return false;
                    }

                    event.consume();
                    SWT_AWTBridge.invokeSwtAwayFromAwt(display, () -> display.asyncExec(() -> {
                        if (!isDisposed()) {
                            traverse(traversal);
                        }
                    }));
                    return true;
                };
                traversalDispatcherRef.set(traversalDispatcher);
                KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(traversalDispatcher);
            }
        });

        // possible hack around invalid layout issue
        var dimension = SWT_AWTBridge.computeInEDT(() -> componentRef.get().getPreferredSize());
        setSize(dimension.width, dimension.height);
    }

    static int swtTraversalDirection(KeyEvent event, Component focusOwner, Container focusCycleRoot) {
        if (event.getID() != KeyEvent.KEY_PRESSED || event.getKeyCode() != KeyEvent.VK_TAB
                || focusOwner == null || !focusCycleRoot.isAncestorOf(focusOwner)) {
            return SWT.TRAVERSE_NONE;
        }

        var backwards = (event.getModifiersEx() & KeyEvent.SHIFT_DOWN_MASK) != 0;
        var policy = focusCycleRoot.getFocusTraversalPolicy();
        var boundary = backwards
                ? policy.getFirstComponent(focusCycleRoot)
                : policy.getLastComponent(focusCycleRoot);
        if (focusOwner != boundary) {
            return SWT.TRAVERSE_NONE;
        }

        return backwards ? SWT.TRAVERSE_TAB_PREVIOUS : SWT.TRAVERSE_TAB_NEXT;
    }

    /**
     * Replaces the removed {@code JApplet} containment behavior needed by the SWT/AWT bridge.
     * {@link Panel} supplies the heavyweight native peer recommended by {@link SWT_AWT}, unlike a
     * lightweight {@code JPanel}; {@link JRootPane} supplies Swing's content, layered, and glass panes.
     *
     * @see <a href="https://help.eclipse.org/latest/rtopic/org.eclipse.platform.doc.isv/reference/api/org/eclipse/swt/awt/SWT_AWT.html">SWT_AWT API documentation</a>
     * @see <a href="https://www.eclipse.org/articles/Article-Swing-SWT-Integration/">Swing/SWT Integration</a>
     */
    static final class SwingRootPaneContainer extends Panel implements RootPaneContainer {
        private final JRootPane rootPane = new JRootPane();

        SwingRootPaneContainer(JComponent content) {
            super(new BorderLayout());
            // JApplet also made its root pane opaque, so Swing painting has an opaque ancestor.
            rootPane.setOpaque(true);
            rootPane.getContentPane().add(content, BorderLayout.CENTER);
            add(rootPane, BorderLayout.CENTER);
        }

        /**
         * Preserves the existing workaround that avoids SWT/AWT input-context deadlocks.
         */
        @Override
        public InputContext getInputContext() {
            return null;
        }

        /**
         * Avoids the unnecessary heavyweight background clear, as JApplet did.
         */
        @Override
        public void update(Graphics graphics) {
            paint(graphics);
        }

        @Override
        public JRootPane getRootPane() {
            return rootPane;
        }

        @Override
        public void setContentPane(Container contentPane) {
            rootPane.setContentPane(contentPane);
        }

        @Override
        public Container getContentPane() {
            return rootPane.getContentPane();
        }

        @Override
        public void setLayeredPane(JLayeredPane layeredPane) {
            rootPane.setLayeredPane(layeredPane);
        }

        @Override
        public JLayeredPane getLayeredPane() {
            return rootPane.getLayeredPane();
        }

        @Override
        public void setGlassPane(Component glassPane) {
            rootPane.setGlassPane(glassPane);
        }

        @Override
        public Component getGlassPane() {
            return rootPane.getGlassPane();
        }
    }
}
