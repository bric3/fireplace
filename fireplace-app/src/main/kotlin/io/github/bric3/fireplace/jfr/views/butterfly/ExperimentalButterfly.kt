package io.github.bric3.fireplace.jfr.views.butterfly

import io.github.bric3.fireplace.jfr.support.JFRLoaderBinder
import io.github.bric3.fireplace.jfr.support.JfrAnalyzer
import io.github.bric3.fireplace.jfr.support.stacktraceButterflyModel
import io.github.bric3.fireplace.ui.ButterflyPane
import io.github.bric3.fireplace.ui.ViewPanel

class ExperimentalButterfly(private val jfrBinder: JFRLoaderBinder) : ViewPanel {
    override val identifier = "Experimental Butterfly"

    override val view by lazy {
        val butterflyPane = ButterflyPane()

        jfrBinder.bindEvents(JfrAnalyzer::executionSamples) {
            val stacktraceButterflyModel = it.stacktraceButterflyModel(nodeSelector = { frame ->
                frame.method.methodName.contains("computeUnderProgress") && frame.method.type.typeName.contains("CoreProgressManager")
            })

            butterflyPane.setStacktraceButterflyModel(stacktraceButterflyModel)
        }

        butterflyPane
    }
}
