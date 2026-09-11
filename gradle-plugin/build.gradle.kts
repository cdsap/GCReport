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

// Project directory is gradle-plugin; override so Maven coordinates are descriptive.
publishing {
    publications {
        create<MavenPublication>("pluginMaven") {
            artifactId = "gcreport-gradle-plugin"
        }
    }
}

dependencies {
    implementation(libs.develocity)
    implementation(libs.picnic)
    testImplementation(platform(libs.junit))
    testImplementation(libs.ktor.client.cio)
    testImplementation(libs.gson)
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
    dependsOn(tasks.named("generatePomFileForPluginMavenPublication"))
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
