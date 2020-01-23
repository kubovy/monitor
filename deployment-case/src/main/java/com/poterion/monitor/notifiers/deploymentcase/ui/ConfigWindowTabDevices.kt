package com.poterion.monitor.notifiers.deploymentcase.ui

import com.poterion.utils.javafx.autoFitTable
import com.poterion.monitor.notifiers.deploymentcase.control.toData
import com.poterion.monitor.notifiers.deploymentcase.data.Device
import com.poterion.monitor.notifiers.deploymentcase.data.DeviceKind
import com.poterion.monitor.notifiers.deploymentcase.data.SharedUiData
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
		SharedUiData.devicesProperty.addListener { _, _, devices ->
			tableDevices.items = devices
			tableDevices.items.addListener(ListChangeListener { tableDevices.refresh() })
			tableDevices.refresh()
			tableDevices.autoFitTable()
		}
		tableDevices.items = SharedUiData.devices
		tableDevices.items.addListener(ListChangeListener { tableDevices.refresh() })
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