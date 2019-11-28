package com.poterion.monitor.api.controllers

import com.poterion.monitor.api.modules.Module
import com.poterion.monitor.data.ApplicationConfiguration
import com.poterion.monitor.data.notifiers.NotifierConfig
import com.poterion.monitor.data.services.ServiceConfig
import javafx.collections.ObservableList
import javafx.stage.Stage

interface ControllerInterface {
	val stage: Stage
	val applicationConfiguration: ApplicationConfiguration
	val modules: List<Module<*, *>>
	val services: ObservableList<Service<ServiceConfig>>
	val notifiers: ObservableList<Notifier<NotifierConfig>>

	fun add(module: Module<*, *>): ModuleInstanceInterface<*>?
	fun quit()
	fun saveConfig()
	fun registerForConfigUpdates(listener: (ApplicationConfiguration) -> Unit)
}