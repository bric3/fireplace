package io.github.bric3.fireplace.views.general

import io.github.bric3.fireplace.JFRBinder
import io.github.bric3.fireplace.JfrAnalyzer
import io.github.bric3.fireplace.simpleReadOnlyTable
import io.github.bric3.fireplace.unwrappedTable
import io.github.bric3.fireplace.views.ViewPanel
import javax.swing.JComponent
import javax.swing.JScrollPane

class SystemProperties(private val jfrBinder: JFRBinder) : ViewPanel {
    override val identifier = "System properties"

    private val jvmSystemProps: JScrollPane by lazy {
        simpleReadOnlyTable(
            arrayOf(),
            arrayOf("Key", "Value")
        ).apply {
            jfrBinder.bindEvents<Map<String, String>>(
                JfrAnalyzer::jvmSystemProperties
            ) { props ->
                unwrappedTable().model.setData(
                    props.map<String, String, Array<String>> { arrayOf<String>(it.key, it.value) }
                        .toTypedArray<Array<String>>()
                )
            }
        }
    }

    override fun getView(): JComponent = jvmSystemProps
}