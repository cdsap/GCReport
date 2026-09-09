package io.github.cdsap.gcreport.plugin

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class GradlePluginBuildConfigTest {
    private val buildGradleContents: String by lazy {
        File("build.gradle.kts").canonicalFile.readText()
    }

    @Test
    fun `gradle plugin build declares junit platform launcher on test runtime classpath`() {
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
    fun `gradle plugin build keeps kotlin jvm and drops kotlin-dsl`() {
        assertTrue(
            buildGradleContents.contains("alias(libs.plugins.kotlin.jvm)"),
            "build.gradle.kts must apply the kotlin jvm plugin explicitly",
        )
        assertFalse(
            Regex("""\bkotlin-dsl\b""").containsMatchIn(buildGradleContents),
            "build.gradle.kts must not apply kotlin-dsl (no precompiled script plugins)",
        )
    }

    @Test
    fun `gradle plugin build drops plugins applied by plugin-publish`() {
        assertTrue(
            buildGradleContents.contains("alias(libs.plugins.gradle.publish)"),
            "build.gradle.kts must apply com.gradle.plugin-publish",
        )
        assertFalse(
            Regex("""\bjava-gradle-plugin\b""").containsMatchIn(buildGradleContents),
            "build.gradle.kts must not declare java-gradle-plugin (applied by plugin-publish)",
        )
        assertFalse(
            Regex("""\bmaven-publish\b""").containsMatchIn(buildGradleContents),
            "build.gradle.kts must not declare maven-publish (applied by plugin-publish)",
        )
    }

    @Test
    fun `gradle plugin build declares gradleApi explicitly`() {
        assertTrue(
            buildGradleContents.contains("implementation(gradleApi())"),
            "build.gradle.kts must declare gradleApi() once kotlin-dsl is removed",
        )
    }
}
