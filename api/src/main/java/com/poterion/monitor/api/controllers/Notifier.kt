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
import com.poterion.monitor.api.utils.toIcon
import com.poterion.monitor.data.Priority
import com.poterion.monitor.data.Status
import com.poterion.monitor.data.notifiers.NotifierConfig
import com.poterion.monitor.data.notifiers.NotifierServiceReference
import com.poterion.monitor.data.services.ServiceConfig
import com.poterion.monitor.data.services.ServiceSubConfig
import com.poterion.utils.javafx.Icon
import com.poterion.utils.javafx.bindFiltered
import com.poterion.utils.javafx.mapped
import com.poterion.utils.javafx.toObservableList
import com.poterion.utils.kotlin.noop
import javafx.collections.FXCollections
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.scene.Node
import javafx.scene.Parent

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
abstract class Notifier<out Config : NotifierConfig>(config: Config) : AbstractModule<Config>(config) {

	override val navigationRoot: NavigationItem
		get() = NavigationItem(
				titleProperty = config.nameProperty,
				icon = definition.icon,
				sub = listOf(
						NavigationItem(
								title = "Enabled",
								checkedProperty = config.enabledProperty.asObject(),
								action = { config.enabled = !config.enabled })
				))

	private val String.getService: ServiceConfig<out ServiceSubConfig>?
		get() = controller.applicationConfiguration.serviceMap[this]

	private val String.getServiceIcon: Icon?
		get() = getService
				.let { conf -> controller.modules.find { module -> module.configClass == conf?.let { it::class } } }
				?.icon

	var selectedServices: ObservableList<Service<ServiceConfig<out ServiceSubConfig>>> = FXCollections
			.emptyObservableList()
		get() {
			if (field == FXCollections.emptyObservableList<Service<ServiceConfig<out ServiceSubConfig>>>()) {
				field = controller
						.services
						.bindFiltered(config.services) { service ->
							config.services.isEmpty() || config.services.any { it.uuid == service.config.uuid }
						}
			}
			return field
		}
		private set

	private var serviceTableSettingsPlugin: TableSettingsPlugin<NotifierServiceReference>? = null
		get() {
			if (field == null) field = TableSettingsPlugin(
					tableName = "serviceTable",
					buttonText = "Add service",
					controller = controller,
					config = config,
					createItem = { NotifierServiceReference() },
					items = config.services,
					displayName = { uuid?.let { controller.applicationConfiguration.serviceMap[it] }?.name ?: "" },
					columnDefinitions = listOf(
							TableSettingsPlugin.ColumnDefinition(
									name = "Service Name",
									property = { uuidProperty },
									title = { controller.applicationConfiguration.serviceMap[this]?.name ?: "" },
									icon = { getServiceIcon },
									initialValue = null,
									options = controller
											.applicationConfiguration
											.services
											.sorted(compareBy { it.name })
											.mapped { it?.uuid }
											.bindFiltered(config.services) { uuid ->
												config.services.isEmpty() || !config.services.any { it.uuid == uuid }
											}
							),
							TableSettingsPlugin.ColumnDefinition(
									name = "Minimum Priority",
									property = { minPriorityProperty },
									initialValue = Priority.NONE,
									isEditable = true,
									icon = { toIcon() },
									options = Priority.values().toObservableList()),
							TableSettingsPlugin.ColumnDefinition(
									name = "Minimum Status",
									property = { minStatusProperty },
									initialValue = Status.NONE,
									isEditable = true,
									icon = { toIcon() },
									options = Status.values().toObservableList())),
					comparator = compareBy { controller.applicationConfiguration.serviceMap[it.uuid]?.name ?: "" },
					newItemValidator = { reference ->
						!config.services.map { it.uuid }.contains(reference.uuid) && reference.uuid != null
					},
					onSave = this::onServicesChanged)
			return field
		}

	override val configurationRowsLast: List<Pair<Node, Node>>
		get() = super.configurationRowsLast + listOfNotNull(serviceTableSettingsPlugin?.rowNewItem)

	override val configurationAddition: List<Parent>
		get() = super.configurationAddition + listOfNotNull(serviceTableSettingsPlugin?.vbox)

	override fun initialize() {
		super.initialize()
		controller.applicationConfiguration.services.addListener(ListChangeListener { change ->
			while (change.next()) if (change.wasRemoved()) config.services
					.removeAll { service -> change.removed.map { it.uuid }.contains(service.uuid) }
		})
		config.enabledProperty.addListener { _, _, _ -> controller.saveConfig() }
	}

	open fun onServicesChanged() = noop()

	abstract fun update()

	abstract fun shutdown()
}