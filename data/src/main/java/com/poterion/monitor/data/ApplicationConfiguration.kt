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
		var windowWidth: Double = 1200.0,
		var windowHeight: Double = 1000.0,
		var alertTitleWidth: Double = 200.0,
		var alertServiceWidth: Double = 200.0,
		var alertLabelsWidth: Double = 200.0,
		var commonSplit: Double = 0.3,
		val services: MutableList<ServiceConfig> = mutableListOf(),
		val notifiers: MutableList<NotifierConfig> = mutableListOf())
