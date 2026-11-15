package io.github.cdsap.gcreport.plugin.output

internal object CsvReportProjection {
    fun collection(counts: Map<String, Int>): String = project("Collection type,Occurrences", counts.toList())

    fun histogram(entries: List<Pair<String, Int>>): String = project("Bucket,Occurrences", entries)

    private fun project(
        header: String,
        rows: List<Pair<String, Int>>,
    ): String =
        buildString {
            appendLine(header)
            rows.forEach { (label, count) ->
                appendLine("$label,$count")
            }
        }
}
