package io.github.cdsap.gcreport.plugin

import io.github.cdsap.gcreport.plugin.model.Bucket
import io.github.cdsap.gcreport.plugin.parser.GCLogLoader
import io.github.cdsap.gcreport.plugin.report.GCReportOutputOptions
import io.github.cdsap.gcreport.plugin.report.GCReportWriter
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.tooling.events.FinishEvent
import org.gradle.tooling.events.OperationCompletionListener

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
        GCReportWriter(
            parameters.buildOutput.get().asFile,
            GCReportOutputOptions(
                reportEnabled = parameters.enabledReport.get(),
                histogramEnabled = parameters.histogramEnabled.get(),
                histogramBucket = parameters.histogramBucket.get(),
                gbosJsonEnabled = parameters.gbosJsonEnabled.get(),
                gbosNdjsonEnabled = parameters.gbosNdjsonEnabled.get(),
            ),
        ).write(GCLogLoader().load(parameters.logs.get()))
    }
}
