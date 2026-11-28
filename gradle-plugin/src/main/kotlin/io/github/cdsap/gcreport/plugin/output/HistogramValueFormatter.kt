package io.github.cdsap.gcreport.plugin.output

internal object HistogramValueFormatter {
    fun format(histogram: List<Pair<String, Int>>): String {
        var histogramText = "["
        histogram.forEach {
            histogramText += "\"${it.first}\": \"${it.second}\", "
        }
        return "${histogramText.dropLast(1)}]"
    }
}
