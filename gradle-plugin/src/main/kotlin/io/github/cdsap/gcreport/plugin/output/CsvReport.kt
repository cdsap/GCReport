package io.github.cdsap.gcreport.plugin.output

internal fun formatCsvReport(
    header: String,
    rows: List<Pair<String, Int>>,
): String =
    buildString {
        appendLine(header)
        rows.forEach { (label, count) ->
            appendLine("$label,$count")
        }
    }
