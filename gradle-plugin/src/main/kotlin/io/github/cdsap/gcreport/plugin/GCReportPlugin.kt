package io.github.cdsap.gcreport.plugin

import io.github.cdsap.gcreport.plugin.report.DevelocityPresence
import io.github.cdsap.gcreport.plugin.report.DevelocitySupport
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.build.event.BuildEventsListenerRegistry
import org.gradle.internal.extensions.core.serviceOf
import org.gradle.kotlin.dsl.create

class GCReportPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.extensions.create<GCReportExtension>("gcReport")
        // Resolve Develocity without hard-referencing its types (compileOnly; may be absent).
        val develocityExtension = DevelocityPresence.findExtension(target.gradle.rootProject)
        target.gradle.rootProject {
            val extension = target.extensions.getByName("gcReport") as GCReportExtension
            if (develocityExtension != null) {
                createService(target, extension, extension.enableConsoleLog)
                DevelocitySupport.register(develocityExtension, extension)
            } else {
                createService(target, extension)
            }
        }
    }

    private fun createService(
        project: Project,
        extension: GCReportExtension,
        enableLog: Property<Boolean>? = null,
    ) {
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
