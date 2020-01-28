package com.poterion.monitor.api.modules

import com.poterion.monitor.api.controllers.ControllerInterface
import com.poterion.monitor.api.controllers.ModuleInstanceInterface
import com.poterion.monitor.data.ModuleConfig
import com.poterion.monitor.data.data.ApplicationConfiguration
import com.poterion.utils.javafx.Icon
import kotlin.reflect.KClass

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
interface Module<out Configuration : ModuleConfig, out Controller : ModuleInstanceInterface<Configuration>> {
	/** Configuration class */
	val configClass: KClass<out Configuration>

	/** Title */
	val title: String

	/** Module icon shown in the context menu of the tray. */
	val icon: Icon

	/** Exactly one module needs to be configured */
	val singleton: Boolean
		get() = false

	/**
	 * Create a new UI controller.
	 *
	 * @param controller Controller
	 * @param applicationConfiguration Configuration
	 */
	fun createController(controller: ControllerInterface, applicationConfiguration: ApplicationConfiguration): Controller? = null

	/**
	 * Load all UI controllers from configuration.
	 *
	 * @param controller Controller
	 * @param applicationConfiguration Configuration
	 */
	fun loadControllers(controller: ControllerInterface, applicationConfiguration: ApplicationConfiguration): Collection<Controller>
}