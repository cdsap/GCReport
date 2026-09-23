plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.gradle.publish)
    alias(libs.plugins.ktlint)
}

group = "io.github.cdsap"
version = "0.1.0"

// Project directory is gradle-plugin; override so Maven coordinates are descriptive.
publishing {
    publications {
        create<MavenPublication>("pluginMaven") {
            artifactId = "gcreport-gradle-plugin"
        }
    }
}

kotlin {
    jvmToolchain(11)
}

dependencies {
    implementation(gradleApi())
    compileOnly(libs.develocity)
    implementation(libs.picnic)
    testImplementation(libs.develocity)
    testImplementation(platform(libs.junit))
    testImplementation(libs.ktor.client.cio)
    testImplementation(libs.gson)
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks.test {
    useJUnitPlatform()
    dependsOn(tasks.named("generatePomFileForPluginMavenPublication"))
    // Keep TestKit fixtures on the same Develocity version as libs.versions.toml
    systemProperty("develocityPluginVersion", libs.versions.develocity.get())
}

tasks.validatePlugins {
    enableStricterValidation = true
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
            description =
                "Gradle settings plugin that collects GC metrics based on the GC logs generated during the build"
            tags = listOf("kotlin", "gc", "performance")
        }
        create("GCReportProject") {
            id = "io.github.cdsap.gcreport.project"
            implementationClass = "io.github.cdsap.gcreport.plugin.GCReportProjectPlugin"
            displayName = "GC Report (project compatibility)"
            description =
                "Compatibility project plugin for GC Report; prefer applying io.github.cdsap.gcreport from settings"
            tags = listOf("kotlin", "gc", "performance")
        }
    }
}
