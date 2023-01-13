package io.github.bric3.fireplace.views.cpu

import io.github.bric3.fireplace.JFRBinder
import io.github.bric3.fireplace.JfrAnalyzer
import io.github.bric3.fireplace.views.FlameGraphPane
import io.github.bric3.fireplace.views.ViewPanel
import javax.swing.JComponent

class MethodCpuSample(private val jfrBinder: JFRBinder) : ViewPanel {
    override val identifier = "CPU"

    private val flameGraphPane by lazy {
        FlameGraphPane().apply {
            jfrBinder.bindEvents(
                JfrAnalyzer::stackTraceCPUFun,
                this::setStacktraceTreeModel,
            )
        }
    }

    override fun getView(): JComponent = flameGraphPane
}