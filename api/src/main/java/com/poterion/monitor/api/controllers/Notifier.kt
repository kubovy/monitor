package com.poterion.monitor.api.controllers

import com.poterion.monitor.api.ui.NavigationItem
import com.poterion.monitor.data.notifiers.NotifierAction
import com.poterion.monitor.data.notifiers.NotifierConfig
import dorkbox.systemTray.MenuItem

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
abstract class Notifier<out Config : NotifierConfig>(override val config: Config) : ModuleInterface<Config> {

	override val navigationRoot: NavigationItem
		get() = NavigationItem(
				title = config.name,
				icon = icon,
				update = { entry, _ -> (entry as? MenuItem)?.text = config.name },
				sub = mutableListOf(
						NavigationItem(
								title = "Enabled",
								checked = config.enabled,
								update = { entry, _ -> (entry as? MenuItem)?.enabled = config.enabled },
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