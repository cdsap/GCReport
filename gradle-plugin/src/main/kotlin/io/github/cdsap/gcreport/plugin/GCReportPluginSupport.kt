package io.github.cdsap.gcreport.plugin

import io.github.cdsap.gcreport.plugin.model.Bucket
import io.github.cdsap.gcreport.plugin.report.DevelocitySupport
import org.gradle.api.Project
import org.gradle.api.invocation.Gradle
import org.gradle.api.plugins.ExtensionAware
import org.gradle.build.event.BuildEventsListenerRegistry

/**
 * Shared registration for settings and project (compatibility) plugin entry points.
 * Ensures build-service registration and Develocity hooks run once per build.
 */
internal object GCReportPluginSupport {
    private const val SERVICE_NAME = "gcReportService"

    fun createExtension(host: ExtensionAware): GCReportExtension =
        host.extensions.create("gcReport", GCReportExtension::class.java).apply {
            histogramEnabled.convention(false)
            histogramBucket.convention(Bucket.FreedmanDiaconis)
            enableConsoleLog.convention(false)
        }

    fun configure(
        gradle: Gradle,
        registry: BuildEventsListenerRegistry,
        extension: GCReportExtension,
        findDevelocity: (rootProject: Project) -> Any?,
    ) {
        gradle.rootProject { rootProject ->
            if (gradle.sharedServices.registrations.findByName(SERVICE_NAME) != null) {
                return@rootProject
            }
            val develocityExtension = findDevelocity(rootProject)
            val buildOutput = rootProject.layout.buildDirectory.dir("reports/gcreport")
            val serviceHandler =
                if (develocityExtension != null) {
                    ServiceHandler(gradle, buildOutput, extension, extension.enableConsoleLog)
                } else {
                    ServiceHandler(gradle, buildOutput, extension)
                }
            registry.onTaskCompletion(serviceHandler.createService(SERVICE_NAME))
            if (develocityExtension != null) {
                DevelocitySupport.register(develocityExtension, extension)
            }
        }
    }
}
