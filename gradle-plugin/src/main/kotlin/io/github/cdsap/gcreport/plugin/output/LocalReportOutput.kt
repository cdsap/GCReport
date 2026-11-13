package io.github.cdsap.gcreport.plugin.output

import com.jakewharton.picnic.TextAlignment
import com.jakewharton.picnic.table
import io.github.cdsap.gcreport.plugin.extensions.getFileName
import io.github.cdsap.gcreport.plugin.model.Bucket
import io.github.cdsap.gcreport.plugin.model.GCReportMetrics

internal object LocalReportOutput {
    data class Histogram(
        val bucket: Bucket,
        val rows: List<Pair<String, Int>>,
    )

    data class Payloads(
        val collectionCsv: String,
        val histogramCsv: String?,
        val consoleTables: List<String>,
    )

    fun render(
        logName: String,
        metrics: GCReportMetrics,
        histogram: Histogram?,
    ): Payloads {
        val collectionRows = metrics.collectionTypeCounts().toList()
        return Payloads(
            collectionCsv = formatCsvReport("Collection type,Occurrences", collectionRows),
            histogramCsv = histogram?.let { formatCsvReport("Bucket,Occurrences", it.rows) },
            consoleTables =
                buildList {
                    add(renderCollectionTable(logName, collectionRows))
                    if (histogram != null) {
                        add(renderHistogramTable(logName, histogram))
                    }
                },
        )
    }

    private fun renderCollectionTable(
        logName: String,
        rows: List<Pair<String, Int>>,
    ): String =
        table {
            cellStyle {
                alignment = TextAlignment.MiddleLeft
                paddingLeft = 1
                paddingRight = 1
                border = true
            }
            row {
                cell("GC Log: ${logName.getFileName()}") {
                    columnSpan = 2
                }
            }
            row("Collection type", "Occurrences")
            rows.forEach { (description, count) ->
                row {
                    cell(description)
                    cell(count) {
                        alignment = TextAlignment.MiddleRight
                    }
                }
            }
        }.toString()

    private fun renderHistogramTable(
        logName: String,
        histogram: Histogram,
    ): String =
        table {
            cellStyle {
                alignment = TextAlignment.MiddleLeft
                paddingLeft = 1
                paddingRight = 1
                border = true
            }
            row {
                cell("GC Histogram: ${logName.getFileName()}") {
                    columnSpan = 2
                }
            }
            row {
                cell("Type: ${histogram.bucket.name}") {
                    columnSpan = 2
                }
            }
            row("Bucket", "Occurrences")
            histogram.rows.forEach { (bucket, count) ->
                row {
                    cell(bucket)
                    cell(count) {
                        alignment = TextAlignment.MiddleRight
                    }
                }
            }
        }.toString()
}
