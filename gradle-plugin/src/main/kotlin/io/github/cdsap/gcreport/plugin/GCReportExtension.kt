package io.github.cdsap.gcreport.plugin

import io.github.cdsap.gcreport.plugin.model.Bucket
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

abstract class GCReportExtension {
    abstract val logs: ListProperty<String>

    /** Enables the local GBOS JSON and NDJSON sinks. */
    abstract val gbosEnabled: Property<Boolean>

    /** Enables the GBOS Develocity projection when Develocity is present. */
    abstract val gbosDevelocityEnabled: Property<Boolean>

    /** Keeps the existing `gc-*` Develocity values enabled by default. */
    abstract val legacyDevelocityValuesEnabled: Property<Boolean>
    abstract val histogramEnabled: Property<Boolean>
    abstract val histogramBucket: Property<Bucket>
    abstract val enableConsoleLog: Property<Boolean>
}
