package io.github.bric3.fireplace.views

import javax.swing.JComponent

interface ViewPanel {
    val identifier: String
    val view: JComponent
}