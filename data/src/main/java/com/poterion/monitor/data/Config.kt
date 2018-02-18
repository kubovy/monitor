package com.poterion.monitor.data

import com.poterion.monitor.data.notifiers.NotifierConfig
import com.poterion.monitor.data.services.ServiceConfig

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
data class Config(
		var services: List<ServiceConfig> = emptyList(),
		var notifiers: List<NotifierConfig> = emptyList())
