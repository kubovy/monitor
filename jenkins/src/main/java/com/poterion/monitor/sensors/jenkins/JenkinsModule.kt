package com.poterion.monitor.sensors.jenkins

import com.poterion.monitor.api.modules.ServiceModule
import com.poterion.monitor.data.Config
import com.poterion.monitor.data.services.JenkinsConfig
import com.poterion.monitor.sensors.jenkins.control.JenkinsServiceController
import kotlin.reflect.KClass

object JenkinsModule : ServiceModule<JenkinsConfig, JenkinsServiceController> {
    override val configClass: KClass<JenkinsConfig> = JenkinsConfig::class
    override fun createControllers(config: Config): Collection<JenkinsServiceController> = config.services
            .filter { it is JenkinsConfig }
            .map { it as JenkinsConfig }
            .map { JenkinsServiceController(it) }
}