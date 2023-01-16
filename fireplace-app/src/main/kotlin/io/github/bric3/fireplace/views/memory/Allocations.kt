package io.github.bric3.fireplace.views.memory

import io.github.bric3.fireplace.JFRBinder
import io.github.bric3.fireplace.JfrAnalyzer
import io.github.bric3.fireplace.views.FlameGraphPane
import io.github.bric3.fireplace.views.ViewPanel

class Allocations(private val jfrBinder: JFRBinder) : ViewPanel {
    override val identifier = "Allocations"

    override val view by lazy {
        FlameGraphPane().apply {
            jfrBinder.bindEvents(
                JfrAnalyzer::stackTraceAllocationFun,
                this::setStacktraceTreeModel
            )
        }
    }
}