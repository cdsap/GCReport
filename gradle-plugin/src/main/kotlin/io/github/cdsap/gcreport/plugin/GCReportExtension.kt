package io.github.cdsap.gcreport.plugin

import io.github.cdsap.gcreport.plugin.model.Bucket
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

abstract class GCReportExtension {
    abstract val logs: ListProperty<String>
    abstract val histogramEnabled: Property<Boolean>
    abstract val histogramBucket: Property<Bucket>
    abstract val enableConsoleLog: Property<Boolean>
}
