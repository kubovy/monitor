package com.poterion.monitor.api.controllers

import com.poterion.monitor.api.ui.NavigationItem
import com.poterion.monitor.data.notifiers.NotifierAction
import com.poterion.monitor.data.notifiers.NotifierConfig

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
abstract class Notifier<out Config : NotifierConfig>(override val config: Config) : ModuleInstanceInterface<Config> {

	override val navigationRoot: NavigationItem
		get() = NavigationItem(
				title = config.name,
				icon = definition.icon,
				sub = mutableListOf(
						NavigationItem(
								title = "Enabled",
								checked = config.enabled,
								action = {
									execute(NotifierAction.TOGGLE)
									if (!config.enabled) execute(NotifierAction.SHUTDOWN)
								})
				))

	/**
	 * Action execution.
	 *
	 * @param action Notifier action to execute.
	 */
	abstract fun execute(action: NotifierAction)
}