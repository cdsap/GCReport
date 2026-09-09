package io.github.cdsap.gcreport.plugin

import com.gradle.develocity.agent.gradle.DevelocityConfiguration
import io.github.cdsap.gcreport.plugin.model.Bucket
import io.github.cdsap.gcreport.plugin.report.DevelocityReport
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create

class GCReportPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        val extension =
            target.extensions.create<GCReportExtension>("gcReport").apply {
                histogramEnabled.convention(false)
                histogramBucket.convention(Bucket.FreedmanDiaconis)
                enableConsoleLog.convention(false)
            }
        val develocityConfiguration =
            target.gradle.rootProject.extensions.findByType(DevelocityConfiguration::class.java)
        target.gradle.rootProject {
            if (develocityConfiguration != null) {
                ServiceHandler(target, extension, extension.enableConsoleLog).createService()
                DevelocityReport(develocityConfiguration, extension).report()
            } else {
                ServiceHandler(target, extension).createService()
            }
        }
    }
}
