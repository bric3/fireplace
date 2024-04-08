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

        jfrBinder.bindEvents({
            val executionSamples = JfrAnalyzer.executionSamples(it)
            executionSamples.stacktraceButterflyModel(nodeSelector = { frame ->
                (frame.method.type.typeName == "StupidMain" && frame.method.methodName == "work") ||
                        (frame.method.type.typeName.contains("CoreProgressManager") && frame.method.methodName.contains(
                            "computeUnderProgress"
                        ))
            })
        }) {
            butterflyPane.setStacktraceButterflyModelAsync(it)
        }

        butterflyPane
    }
}
