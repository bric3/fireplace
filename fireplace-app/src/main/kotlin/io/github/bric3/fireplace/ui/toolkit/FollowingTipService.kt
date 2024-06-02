package io.github.bric3.fireplace.ui.toolkit

import java.awt.AWTEvent
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.LayoutManager
import java.awt.RenderingHints
import java.awt.event.AWTEventListener
import java.awt.event.FocusEvent.FOCUS_LOST
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseEvent.MOUSE_DRAGGED
import java.awt.event.MouseEvent.MOUSE_ENTERED
import java.awt.event.MouseEvent.MOUSE_EXITED
import java.awt.event.MouseEvent.MOUSE_MOVED
import java.awt.event.MouseEvent.MOUSE_WHEEL
import java.awt.event.WindowEvent.WINDOW_LOST_FOCUS
import java.util.*
import javax.swing.*

/**
 * A tooltip that follows the mouse cursor.
 */
object FollowingTipService {
    private val followingTip = FollowingTip()

    fun <T : JComponent> enableFor(
        component: T,
        contentProvider: (c: T, MouseEvent) -> JComponent?
    ) {
        // only attach when the component hierarchy is added to a JFrame
        component.addHierarchyListener {
            if (it.changed is JFrame) {
                followingTip.install(component, contentProvider)
            }
        }
    }

    fun disableFor(component: JComponent) {
        followingTip.deinstall(component)
    }
}

private class FollowingTip {
    private lateinit var tipWindow: JWindow
    private lateinit var tipPopup: Popup
    private val contentContainer = RoundedPanel(layout = BorderLayout(), radius = 10).apply {
        border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    }
    private val contentProviders = WeakHashMap<JComponent, (JComponent, MouseEvent) -> JComponent?>()

    /**
     * A dummy mouse listener that is used to make the component eligible to receive mouse events.
     */
    private val dummyMouseListener = object : MouseAdapter() {}

    /**
     * The mouse event handler that will update the tooltip position and content.
     */
    private val mouseHandler = AWTEventListener { e: AWTEvent ->
        require(::tipWindow.isInitialized) { "FollowingTip is not properly initialized" }
        require(::tipPopup.isInitialized) { "FollowingTip is not properly initialized" }

        val ownerWindow = tipWindow.owner
        val event: MouseEvent?
        val component: Component
        when (e.id) {
            MOUSE_ENTERED, MOUSE_MOVED, MOUSE_DRAGGED, MOUSE_WHEEL -> {
                // Don't bother to show tip if the owner window is not focused or active
                if (!ownerWindow.isActive || !ownerWindow.isFocused) {
                    tipWindow.isVisible = false
                    return@AWTEventListener
                }
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
                    if (content == null || !ownerWindow.isActive || !ownerWindow.isFocused) {
                        tipWindow.isVisible = false
                        return@AWTEventListener
                    }

                    contentContainer.add(content)
                    contentContainer.bgColor = content.background
                    tipWindow.pack()
                    tipWindow.isVisible = true
                }
            }

            MOUSE_EXITED -> {
                event = e as MouseEvent
                component = e.component
                val p = SwingUtilities.convertPoint(component, event.point, ownerWindow)
                if (!ownerWindow.contains(p) || !ownerWindow.isActive || !ownerWindow.isFocused) {
                    tipWindow.isVisible = false
                }
            }

            WINDOW_LOST_FOCUS, FOCUS_LOST -> {
                tipWindow.isVisible = false
            }

            else -> {}
        }
    }

    fun <T : JComponent> install(component: T, contentProvider: (T, MouseEvent) -> JComponent?) {
        @Suppress("UNCHECKED_CAST")
        contentProviders[component] = contentProvider as ((JComponent, MouseEvent) -> JComponent?)

        // To receive mouse events, the component must be told to listen to mouse events,
        // this can be done either from within (e.g., via the protected `Component::enableEvents(mask)`),
        // or from outside (e.g., via `Component::addMouseListener`). Without this, the component
        // will not be eligible to receive events (even those from the `AWTEventListener`).
        component.addMouseListener(dummyMouseListener)

        if (!::tipPopup.isInitialized) {
            createTipWindow(component)
            
            tipWindow.owner.toolkit.addAWTEventListener(
                mouseHandler,
                AWTEvent.MOUSE_EVENT_MASK
                        or AWTEvent.MOUSE_MOTION_EVENT_MASK
                        or AWTEvent.MOUSE_WHEEL_EVENT_MASK
                        or AWTEvent.WINDOW_FOCUS_EVENT_MASK
                        or AWTEvent.FOCUS_EVENT_MASK
            )
        }
    }

    fun deinstall(component: JComponent) {
        val location = tipWindow.locationOnScreen.apply {
            SwingUtilities.convertPointFromScreen(this, component)
        }
        if (component.contains(location)) {
            tipWindow.isVisible = false
        }

        contentProviders.remove(component)
        component.removeMouseListener(dummyMouseListener)
    }

    private fun createTipWindow(component: JComponent) {
        // Use the window of PopupFactory as the tooltip window,
        // because it creates a proper heavy-weight Window with everything set up,
        // including double buffering disabled, which works better for the following tip.
        val parentWindow = SwingUtilities.getWindowAncestor(component)
        val popupFactory = PopupFactory.getSharedInstance()
        tipPopup = popupFactory.getPopup(parentWindow, contentContainer, 0, 0)
        tipWindow = SwingUtilities.getWindowAncestor(contentContainer) as JWindow
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
        graphics.color = bgColor ?: UIUtil.Colors.backgroundColor
        graphics.fillRoundRect(0, 0, this.width - 1, this.height - 1, radius, radius)

        graphics.color = borderColor ?: UIUtil.Colors.borderColor
        graphics.drawRoundRect(0, 0, this.width - 1, this.height - 1, radius, radius)
    }
}