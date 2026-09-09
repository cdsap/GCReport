package io.github.cdsap.gcreport.plugin

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class GradlePluginBuildConfigTest {
    @Test
    fun `gradle plugin build declares junit platform launcher on test runtime classpath`() {
        val buildGradle = File("build.gradle.kts").canonicalFile
        val buildGradleContents = buildGradle.readText()

        assertTrue(
            buildGradleContents.contains("testRuntimeOnly(libs.junit.platform.launcher)"),
            "build.gradle.kts must declare junit-platform-launcher on the test runtime classpath",
        )
    }

    @Test
    fun `version catalog declares junit platform launcher`() {
        val versionCatalog = File("../gradle/libs.versions.toml").canonicalFile
        val versionCatalogContents = versionCatalog.readText()

        assertTrue(
            versionCatalogContents.contains("junit-platform-launcher"),
            "gradle/libs.versions.toml must declare junit-platform-launcher",
        )
    }

    @Test
    fun `develocity is compileOnly so it is not published as a runtime dependency`() {
        val buildGradleContents = File("build.gradle.kts").canonicalFile.readText()

        assertTrue(
            buildGradleContents.contains("compileOnly(libs.develocity)"),
            "build.gradle.kts must declare develocity as compileOnly",
        )
        assertTrue(
            buildGradleContents.contains("testImplementation(libs.develocity)"),
            "build.gradle.kts must declare develocity on the test classpath",
        )
        assertFalse(
            buildGradleContents.contains("implementation(libs.develocity)"),
            "build.gradle.kts must not declare develocity as implementation (leaks onto consumers)",
        )
        assertTrue(
            buildGradleContents.contains("pluginUnderTestMetadata"),
            "build.gradle.kts must keep Develocity on the TestKit plugin classpath via pluginUnderTestMetadata",
        )
    }

    @Test
    fun `GCReportPlugin does not hard-reference DevelocityConfiguration`() {
        val pluginSource =
            File("src/main/kotlin/io/github/cdsap/gcreport/plugin/GCReportPlugin.kt").canonicalFile

        assertFalse(
            pluginSource.readText().contains("DevelocityConfiguration"),
            "GCReportPlugin must not reference DevelocityConfiguration so it can load without Develocity on the classpath",
        )
    }
}
