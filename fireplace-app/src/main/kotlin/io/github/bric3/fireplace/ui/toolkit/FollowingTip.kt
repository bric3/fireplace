package io.github.bric3.fireplace.ui.toolkit

import java.awt.AWTEvent
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.LayoutManager
import java.awt.RenderingHints
import java.awt.Window
import java.awt.event.AWTEventListener
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseEvent.MOUSE_DRAGGED
import java.awt.event.MouseEvent.MOUSE_ENTERED
import java.awt.event.MouseEvent.MOUSE_EXITED
import java.awt.event.MouseEvent.MOUSE_MOVED
import java.util.*
import javax.swing.*

class FollowingTip {
    private lateinit var tipWindow: JWindow
    private val contentContainer = RoundedPanel(layout = BorderLayout(), radius = 10).apply {
        border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    }
    private val contentProviders = WeakHashMap<JComponent, (JComponent, MouseEvent) -> JComponent?>()

    private val mouseHandler = AWTEventListener { e: AWTEvent ->
        require(::tipWindow.isInitialized) { "FollowingTip is not proper;y initialized" }

        val ownerWindow = tipWindow.owner
        val event: MouseEvent?
        val component: Component
        when (e.id) {
            MOUSE_ENTERED, MOUSE_MOVED, MOUSE_DRAGGED -> {
                event = e as MouseEvent
                component = e.component
                if (ownerWindow.isAncestorOf(component) && component is JComponent) {
                    val loc = event.locationOnScreen
                    tipWindow.setLocation(loc.x + 15, loc.y + 15)
                    contentContainer.removeAll()

                    // find content provider in the current component, or its parents
                    var c = component
                    var contentProvider = contentProviders[c]
                    while (contentProvider == null && c.parent != null) {
                        c = c.parent
                        contentProvider = contentProviders[c]
                    }

                    val content = contentProvider?.invoke(component, event)
                    if (content == null) {
                        tipWindow.isVisible = false
                        return@AWTEventListener
                    }
                    contentContainer.add(content)
                    contentContainer.bgColor = content.background
                    contentContainer.repaint()
                    tipWindow.pack()
                    tipWindow.isVisible = true
                }
            }

            MOUSE_EXITED -> {
                event = e as MouseEvent
                component = e.component
                val p = SwingUtilities.convertPoint(component, event.point, ownerWindow)
                if (!ownerWindow.contains(p)) {
                    tipWindow.isVisible = false
                }
            }

            else -> {}
        }
    }

    private fun <T : JComponent> attachToParent(c: T, contentProvider: (T, MouseEvent) -> JComponent?) {
        if (contentProviders.contains(c)) {
            return
        }
        @Suppress("UNCHECKED_CAST")
        contentProviders[c] = contentProvider as ((JComponent, MouseEvent) -> JComponent?)

        val parentWindow = SwingUtilities.getWindowAncestor(c)
        tipWindow = JWindow(parentWindow).apply {
            type = Window.Type.POPUP
            focusableWindowState = false
            contentPane.add(contentContainer)
            background = Color(0, true)
        }
    }

    fun activate() {
        val window = tipWindow.owner
        window.toolkit.addAWTEventListener(
            mouseHandler,
            AWTEvent.MOUSE_EVENT_MASK or AWTEvent.MOUSE_MOTION_EVENT_MASK
        )

        val p = window.mousePosition
        if (p != null) {
            SwingUtilities.convertPointToScreen(p, window)
            tipWindow.setLocation(p.x + 10, p.y + 10)
        }
    }

    fun deactivate() {
        val window = tipWindow.owner
        window.toolkit.removeAWTEventListener(mouseHandler)
        tipWindow.isVisible = false
    }

    fun <T : JComponent> enableFor(
        component: T,
        contentProvider: (c: T, MouseEvent) -> JComponent?
    ) {
        component.addHierarchyListener {
            if (it.changed is JFrame) {
                attachToParent(component, contentProvider)
                activate()
            }
        }

        component.addComponentListener(object : ComponentAdapter() {
            override fun componentHidden(e: ComponentEvent) =
                deactivate()
        })

        component.addMouseListener(object : MouseAdapter() {
            override fun mouseEntered(e: MouseEvent) = activate()
            override fun mouseExited(e: MouseEvent) = deactivate()
        })
    }
}

private class RoundedPanel(
    layout: LayoutManager? = null,
    var bgColor: Color? = null,
    var borderColor: Color? = null,
    var radius: Int
) : JPanel(layout) {
    init {
        isOpaque = false
        background = Color(0, true)
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val graphics = g as Graphics2D
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        // Draws the rounded panel with borders.
        graphics.color = bgColor ?: background
        graphics.fillRoundRect(0, 0, this.width - 1, this.height - 1, radius, radius)

        graphics.color = borderColor ?: foreground
        graphics.drawRoundRect(0, 0, this.width - 1, this.height - 1, radius, radius)
    }
}