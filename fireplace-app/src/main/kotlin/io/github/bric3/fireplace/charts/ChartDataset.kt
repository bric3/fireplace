package io.github.bric3.fireplace.charts

interface ChartDataset {
    val label: String
    val rangeOfX: Range<Long>
    val rangeOfY: Range<Double>
    val itemCount: Int
    fun xAt(index: Int): Long
    fun yAt(index: Int): Double?
}