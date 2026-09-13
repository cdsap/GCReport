package io.github.cdsap.gcreport.plugin

import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider

class ServiceHandler(
    private val project: Project,
    private val extension: GCReportExtension,
    private val enableLog: Property<Boolean>? = null,
) {
    fun createService(): Provider<GCReportService> {
        val service: Provider<GCReportService> =
            project.gradle.sharedServices.registerIfAbsent(
                "gcReportService",
                GCReportService::class.java,
            ) {
                parameters.logs.set(extension.logs)
                parameters.histogramEnabled.set(extension.histogramEnabled)
                parameters.histogramBucket.set(extension.histogramBucket)
                parameters.buildOutput.set(project.layout.buildDirectory.dir("reports/gcreport"))
                if (enableLog == null) {
                    parameters.enabledReport.convention(true)
                } else {
                    parameters.enabledReport.set(enableLog)
                }
            }
        return service
    }
}
