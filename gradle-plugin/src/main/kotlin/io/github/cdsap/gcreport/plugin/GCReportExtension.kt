package io.github.cdsap.gcreport.plugin

import io.github.cdsap.gcreport.plugin.model.Bucket
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

abstract class GCReportExtension {
    abstract val logs: ListProperty<String>

    /**
     * Enables all GBOS outputs this plugin supports: local JSON and NDJSON sinks, and the
     * Develocity GBOS projection when Develocity is present. Defaults to `false`; also
     * controllable with the Gradle property `gcReport.gbos.enabled`.
     */
    abstract val gbosEnabled: Property<Boolean>
    abstract val legacyDevelocityValuesEnabled: Property<Boolean>
    abstract val histogramEnabled: Property<Boolean>
    abstract val histogramBucket: Property<Bucket>
    abstract val enableConsoleLog: Property<Boolean>
}
