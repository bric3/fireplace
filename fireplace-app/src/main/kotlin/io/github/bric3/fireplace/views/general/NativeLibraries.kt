package io.github.bric3.fireplace.views.general

import io.github.bric3.fireplace.JFRBinder
import io.github.bric3.fireplace.JfrAnalyzer
import io.github.bric3.fireplace.simpleReadOnlyTable
import io.github.bric3.fireplace.unwrappedTable
import io.github.bric3.fireplace.views.ViewPanel
import javax.swing.JComponent

class NativeLibraries(private val jfrBinder: JFRBinder) : ViewPanel {
    override val identifier = "Native libraries"

    private val nativeLibs by lazy {
        simpleReadOnlyTable(
            arrayOf(),
            arrayOf("Path")
        ).apply {
            jfrBinder.bindEvents(
                JfrAnalyzer::nativeLibraries
            ) { libs ->
                unwrappedTable().model.setData(
                    libs.map { arrayOf(it) }.toTypedArray()
                )
            }
        }
    }

    override fun getView(): JComponent = nativeLibs
}