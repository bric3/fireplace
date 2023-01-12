package io.github.bric3.fireplace.views

import javax.swing.*

interface ViewPanel {
    val identifier: String
    fun getView(): JComponent
}