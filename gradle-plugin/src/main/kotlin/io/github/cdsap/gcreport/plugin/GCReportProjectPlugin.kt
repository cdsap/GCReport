package io.github.cdsap.gcreport.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.build.event.BuildEventsListenerRegistry
import javax.inject.Inject

/**
 * Project-only alias (`io.github.cdsap.gcreport.project`) with the same project wiring as
 * `io.github.cdsap.gcreport` applied from a build script.
 */
abstract class GCReportProjectPlugin
    @Inject
    constructor(
        private val registry: BuildEventsListenerRegistry,
    ) : Plugin<Project> {
        override fun apply(target: Project) {
            GCReportPluginSupport.applyToProject(target, registry)
        }
    }
