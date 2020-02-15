package com.poterion.monitor.control

import com.poterion.monitor.api.StatusCollector
import com.poterion.monitor.api.controllers.ControllerInterface
import com.poterion.monitor.api.controllers.ModuleInstanceInterface
import com.poterion.monitor.api.controllers.Notifier
import com.poterion.monitor.api.controllers.Service
import com.poterion.monitor.api.modules.Module
import com.poterion.monitor.api.modules.NotifierModule
import com.poterion.monitor.api.modules.ServiceModule
import com.poterion.monitor.api.objectMapper
import com.poterion.monitor.data.ModuleDeserializer
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
class ApplicationController(override val stage: Stage, configFileName: String, vararg modules: Module<*, *>):
		ControllerInterface {
	companion object {
		private val LOGGER: Logger = LoggerFactory.getLogger(ApplicationController::class.java)
	}

	private val configFile = File(configFileName)
	override val modules = mutableListOf<Module<*, *>>()
	override val services: ObservableList<Service<ServiceConfig>> = FXCollections.observableArrayList()
	override val notifiers: ObservableList<Notifier<NotifierConfig>> = FXCollections.observableArrayList()

	override var applicationConfiguration: ApplicationConfiguration = ApplicationConfiguration()
	private val configuration: PublishSubject<ApplicationConfiguration> = PublishSubject
			.create<ApplicationConfiguration>()
	private val configListeners = mutableListOf<(ApplicationConfiguration) -> Unit>()

	init {
		LOGGER.info("Initializing with ${configFile.absolutePath} config file")
		modules.forEach { registerModule(it) }
		if (configFile.exists()) try {
			applicationConfiguration = objectMapper.readValue(configFile, ApplicationConfiguration::class.java)
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
		ModuleDeserializer.register(module.configClass)
		when (module) {
			is ServiceModule<*, *> -> ServiceDeserializer.register(module.configClass)
			is NotifierModule<*, *> -> NotifierDeserializer.register(module.configClass)
		}
	}

	fun start() {
		LOGGER.info("Starting...")

		for (module in modules) {
			LOGGER.info("Loading ${module.title} module...")
			module.loadControllers(this, applicationConfiguration).forEach { ctrl ->
				when (ctrl) {
					is Service<*> -> services.add(ctrl as Service<ServiceConfig>)
					is Notifier<*> -> notifiers.add(ctrl as Notifier<NotifierConfig>)
				}
			}
		}
		services.sortBy { it.config.priority }
		notifiers.sortBy { it.config.name }

		(services + notifiers).forEach { it.initialize() }

		ControllerWorker.start(services)

		stage.setOnCloseRequest { if (notifiers.map { it.exitRequest }.reduce { acc, b -> acc && b }) quit() }

		StatusCollector.status.sample(10, TimeUnit.SECONDS, true).subscribe { collector ->
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

		configuration.sample(1, TimeUnit.SECONDS, true).subscribe { configuration ->
			val backupFile = File(configFile.absolutePath + "-" + LocalDateTime.now().toString())
			try {
				configListeners.forEach { it.invoke(configuration) }
				val tempFile = File(configFile.absolutePath + ".tmp")
				var success = tempFile.parentFile.exists() || tempFile.parentFile.mkdirs()
				objectMapper.writeValue(tempFile, configuration)
				success = success
						&& (!backupFile.exists() || backupFile.delete())
						&& (configFile.parentFile.exists() || configFile.parentFile.mkdirs())
						&& (!configFile.exists() || configFile.renameTo(backupFile))
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
		return module.createController(this, applicationConfiguration)?.let { add(it) }
	}

	override fun add(controller: ModuleInstanceInterface<*>): ModuleInstanceInterface<*>? {
		when (controller) {
			is Service<*> -> {
				applicationConfiguration.services[controller.config.uuid] = controller.config
				services.add(controller as Service<ServiceConfig>)
			}
			is Notifier<*> -> {
				applicationConfiguration.notifiers[controller.config.uuid] = controller.config
				notifiers.add(controller as Notifier<NotifierConfig>)
			}
			else -> return null
		}

		controller.initialize()
		return controller
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