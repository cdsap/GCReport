package io.github.cdsap.gcreport.plugin

import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.build.event.BuildEventsListenerRegistry
import org.gradle.internal.extensions.core.serviceOf

class ServiceHandler(
    private val project: Project,
    private val extension: GCReportExtension,
    private val enableLog: Property<Boolean>? = null,
) {
    fun createService() {
        val service: Provider<GCReportService> =
            project.gradle.sharedServices.registerIfAbsent(
                "gcReportService",
                GCReportService::class.java,
            ) {
                val buildOutput = project.layout.buildDirectory.dir("reports/gcreport")
                parameters.logs = extension.logs
                parameters.histogramEnabled = extension.histogramEnabled
                parameters.histogramBucket = extension.histogramBucket
                parameters.buildOutput = buildOutput
                parameters.enabledReport = if (enableLog == null) project.provider { true } else enableLog
            }
        project.serviceOf<BuildEventsListenerRegistry>().onTaskCompletion(service)
    }
}
