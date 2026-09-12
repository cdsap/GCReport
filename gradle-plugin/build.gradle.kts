plugins {
    alias(libs.plugins.kotlin.jvm)
    `kotlin-dsl`
    `java-gradle-plugin`
    `maven-publish`
    alias(libs.plugins.gradle.publish)
    alias(libs.plugins.ktlint)
}

group = "io.github.cdsap"
version = "0.1.0"

dependencies {
    compileOnly(libs.develocity)
    implementation(libs.picnic)
    testImplementation(libs.develocity)
    testImplementation(platform(libs.junit))
    testImplementation(libs.ktor.client.cio)
    testImplementation(libs.gson)
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}

// TestKit uses an isolated plugin classpath; include Develocity there so optional integration
// tests can load DevelocityConfiguration without publishing it as a consumer runtime dependency.
tasks.named<PluginUnderTestMetadata>("pluginUnderTestMetadata") {
    pluginClasspath.from(
        configurations.compileClasspath.map { classpath ->
            classpath.filter { it.name.startsWith("develocity-gradle-plugin") }
        },
    )
}

gradlePlugin {
    website = "https://github.com/cdsap/GCReport"
    vcsUrl = "https://github.com/cdsap/GCReport.git"
    plugins {
        create("GCReport") {
            id = "io.github.cdsap.gcreport"
            implementationClass = "io.github.cdsap.gcreport.plugin.GCReportPlugin"
            displayName = "GC Report"
            description = "Gradle plugin that collects GC metrics based on the GC logs generated during the build"
            tags = listOf("kotlin", "gc", "performance")
        }
    }
}
