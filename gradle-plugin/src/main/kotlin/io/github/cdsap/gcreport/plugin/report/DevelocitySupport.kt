package io.github.cdsap.gcreport.plugin.report

import com.gradle.develocity.agent.gradle.DevelocityConfiguration
import io.github.cdsap.gcreport.plugin.GCReportExtension

/** Bridges to Develocity types that are only available when the Develocity plugin is applied. */
internal object DevelocitySupport {
    fun register(
        develocityExtension: Any,
        extension: GCReportExtension,
    ) {
        DevelocityReport(develocityExtension as DevelocityConfiguration, extension).report()
    }
}
