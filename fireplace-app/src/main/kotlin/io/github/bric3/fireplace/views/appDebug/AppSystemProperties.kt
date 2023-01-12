package io.github.bric3.fireplace.views.appDebug

import io.github.bric3.fireplace.Utils
import io.github.bric3.fireplace.simpleReadOnlyTable
import io.github.bric3.fireplace.views.ViewPanel
import javax.swing.JComponent

class AppSystemProperties : ViewPanel {
    override val identifier: String = "App System properties"

    override fun getView(): JComponent {
        return simpleReadOnlyTable(
            System.getProperties().map { arrayOf(it.key, it.value) }.toTypedArray(),
            arrayOf("Key", "Value")
        )
    }

    companion object {
        fun isActive(): Boolean = Utils.isDebugging() || Utils.isFireplaceDebug()
    }
}