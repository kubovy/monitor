package com.poterion.monitor.control

import com.fasterxml.jackson.core.Version
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.poterion.monitor.api.StatusCollector
import com.poterion.monitor.api.controllers.ControllerInterface
import com.poterion.monitor.api.controllers.Notifier
import com.poterion.monitor.api.controllers.Service
import com.poterion.monitor.api.modules.Module
import com.poterion.monitor.api.modules.NotifierModule
import com.poterion.monitor.api.modules.ServiceModule
import com.poterion.monitor.data.Config
import com.poterion.monitor.data.notifiers.NotifierAction
import com.poterion.monitor.data.notifiers.NotifierConfig
import com.poterion.monitor.data.notifiers.NotifierDeserializer
import com.poterion.monitor.data.services.ServiceConfig
import com.poterion.monitor.data.services.ServiceDeserializer
import javafx.application.Platform
import javafx.stage.Stage
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.util.concurrent.Executors

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
class Controller(override val stage: Stage, configFileName: String = "config.yaml") : ControllerInterface {
	companion object {
		val LOGGER: Logger = LoggerFactory.getLogger(Controller::class.java)
	}

	private val configFile = File(configFileName)
	private val executor = Executors.newSingleThreadExecutor()
	private var worker: ControllerWorker? = null
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
	private val _modules = mutableListOf<Module<*, *>>()
	private val _services = mutableListOf<Service<ServiceConfig>>()
	override val services: List<Service<ServiceConfig>>
		get() = _services
	private val serviceLastChecked = mutableMapOf<String, Long>()
	private val _notifiers = mutableListOf<Notifier<NotifierConfig>>()
	override val notifiers: List<Notifier<NotifierConfig>>
		get() = _notifiers

	override var config: Config = Config()
	private val configListeners = mutableListOf<(Config) -> Unit>()

	fun registerModule(module: Module<*, *>) {
		LOGGER.info("Registring ${module::class.java.name} module")
		_modules.add(module)
		when (module) {
			is ServiceModule<*, *> -> ServiceDeserializer.register(module.configClass)
			is NotifierModule<*, *> -> NotifierDeserializer.register(module.configClass)
		}
	}

	fun start() {
		LOGGER.info("Starting with ${configFile.absolutePath} config file")
		config = mapper.readValue(configFile, Config::class.java)

		for (module in _modules) when (module) {
			is ServiceModule<*, *> -> _services.addAll(module.createControllers(this, config))
			is NotifierModule<*, *> -> _notifiers.addAll(module.createControllers(this, config))
		}
		_services.sortBy { it.config.priority }
		_notifiers.sortBy { it.config.name }

		(services + notifiers).forEach { it.initialize() }

		worker?.stop()
		worker = ControllerWorker { Platform.runLater { check() } }
		executor.submit(worker)
	}

	override fun check(force: Boolean) {
		val now = System.currentTimeMillis()
		_services
				.filter { force || (now - (serviceLastChecked[it.config.name] ?: 0L)) > it.config.checkInterval }
				.forEach { service ->
					serviceLastChecked[service.config.name] = System.currentTimeMillis()
					service.check { StatusCollector.update(it) }
				}
	}

	override fun quit() {
		executor.shutdown()
		worker?.stop()
		executor.shutdownNow()
		_notifiers.forEach { it.execute(NotifierAction.SHUTDOWN) }
		saveConfig()
		System.exit(0) // not necessary if all non-daemon threads have stopped.
	}

	override fun registerForConfigUpdates(listener: (Config) -> Unit) {
		configListeners.add(listener)
	}

	override fun triggerUpdate() {
		configListeners.forEach { it.invoke(config) }
	}

	override fun saveConfig() = try {
		configListeners.forEach { it.invoke(config) }
		check(force = true)
		mapper.writeValue(configFile, config)
	} catch (e: Exception) {
		LOGGER.error(e.message, e)
	}
}