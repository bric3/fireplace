package io.github.bric3.fireplace.views.appDebug

import io.github.bric3.fireplace.Utils
import io.github.bric3.fireplace.simpleReadOnlyTable
import io.github.bric3.fireplace.views.ViewPanel

class AppSystemProperties : ViewPanel {
    override val identifier: String = "App System properties"

    override val view by lazy {
        simpleReadOnlyTable(
            System.getProperties().map { arrayOf(it.key, it.value) }.toTypedArray(),
            arrayOf("Key", "Value")
        )
    }

    companion object {
        fun isActive(): Boolean = Utils.isDebugging() || Utils.isFireplaceDebug()
    }
}