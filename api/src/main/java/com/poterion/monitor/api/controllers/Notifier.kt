/******************************************************************************
 * Copyright (C) 2020 Jan Kubovy (jan@kubovy.eu)                              *
 *                                                                            *
 * This program is free software: you can redistribute it and/or modify       *
 * it under the terms of the GNU General Public License as published by       *
 * the Free Software Foundation, either version 3 of the License, or          *
 * (at your option) any later version.                                        *
 *                                                                            *
 * This program is distributed in the hope that it will be useful,            *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of             *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the              *
 * GNU General Public License for more details.                               *
 *                                                                            *
 * You should have received a copy of the GNU General Public License          *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.      *
 ******************************************************************************/
package com.poterion.monitor.api.controllers

import com.poterion.monitor.api.ui.NavigationItem
import com.poterion.monitor.api.ui.TableSettingsPlugin
import com.poterion.monitor.data.notifiers.NotifierAction
import com.poterion.monitor.data.notifiers.NotifierConfig
import com.poterion.monitor.data.services.ServiceConfig
import com.poterion.utils.javafx.Icon
import com.poterion.utils.kotlin.noop
import javafx.scene.Node
import javafx.scene.Parent

/**
 * @author Jan Kubovy [jan@kubovy.eu]
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

	val selectedServices: Collection<Service<ServiceConfig>>
		get() = controller.services.filter { config.services.isEmpty() || config.services.contains(it.config.uuid) }

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
					comparator = compareBy { it.getService?.name },
					onSave = this::onServicesChanged)
			return field
		}

	override val configurationRowsLast: List<Pair<Node, Node>>
		get() = super.configurationRowsLast + listOfNotNull(serviceTableSettingsPlugin?.rowNewItem)

	override val configurationAddition: List<Parent>
		get() = super.configurationAddition + listOfNotNull(serviceTableSettingsPlugin?.vbox)


	open fun onServicesChanged() = noop()

	/**
	 * Action execution.
	 *
	 * @param action Notifier action to execute.
	 */
	abstract fun execute(action: NotifierAction)
}