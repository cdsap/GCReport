package io.github.cdsap.gcreport.plugin

import org.gradle.api.provider.ProviderFactory

/** Gradle-property conventions for the independent local and Develocity GBOS sinks. */
internal object GbosOptIn {
    const val PROPERTY_ENABLED = "gcReport.gbos.enabled"
    const val PROPERTY_DEVELOCITY_ENABLED = "gcReport.gbos.develocity.enabled"

    fun configureConventions(
        extension: GCReportExtension,
        providers: ProviderFactory,
    ) {
        extension.gbosEnabled.convention(
            providers.gradleProperty(PROPERTY_ENABLED).map(String::toBoolean).orElse(false),
        )
        extension.gbosDevelocityEnabled.convention(
            providers.gradleProperty(PROPERTY_DEVELOCITY_ENABLED).map(String::toBoolean).orElse(false),
        )
        extension.legacyDevelocityValuesEnabled.convention(true)
    }
}
