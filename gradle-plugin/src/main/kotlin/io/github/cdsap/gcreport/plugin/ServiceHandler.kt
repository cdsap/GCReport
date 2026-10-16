package io.github.cdsap.gcreport.plugin

import org.gradle.api.file.Directory
import org.gradle.api.invocation.Gradle
import org.gradle.api.provider.Provider

class ServiceHandler(
    private val gradle: Gradle,
    private val buildOutput: Provider<Directory>,
    private val extension: GCReportExtension,
    private val enabledReport: Provider<Boolean>? = null,
) {
    fun createService(serviceName: String = "gcReportService"): Provider<GCReportService> {
        val service: Provider<GCReportService> =
            gradle.sharedServices.registerIfAbsent(
                serviceName,
                GCReportService::class.java,
            ) { serviceSpec ->
                serviceSpec.parameters.logs.set(extension.logs)
                serviceSpec.parameters.histogramEnabled.set(extension.histogramEnabled)
                serviceSpec.parameters.histogramBucket.set(extension.histogramBucket)
                serviceSpec.parameters.buildOutput.set(buildOutput)
                // Both local sinks follow the single gbosEnabled opt-in.
                serviceSpec.parameters.gbosJsonEnabled.set(extension.gbosEnabled)
                serviceSpec.parameters.gbosNdjsonEnabled.set(extension.gbosEnabled)
                if (enabledReport == null) {
                    serviceSpec.parameters.enabledReport.convention(true)
                } else {
                    serviceSpec.parameters.enabledReport.convention(enabledReport)
                }
            }
        return service
    }
}
