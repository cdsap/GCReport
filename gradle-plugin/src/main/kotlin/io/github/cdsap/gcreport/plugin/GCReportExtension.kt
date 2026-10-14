package io.github.cdsap.gcreport.plugin

import io.github.cdsap.gcreport.plugin.model.Bucket
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

abstract class GCReportExtension {
    abstract val logs: ListProperty<String>

    /** @deprecated Use [gbosJsonEnabled] and [gbosNdjsonEnabled] instead. */
    @Deprecated("Use gbosJsonEnabled and gbosNdjsonEnabled instead")
    abstract val gbosEnabled: Property<Boolean>
    abstract val gbosJsonEnabled: Property<Boolean>
    abstract val gbosNdjsonEnabled: Property<Boolean>
    abstract val histogramEnabled: Property<Boolean>
    abstract val histogramBucket: Property<Bucket>
    abstract val enableConsoleLog: Property<Boolean>
}
