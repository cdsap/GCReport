package io.github.cdsap.gcreport.plugin.output

import com.jakewharton.picnic.TextAlignment
import com.jakewharton.picnic.table
import io.github.cdsap.gcreport.plugin.extensions.getFileName
import io.github.cdsap.gcreport.plugin.extensions.getFileNameCsvLog
import io.github.cdsap.gcreport.plugin.histogram.Histogram
import io.github.cdsap.gcreport.plugin.model.Bucket
import io.github.cdsap.gcreport.plugin.model.GCEntry
import io.github.cdsap.gcreport.plugin.model.GCReportMetrics
import org.gradle.api.logging.Logging
import java.io.File

internal class ConsoleReportWriter(
    private val buildOutput: File,
    private val histogramEnabled: Boolean,
    private val histogramBucket: Bucket,
) {
    private val logger = Logging.getLogger(ConsoleReportWriter::class.java)

    fun write(
        logFile: File,
        entries: List<GCEntry>,
    ) {
        val metrics = GCReportMetrics.from(entries)
        val file = File(buildOutput, logFile.path.getFileNameCsvLog())
        file.parentFile.mkdirs()

        val headers = "Collection type,Occurrences\n"
        val content = StringBuilder()
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
                    content.append("$description,$count\n")
                    row {
                        cell(description)
                        cell(count) {
                            alignment = TextAlignment.MiddleRight
                        }
                    }
                }
            }.toString(),
        )
        file.writeText(headers + content)

        if (histogramEnabled) {
            val fileHistogram = File(buildOutput, "histogram_${logFile.path.getFileNameCsvLog()}")
            fileHistogram.parentFile.mkdirs()
            val headersHistogram = "Bucket,Occurrences\n"
            val contentHistogram = StringBuilder()

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
                    val histogram = Histogram(histogramBucket)
                    val histogramEntries = histogram.getHistogram(metrics.entriesForHistogram())
                    histogramEntries.forEach { (bucket, count) ->
                        contentHistogram.append("$bucket,$count\n")
                        row {
                            cell(bucket)
                            cell(count) {
                                alignment = TextAlignment.MiddleRight
                            }
                        }
                    }
                }.toString(),
            )
            fileHistogram.writeText(headersHistogram + contentHistogram)
        }
    }
}
