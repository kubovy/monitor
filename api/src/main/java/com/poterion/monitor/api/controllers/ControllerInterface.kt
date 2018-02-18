package com.poterion.monitor.api.controllers

import com.poterion.monitor.data.Config
import com.poterion.monitor.data.notifiers.NotifierConfig
import com.poterion.monitor.data.services.ServiceConfig
import javafx.stage.Stage

interface ControllerInterface {
	val stage: Stage
	val serviceControllers: List<ServiceController<ServiceConfig>>
	val notifierControllers: List<NotifierController<NotifierConfig>>
	fun check(force: Boolean = false)
	fun quit()
	fun saveConfig()
	fun registerForConfigUpdates(listener: (Config) -> Unit)
	fun updateConfig()
}