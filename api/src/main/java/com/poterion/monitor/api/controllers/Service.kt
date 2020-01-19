package com.poterion.monitor.api.controllers

import com.poterion.monitor.api.ui.NavigationItem
import com.poterion.monitor.data.StatusItem
import com.poterion.monitor.data.services.ServiceConfig
import retrofit2.Retrofit

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
abstract class Service<out Config : ServiceConfig>(final override val config: Config) : ModuleInstanceInterface<Config> {
	override val navigationRoot: NavigationItem
		get() = NavigationItem(
				title = config.name,
				icon = definition.icon,
				sub = mutableListOf())

	val http = HttpServiceModule(config)

	protected val retrofit: Retrofit?
		get() = http.retrofit

	/**
	 * Check implementation.
	 *
	 * @param updater Status updater callback
	 */
	abstract fun check(updater: (Collection<StatusItem>) -> Unit)
}