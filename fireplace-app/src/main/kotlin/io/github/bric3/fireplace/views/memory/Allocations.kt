package io.github.bric3.fireplace.views.memory

import io.github.bric3.fireplace.JFRBinder
import io.github.bric3.fireplace.JfrAnalyzer
import io.github.bric3.fireplace.views.FlameGraphPane
import io.github.bric3.fireplace.views.ViewPanel
import javax.swing.JComponent

class Allocations(jfrBinder: JFRBinder) : ViewPanel {
    override val identifier = "Allocations"

    private val allocationPane: FlameGraphPane =
        FlameGraphPane().apply {
            jfrBinder.bindEvents(
                JfrAnalyzer::stackTraceAllocationFun,
                this::setStacktraceTreeModel
            )
        }

    override fun getView(): JComponent = allocationPane
}