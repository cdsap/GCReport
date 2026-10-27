package io.github.cdsap.gcreport.plugin

import io.github.cdsap.gbos.core.GbosObservation
import io.github.cdsap.gcreport.plugin.model.Bucket
import io.github.cdsap.gcreport.plugin.model.GCReportMetrics
import io.github.cdsap.gcreport.plugin.output.GCReportFileWriter
import io.github.cdsap.gcreport.plugin.output.GbosDevelocityProjection
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
        val buildOutput = parameters.buildOutput.get().asFile
        val reportWriter =
            GCReportFileWriter(
                buildOutput,
                parameters.histogramEnabled.get(),
                parameters.histogramBucket.get(),
            )
        val reportEnabled = parameters.enabledReport.get()
        val observations = mutableListOf<GbosObservation>()

        GCLogLoader().load(parameters.logs.get()).forEach { log ->
            if (log.entries.isNotEmpty()) {
                if (reportEnabled) {
                    reportWriter.write(log.file, GCReportMetrics.from(log.entries))
                }
                observations += GbosDevelocityProjection.observations(log.file.path, log.entries)
            }
        }

        GbosOutputReport(
            File(buildOutput, "gbos.json"),
            File(buildOutput, "gbos.ndjson"),
        ).write(
            observations,
            parameters.gbosJsonEnabled.get(),
            parameters.gbosNdjsonEnabled.get(),
        )
    }
}
