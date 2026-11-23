package io.github.cdsap.gcreport.plugin.output

import com.jakewharton.picnic.TextAlignment
import com.jakewharton.picnic.table
import io.github.cdsap.gcreport.plugin.extensions.getFileName
import io.github.cdsap.gcreport.plugin.histogram.Histogram
import io.github.cdsap.gcreport.plugin.model.Bucket
import io.github.cdsap.gcreport.plugin.model.GCReportMetrics

internal object LocalReportProjection {
    data class Output(
        val collectionCsv: String,
        val histogramCsv: String?,
        val consoleTables: List<String>,
    )

    fun project(
        logName: String,
        metrics: GCReportMetrics,
        histogramBucket: Bucket?,
    ): Output {
        val collectionRows = metrics.collectionTypeCounts().toList()
        val histogramRows =
            histogramBucket?.let { bucket ->
                Histogram(bucket).getHistogram(metrics.entriesForHistogram())
            }
        return Output(
            collectionCsv = CollectionTypeReport.csv(collectionRows),
            histogramCsv = histogramRows?.let { CsvReportFormatter.format("Bucket", "Occurrences", it) },
            consoleTables =
                buildList {
                    add(CollectionTypeReport.consoleTable(logName.getFileName(), collectionRows))
                    if (histogramBucket != null && histogramRows != null) {
                        add(renderHistogramTable(logName, histogramBucket, histogramRows))
                    }
                },
        )
    }

    private fun renderHistogramTable(
        logName: String,
        bucket: Bucket,
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
                cell("GC Histogram: ${logName.getFileName()}") {
                    columnSpan = 2
                }
            }
            row {
                cell("Type: ${bucket.name}") {
                    columnSpan = 2
                }
            }
            row("Bucket", "Occurrences")
            rows.forEach { (histogramBucket, count) ->
                row {
                    cell(histogramBucket)
                    cell(count) {
                        alignment = TextAlignment.MiddleRight
                    }
                }
            }
        }.toString()
}
