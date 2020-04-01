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
package com.poterion.monitor.notifiers.devopslight.control

import com.poterion.communication.serial.MessageKind
import com.poterion.communication.serial.communicator.BluetoothCommunicator
import com.poterion.communication.serial.communicator.Channel
import com.poterion.communication.serial.communicator.USBCommunicator
import com.poterion.communication.serial.extensions.RgbLightCommunicatorExtension
import com.poterion.communication.serial.listeners.CommunicatorListener
import com.poterion.communication.serial.payload.DeviceCapabilities
import com.poterion.communication.serial.payload.RgbLightConfiguration
import com.poterion.communication.serial.scanner.ScannerListener
import com.poterion.communication.serial.scanner.USBScanner
import com.poterion.monitor.api.CommonIcon
import com.poterion.monitor.api.StatusCollector
import com.poterion.monitor.api.controllers.ControllerInterface
import com.poterion.monitor.api.controllers.ModuleInstanceInterface
import com.poterion.monitor.api.controllers.Notifier
import com.poterion.monitor.api.modules.Module
import com.poterion.monitor.api.topStatus
import com.poterion.monitor.api.topStatuses
import com.poterion.monitor.api.ui.NavigationItem
import com.poterion.monitor.data.Status
import com.poterion.monitor.data.StatusItem
import com.poterion.monitor.notifiers.devopslight.DevOpsLight
import com.poterion.monitor.notifiers.devopslight.DevOpsLightIcon
import com.poterion.monitor.notifiers.devopslight.data.DevOpsLightConfig
import com.poterion.monitor.notifiers.devopslight.data.DevOpsLightItemConfig
import com.poterion.monitor.notifiers.devopslight.ui.ConfigWindowController
import com.poterion.utils.javafx.Icon
import com.poterion.utils.javafx.mapped
import com.poterion.utils.kotlin.noop
import com.poterion.utils.kotlin.setAll
import javafx.application.Platform
import javafx.beans.property.BooleanProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.control.CheckBox
import javafx.scene.control.ChoiceBox
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.util.StringConverter
import jssc.SerialPortList
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
class DevOpsLightNotifier(override val controller: ControllerInterface, config: DevOpsLightConfig) :
		Notifier<DevOpsLightConfig>(config), CommunicatorListener {

	companion object {
		val LOGGER: Logger = LoggerFactory.getLogger(DevOpsLightNotifier::class.java)
	}

	override val definition: Module<DevOpsLightConfig, ModuleInstanceInterface<DevOpsLightConfig>> = DevOpsLight
	val bluetoothCommunicator = RgbLightCommunicatorExtension(BluetoothCommunicator())
	val usbCommunicator = RgbLightCommunicatorExtension(USBCommunicator())
	private val communicator: RgbLightCommunicatorExtension<*>?
		get() = when {
			usbCommunicator.isConnected -> usbCommunicator
			bluetoothCommunicator.isConnected -> bluetoothCommunicator
			config.usbPort.isNotBlank() -> usbCommunicator
			config.deviceAddress.isNotBlank() -> bluetoothCommunicator
			else -> null
		}

	private val usbScannerListener = object : ScannerListener<USBCommunicator.Descriptor> {
		override fun onAvailableDevicesChanged(channel: Channel, devices: Collection<USBCommunicator.Descriptor>) {
			Platform.runLater { updateSerialPorts(devices) }
		}
	}

	private val usbPortList = FXCollections.observableArrayList(listOf(""))
	private val usbPortConnectedMap = mutableMapOf<String, Boolean>()
	private var lastState = emptyList<RgbLightConfiguration>()

	private val connectedProperty: BooleanProperty = SimpleBooleanProperty(bluetoothCommunicator.isConnected)

	internal val titleComparator: Comparator<String> = Comparator { n1, n2 ->
		when {
			n1 == "Default" && n2 != "Default" -> -1
			n1 != "Default" && n2 == "Default" -> 1
			else -> compareValues(n1, n2)
		}
	}

	private val itemConfigComparator: Comparator<DevOpsLightItemConfig> = Comparator { c1, c2 ->
		titleComparator.compare(c1.title, c2.title)
	}

	private val DevOpsLightItemConfig.title: String
		get() = controller.applicationConfiguration.serviceMap[id]
				?.name
				?.let { name -> "${name}${subId?.let { " (${it})" } ?: ""}" }
				?: "Default"

	private var _navigationRoot: NavigationItem? = null
	override val navigationRoot: NavigationItem
		get() = _navigationRoot ?: NavigationItem(
				uuid = config.uuid,
				titleProperty = config.nameProperty,
				icon = definition.icon,
				sub = listOf(
						NavigationItem(
								title = "Enabled",
								checkedProperty = config.enabledProperty.asObject()),
						NavigationItem(
								titleProperty = SimpleStringProperty().also { property ->
									property.bind(connectedProperty.asObject()
											.mapped { if (it == true) "Disconnect" else "Connect" })
								},
								checkedProperty = connectedProperty.asObject(),
								iconProperty = SimpleObjectProperty<Icon?>().also { property ->
									property.bind(connectedProperty.asObject().mapped {
										if (it == true) DevOpsLightIcon.CONNECTED else DevOpsLightIcon.DISCONNECTED
									})
								},
								action = {
									if (connectedProperty.get()) communicator?.disconnect()
									else communicator?.connect()
								}),
						NavigationItem(),
						*config.items.sortedWith(itemConfigComparator).map {
							NavigationItem(title = it.title, sub = it.getSubMenu())
						}.toTypedArray()
				))
				.also { _navigationRoot = it }

	private fun DevOpsLightItemConfig.getSubMenu() = listOf(
			Triple("None", CommonIcon.PRIORITY_NONE, statusNone),
			Triple("Unknown", CommonIcon.STATUS_UNKNOWN, statusUnknown),
			Triple("OK", CommonIcon.STATUS_OK, statusOk),
			Triple("Info", CommonIcon.STATUS_INFO, statusInfo),
			Triple("Notification", CommonIcon.STATUS_NOTIFICATION, statusNotification),
			Triple("Connection Error", CommonIcon.BROKEN_LINK, statusConnectionError),
			Triple("Service Error", CommonIcon.UNAVAILABLE, statusServiceError),
			Triple("Warning", CommonIcon.STATUS_WARNING, statusWarning),
			Triple("Error", CommonIcon.STATUS_ERROR, statusError),
			Triple("Fatal", CommonIcon.STATUS_FATAL, statusFatal))
			.filter { (_, _, lights) -> lights.isNotEmpty() }
			.map { (t, i, l) -> NavigationItem(title = t, icon = i, action = { changeLights(l) }) }

	override val configurationRows: List<Pair<Node, Node>>
		get() = super.configurationRows + listOf(
				Label("Bluetooth Address").apply {
					maxWidth = Double.MAX_VALUE
					maxHeight = Double.MAX_VALUE
					alignment = Pos.CENTER_RIGHT
				} to TextField(config.deviceAddress).apply {
					maxWidth = 150.0
					promptText = "00:00:00:00:00:00"
					textProperty().bindBidirectional(config.deviceAddressProperty)
					focusedProperty().addListener { _, _, focused -> if (!focused) controller.saveConfig() }
				},
				Label("USB Port").apply {
					maxWidth = Double.MAX_VALUE
					maxHeight = Double.MAX_VALUE
					alignment = Pos.CENTER_RIGHT
				} to ChoiceBox(usbPortList.sorted()).apply {
					converter = object : StringConverter<String>() {
						override fun toString(port: String?): String? = usbPortConnectedMap.getOrDefault(port, false)
								.let { connected -> "${port}${if (!connected) " (disconnected)" else ""}" }

						override fun fromString(string: String?): String? = string
								?.let { "(.*)( \\(disconnected\\))?".toRegex() }
								?.matchEntire(string)
								?.groupValues
								?.takeIf { it.isNotEmpty() }
								?.get(0)
					}
					valueProperty().bindBidirectional(config.usbPortProperty)
					selectionModel.selectedItemProperty().addListener { _, _, _ -> controller.saveConfig() }
				},
				Label("On demand connection").apply {
					maxWidth = Double.MAX_VALUE
					maxHeight = Double.MAX_VALUE
					alignment = Pos.CENTER_RIGHT
				} to CheckBox().apply {
					maxHeight = Double.MAX_VALUE
					selectedProperty().bindBidirectional(config.onDemandConnectionProperty)
					selectedProperty().addListener { _, _, _ -> controller.saveConfig() }
				})

	private var _configurationTab: Pair<Parent, ConfigWindowController>? = null
		get() {
			if (field == null) field = ConfigWindowController.getRoot(config, this)
			return field
		}

	private val configurationTabController: ConfigWindowController?
		get() = _configurationTab?.second

	override val configurationTab: Parent?
		get() = _configurationTab?.first

	override fun initialize() {
		super.initialize()
		updateSerialPorts(SerialPortList.getPortNames().map { USBCommunicator.Descriptor(it) })
		USBScanner.register(usbScannerListener)

		bluetoothCommunicator.connectionDescriptor = BluetoothCommunicator.Descriptor(config.deviceAddress, 6)
		usbCommunicator.connectionDescriptor = USBCommunicator.Descriptor(config.usbPort)
		bluetoothCommunicator.register(this)
		usbCommunicator.register(this)

		config.deviceAddressProperty.addListener { _, _, deviceAddress ->
			bluetoothCommunicator.connectionDescriptor = BluetoothCommunicator.Descriptor(deviceAddress, 6)
			if (bluetoothCommunicator.isConnected) bluetoothCommunicator.connect()
		}

		config.usbPortProperty.addListener { _, _, usbPort ->
			if ("[0-9A-Fa-f]{2}(:[0-9A-Fa-f]{2}){5}".toRegex().matches(usbPort)) {
				usbCommunicator.connectionDescriptor = USBCommunicator.Descriptor(usbPort)
				if (usbCommunicator.isConnected) usbCommunicator.connect()
			}
		}

		if (config.enabled && !config.onDemandConnection) {
			bluetoothCommunicator.connect()
			usbCommunicator.connect()
		}

		config.enabledProperty.addListener { _, _, enabled ->
			if (enabled) {
				if (!config.onDemandConnection) {
					bluetoothCommunicator.connect(BluetoothCommunicator.Descriptor(config.deviceAddress, 6))
					usbCommunicator.connect(USBCommunicator.Descriptor(config.usbPort))
				}
				changeLights(lastState)
			} else {
				bluetoothCommunicator.disconnect()
				usbCommunicator.disconnect()
				changeLights(listOf(RgbLightConfiguration()))
			}
		}
	}

	override fun onConnect(channel: Channel) {
		if (channel == communicator?.channel) connectedProperty.set(true)
	}

	override fun onConnectionReady(channel: Channel) = noop()

	override fun onConnecting(channel: Channel) {
		if (channel == communicator?.channel) connectedProperty.set(false)
	}

	override fun onDisconnect(channel: Channel) {
		if (channel == communicator?.channel) connectedProperty.set(false)
	}

	override fun onMessageReceived(channel: Channel, message: IntArray) = noop()

	override fun onMessagePrepare(channel: Channel) = noop()

	override fun onMessageSent(channel: Channel, message: IntArray, remaining: Int) = noop()

	override fun onDeviceCapabilitiesChanged(channel: Channel, capabilities: DeviceCapabilities) = noop()

	override fun onDeviceNameChanged(channel: Channel, name: String) = noop()

	/**
	 * @see MessageKind.LIGHT
	 */
	internal fun changeLights(lightConfigurations: List<RgbLightConfiguration>?) {
		if (lightConfigurations != null) {
			lightConfigurations.forEachIndexed { index, lightConfiguration ->
				communicator?.sendRgbLightSet(0, lightConfiguration, index == 0)
			}
			configurationTabController?.changeLights(lightConfigurations)
		}
	}

	override fun update() {
		val lights = if (config.combineMultipleServices) StatusCollector.items
				.topStatuses(controller.applicationConfiguration.silencedMap.keys, config.minPriority,
						config.minStatus, config.services)
				.also { LOGGER.debug("${if (config.enabled) "Changing" else "Skipping"}: ${it}") }
				.mapNotNull { item -> item.toLightConfig(item.status)?.let { it to item.status } }
				.distinctBy { (config, _) -> config.id to config.subId }
				.map { (config, status) -> config.toLights(status) }
				.flatten()
				.takeIf { it.isNotEmpty() }
				?: config.items.firstOrNull { it.id == "" }?.statusOk
		else StatusCollector.items
				.topStatus(controller.applicationConfiguration.silencedMap.keys, config.minPriority,
						config.minStatus, config.services)
				.also { LOGGER.debug("${if (config.enabled) "Changing" else "Skipping"}: ${it}") }
				?.let { it.toLightConfig(it.status)?.toLights(it.status) }
				?: config.items.firstOrNull { it.id == "" }?.statusOk

		lights?.also { lastState = it }
				?.takeIf { config.enabled }
				?.also { changeLights(it) }
	}

	override fun shutdown() = changeLights(listOf(RgbLightConfiguration()))

	private fun updateSerialPorts(devices: Collection<USBCommunicator.Descriptor>) {
		val portNames = (listOf("") + devices.map { it.portName }).toMutableList()
		val selectedNotPresent = !portNames.contains(config.usbPort)
		val selectedChanged = selectedNotPresent
				|| usbPortConnectedMap.getOrDefault(config.usbPort, false) != portNames.contains(config.usbPort)
		usbPortConnectedMap.setAll(portNames.map { it to true }.toMap())

		if (selectedNotPresent) portNames.add(config.usbPort)
		if (selectedChanged) portNames.add("CHANGE")

		val removedPorts = usbPortList - portNames
		val addedPorts = portNames - usbPortList
		usbPortList.addAll(addedPorts)
		usbPortList.removeAll(removedPorts + listOf("CHANGE"))
	}

	private fun StatusItem.toLightConfig(status: Status): DevOpsLightItemConfig? = config.items
			.find { it.id == serviceId && configIds.contains(it.subId) }?.takeIf { it.toLights(status).isNotEmpty() }
			?: config.items.find { it.id == serviceId && it.subId == null }?.takeIf { it.toLights(status).isNotEmpty() }
			?: config.items.find { it.id == null || it.id == "" }?.takeIf { it.toLights(status).isNotEmpty() }

	private fun DevOpsLightItemConfig.toLights(status: Status): List<RgbLightConfiguration> {
		return when (status) {
			Status.NONE, Status.OFF -> statusNone
			Status.UNKNOWN -> statusUnknown
			Status.OK -> statusOk
			Status.INFO -> statusInfo
			Status.NOTIFICATION -> statusNotification
			Status.CONNECTION_ERROR -> statusConnectionError
			Status.SERVICE_ERROR -> statusServiceError
			Status.WARNING -> statusWarning
			Status.ERROR -> statusError
			Status.FATAL -> statusFatal
			else -> statusNone
		}
	}
}