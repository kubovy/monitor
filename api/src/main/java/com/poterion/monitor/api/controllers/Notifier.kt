package com.poterion.monitor.api.controllers

import com.poterion.monitor.api.ui.Icon
import com.poterion.monitor.api.ui.NavigationItem
import com.poterion.monitor.api.ui.TableSettingsPlugin
import com.poterion.monitor.data.notifiers.NotifierAction
import com.poterion.monitor.data.notifiers.NotifierConfig
import com.poterion.monitor.data.services.ServiceConfig
import javafx.scene.Node
import javafx.scene.Parent

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
abstract class Notifier<out Config : NotifierConfig>(config: Config) : AbstractModule<Config>(config) {

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

	private val String.getService: ServiceConfig?
		get() = controller.applicationConfiguration.services[this]

	private val String.getServiceIcon: Icon?
		get() = getService
				.let { conf -> controller.modules.find { module -> module.configClass == conf?.let { it::class } } }
				?.icon

	private var serviceTableSettingsPlugin: TableSettingsPlugin<String>? = null
		get() {
			if (field == null) field = TableSettingsPlugin(
					tableName = "serviceTable",
					buttonText = "Add service",
					controller = controller,
					config = config,
					createItem = { "" },
					items = config.services,
					displayName = { getService?.name ?: "" },
					columnDefinitions = listOf(
							TableSettingsPlugin.ColumnDefinition(
									name = "Service Name",
									getter = { this },
									mutator = { it },
									title = { getService?.name ?: "" },
									icon = { getServiceIcon },
									initialValue = "",
									options = {
										controller
												.applicationConfiguration
												.services
												.filterKeys { id -> !config.services.contains(id) }
												.values
												.sortedBy { it.name }
												.map { it.uuid }
									})),
					comparator = compareBy { it.getService?.name })
			return field
		}

	override val configurationRowsLast: List<Pair<Node, Node>>
		get() = super.configurationRowsLast + listOfNotNull(serviceTableSettingsPlugin?.rowNewItem)

	override val configurationAddition: List<Parent>
		get() = super.configurationAddition + listOfNotNull(serviceTableSettingsPlugin?.vbox)

	/**
	 * Action execution.
	 *
	 * @param action Notifier action to execute.
	 */
	abstract fun execute(action: NotifierAction)
}