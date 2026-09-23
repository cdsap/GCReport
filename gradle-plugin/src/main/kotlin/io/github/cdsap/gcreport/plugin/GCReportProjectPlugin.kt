package io.github.cdsap.gcreport.plugin

import io.github.cdsap.gcreport.plugin.report.DevelocityPresence
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.build.event.BuildEventsListenerRegistry
import javax.inject.Inject

/**
 * Compatibility project plugin for consumers that still apply GCReport from a build script.
 * Prefer the settings plugin (`io.github.cdsap.gcreport`) for new and multi-project builds.
 */
abstract class GCReportProjectPlugin
    @Inject
    constructor(
        private val registry: BuildEventsListenerRegistry,
    ) : Plugin<Project> {
        override fun apply(target: Project) {
            val extension = GCReportPluginSupport.createExtension(target)
            GCReportPluginSupport.configure(target.gradle, registry, extension) { rootProject ->
                DevelocityPresence.findExtension(rootProject)
            }
        }
    }
