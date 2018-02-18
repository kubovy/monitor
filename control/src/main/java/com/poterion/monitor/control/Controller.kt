package com.poterion.monitor.control

import com.fasterxml.jackson.core.Version
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.poterion.monitor.api.StatusCollector
import com.poterion.monitor.api.controllers.ControllerInterface
import com.poterion.monitor.api.controllers.NotifierController
import com.poterion.monitor.api.controllers.ServiceController
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
import org.slf4j.LoggerFactory
import java.io.File
import java.util.concurrent.Executors
import kotlin.reflect.KClass

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
class Controller(override val stage: Stage, configFileName: String = "config.yaml") : ControllerInterface {
	companion object {
		val LOGGER = LoggerFactory.getLogger(Controller::class.java)
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
			configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		}
	private val modules = mutableListOf<Module<*, *>>()
	private val _serviceControllers = mutableListOf<ServiceController<ServiceConfig>>()
	override val serviceControllers: List<ServiceController<ServiceConfig>>
		get() = _serviceControllers
	private val serviceLastChecked = mutableMapOf<String, Long>()
	private val _notifierControllers = mutableListOf<NotifierController<NotifierConfig>>()
	override val notifierControllers: List<NotifierController<NotifierConfig>>
		get() = _notifierControllers

	var config: Config = Config()
	private val configListeners = mutableListOf<(Config) -> Unit>()

	fun registerModule(module: Module<*, *>) {
		LOGGER.info("Registring ${module::class.java.name} module")
		modules.add(module)
		when (module) {
			is ServiceModule<*, *> -> ServiceDeserializer.register(module.configClass)
			is NotifierModule<*, *> -> NotifierDeserializer.register(module.configClass)
		}
	}

	fun <T : Module<*, *>> getModules(clazz: KClass<T>) = modules
			.filter { clazz.isInstance(it) }
			.map { it as T }

	fun start() {
		LOGGER.info("Starting with ${configFile.absolutePath} config file")
		config = mapper.readValue(configFile, Config::class.java)

		for (module in modules) when (module) {
			is ServiceModule<*, *> -> _serviceControllers.addAll(module.createControllers(this, config))
			is NotifierModule<*, *> -> _notifierControllers.addAll(module.createControllers(this, config))
		}
		_serviceControllers.sortBy { it.config.priority }
		_notifierControllers.sortBy { it.config.name }

		worker?.stop()
		worker = ControllerWorker { Platform.runLater { check() } }
		executor.submit(worker)
	}

	override fun check(force: Boolean) {
		val now = System.currentTimeMillis()
		_serviceControllers
				.filter { force || (now - (serviceLastChecked[it.config.name] ?: 0L)) > it.config.checkInterval }
				.forEach {
					serviceLastChecked[it.config.name] = System.currentTimeMillis()
					it.check { StatusCollector.update(it) }
				}
	}

	fun run(body: () -> Unit) {
		if (Platform.isFxApplicationThread()) body.invoke()
		else Platform.runLater(body)
	}

	override fun quit() {
		executor.shutdown()
		worker?.stop()
		executor.shutdownNow()
		_notifierControllers.forEach { it.execute(NotifierAction.SHUTDOWN) }
		saveConfig()
		System.exit(0) // not necessary if all non-daemon threads have stopped.
	}

	override fun registerForConfigUpdates(listener: (Config) -> Unit) {
		configListeners.add(listener)
	}

	override fun updateConfig() {
		configListeners.forEach { it.invoke(config) }
	}

	override fun saveConfig() {
		try {
			mapper.writeValue(configFile, config)
		} catch (e: Exception) {
			LOGGER.error(e.message, e)
		}
	}
}