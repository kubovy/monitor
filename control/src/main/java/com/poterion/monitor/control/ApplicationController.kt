/******************************************************************************
 * Copyright (C) 2020 Jan Kubovy (jan@kubovy.eu)                              *
 *                                                                            *
 * This program is free software: you can redistribute it and/or modify       *
 * it under the terms of the GNU General Public License as published by       *
 * the Free Software Foundation, either version 3 of the License, or          *
 * (at your option) any later version.                                        *
 *                                                                            *
 * This program is distributed in the hope that it will be useful,            *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of             *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the              *
 * GNU General Public License for more details.                               *
 *                                                                            *
 * You should have received a copy of the GNU General Public License          *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.      *
 ******************************************************************************/
package com.poterion.monitor.control

import com.poterion.monitor.api.Shared
import com.poterion.monitor.api.StatusCollector
import com.poterion.monitor.api.controllers.ControllerInterface
import com.poterion.monitor.api.controllers.ModuleInstanceInterface
import com.poterion.monitor.api.controllers.Notifier
import com.poterion.monitor.api.controllers.Service
import com.poterion.monitor.api.modules.Module
import com.poterion.monitor.api.modules.NotifierModule
import com.poterion.monitor.api.modules.ServiceModule
import com.poterion.monitor.api.objectMapper
import com.poterion.monitor.api.save
import com.poterion.monitor.api.workers.UpdateChecker
import com.poterion.monitor.data.ModuleDeserializer
import com.poterion.monitor.data.data.ApplicationConfiguration
import com.poterion.monitor.data.notifiers.NotifierConfig
import com.poterion.monitor.data.notifiers.NotifierDeserializer
import com.poterion.monitor.data.services.ServiceConfig
import com.poterion.monitor.data.services.ServiceDeserializer
import com.poterion.monitor.data.services.ServiceSubConfig
import com.poterion.utils.javafx.confirmDialog
import com.poterion.utils.javafx.openInExternalApplication
import io.reactivex.subjects.BehaviorSubject
import javafx.application.Platform
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.stage.Stage
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess

/**
 * Application controller
 *
 * @author Jan Kubovy [jan@kubovy.eu]
 */
class ApplicationController(override val stage: Stage, vararg modules: Module<*, *>):
		ControllerInterface {
	companion object {
		private val LOGGER: Logger = LoggerFactory.getLogger(ApplicationController::class.java)
	}

	override val modules = mutableListOf<Module<*, *>>()

	// TODO Make read-only and react on applicationConfiguration.services/notifiers changes
	override val services: ObservableList<Service<ServiceSubConfig, ServiceConfig<out ServiceSubConfig>>> = FXCollections
			.observableArrayList()
	override val notifiers: ObservableList<Notifier<NotifierConfig>> = FXCollections.observableArrayList()

	override var applicationConfiguration: ApplicationConfiguration = ApplicationConfiguration()
		private set
	private val configuration: BehaviorSubject<Boolean> = BehaviorSubject.create()

	init {
		LOGGER.info("Initializing with ${Shared.configFile.absolutePath} config file")
		modules.forEach { registerModule(it) }
		if (Shared.configFile.exists()) try {
			applicationConfiguration = objectMapper.readValue(Shared.configFile, ApplicationConfiguration::class.java)
			applicationConfiguration.version = Shared.properties
					.getProperty("version", "0")
					.takeUnless { it.contains("SNAPSHOT") }
					?: "0"
			(applicationConfiguration.serviceMap as MutableMap<String, ServiceConfig<out ServiceSubConfig>?>)
					.filterValues { it == null }
					.keys
					.forEach { applicationConfiguration.serviceMap.remove(it) }
			(applicationConfiguration.notifierMap as MutableMap<String, NotifierConfig?>)
					.filterValues { it == null }
					.keys
					.forEach { applicationConfiguration.notifierMap.remove(it) }
		} catch (e: Exception) {
			LOGGER.error(e.message, e)
			applicationConfiguration = ApplicationConfiguration()
			Shared.configFile.copyTo(File(Shared.configFile.absolutePath + "-" + LocalDateTime.now().toString()))
		}
	}

	private fun registerModule(module: Module<*, *>) {
		LOGGER.info("Registring ${module::class.java.name} module")
		modules.add(module)
		ModuleDeserializer.register(module.configClass)
		when (module) {
			is ServiceModule<*, ServiceConfig<out ServiceSubConfig>, *> -> ServiceDeserializer.register(module.configClass)
			is NotifierModule<*, *> -> NotifierDeserializer.register(module.configClass)
		}
	}

	fun start() {
		LOGGER.info("Starting...")

		for (module in modules) {
			LOGGER.info("Loading ${module.title} module...")
			module.loadControllers(this, applicationConfiguration).forEach { ctrl ->
				when (ctrl) {
					is Service<*, *> -> services.add(ctrl as Service<ServiceSubConfig, ServiceConfig<out ServiceSubConfig>>)
					is Notifier<NotifierConfig> -> notifiers.add(ctrl)
				}
			}
		}

		(services + notifiers).forEach { it.initialize() }

		ControllerWorker.start(applicationConfiguration, services)

		stage.setOnCloseRequest { if (notifiers.map { it.exitRequest }.reduce { acc, b -> acc && b }) quit() }

		StatusCollector.status.sample(10, TimeUnit.SECONDS, true).subscribe { collector ->
			val removes = collector.items
					.mapNotNull { item -> applicationConfiguration.silencedMap[item.id]?.let { item to it } }
					.filter { (item, silenced) -> silenced.untilChanged && item.startedAt != silenced.lastChange }
					.map { (item, _) -> item.id }
			val changes = collector.items
					.filterNot { removes.contains(it.id) }
					.mapNotNull { item -> applicationConfiguration.silencedMap[item.id]?.let { item to it } }
					.filter { (item, silenced) -> item != silenced.item }
					.map { (item, silenced) -> item.id to silenced.copy(item = item, lastChange = item.startedAt) }
					.toMap()
			if (changes.isNotEmpty() || removes.isNotEmpty()) {
				applicationConfiguration.silencedMap.replaceAll { id, item -> changes[id] ?: item }
				removes.forEach { applicationConfiguration.silencedMap.remove(it) }
				saveConfig()
			}
		}

		configuration.sample(1, TimeUnit.SECONDS, true).subscribe {
			applicationConfiguration.save()
		}

		var lastVersionChecked = applicationConfiguration.version
		UpdateChecker.lastVersion.subscribe { lastVersion ->
			Platform.runLater {
				if (lastVersionChecked != lastVersion) confirmDialog("Update available",
						"Do you want to download the new version?",
						"New version ${lastVersion} is available") {
					openInExternalApplication("https://artifacts.poterion.com/root/poterion-monitor/")
				}
				lastVersionChecked = lastVersion
			}
		}
		UpdateChecker.start(applicationConfiguration)
	}

	override fun add(module: Module<*, *>): ModuleInstanceInterface<*>? {
		return module.createController(this, applicationConfiguration)?.let { add(it) }
	}

	override fun add(controller: ModuleInstanceInterface<*>): ModuleInstanceInterface<*>? {
		when (controller) {
			is Service<*, ServiceConfig<out ServiceSubConfig>> -> {
				applicationConfiguration.serviceMap[controller.config.uuid] = controller.config
				services.add(controller as Service<ServiceSubConfig, ServiceConfig<out ServiceSubConfig>>)
			}
			is Notifier<NotifierConfig> -> {
				applicationConfiguration.notifierMap[controller.config.uuid] = controller.config
				notifiers.add(controller)
			}
			else -> return null
		}

		controller.initialize()
		return controller
	}

	override fun quit() {
		ControllerWorker.stop()
		notifiers.forEach { it.shutdown() }
		saveConfig()
		exitProcess(0) // not necessary if all non-daemon threads have stopped.
	}

	override fun saveConfig() {
		configuration.onNext(true)
	}
}