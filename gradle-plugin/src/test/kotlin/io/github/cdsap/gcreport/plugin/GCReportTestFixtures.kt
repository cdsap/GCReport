package io.github.cdsap.gcreport.plugin

import java.io.File

/**
 * Shared TestKit fixture helpers for applying GCReport from settings (primary) or project (compat).
 */
internal object GCReportTestFixtures {
    fun writeKotlinSettings(
        projectDir: File,
        gcLog: String,
        extraGcReportConfig: String = "",
        extraSettings: String = "",
    ) {
        File(projectDir, "settings.gradle.kts").writeText(
            """
            pluginManagement {
                repositories {
                    gradlePluginPortal()
                    mavenCentral()
                }
            }
            plugins {
                id("io.github.cdsap.gcreport")
            }
            gcReport {
                logs.set(listOf("$gcLog"))
                $extraGcReportConfig
            }
            $extraSettings
            """.trimIndent(),
        )
    }

    fun writeGroovySettings(
        projectDir: File,
        gcLog: String,
        extraGcReportConfig: String = "",
        extraSettings: String = "",
    ) {
        File(projectDir, "settings.gradle").writeText(
            """
            pluginManagement {
                repositories {
                    gradlePluginPortal()
                    mavenCentral()
                }
            }
            plugins {
                id 'io.github.cdsap.gcreport'
            }
            gcReport {
                logs = ['$gcLog']
                $extraGcReportConfig
            }
            $extraSettings
            """.trimIndent(),
        )
    }

    fun writeMinimalBuild(
        projectDir: File,
        content: String =
            """
            plugins {
                java
            }
            """.trimIndent(),
    ) {
        File(projectDir, "build.gradle.kts").writeText(content)
    }

    fun writeJvmGcProperties(
        projectDir: File,
        gcLog: String,
        extraJvmArgs: String = "",
    ) {
        File(projectDir, "gradle.properties").writeText(
            """
            org.gradle.jvmargs=-Xlog:gc*:file=$gcLog$extraJvmArgs
            """.trimIndent(),
        )
    }
}
