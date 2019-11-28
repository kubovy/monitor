package com.poterion.monitor.control

import com.fasterxml.jackson.core.Version
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.poterion.monitor.api.StatusCollector
import com.poterion.monitor.api.controllers.ControllerInterface
import com.poterion.monitor.api.controllers.ModuleInstanceInterface
import com.poterion.monitor.api.controllers.Notifier
import com.poterion.monitor.api.controllers.Service
import com.poterion.monitor.api.modules.Module
import com.poterion.monitor.api.modules.NotifierModule
import com.poterion.monitor.api.modules.ServiceModule
import com.poterion.monitor.data.ApplicationConfiguration
import com.poterion.monitor.data.notifiers.NotifierAction
import com.poterion.monitor.data.notifiers.NotifierConfig
import com.poterion.monitor.data.notifiers.NotifierDeserializer
import com.poterion.monitor.data.services.ServiceConfig
import com.poterion.monitor.data.services.ServiceDeserializer
import javafx.application.Platform
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.stage.Stage
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.time.LocalDateTime
import java.util.concurrent.Executors
import kotlin.system.exitProcess

/**
 * Application controller
 *
 * @author Jan Kubovy <jan@kubovy.eu>
 */
class ApplicationController(override val stage: Stage, configFileName: String = "config.yaml") : ControllerInterface {
	companion object {
		val LOGGER: Logger = LoggerFactory.getLogger(ApplicationController::class.java)
	}

	private val configFile = File(configFileName)
	private val mapper
		get() = ObjectMapper(YAMLFactory()).apply {
			registerModule(SimpleModule("PolymorphicServiceDeserializerModule", Version.unknownVersion()).apply {
				addDeserializer(ServiceConfig::class.java, ServiceDeserializer)
			})
			registerModule(SimpleModule("PolymorphicNotifierDeserializerModule", Version.unknownVersion()).apply {
				addDeserializer(NotifierConfig::class.java, NotifierDeserializer)
			})
			configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
		}
	override val modules = mutableListOf<Module<*, *>>()
	override val services: ObservableList<Service<ServiceConfig>> = FXCollections.observableArrayList()
	override val notifiers: ObservableList<Notifier<NotifierConfig>> = FXCollections.observableArrayList()

	override var applicationConfiguration: ApplicationConfiguration = ApplicationConfiguration()
	private val configListeners = mutableListOf<(ApplicationConfiguration) -> Unit>()

	fun registerModule(module: Module<*, *>) {
		LOGGER.info("Registring ${module::class.java.name} module")
		modules.add(module)
		when (module) {
			is ServiceModule<*, *> -> ServiceDeserializer.register(module.configClass)
			is NotifierModule<*, *> -> NotifierDeserializer.register(module.configClass)
		}
	}

	fun start() {
		LOGGER.info("Starting with ${configFile.absolutePath} config file")
		if (configFile.exists()) try {
			applicationConfiguration = mapper.readValue(configFile, ApplicationConfiguration::class.java)
		} catch (e: Exception) {
			configFile.copyTo(File(configFile.absolutePath + "-" + LocalDateTime.now().toString()))
		}

		for (module in modules) {
			LOGGER.info("Loading ${module.title} module...")
			when (module) {
				is ServiceModule<*, *> -> services.addAll(module.loadControllers(this, applicationConfiguration))
				is NotifierModule<*, *> -> notifiers.addAll(module.loadControllers(this, applicationConfiguration))
			}
		}
		services.sortBy { it.config.priority }
		notifiers.sortBy { it.config.name }

		(services + notifiers).forEach { it.initialize() }

		ControllerWorker.start(services)

		stage.setOnCloseRequest { if (notifiers.map { it.exitRequest }.reduce { acc, b -> acc && b }) quit() }
	}

	override fun add(module: Module<*, *>): ModuleInstanceInterface<*>? {
		when (module) {
			is ServiceModule<*, *> -> module.createController(this, applicationConfiguration)?.also { controller ->
				services.add(controller)
				services.sortBy { controller.config.priority }
				return controller
			}
			is NotifierModule<*, *> -> module.createController(this, applicationConfiguration)?.also { controller ->
				notifiers.add(controller)
				notifiers.sortBy { controller.config.name }
				return controller
			}
		}
		return null
	}

	override fun quit() {
		ControllerWorker.stop()
		notifiers.forEach { it.execute(NotifierAction.SHUTDOWN) }
		saveConfig()
		exitProcess(0) // not necessary if all non-daemon threads have stopped.
	}

	override fun registerForConfigUpdates(listener: (ApplicationConfiguration) -> Unit) {
		configListeners.add(listener)
	}

	override fun saveConfig() = try {
		configListeners.forEach { it.invoke(applicationConfiguration) }
		mapper.writeValue(configFile, applicationConfiguration)
	} catch (e: Exception) {
		LOGGER.error(e.message, e)
	}
}