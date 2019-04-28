package com.poterion.monitor.notifiers.deploymentcase.api

import com.poterion.monitor.notifiers.deploymentcase.data.Configuration
import com.poterion.monitor.notifiers.deploymentcase.data.DeploymentCaseConfig

interface ConfigurationContributer {
    fun notifyNewConfiguration(configuration: Configuration)
    fun updateConfiguration(config: DeploymentCaseConfig, configuration: Configuration?)
}