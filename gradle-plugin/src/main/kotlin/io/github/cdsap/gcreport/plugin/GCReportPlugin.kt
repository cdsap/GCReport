package io.github.cdsap.gcreport.plugin

import com.gradle.develocity.agent.gradle.DevelocityConfiguration
import io.github.cdsap.gcreport.plugin.report.DevelocityReport
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
        val develocityConfiguration =
            target.gradle.rootProject.extensions.findByType(DevelocityConfiguration::class.java)
        target.gradle.rootProject {
            val extension = target.extensions.getByName("gcReport") as GCReportExtension
            if (develocityConfiguration != null) {
                createService(target, extension, extension.enableConsoleLog)
                DevelocityReport(develocityConfiguration, extension).report()
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
