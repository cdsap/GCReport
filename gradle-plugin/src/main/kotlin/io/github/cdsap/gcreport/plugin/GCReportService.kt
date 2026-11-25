package io.github.cdsap.gcreport.plugin

import io.github.cdsap.gbos.core.GbosObservation
import io.github.cdsap.gcreport.plugin.model.Bucket
import io.github.cdsap.gcreport.plugin.model.GCReportMetrics
import io.github.cdsap.gcreport.plugin.model.ParsedGCLog
import io.github.cdsap.gcreport.plugin.output.GbosDevelocityProjection
import io.github.cdsap.gcreport.plugin.output.GbosOutputReport
import io.github.cdsap.gcreport.plugin.output.LocalGcReport
import io.github.cdsap.gcreport.plugin.parser.GCLogLoader
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.logging.Logging
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.tooling.events.FinishEvent
import org.gradle.tooling.events.OperationCompletionListener
import java.io.File

abstract class GCReportService : BuildService<GCReportService.Params>, AutoCloseable, OperationCompletionListener {
    private val logger = Logging.getLogger(GCReportService::class.java)

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
        val histogramBucket = parameters.histogramBucket.get().takeIf { parameters.histogramEnabled.get() }
        val reportEnabled = parameters.enabledReport.get()
        val observations = mutableListOf<GbosObservation>()

        for (log in GCLogLoader().load(parameters.logs.get())) {
            if (log.entries.isEmpty()) continue
            observations += reportLog(log, buildOutput, reportEnabled, histogramBucket)
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

    private fun reportLog(
        log: ParsedGCLog,
        buildOutput: File,
        reportEnabled: Boolean,
        histogramBucket: Bucket?,
    ): List<GbosObservation> {
        if (reportEnabled) {
            LocalGcReport(logger::lifecycle).report(
                GCReportMetrics.from(log.entries),
                log.file.path,
                buildOutput,
                histogramBucket,
            )
        }
        return GbosDevelocityProjection.observations(log.file.path, log.entries)
    }
}
