package io.github.cdsap.gcreport.plugin.output

import com.jakewharton.picnic.TextAlignment
import com.jakewharton.picnic.table
import io.github.cdsap.gcreport.plugin.extensions.getFileName
import io.github.cdsap.gcreport.plugin.histogram.Histogram
import io.github.cdsap.gcreport.plugin.model.Bucket
import io.github.cdsap.gcreport.plugin.model.GCReportMetrics
import org.gradle.api.logging.Logging
import java.io.File

internal class GCReportFileWriter(
    private val buildOutput: File,
    private val histogramEnabled: Boolean,
    private val histogramBucket: Bucket,
) {
    private val logger = Logging.getLogger(GCReportFileWriter::class.java)
    private val csvWriter = CsvReportWriter(buildOutput, histogramEnabled, histogramBucket)

    fun write(
        logFile: File,
        metrics: GCReportMetrics,
    ) {
        csvWriter.write(logFile, metrics)
        logger.lifecycle(
            table {
                cellStyle {
                    alignment = TextAlignment.MiddleLeft
                    paddingLeft = 1
                    paddingRight = 1
                    border = true
                }
                row {
                    cell("GC Log: ${logFile.path.getFileName()}") {
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
            }.toString(),
        )

        if (histogramEnabled) {
            logger.lifecycle(
                table {
                    cellStyle {
                        alignment = TextAlignment.MiddleLeft
                        paddingLeft = 1
                        paddingRight = 1
                        border = true
                    }
                    row {
                        cell("GC Histogram: ${logFile.path.getFileName()}") {
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
                }.toString(),
            )
        }
    }
}
