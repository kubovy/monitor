package com.poterion.monitor.api.modules

import com.poterion.monitor.api.controllers.ControllerInterface
import com.poterion.monitor.api.ui.Icon
import com.poterion.monitor.data.ApplicationConfiguration
import kotlin.reflect.KClass

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
interface Module<out Configuration : Any, out Controller : Any> {
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