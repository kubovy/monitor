package com.poterion.monitor.control

import com.fasterxml.jackson.core.Version
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule
import com.poterion.monitor.api.StatusCollector
import com.poterion.monitor.api.controllers.ControllerInterface
import com.poterion.monitor.api.controllers.ModuleInstanceInterface
import com.poterion.monitor.api.controllers.Notifier
import com.poterion.monitor.api.controllers.Service
import com.poterion.monitor.api.modules.Module
import com.poterion.monitor.api.modules.NotifierModule
import com.poterion.monitor.api.modules.ServiceModule
import com.poterion.monitor.data.auth.AuthConfig
import com.poterion.monitor.data.auth.AuthDeserializer
import com.poterion.monitor.data.data.ApplicationConfiguration
import com.poterion.monitor.data.notifiers.NotifierAction
import com.poterion.monitor.data.notifiers.NotifierConfig
import com.poterion.monitor.data.notifiers.NotifierDeserializer
import com.poterion.monitor.data.services.ServiceConfig
import com.poterion.monitor.data.services.ServiceDeserializer
import io.reactivex.subjects.PublishSubject
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
class ApplicationController(override val stage: Stage, configFileName: String, vararg modules: Module<*, *>) : ControllerInterface {
	companion object {
		private val LOGGER: Logger = LoggerFactory.getLogger(ApplicationController::class.java)
	}

	private val configFile = File(configFileName)
	private val mapper
		get() = ObjectMapper(YAMLFactory()).apply {
			registerModule(ParameterNamesModule())
			registerModule(Jdk8Module())
			registerModule(JavaTimeModule())
			registerModule(SimpleModule("PolymorphicServiceDeserializerModule", Version.unknownVersion()).apply {
				addDeserializer(AuthConfig::class.java, AuthDeserializer)
			})
			registerModule(SimpleModule("PolymorphicServiceDeserializerModule", Version.unknownVersion()).apply {
				addDeserializer(ServiceConfig::class.java, ServiceDeserializer)
			})
			registerModule(SimpleModule("PolymorphicNotifierDeserializerModule", Version.unknownVersion()).apply {
				addDeserializer(NotifierConfig::class.java, NotifierDeserializer)
			})
			configure(SerializationFeature.CLOSE_CLOSEABLE, true)
			configure(SerializationFeature.FLUSH_AFTER_WRITE_VALUE, true)
			configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, true)
			configure(SerializationFeature.WRITE_DATE_KEYS_AS_TIMESTAMPS, true)
			//configure(SerializationFeature.WRITE_ENUMS_USING_TO_STRING, true)
			configure(DeserializationFeature.WRAP_EXCEPTIONS, true)
			configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)
			configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false)
			configure(DeserializationFeature.FAIL_ON_NULL_CREATOR_PROPERTIES, false)
			configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false)
			configure(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES, false)
			configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
		}
	override val modules = mutableListOf<Module<*, *>>()
	override val services: ObservableList<Service<ServiceConfig>> = FXCollections.observableArrayList()
	override val notifiers: ObservableList<Notifier<NotifierConfig>> = FXCollections.observableArrayList()

	override lateinit var applicationConfiguration: ApplicationConfiguration
	private val configuration: PublishSubject<ApplicationConfiguration> = PublishSubject.create<ApplicationConfiguration>()
	private val configListeners = mutableListOf<(ApplicationConfiguration) -> Unit>()

	init {
		LOGGER.info("Initializing with ${configFile.absolutePath} config file")
		modules.forEach { registerModule(it) }
		if (configFile.exists()) try {
			applicationConfiguration = mapper.readValue(configFile, ApplicationConfiguration::class.java)
			(applicationConfiguration.services as MutableMap<String, ServiceConfig?>)
					.filterValues { it == null }
					.keys
					.forEach { applicationConfiguration.services.remove(it) }
			(applicationConfiguration.notifiers as MutableMap<String, NotifierConfig?>)
					.filterValues { it == null }
					.keys
					.forEach { applicationConfiguration.notifiers.remove(it) }
		} catch (e: Exception) {
			LOGGER.error(e.message, e)
			applicationConfiguration = ApplicationConfiguration()
			configFile.copyTo(File(configFile.absolutePath + "-" + LocalDateTime.now().toString()))
		}
	}

	private fun registerModule(module: Module<*, *>) {
		LOGGER.info("Registring ${module::class.java.name} module")
		modules.add(module)
		when (module) {
			is ServiceModule<*, *> -> ServiceDeserializer.register(module.configClass)
			is NotifierModule<*, *> -> NotifierDeserializer.register(module.configClass)
		}
	}

	fun start() {
		LOGGER.info("Starting...")

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

		StatusCollector.status.sample(10, TimeUnit.SECONDS).subscribe { collector ->
			val removes = collector.items
					.mapNotNull { item -> applicationConfiguration.silenced[item.id]?.let { item to it } }
					.filter { (item, silenced) -> silenced.untilChanged && item.startedAt != silenced.lastChange }
					.map { (item, _) -> item.id }
			val changes = collector.items
					.filterNot { removes.contains(it.id) }
					.mapNotNull { item -> applicationConfiguration.silenced[item.id]?.let { item to it } }
					.filter { (item, silenced) -> item != silenced.item }
					.map { (item, silenced) -> item.id to silenced.copy(item = item, lastChange = item.startedAt) }
					.toMap()
			if (changes.isNotEmpty() || removes.isNotEmpty()) {
				applicationConfiguration.silenced.replaceAll { id, item -> changes[id] ?: item }
				removes.forEach { applicationConfiguration.silenced.remove(it) }
				saveConfig()
			}
		}

		configuration.sample(1, TimeUnit.SECONDS).subscribe { configuration ->
			val backupFile = File(configFile.absolutePath + "-" + LocalDateTime.now().toString())
			try {
				configListeners.forEach { it.invoke(configuration) }
				val tempFile = File(configFile.absolutePath + ".tmp")
				mapper.writeValue(tempFile, configuration)
				val success = (!backupFile.exists() || backupFile.delete())
						&& (configFile.parentFile.exists() || configFile.parentFile.mkdirs())
						&& configFile.renameTo(backupFile)
						&& tempFile.renameTo(configFile.absoluteFile)
				if (success) backupFile.delete()
				else LOGGER.error("Failed saving configuration to ${configFile.absolutePath} (backup ${backupFile})")
			} catch (e: Exception) {
				LOGGER.error(e.message, e)
			} finally {
				if (!configFile.exists() && backupFile.exists() && !backupFile.renameTo(configFile)) {
					LOGGER.error("Restoring ${backupFile} failed!")
				}
			}
		}
	}

	override fun add(module: Module<*, *>): ModuleInstanceInterface<*>? {
		return when (module) {
			is ServiceModule<*, *> -> module.createController(this, applicationConfiguration)?.also { controller ->
				services.add(controller)
				services.sortBy { controller.config.priority }
			}
			is NotifierModule<*, *> -> module.createController(this, applicationConfiguration)?.also { controller ->
				notifiers.add(controller)
				notifiers.sortBy { controller.config.name }
			}
			else -> null
		}?.also { (it as? ModuleInstanceInterface<*>)?.initialize() }
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

	override fun saveConfig() {
		configuration.onNext(applicationConfiguration.copy())
	}
}