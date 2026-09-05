package io.github.cdsap.gcreport.plugin

import com.jakewharton.picnic.TextAlignment
import com.jakewharton.picnic.table
import io.github.cdsap.gcreport.plugin.extensions.getFileName
import io.github.cdsap.gcreport.plugin.extensions.getFileNameCsvLog
import io.github.cdsap.gcreport.plugin.histogram.Histogram
import io.github.cdsap.gcreport.plugin.model.Bucket
import io.github.cdsap.gcreport.plugin.model.GCCollectionMetrics
import io.github.cdsap.gcreport.plugin.parser.GCLogReader
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.tooling.events.FinishEvent
import org.gradle.tooling.events.OperationCompletionListener
import java.io.File

abstract class GCReportService : BuildService<GCReportService.Params>, AutoCloseable, OperationCompletionListener {
    interface Params : BuildServiceParameters {
        var logs: Provider<List<String>>
        var histogramEnabled: Provider<Boolean>
        var histogramBucket: Provider<Bucket>
        var buildOutput: Provider<Directory>
        var enabledReport: Provider<Boolean>
    }

    override fun onFinish(event: FinishEvent?) {}

    override fun close() {
        if (parameters.enabledReport.get()) {
            parameters.logs.get().filter { File(it).exists() }.forEach {
                val gcEntries = GCLogReader(File(it)).parse()
                if (gcEntries.isEmpty()) {
                    return
                }

                val file = File("${parameters.buildOutput.get()}/${it.getFileNameCsvLog()}")
                file.parentFile.mkdirs()

                val headers = "Collection type,Occurrences\n"
                var content = ""
                val collectionMetrics = GCCollectionMetrics(gcEntries)

                println(
                    table {
                        cellStyle {
                            alignment = TextAlignment.MiddleLeft
                            paddingLeft = 1
                            paddingRight = 1
                            border = true
                        }
                        row {
                            cell("GC Log: ${it.getFileName()}") {
                                columnSpan = 2
                            }
                        }
                        row("Collection type", "Occurrences")
                        collectionMetrics.countsByDescription().forEach { (description, count) ->
                            content += "$description,$count\n"
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

                if (parameters.histogramEnabled.get()) {
                    val fileHistogram = File("${parameters.buildOutput.get()}/histogram_${it.getFileNameCsvLog()}")
                    fileHistogram.parentFile.mkdirs()
                    val headersHistogram = "Bucket,Occurrences\n"
                    var contentHistogram = ""

                    println(
                        table {
                            cellStyle {
                                alignment = TextAlignment.MiddleLeft
                                paddingLeft = 1
                                paddingRight = 1
                                border = true
                            }
                            row {
                                cell("GC Histogram: ${it.getFileName()}") {
                                    columnSpan = 2
                                }
                            }
                            row {
                                cell("Type: ${parameters.histogramBucket.get().name}") {
                                    columnSpan = 2
                                }
                            }
                            row("Bucket", "Occurrences")
                            val histogram = Histogram(parameters.histogramBucket.get())
                            val entries = histogram.getHistogram(collectionMetrics.entriesForHistogram())
                            entries.forEach {
                                contentHistogram += "${it.first},${it.second}\n"
                                row {
                                    cell(it.first)
                                    cell(it.second) {
                                        alignment = TextAlignment.MiddleRight
                                    }
                                }
                            }
                        },
                    )
                    fileHistogram.writeText(headersHistogram + contentHistogram)
                }
            }
        }
    }
}
