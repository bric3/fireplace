package io.github.bric3.fireplace.ui

import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
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
