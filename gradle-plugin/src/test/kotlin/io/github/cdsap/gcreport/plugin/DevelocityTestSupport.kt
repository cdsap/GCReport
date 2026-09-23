package io.github.cdsap.gcreport.plugin

internal object DevelocityTestSupport {
    fun pluginVersion(): String =
        checkNotNull(System.getProperty("develocityPluginVersion")) {
            "Missing develocityPluginVersion system property; set it from libs.versions.develocity in build.gradle.kts"
        }

    fun settingsScript(
        configureBody: String,
        includeGcReport: Boolean = false,
        gcReportBody: String = "",
    ): String {
        val pluginsBlock =
            if (includeGcReport) {
                """
                plugins {
                    id("com.gradle.develocity") version "${pluginVersion()}"
                    id("io.github.cdsap.gcreport")
                }
                """.trimIndent()
            } else {
                """
                plugins {
                    id("com.gradle.develocity") version "${pluginVersion()}"
                }
                """.trimIndent()
            }
        val gcReportBlock =
            if (includeGcReport) {
                """
                gcReport {
                    $gcReportBody
                }
                """.trimIndent()
            } else {
                ""
            }
        return buildString {
            appendLine(
                """
                pluginManagement {
                    repositories {
                        gradlePluginPortal()
                        mavenCentral()
                    }
                }
                """.trimIndent(),
            )
            appendLine(pluginsBlock)
            appendLine(
                """
                develocity {
                    $configureBody
                }
                """.trimIndent(),
            )
            if (gcReportBlock.isNotEmpty()) {
                appendLine(gcReportBlock)
            }
        }.trimEnd()
    }
}
