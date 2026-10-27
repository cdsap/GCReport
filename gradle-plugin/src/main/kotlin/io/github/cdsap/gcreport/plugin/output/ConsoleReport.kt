package io.github.cdsap.gcreport.plugin.output

import com.jakewharton.picnic.TextAlignment
import com.jakewharton.picnic.table
import io.github.cdsap.gcreport.plugin.extensions.getFileName
import io.github.cdsap.gcreport.plugin.histogram.Histogram
import io.github.cdsap.gcreport.plugin.model.Bucket
import io.github.cdsap.gcreport.plugin.model.GCReportMetrics

internal class ConsoleReport(
    private val histogramEnabled: Boolean,
    private val histogramBucket: Bucket,
) {
    fun render(
        logName: String,
        metrics: GCReportMetrics,
    ): List<String> {
        return buildList {
            add(renderCollectionTable(logName, metrics))
            if (histogramEnabled) {
                add(renderHistogramTable(logName, metrics))
            }
        }
    }

    private fun renderCollectionTable(
        logName: String,
        metrics: GCReportMetrics,
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
            metrics.collectionTypeCounts().forEach { (description, count) ->
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
        metrics: GCReportMetrics,
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
                cell("Type: ${histogramBucket.name}") {
                    columnSpan = 2
                }
            }
            row("Bucket", "Occurrences")
            Histogram(histogramBucket).getHistogram(metrics.entriesForHistogram()).forEach { (bucket, count) ->
                row {
                    cell(bucket)
                    cell(count) {
                        alignment = TextAlignment.MiddleRight
                    }
                }
            }
        }.toString()
}
