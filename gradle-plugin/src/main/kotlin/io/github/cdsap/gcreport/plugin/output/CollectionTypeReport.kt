package io.github.cdsap.gcreport.plugin.output

import com.jakewharton.picnic.TextAlignment
import com.jakewharton.picnic.table

internal object CollectionTypeReport {
    fun consoleTable(
        logDisplayName: String,
        counts: List<Pair<String, Int>>,
    ): String =
        table {
            cellStyle {
                alignment = TextAlignment.MiddleLeft
                paddingLeft = 1
                paddingRight = 1
                border = true
            }
            row {
                cell("GC Log: $logDisplayName") {
                    columnSpan = 2
                }
            }
            row("Collection type", "Occurrences")
            counts.forEach { (description, count) ->
                row {
                    cell(description)
                    cell(count) {
                        alignment = TextAlignment.MiddleRight
                    }
                }
            }
        }.toString()

    fun csv(counts: List<Pair<String, Int>>): String = CsvReportFormatter.format("Collection type", "Occurrences", counts)
}
