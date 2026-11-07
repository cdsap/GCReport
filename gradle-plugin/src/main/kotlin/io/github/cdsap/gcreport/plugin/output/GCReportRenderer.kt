package io.github.cdsap.gcreport.plugin.output

import com.jakewharton.picnic.TextAlignment
import com.jakewharton.picnic.table
import io.github.cdsap.gcreport.plugin.extensions.getFileName
import io.github.cdsap.gcreport.plugin.histogram.Histogram
import io.github.cdsap.gcreport.plugin.model.Bucket
import io.github.cdsap.gcreport.plugin.model.GCReportMetrics

internal data class GCReportRenderingOptions(
    val histogramEnabled: Boolean,
    val histogramBucket: Bucket,
)

internal data class GCReportContent(
    val collectionCsv: String,
    val histogramCsv: String?,
    val consoleReports: List<String>,
)

internal class GCReportRenderer(
    private val options: GCReportRenderingOptions,
) {
    fun render(
        logName: String,
        metrics: GCReportMetrics,
    ): GCReportContent =
        GCReportContent(
            collectionCsv = renderCollectionCsv(metrics),
            histogramCsv = if (options.histogramEnabled) renderHistogramCsv(metrics) else null,
            consoleReports =
                buildList {
                    add(renderCollectionTable(logName, metrics))
                    if (options.histogramEnabled) {
                        add(renderHistogramTable(logName, metrics))
                    }
                },
        )

    private fun renderCollectionCsv(metrics: GCReportMetrics): String =
        buildString {
            appendLine("Collection type,Occurrences")
            metrics.collectionTypeCounts().forEach { (description, count) ->
                appendLine("$description,$count")
            }
        }

    private fun renderHistogramCsv(metrics: GCReportMetrics): String =
        buildString {
            appendLine("Bucket,Occurrences")
            Histogram(options.histogramBucket).getHistogram(metrics.entriesForHistogram()).forEach { (bucket, count) ->
                appendLine("$bucket,$count")
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
                cell("Type: ${options.histogramBucket.name}") {
                    columnSpan = 2
                }
            }
            row("Bucket", "Occurrences")
            Histogram(options.histogramBucket).getHistogram(metrics.entriesForHistogram()).forEach { (bucket, count) ->
                row {
                    cell(bucket)
                    cell(count) {
                        alignment = TextAlignment.MiddleRight
                    }
                }
            }
        }.toString()
}
