package io.github.cdsap.gcreport.plugin.report

import io.github.cdsap.gcreport.plugin.model.Bucket
import io.github.cdsap.gcreport.plugin.model.GCEntry
import io.github.cdsap.gcreport.plugin.output.GCReportFileWriter
import java.io.File

internal class GCLogReportWriter(
    buildOutput: File,
    histogramEnabled: Boolean,
    histogramBucket: Bucket,
) {
    private val fileWriter = GCReportFileWriter(buildOutput, histogramEnabled, histogramBucket)

    fun write(
        logFile: File,
        entries: List<GCEntry>,
    ) {
        fileWriter.write(logFile, entries)
    }
}
