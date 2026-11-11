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
    ): GCReportContent {
        val collectionRows = metrics.collectionTypeCounts().toList()
        val histogramRows =
            if (options.histogramEnabled) {
                Histogram(options.histogramBucket).getHistogram(metrics.entriesForHistogram())
            } else {
                null
            }
        return GCReportContent(
            collectionCsv = formatCsvReport("Collection type,Occurrences", collectionRows),
            histogramCsv = histogramRows?.let { formatCsvReport("Bucket,Occurrences", it) },
            consoleReports =
                buildList {
                    add(renderCollectionTable(logName, collectionRows))
                    if (histogramRows != null) {
                        add(renderHistogramTable(logName, histogramRows))
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
                cell("Type: ${options.histogramBucket.name}") {
                    columnSpan = 2
                }
            }
            row("Bucket", "Occurrences")
            rows.forEach { (bucket, count) ->
                row {
                    cell(bucket)
                    cell(count) {
                        alignment = TextAlignment.MiddleRight
                    }
                }
            }
        }.toString()
}
