package io.github.bric3.fireplace.views.general

import io.github.bric3.fireplace.JFRBinder
import io.github.bric3.fireplace.JfrAnalyzer
import io.github.bric3.fireplace.views.ViewPanel
import javax.swing.JComponent
import javax.swing.JTextArea

class SystemProperties(jfrBinder: JFRBinder) : ViewPanel  {
    override val identifier = "System properties"

    private val systemPropertiesPane: JTextArea =
        JTextArea().apply {
            isEditable = false
            dropTarget = null
            caret.isVisible = true
            caret.isSelectionVisible = true
            jfrBinder.bindEvents(
                JfrAnalyzer::jvmSystemProperties,
                this::setText
            )
        }

    override fun getView(): JComponent = systemPropertiesPane
}