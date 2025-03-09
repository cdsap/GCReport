package io.github.cdsap.gcreport.plugin.report

import io.github.cdsap.gcreport.plugin.ServiceHandler

class ConsoleReport(
    private val service: ServiceHandler,
) {
    fun report() {
        service.createService()
    }
}
