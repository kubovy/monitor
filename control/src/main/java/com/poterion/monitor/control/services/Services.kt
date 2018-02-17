package com.poterion.monitor.control.services

import com.poterion.monitor.api.ServiceController
import com.poterion.monitor.api.StatusCollector
import com.poterion.monitor.data.Config
import com.poterion.monitor.data.services.JenkinsConfig
import com.poterion.monitor.data.services.ServiceConfig
import com.poterion.monitor.data.services.SonarConfig

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
object Services {
    private var controllers: Collection<ServiceController<*>> = emptyList()
    private val lastChecked = mutableMapOf<String, Long>()

    fun configure(config: Config) {
        controllers = config.services.map { Services.create(it) }
    }

    fun check(force: Boolean = false) {
        val now = System.currentTimeMillis()
        controllers
                .filter { force || (now - (lastChecked[it.config.name] ?: 0L)) > it.config.checkInterval }
                .forEach { it.checkAndUpdate() }
    }

    private fun create(serviceConfig: ServiceConfig) = when (serviceConfig) {
        is JenkinsConfig -> JenkinsServiceController(serviceConfig)
        is SonarConfig -> SonarServiceController(serviceConfig)
        else -> throw Exception("Unknown ${serviceConfig::class} service definition")
    }

    private fun ServiceController<*>.checkAndUpdate() {
        lastChecked[config.name] = System.currentTimeMillis()
        check { StatusCollector.update(it) }
    }
}