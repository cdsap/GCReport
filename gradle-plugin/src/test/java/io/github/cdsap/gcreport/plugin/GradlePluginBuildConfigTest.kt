package io.github.cdsap.gcreport.plugin

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class GradlePluginBuildConfigTest {
    @Test
    fun `settings centralizes dependency repositories and rejects project repositories`() {
        val settingsGradle = File("../settings.gradle.kts").canonicalFile
        val settingsContents = settingsGradle.readText()

        assertTrue(
            settingsContents.contains("repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS"),
            "settings.gradle.kts must reject project repository declarations",
        )
        assertTrue(
            settingsContents.contains("mavenCentral()"),
            "settings.gradle.kts must declare Maven Central for dependency resolution",
        )
        assertTrue(
            settingsContents.contains("gradlePluginPortal()"),
            "settings.gradle.kts must keep the Plugin Portal for develocity-gradle-plugin resolution",
        )
        assertTrue(
            Regex("""dependencyResolutionManagement\s*\{[\s\S]*gradlePluginPortal\(\)""").containsMatchIn(settingsContents),
            "gradlePluginPortal() must remain under dependencyResolutionManagement for implementation deps",
        )
    }

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
}
