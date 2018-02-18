package com.poterion.monitor.api.controllers

import com.poterion.monitor.api.ui.Item
import com.poterion.monitor.data.notifiers.NotifierAction
import com.poterion.monitor.data.notifiers.NotifierConfig

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
abstract class NotifierController<out Config : NotifierConfig>(val config: Config) : ModuleController {
	override val menuItem: Item
		get() = Item(title = config.name, sub = mutableListOf(
				Item(title = "Enabled", checked = config.enabled, update = { config.enabled }, action = {
					execute(NotifierAction.TOGGLE)
					if (!config.enabled) execute(NotifierAction.SHUTDOWN)
				})
		))

	abstract fun execute(action: NotifierAction, vararg params: String)
}