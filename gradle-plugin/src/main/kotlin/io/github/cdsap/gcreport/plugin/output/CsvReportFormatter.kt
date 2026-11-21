package io.github.cdsap.gcreport.plugin.output

internal object CsvReportFormatter {
    fun format(
        firstHeader: String,
        secondHeader: String,
        rows: List<Pair<String, Int>>,
    ): String =
        buildString {
            appendLine("$firstHeader,$secondHeader")
            rows.forEach { (label, count) ->
                appendLine("$label,$count")
            }
        }
}
