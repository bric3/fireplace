package io.github.bric3.fireplace.charts

import java.awt.Color


data class ChartSpecification(
    val dataset: XYDataset,
    val label:String,
    val renderer: RendererDescriptor,

) {
    sealed interface RendererDescriptor

    data class LineRendererDescriptor(
        val lineColor: Color,
        val fillColors: List<Color>?,
    ) : RendererDescriptor
}

