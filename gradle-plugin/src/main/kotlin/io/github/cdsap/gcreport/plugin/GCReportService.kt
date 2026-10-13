package io.github.cdsap.gcreport.plugin

import io.github.cdsap.gcreport.plugin.model.Bucket
import io.github.cdsap.gcreport.plugin.parser.GCLogReader
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
    }

    override fun onFinish(event: FinishEvent?) {}

    override fun close() {
        if (parameters.enabledReport.get()) {
            val writer =
                GCReportWriter(
                    parameters.buildOutput.get().asFile,
                    parameters.histogramEnabled.get(),
                    parameters.histogramBucket.get(),
                )
            for (log in parameters.logs.get().filter { File(it).exists() }) {
                processLog(File(log), writer)
            }
        }
    }

    private fun processLog(
        logFile: File,
        writer: GCReportWriter,
    ) {
        val gcEntries = GCLogReader(logFile).parse()
        if (gcEntries.isEmpty()) {
            return
        }
        writer.write(logFile, gcEntries)
    }
}
