/*
 * Fireplace
 *
 * Copyright (c) 2021, Today - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.github.bric3.fireplace.ui.toolkit

import io.github.bric3.fireplace.core.ui.Colors
import io.github.bric3.fireplace.core.ui.LightDarkColor
import io.github.bric3.fireplace.ui.toolkit.Painter.Companion.blurOf
import io.github.bric3.fireplace.ui.toolkit.Painter.Companion.translucent
import java.awt.Color
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.awt.image.BufferedImageOp
import java.awt.image.ConvolveOp
import java.awt.image.Kernel
import javax.swing.*

/**
 * Experimental interface to paint something on a component.
 *
 * @see JPanelWithPainter
 * @see translucent
 * @see blurOf
 */
@FunctionalInterface
interface Painter{
    fun paint(g2: Graphics2D, c: JComponent)

    companion object {
        fun compose(vararg painters: Painter) = object : Painter {
            override fun paint(g2: Graphics2D, c: JComponent) {
                painters.forEach { it.paint(g2, c) }
            }
        }

        /**
         * A translucent painter.
         */
        fun translucent(
            translucentColor: Color = LightDarkColor(
                Colors.translucent_white_B0,
                Colors.translucent_black_80
            )
        ) = object : Painter {
            override fun paint(g2: Graphics2D, c: JComponent) {
                g2.color = translucentColor
                g2.fillRect(0, 0, c.width, c.height)
            }
        }

        /**
         * A painter that blurs the background of a component, using passed component as the source.
         */
        fun blurOf(bgComp: JComponent) = object : Painter {
            private lateinit var mOffscreenImage: BufferedImage
            private val mOperation: BufferedImageOp = kotlin.run {
                // matrix explained here https://www.jhlabs.com/ip/blurring.html
                val ninth = 1.0f / 9.0f
                val blurKernel = floatArrayOf(
                    ninth, ninth, ninth,
                    ninth, ninth, ninth,
                    ninth, ninth, ninth
                )
                ConvolveOp(
                    Kernel(3, 3, blurKernel),
                    ConvolveOp.EDGE_NO_OP, null
                )
            }

            override fun paint(g2: Graphics2D, c: JComponent) {
                // Only create the offscreen image if the one we have
                // is the wrong size.
                if (bgComp.width == 0 || bgComp.height == 0) {
                    return
                }

                if (!this::mOffscreenImage.isInitialized
                    || mOffscreenImage.width != bgComp.width
                    || mOffscreenImage.height != bgComp.height
                ) {
                    mOffscreenImage = BufferedImage(bgComp.width, bgComp.height, BufferedImage.TYPE_INT_ARGB)
                }
                val captureG2 = mOffscreenImage.createGraphics()
                captureG2.clip = g2.clip
                bgComp.paint(captureG2)
                captureG2.dispose()

                g2.drawImage(mOffscreenImage, mOperation, 0, 0)
            }
        }
    }
}