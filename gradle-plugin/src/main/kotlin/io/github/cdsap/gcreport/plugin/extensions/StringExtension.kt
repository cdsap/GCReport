package io.github.cdsap.gcreport.plugin.extensions

fun String.getFileName(): String {
    return this.substringAfterLast("/")
}

fun String.getFileNameCsvLog(): String {
    return this.getFileName().replace(".log", ".csv")
}
