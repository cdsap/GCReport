package io.github.cdsap.gcreport.plugin

import io.github.cdsap.gbos.core.GbosObservation
import io.github.cdsap.gcreport.plugin.model.Bucket
import io.github.cdsap.gcreport.plugin.model.GCEntry
import io.github.cdsap.gcreport.plugin.output.ConsoleReportWriter
import io.github.cdsap.gcreport.plugin.output.GbosOutputReport
import io.github.cdsap.gcreport.plugin.parser.GCLogLoader
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.tooling.events.FinishEvent
import org.gradle.tooling.events.OperationCompletionListener
import java.io.File

abstract class GCReportService : BuildService<GCReportService.Params>, AutoCloseable, OperationCompletionListener {
    interface Params : BuildServiceParameters {
        val logs: ListProperty<String>
        val histogramEnabled: Property<Boolean>
        val histogramBucket: Property<Bucket>
        val buildOutput: DirectoryProperty
        val enabledReport: Property<Boolean>
        val gbosJsonEnabled: Property<Boolean>
        val gbosNdjsonEnabled: Property<Boolean>
    }

    override fun onFinish(event: FinishEvent?) {}

    override fun close() {
        val outputJson = File(parameters.buildOutput.get().asFile, "gbos.json")
        val outputNdjson = File(parameters.buildOutput.get().asFile, "gbos.ndjson")
        if (
            parameters.enabledReport.get() ||
            parameters.gbosJsonEnabled.get() ||
            parameters.gbosNdjsonEnabled.get()
        ) {
            val writer =
                if (parameters.enabledReport.get()) {
                    ConsoleReportWriter(
                        parameters.buildOutput.get().asFile,
                        parameters.histogramEnabled.get(),
                        parameters.histogramBucket.get(),
                    )
                } else {
                    null
                }
            val observations = mutableListOf<GbosObservation>()
            for (log in GCLogLoader().load(parameters.logs.get())) {
                observations += processLog(log.file, log.entries, writer)
            }
            GbosOutputReport(outputJson, outputNdjson).write(
                observations,
                parameters.gbosJsonEnabled.get(),
                parameters.gbosNdjsonEnabled.get(),
            )
        } else {
            GbosOutputReport(outputJson, outputNdjson).write(emptyList(), false, false)
        }
    }

    private fun processLog(
        logFile: File,
        gcEntries: List<GCEntry>,
        writer: ConsoleReportWriter?,
    ): List<GbosObservation> {
        if (gcEntries.isEmpty()) {
            return emptyList()
        }
        writer?.write(logFile, gcEntries)
        return io.github.cdsap.gcreport.plugin.output.GbosDevelocityProjection.observations(logFile.path, gcEntries)
    }
}
