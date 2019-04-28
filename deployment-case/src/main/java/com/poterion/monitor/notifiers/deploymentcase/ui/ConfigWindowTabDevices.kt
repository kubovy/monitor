package com.poterion.monitor.notifiers.deploymentcase.ui

import com.poterion.monitor.notifiers.deploymentcase.api.ConfigurationContributer
import com.poterion.monitor.notifiers.deploymentcase.control.toData
import com.poterion.monitor.notifiers.deploymentcase.data.*
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.control.TableCell
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
import javafx.scene.control.cell.PropertyValueFactory
import org.slf4j.Logger
import org.slf4j.LoggerFactory


class ConfigWindowTabDevices : ConfigurationContributer {
    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(ConfigWindowTabDevices::class.java)

        internal fun getRoot(saveConfig: () -> Unit): Pair<ConfigWindowTabDevices, Parent> =
                FXMLLoader(ConfigWindowTabDevices::class.java.getResource("config-window-tab-devices.fxml"))
                        .let { it.load<Parent>() to it.getController<ConfigWindowTabDevices>() }
                        .let { (root, ctrl) ->
                            ctrl.saveConfig = saveConfig
                            ctrl to root
                        }

    }

    @FXML
    private lateinit var tableDevices: TableView<Device>
    @FXML
    private lateinit var columnDevicesName: TableColumn<Device, String>
    @FXML
    private lateinit var columnDevicesKind: TableColumn<Device, DeviceKind>
    @FXML
    private lateinit var columnDevicesKey: TableColumn<Device, String>
    @FXML
    private lateinit var columnDevicesId: TableColumn<Device, String>

    private lateinit var saveConfig: () -> Unit

    @FXML
    fun initialize() {
        columnDevicesName.initEditableText("name", { null }, { device, name -> device.name = name }, { saveConfig() })
        columnDevicesKind.init("kind") { _, kind -> kind?.description ?: "" }
        columnDevicesKey.init("key") { _, key -> key ?: "" }
        columnDevicesId.init("key") { device, _ -> device?.toData()?.let { "0x%02X".format(it) } ?: "" }
        //tableDevices.focusedProperty().addListener { _, _, focused -> if (!focused) saveConfig() }
        tableDevices.sortOrder.setAll(columnDevicesName)
        SharedUiData.devicesProperty.bindContent(tableDevices.items)
    }

    override fun notifyNewConfiguration(configuration: Configuration) {
        tableDevices.items.clear()
        tableDevices.items.addAll((0..39)
                .map { key -> key to configuration.devices.find { it.kind == DeviceKind.MCP23017 && it.key == "${key}" }?.name }
                .map { (key, name) -> Device(name ?: "", DeviceKind.MCP23017, "${key}") })
        tableDevices.items.addAll((0..31)
                .map { key -> key to configuration.devices.find { it.kind == DeviceKind.WS281x && it.key == "${key}" }?.name }
                .map { (key, name) ->
                    Device(name ?: "", DeviceKind.WS281x, "${key}")
                })
        tableDevices.items.addAll(arrayOf("connected", "trigger")
                .map { key -> key to configuration.devices.find { it.kind == DeviceKind.BLUETOOTH && it.key == key }?.name }
                .map { (key, name) -> Device(name ?: "bt_${key}", DeviceKind.BLUETOOTH, key) })
        tableDevices.items.addAll(arrayOf("message", "backlight", "reset", "clear")
                .map { key -> key to configuration.devices.find { it.kind == DeviceKind.LCD && it.key == key }?.name }
                .map { (key, name) -> Device(name ?: "lcd_${key}", DeviceKind.LCD, key) })
    }

    override fun updateConfiguration(config: DeploymentCaseConfig, configuration: Configuration?) {
        configuration?.devices = tableDevices.items.filterNotNull()
    }

    private fun <Entry, Type> TableColumn<Entry, Type>.init(propertyName: String, transformer: (Entry?, Type?) -> String) {
        cellValueFactory = PropertyValueFactory<Entry, Type>(propertyName)
        setCellFactory {
            object : TableCell<Entry, Type>() {
                override fun updateItem(item: Type?, empty: Boolean) {
                    super.updateItem(item, empty)
                    text = transformer(tableRow.item as? Entry, item)
                }
            }
        }
    }
}