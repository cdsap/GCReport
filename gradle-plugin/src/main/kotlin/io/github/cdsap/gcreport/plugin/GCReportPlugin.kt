package io.github.cdsap.gcreport.plugin

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.initialization.Settings
import org.gradle.build.event.BuildEventsListenerRegistry
import javax.inject.Inject

/**
 * Configures GC report collection once for the whole build.
 * Apply with id `io.github.cdsap.gcreport` from `settings.gradle(.kts)` (preferred) or from a
 * project build script.
 */
abstract class GCReportPlugin
    @Inject
    constructor(
        private val registry: BuildEventsListenerRegistry,
    ) : Plugin<Any> {
        override fun apply(target: Any) {
            when (target) {
                is Settings -> GCReportPluginSupport.applyToSettings(target, registry)
                is Project -> GCReportPluginSupport.applyToProject(target, registry)
                else -> throw GradleException(
                    "io.github.cdsap.gcreport can only be applied to Settings or Project, not ${target.javaClass.name}",
                )
            }
        }
    }
