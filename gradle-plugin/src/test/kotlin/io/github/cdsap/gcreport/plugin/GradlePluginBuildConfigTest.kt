package io.github.cdsap.gcreport.plugin

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.DataInputStream
import java.io.File

class GradlePluginBuildConfigTest {
    private val buildGradleContents: String by lazy {
        File("build.gradle.kts").canonicalFile.readText()
    }

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
        assertTrue(
            buildGradleContents.contains("testRuntimeOnly(libs.junit.platform.launcher)"),
            "build.gradle.kts must declare junit-platform-launcher on the test runtime classpath",
        )
    }

    @Test
    fun `gradle plugin build pins JavaCompile encoding to UTF-8`() {
        assertTrue(
            buildGradleContents.contains("tasks.withType<JavaCompile>().configureEach"),
            "build.gradle.kts must configure JavaCompile tasks",
        )
        assertTrue(
            buildGradleContents.contains("options.encoding = \"UTF-8\""),
            "build.gradle.kts must set JavaCompile options.encoding to UTF-8",
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
        assertTrue(
            buildGradleContents.contains("tasks.validatePlugins"),
            "build.gradle.kts must configure the validatePlugins task",
        )
        assertTrue(
            buildGradleContents.contains("enableStricterValidation = true"),
            "build.gradle.kts must enable validatePlugins.enableStricterValidation",
        )
    }

    @Test
    fun `gradle plugin Maven publication uses descriptive artifactId`() {
        assertTrue(
            buildGradleContents.contains("""create<MavenPublication>("pluginMaven")"""),
            "build.gradle.kts must configure the pluginMaven publication",
        )
        assertTrue(
            buildGradleContents.contains("""artifactId = "gcreport-gradle-plugin""""),
            "pluginMaven publication must use descriptive artifactId gcreport-gradle-plugin",
        )
    }

    @Test
    fun `gradle plugin build pins jvm toolchain to java 11`() {
        assertTrue(
            buildGradleContents.contains("jvmToolchain(11)"),
            "build.gradle.kts must pin kotlin jvmToolchain(11) so published bytecode is stable",
        )
    }

    @Test
    fun `generated pluginMaven POM has descriptive artifactId`() {
        val pomFile = File("build/publications/pluginMaven/pom-default.xml").canonicalFile
        assertTrue(
            pomFile.isFile,
            "Expected generated POM at ${pomFile.path}; run generatePomFileForPluginMavenPublication first",
        )

        val primaryArtifactId =
            Regex("""<artifactId>([^<]+)</artifactId>""")
                .find(pomFile.readText())
                ?.groupValues
                ?.get(1)

        assertTrue(
            primaryArtifactId == "gcreport-gradle-plugin",
            "Expected primary artifactId gcreport-gradle-plugin but was $primaryArtifactId",
        )
    }

    @Test
    fun `develocity is compileOnly so it is not published as a runtime dependency`() {
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
