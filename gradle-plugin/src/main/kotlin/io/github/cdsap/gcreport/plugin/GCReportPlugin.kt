package io.github.cdsap.gcreport.plugin

import io.github.cdsap.gcreport.plugin.model.Bucket
import io.github.cdsap.gcreport.plugin.report.DevelocityPresence
import io.github.cdsap.gcreport.plugin.report.DevelocitySupport
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
        // Resolve Develocity without hard-referencing its types (compileOnly; may be absent).
        val develocityExtension = DevelocityPresence.findExtension(target.gradle.rootProject)
        target.gradle.rootProject {
            if (develocityExtension != null) {
                ServiceHandler(target, extension, extension.enableConsoleLog).createService()
                DevelocitySupport.register(develocityExtension, extension)
            } else {
                ServiceHandler(target, extension).createService()
            }
        }
    }
}
