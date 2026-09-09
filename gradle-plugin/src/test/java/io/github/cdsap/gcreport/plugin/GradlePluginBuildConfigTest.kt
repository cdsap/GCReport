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
    fun `test task injects Develocity plugin version from the version catalog`() {
        val buildGradle = File("build.gradle.kts").canonicalFile
        val buildGradleContents = buildGradle.readText()

        assertTrue(
            buildGradleContents.contains("systemProperty(\"develocityPluginVersion\", libs.versions.develocity.get())"),
            "build.gradle.kts must inject libs.versions.develocity into tests",
        )

        val versionCatalog = File("../gradle/libs.versions.toml").canonicalFile.readText()
        val catalogVersion =
            Regex("""(?m)^develocity\s*=\s*"([^"]+)"""").find(versionCatalog)?.groupValues?.get(1)
        requireNotNull(catalogVersion) { "develocity version missing from libs.versions.toml" }

        assertTrue(
            DevelocityTestSupport.pluginVersion() == catalogVersion,
            "TestKit Develocity version must match the version catalog ($catalogVersion)",
        )
    }
}
