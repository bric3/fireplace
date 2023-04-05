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

import io.github.bric3.fireplace.swt_awt.SWT_AWTBridge;
import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
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
 * Don't forget to use {@link SWT_AWTBridge} methods to dispatch events from SWT to AWT.
 * </p>
 *
 * @see SWT_AWTBridge
 */
@SuppressWarnings("unused")
public class EmbeddingComposite extends Composite {

    /**
     * Create the embedded composite with {@link SWT#EMBEDDED} and {@link SWT#NO_BACKGROUND} styles.
     * @param parent the parent composite
     */
    public EmbeddingComposite(Composite parent) {
        this(parent, SWT.NONE);
    }

    /**
     * Create the embedded composite with {@link SWT#EMBEDDED} and {@link SWT#NO_BACKGROUND} styles with additional passed styles.
     * <p>
     *     Note that {@link SWT#BORDER} is not supported.
     * </p>
     *
     * @param parent the parent composite
     * @param style the additional style
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
     * Initialize the AWT frame that will host the Swing component.
     *
     * <p>
     *     Takes care of initializing the AWT frame and the Swing component on the AWT Event Dispatch Thread.
     *     Note this method is blocking until the Swing component is initialized. But don't block the SWT Event thread.
     * </p>
     *
     * @param jComponentSupplier The jComponent supplier to be called on the AWT Event Dispatch Thread.
     * @see SWT_AWTBridge
     */
    public void init(Supplier<JComponent> jComponentSupplier) {
        var frame = SWT_AWT.new_Frame(this);
        frame.getInputContext(); // get the input context first to avoid deadlock

        // needed to properly terminate the app on close
        addDisposeListener(e -> {
            System.out.println("embedded composite disposed");
            SWT_AWTBridge.invokeInEDTAndWait(() -> {
                try {
                    frame.removeNotify();
                } catch (Exception ignored) {
                }
            });
        });

        var componentRef = new AtomicReference<JComponent>(null);
        SWT_AWTBridge.invokeInEDTAndWait(() -> {
            var jComponent = jComponentSupplier.get();
            componentRef.set(jComponent);

            /*
             * Bug 228221 - SWT no longer receives key events in KeyAdapter when using SWT_AWT.new_Frame AWT frame
             * Use a RootPaneContainer e.g. JApplet to embed the swing panel in the SWT part
             * https://bugs.eclipse.org/bugs/show_bug.cgi?id=228221
             * http://www.eclipse.org/articles/article.php?file=Article-Swing-SWT-Integration/index.html
             * The proposal to use JApplet instead of JPanel no longer works (eclipse photon java 8 and 11),
             * key events are only partially propagated to the underlying SWT event queue.
             *
             * Possible workaround for SWT hanging on some swing events
             * https://bugs.eclipse.org/bugs/show_bug.cgi?id=291326
             * https://bugs.eclipse.org/bugs/show_bug.cgi?id=376561
             */
            @SuppressWarnings("deprecation") var applet = new JApplet() {
                @Override
                public InputContext getInputContext() {
                    return null;
                }
            };

            /*
             * In JRE 1.4, the JApplet makes itself a focus cycle root. This
             * interferes with the focus handling installed on the parent frame, so
             * change it back to a non-root here.
             */
            applet.setFocusCycleRoot(false);

            /*
             * Use the following approach to add the component to the applet
             * otherwise AWT / SWT can deadlock.
             *
             * DO NOT USE: applet.getRootPane().getContentPane().add(component)
             */
            {
                applet.setLayout(new BorderLayout());
                applet.add(jComponent, BorderLayout.CENTER);
            }

            frame.add(applet);
            frame.addKeyListener(new KeyAdapter() {
                @Override
                public void keyReleased(KeyEvent e) {
                    if ((e.getModifiersEx() & InputEvent.META_DOWN_MASK) != 0) {
                        if (e.getKeyChar() == 'q') {
                            System.out.println("cmd+q pressed");
                            /* This code tries to gracefully handle the exit, but
                             * In an SWT_AWT listener there's a listener that handle the FocusOut event but the call to
                             * synthesizeWindowActivation may produce an NPE if the frame is still active / focused.
                             *
                             * Another approach is possible using System.exit(0), however the app exits abruptly.
                             */
                            frame.dispose();
                            SWT_AWTBridge.invokeSwtAwayFromAwt(EmbeddingComposite.this.getDisplay(), () -> {
                                EmbeddingComposite.this.getDisplay().dispose();
                            });
                        }
                    }
                }
            });
        });

        // possible hack around invalid layout issue
        var dimension = SWT_AWTBridge.computeInEDT(() -> componentRef.get().getPreferredSize());
        setSize(dimension.width, dimension.height);
    }
}
