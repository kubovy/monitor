package com.poterion.monitor.api.controllers

import com.poterion.monitor.api.ui.NavigationItem
import com.poterion.monitor.data.StatusItem
import com.poterion.monitor.data.services.ServiceConfig
import retrofit2.Retrofit

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
abstract class Service<out Config: ServiceConfig>(config: Config): AbstractModule<Config>(config) {
	override val navigationRoot: NavigationItem
		get() = NavigationItem(
				title = config.name,
				icon = definition.icon,
				sub = mutableListOf())

	var http: HttpServiceModule? = null
		get() {
			if (field == null) field = HttpServiceModule(controller.applicationConfiguration, config)
			return field
		}
		private set

	var refresh: Boolean = false

	protected val retrofit: Retrofit?
		get() = http?.retrofit

	/**
	 * Check implementation.
	 *
	 * @param updater Status updater callback
	 */
	abstract fun check(updater: (Collection<StatusItem>) -> Unit)
}