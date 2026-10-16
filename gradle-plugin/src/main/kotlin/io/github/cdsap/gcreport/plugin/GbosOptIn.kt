package io.github.cdsap.gcreport.plugin

import org.gradle.api.provider.ProviderFactory

/**
 * Single GBOS opt-in: one Gradle property and the public DSL [GCReportExtension.gbosEnabled]
 * enable JSON, NDJSON, and Develocity GBOS outputs together.
 */
internal object GbosOptIn {
    const val PROPERTY_ENABLED = "gcReport.gbos.enabled"

    fun configureConventions(
        extension: GCReportExtension,
        providers: ProviderFactory,
    ) {
        extension.gbosEnabled.convention(
            providers.gradleProperty(PROPERTY_ENABLED).map(String::toBoolean).orElse(false),
        )
    }
}
