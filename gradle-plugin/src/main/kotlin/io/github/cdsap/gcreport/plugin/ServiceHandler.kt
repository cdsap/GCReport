package io.github.cdsap.gcreport.plugin

import org.gradle.api.file.Directory
import org.gradle.api.invocation.Gradle
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider

class ServiceHandler(
    private val gradle: Gradle,
    private val buildOutput: Provider<Directory>,
    private val extension: GCReportExtension,
    private val enableLog: Property<Boolean>? = null,
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
                if (enableLog == null) {
                    serviceSpec.parameters.enabledReport.convention(true)
                } else {
                    serviceSpec.parameters.enabledReport.set(enableLog)
                }
            }
        return service
    }
}
