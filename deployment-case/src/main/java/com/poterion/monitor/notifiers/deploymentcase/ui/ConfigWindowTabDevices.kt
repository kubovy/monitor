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
package com.poterion.monitor.notifiers.deploymentcase.ui

import com.poterion.monitor.notifiers.deploymentcase.control.toData
import com.poterion.monitor.notifiers.deploymentcase.data.Device
import com.poterion.monitor.notifiers.deploymentcase.data.DeviceKind
import com.poterion.monitor.notifiers.deploymentcase.data.SharedUiData
import com.poterion.utils.javafx.autoFitTable
import javafx.collections.ListChangeListener
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.control.TableCell
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
import javafx.scene.control.cell.PropertyValueFactory

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
class ConfigWindowTabDevices {
	companion object {
		internal fun getRoot(saveConfig: () -> Unit): Pair<ConfigWindowTabDevices, Parent> =
				FXMLLoader(ConfigWindowTabDevices::class.java.getResource("config-window-tab-devices.fxml"))
						.let { it.load<Parent>() to it.getController<ConfigWindowTabDevices>() }
						.let { (root, ctrl) ->
							ctrl.saveConfig = saveConfig
							ctrl to root
						}

	}

	@FXML private lateinit var tableDevices: TableView<Device>
	@FXML private lateinit var columnDevicesName: TableColumn<Device, String>
	@FXML private lateinit var columnDevicesKind: TableColumn<Device, DeviceKind>
	@FXML private lateinit var columnDevicesKey: TableColumn<Device, String>
	@FXML private lateinit var columnDevicesId: TableColumn<Device, String>

	private lateinit var saveConfig: () -> Unit
	private val deviceChangeListener = ListChangeListener<Device> { tableDevices.refresh() }

	@FXML
	fun initialize() {
		columnDevicesName.initEditableText(
				propertyName = "name",
				styler = { null },
				isReadOnly = { false },
				itemUpdater = { device, name -> device.name = name },
				saveConfig = { saveConfig() })
		columnDevicesKind.init("kind") { _, kind -> kind?.description ?: "" }
		columnDevicesKey.init("key") { _, key -> key ?: "" }
		columnDevicesId.init("key") { device, _ -> device?.toData()?.let { "0x%02X".format(it) } ?: "" }
		tableDevices.sortOrder.setAll(columnDevicesName)
		SharedUiData.configurationProperty.addListener { _, old, new ->
			old?.devices?.removeListener(deviceChangeListener)
			new?.devices?.addListener(deviceChangeListener)
			tableDevices.items = new.devices
			tableDevices.refresh()
			tableDevices.autoFitTable()
		}
		SharedUiData.devices?.addListener(deviceChangeListener)
		tableDevices.items = SharedUiData.devices
		tableDevices.refresh()
		tableDevices.autoFitTable()
	}

	private fun <Entry, Type> TableColumn<Entry, Type>.init(propertyName: String, transformer: (Entry?, Type?) -> String) {
		cellValueFactory = PropertyValueFactory<Entry, Type>(propertyName)
		setCellFactory {
			object : TableCell<Entry, Type>() {
				override fun updateItem(item: Type?, empty: Boolean) {
					super.updateItem(item, empty)
					@Suppress("UNCHECKED_CAST")
					text = transformer(tableRow.item as? Entry, item)
				}
			}
		}
	}
}