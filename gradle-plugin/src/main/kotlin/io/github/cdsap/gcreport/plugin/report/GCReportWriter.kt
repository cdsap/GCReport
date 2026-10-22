package io.github.cdsap.gcreport.plugin.report

import io.github.cdsap.gbos.core.GbosObservation
import io.github.cdsap.gcreport.plugin.model.Bucket
import io.github.cdsap.gcreport.plugin.model.GCReportMetrics
import io.github.cdsap.gcreport.plugin.output.GCReportFileWriter
import io.github.cdsap.gcreport.plugin.output.GbosDevelocityProjection
import io.github.cdsap.gcreport.plugin.output.GbosOutputReport
import io.github.cdsap.gcreport.plugin.parser.ParsedGCLog
import java.io.File

internal data class GCReportOutputOptions(
    val reportEnabled: Boolean,
    val histogramEnabled: Boolean,
    val histogramBucket: Bucket,
    val gbosJsonEnabled: Boolean,
    val gbosNdjsonEnabled: Boolean,
)

internal class GCReportWriter(
    private val buildOutput: File,
    private val options: GCReportOutputOptions,
) {
    private val fileWriter =
        GCReportFileWriter(
            buildOutput,
            options.histogramEnabled,
            options.histogramBucket,
        )

    fun write(logs: List<ParsedGCLog>) {
        val observations = mutableListOf<GbosObservation>()
        logs.forEach { log ->
            if (log.entries.isNotEmpty()) {
                if (options.reportEnabled) {
                    fileWriter.write(log.file, GCReportMetrics.from(log.entries))
                }
                observations += GbosDevelocityProjection.observations(log.file.path, log.entries)
            }
        }
        GbosOutputReport(
            File(buildOutput, "gbos.json"),
            File(buildOutput, "gbos.ndjson"),
        ).write(observations, options.gbosJsonEnabled, options.gbosNdjsonEnabled)
    }
}
