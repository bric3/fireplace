package io.github.bric3.fireplace.ui

import io.github.bric3.fireplace.core.ui.Colors
import io.github.bric3.fireplace.core.ui.LightDarkColor
import java.awt.Color
import java.awt.Graphics2D
import java.awt.GraphicsEnvironment
import java.awt.Transparency
import java.awt.image.BufferedImage
import java.awt.image.BufferedImageOp
import java.awt.image.ConvolveOp
import java.awt.image.Kernel
import javax.swing.JComponent

/**
 * Experimental interface to paint something on a component.
 */
interface Painter {
    fun paint(g2: Graphics2D, c: JComponent)

    companion object {
        fun translucent() = object : Painter {
            private val translucentColor: Color = LightDarkColor(
                Colors.translucent_white_D0,
                Colors.translucent_black_80
            )

            override fun paint(g2: Graphics2D, c: JComponent) {
                g2.color = translucentColor
                g2.fillRect(0, 0, c.width, c.height)
            }
        }


        fun blurOf(bgComp: JComponent) = object : Painter {
            private lateinit var mOffscreenImage: BufferedImage
            private val mOperation: BufferedImageOp
            private var isAlreadyPainting : Boolean = false
            private val translucentTint: Color = LightDarkColor(
                Colors.translucent_white_B0,
                Colors.translucent_black_80
            )

            init {
                val ninth = 1.0f / 9.0f
                val blurKernel = floatArrayOf(
                    ninth, ninth, ninth,
                    ninth, ninth, ninth,
                    ninth, ninth, ninth
                )
                mOperation = ConvolveOp(
                    Kernel(3, 3, blurKernel),
                    ConvolveOp.EDGE_NO_OP, null
                )
            }

            override fun paint(g2: Graphics2D, c: JComponent) {
                if (isAlreadyPainting) return

                // Only create the offscreen image if the one we have
                // is the wrong size.
                if(bgComp.width == 0 || bgComp.height == 0) {
                    return
                }

                if (!this::mOffscreenImage.isInitialized
                    || mOffscreenImage.width != bgComp.width
                    || mOffscreenImage.height != bgComp.height) {
                    isAlreadyPainting = true

                    val capture = BufferedImage(bgComp.width, bgComp.height, BufferedImage.TYPE_INT_ARGB)
                    val captureG2 = capture.createGraphics()
                    captureG2.clip = g2.clip
                    bgComp.paint(captureG2)
                    captureG2.dispose()

                    val gc = GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice.defaultConfiguration
                    val blurImage = gc.createCompatibleImage(capture.width, capture.height, Transparency.TRANSLUCENT)
                    blurImage.coerceData(true)

                    val blurryG2 = blurImage.createGraphics()
                    blurryG2.drawImage(capture, mOperation, 0, 0)

                    mOffscreenImage = blurImage
                    isAlreadyPainting = false
                }

                g2.drawImage(mOffscreenImage, 0, 0, null)
                g2.color = translucentTint
                g2.fillRect(0, 0, c.width, c.height)
            }
        }
    }
}