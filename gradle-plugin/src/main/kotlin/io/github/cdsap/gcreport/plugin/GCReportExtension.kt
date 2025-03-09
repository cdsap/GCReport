package io.github.cdsap.gcreport.plugin

import io.github.cdsap.gcreport.plugin.model.Bucket
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

open class GCReportExtension(objects: ObjectFactory) {
    val logs: ListProperty<String> = objects.listProperty(String::class.java)
    val histogramEnabled: Property<Boolean> = objects.property(Boolean::class.java).convention(false)
    val histogramBucket: Property<Bucket> = objects.property(Bucket::class.java).convention(Bucket.FreedmanDiaconis)
    val enableConsoleLog: Property<Boolean> = objects.property(Boolean::class.java).convention(false)
}
