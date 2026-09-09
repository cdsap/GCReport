package io.github.cdsap.gcreport.plugin

import com.gradle.develocity.agent.gradle.DevelocityConfiguration
import io.github.cdsap.gcreport.plugin.report.DevelocityReport
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create

class GCReportPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        val extension = target.extensions.create<GCReportExtension>("gcReport")
        target.gradle.rootProject {
            val develocityConfiguration = extensions.findByType(DevelocityConfiguration::class.java)
            if (develocityConfiguration != null) {
                ServiceHandler(target, extension, extension.enableConsoleLog).createService()
                DevelocityReport(develocityConfiguration, extension).report()
            } else {
                ServiceHandler(target, extension).createService()
            }
        }
    }
}
