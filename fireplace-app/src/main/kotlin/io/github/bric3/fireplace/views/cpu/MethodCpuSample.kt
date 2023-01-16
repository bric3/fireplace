package io.github.bric3.fireplace.views.cpu

import io.github.bric3.fireplace.JFRBinder
import io.github.bric3.fireplace.JfrAnalyzer
import io.github.bric3.fireplace.views.FlameGraphPane
import io.github.bric3.fireplace.views.ViewPanel

class MethodCpuSample(private val jfrBinder: JFRBinder) : ViewPanel {
    override val identifier = "CPU"

    override val view by lazy {
        FlameGraphPane().apply {
            jfrBinder.bindEvents(
                JfrAnalyzer::stackTraceCPUFun,
                this::setStacktraceTreeModel,
            )
        }
    }
}