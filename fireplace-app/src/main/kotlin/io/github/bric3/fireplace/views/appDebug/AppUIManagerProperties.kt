package io.github.bric3.fireplace.views.appDebug

import io.github.bric3.fireplace.Utils
import io.github.bric3.fireplace.simpleReadOnlyTable
import io.github.bric3.fireplace.views.ViewPanel
import javax.swing.JComponent
import javax.swing.UIManager

class AppUIManagerProperties : ViewPanel {
    override val identifier: String = "App UIManager properties"

    private val component by lazy {
        // UIManager.getLookAndFeelDefaults()
        //     .entries
        //     .stream()
        //     .filter { (_, value): Entry<Any?, Any?> -> value is Color }
        //     .map { (key, value): Entry<Any, Any> ->
        //         "$key: " + String.format(
        //             "#%06X",
        //             0xFFFFFF and (value as Color).rgb
        //         )
        //     }
        //     .sorted()
        //     .map {  }

        simpleReadOnlyTable(
            UIManager.getLookAndFeelDefaults()
                .entries
                .map { arrayOf(it.key, it.value) }
                .toTypedArray(),
            arrayOf("Key", "Value")
        )
    }

    override fun getView(): JComponent = component


    companion object {
        fun isActive(): Boolean = Utils.isFireplaceDebug()
    }
}
