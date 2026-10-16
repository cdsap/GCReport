package io.github.cdsap.gcreport.plugin

import io.github.cdsap.gcreport.plugin.model.Bucket
import io.github.cdsap.gcreport.plugin.report.DevelocityPresence
import io.github.cdsap.gcreport.plugin.report.DevelocitySupport
import org.gradle.api.Project
import org.gradle.api.initialization.Settings
import org.gradle.api.invocation.Gradle
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.provider.ProviderFactory
import org.gradle.build.event.BuildEventsListenerRegistry

/**
 * Shared registration for settings and project plugin entry points.
 * Ensures build-service registration and Develocity hooks run once per build.
 */
internal object GCReportPluginSupport {
    private const val SERVICE_NAME = "gcReportService"

    fun applyToSettings(
        settings: Settings,
        registry: BuildEventsListenerRegistry,
    ) {
        val extension = createExtension(settings)
        configure(settings.gradle, registry, extension) { rootProject ->
            DevelocityPresence.findExtension(rootProject)
                ?: DevelocityPresence.findExtension(settings)
        }
    }

    fun applyToProject(
        project: Project,
        registry: BuildEventsListenerRegistry,
    ) {
        val extension = createExtension(project)
        configure(project.gradle, registry, extension) { rootProject ->
            DevelocityPresence.findExtension(rootProject)
        }
    }

    fun createExtension(settings: Settings): GCReportExtension = createExtension(settings, settings.providers)

    fun createExtension(project: Project): GCReportExtension = createExtension(project, project.providers)

    // Idempotent so the legacy id and the `.project` alias (separate plugin classes) can both be
    // applied to the same project without a duplicate-extension failure.
    fun createExtension(
        host: ExtensionAware,
        providers: ProviderFactory,
    ): GCReportExtension =
        host.extensions.findByType(GCReportExtension::class.java)
            ?: host.extensions.create("gcReport", GCReportExtension::class.java).apply {
                histogramEnabled.convention(false)
                histogramBucket.convention(Bucket.FreedmanDiaconis)
                enableConsoleLog.convention(false)
                GbosOptIn.configureConventions(this, providers)
                legacyDevelocityValuesEnabled.convention(true)
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
            val enabledReport = develocityExtension?.let { extension.enableConsoleLog }
            val serviceHandler = ServiceHandler(gradle, buildOutput, extension, enabledReport)
            registry.onTaskCompletion(serviceHandler.createService(SERVICE_NAME))
            if (develocityExtension != null) {
                DevelocitySupport.register(develocityExtension, extension)
            }
        }
    }
}
