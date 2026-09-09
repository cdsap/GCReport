package io.github.cdsap.gcreport.plugin

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
    fun `validatePlugins enables stricter validation`() {
        val buildGradle = File("build.gradle.kts").canonicalFile
        val buildGradleContents = buildGradle.readText()

        assertTrue(
            buildGradleContents.contains("tasks.validatePlugins"),
            "build.gradle.kts must configure the validatePlugins task",
        )
        assertTrue(
            buildGradleContents.contains("enableStricterValidation = true"),
            "build.gradle.kts must enable validatePlugins.enableStricterValidation",
        )
    }
}
