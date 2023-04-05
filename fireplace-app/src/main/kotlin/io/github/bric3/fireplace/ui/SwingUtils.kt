/*
 * Fireplace
 *
 * Copyright (c) 2021, Today - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.github.bric3.fireplace.ui

import java.awt.Component
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import javax.swing.JLayeredPane
import javax.swing.JSplitPane

fun JSplitPane.autoSize(proportionalLocation: Double) {
    this.addComponentListener(object : ComponentAdapter() {
        private var firstResize = true
        override fun componentResized(e: ComponentEvent? ) {
            if (firstResize) {
                this@autoSize.setDividerLocation(proportionalLocation)
                firstResize = false
            }
        }
    })
}

/**
 * This function is required in Kotlin for JLayeredPane to properly add components as layer.
 *
 * Otherwise, kotlin will treat [JLayeredPane.DEFAULT_LAYER] constants
 * as `int` and will instead call [`JLayeredPane.add(Component comp, int index)`][JLayeredPane.add]
 * instead of [`JLayeredPane.add(Component comp, Object constraints)`][JLayeredPane.add].
 *
 * Java will generate the following byt code
 *
 * ```
 * GETSTATIC javax/swing/JLayeredPane.MODAL_LAYER : Ljava/lang/Integer;
 * INVOKEVIRTUAL javax/swing/JLayeredPane.add (Ljava/awt/Component;Ljava/lang/Object;)V
 * ```
 *
 * While Kotlin will generate
 * ```
 * INVOKEVIRTUAL java/lang/Integer.intValue ()I
 * INVOKEVIRTUAL javax/swing/JLayeredPane.add (Ljava/awt/Component;I)Ljava/awt/Component;
 * ```
 *
 * These methods make sure the constraint is treated as an [Object].
 */
fun JLayeredPane.addLayer(c: Component, constraint: Int) {
    add(c, constraint as Any)
}
