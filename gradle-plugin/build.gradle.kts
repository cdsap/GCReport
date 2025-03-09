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
    implementation(libs.develocity)
    implementation(libs.picnic)
    testImplementation(platform(libs.junit))
    testImplementation(libs.ktor.client.cio)
    testImplementation(libs.gson)
    testImplementation(libs.junit.jupiter)
}

tasks.test {
    useJUnitPlatform()
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
            tags = listOf("kotlin", "gradle", "gc", "performance")
        }
    }
}
