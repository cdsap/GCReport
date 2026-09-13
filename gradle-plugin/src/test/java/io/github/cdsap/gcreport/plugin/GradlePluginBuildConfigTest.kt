package io.github.cdsap.gcreport.plugin

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.DataInputStream
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

    @Test
    fun `gradle plugin build pins jvm toolchain to java 11`() {
        val buildGradleContents = File("build.gradle.kts").canonicalFile.readText()

        assertTrue(
            buildGradleContents.contains("jvmToolchain(11)"),
            "build.gradle.kts must pin kotlin jvmToolchain(11) so published bytecode is stable",
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
    fun `compiled plugin classes target java 11 bytecode`() {
        val classFile =
            File("build/classes/kotlin/main/io/github/cdsap/gcreport/plugin/GCReportPlugin.class")
                .canonicalFile

        assertTrue(classFile.isFile, "Expected compiled class at ${classFile.path}")
        assertEquals(
            JAVA_11_MAJOR_VERSION,
            classFileMajorVersion(classFile),
            "Published plugin bytecode must target Java 11 (major version $JAVA_11_MAJOR_VERSION)",
        )
    }

    private fun classFileMajorVersion(classFile: File): Int =
        DataInputStream(classFile.inputStream().buffered()).use { input ->
            val magic = input.readInt()
            assertEquals(CLASS_FILE_MAGIC, magic, "File is not a valid JVM class file: ${classFile.path}")
            input.readUnsignedShort() // minor version
            input.readUnsignedShort() // major version
        }

    private companion object {
        const val CLASS_FILE_MAGIC = -0x35014542 // 0xCAFEBABE
        const val JAVA_11_MAJOR_VERSION = 55
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
