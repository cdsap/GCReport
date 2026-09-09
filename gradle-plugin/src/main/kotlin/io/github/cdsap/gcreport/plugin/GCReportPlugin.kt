package io.github.cdsap.gcreport.plugin

import com.gradle.develocity.agent.gradle.DevelocityConfiguration
import io.github.cdsap.gcreport.plugin.report.DevelocityReport
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create

class GCReportPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.extensions.create<GCReportExtension>("gcReport")
        val develocityConfiguration =
            target.gradle.rootProject.extensions.findByType(DevelocityConfiguration::class.java)
        target.gradle.rootProject {
            val extension = target.extensions.getByName("gcReport") as GCReportExtension
            if (develocityConfiguration != null) {
                ServiceHandler(target, extension, extension.enableConsoleLog).createService()
                DevelocityReport(develocityConfiguration, extension).report()
            } else {
                ServiceHandler(target, extension).createService()
            }
        }
    }
}
