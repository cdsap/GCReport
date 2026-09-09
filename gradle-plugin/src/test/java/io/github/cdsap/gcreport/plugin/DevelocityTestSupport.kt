package io.github.cdsap.gcreport.plugin

internal object DevelocityTestSupport {
    fun pluginVersion(): String =
        checkNotNull(System.getProperty("develocityPluginVersion")) {
            "Missing develocityPluginVersion system property; set it from libs.versions.develocity in build.gradle.kts"
        }

    fun settingsScript(configureBody: String): String =
        """
        plugins {
            id("com.gradle.develocity") version "${pluginVersion()}"
        }
        develocity {
            $configureBody
        }
        """.trimIndent()
}
