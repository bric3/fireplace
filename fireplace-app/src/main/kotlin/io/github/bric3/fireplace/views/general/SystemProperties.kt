package io.github.bric3.fireplace.views.general

import io.github.bric3.fireplace.JFRBinder
import io.github.bric3.fireplace.JfrAnalyzer
import io.github.bric3.fireplace.simpleReadOnlyTable
import io.github.bric3.fireplace.unwrappedTable
import io.github.bric3.fireplace.views.ViewPanel
import javax.swing.JComponent

class SystemProperties(private val jfrBinder: JFRBinder) : ViewPanel {
    override val identifier = "System properties"

    override val view: JComponent by lazy {
        simpleReadOnlyTable(
            arrayOf(),
            arrayOf("Key", "Value")
        ).apply {
            jfrBinder.bindEvents(
                JfrAnalyzer::jvmSystemProperties
            ) { props ->
                unwrappedTable().model.setData(
                    props.map { arrayOf(it.key, it.value) }.toTypedArray()
                )
            }
        }
    }
}