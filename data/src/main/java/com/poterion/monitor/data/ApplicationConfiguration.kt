package com.poterion.monitor.data

import com.poterion.monitor.data.notifiers.NotifierConfig
import com.poterion.monitor.data.services.ServiceConfig

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
data class ApplicationConfiguration(
		var btDiscovery: Boolean = false,
		var showOnStartup: Boolean = true,
		var startMinimized: Boolean = false,
		val services: MutableList<ServiceConfig> = mutableListOf(),
		val notifiers: MutableList<NotifierConfig> = mutableListOf())
