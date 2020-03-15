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
import com.poterion.communication.serial.payload.RgbLightConfiguration
import com.poterion.communication.serial.scanner.ScannerListener
import com.poterion.communication.serial.scanner.USBScanner
import com.poterion.monitor.api.CommonIcon
import com.poterion.monitor.api.StatusCollector
import com.poterion.monitor.api.controllers.ControllerInterface
import com.poterion.monitor.api.controllers.ModuleInstanceInterface
import com.poterion.monitor.api.controllers.Notifier
import com.poterion.monitor.api.modules.Module
import com.poterion.monitor.api.ui.NavigationItem
import com.poterion.monitor.data.Status
import com.poterion.monitor.data.StatusItem
import com.poterion.monitor.data.notifiers.NotifierAction
import com.poterion.monitor.notifiers.devopslight.DevOpsLight
import com.poterion.monitor.notifiers.devopslight.DevOpsLightIcon
import com.poterion.monitor.notifiers.devopslight.data.DevOpsLightConfig
import com.poterion.monitor.notifiers.devopslight.data.DevOpsLightItemConfig
import com.poterion.monitor.notifiers.devopslight.ui.ConfigWindowController
import com.poterion.utils.javafx.Icon
import com.poterion.utils.kotlin.setAll
import javafx.application.Platform
import javafx.collections.FXCollections
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.control.*
import javafx.scene.layout.HBox
import javafx.util.StringConverter
import jssc.SerialPortList
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
class DevOpsLightNotifier(override val controller: ControllerInterface, config: DevOpsLightConfig) :
		Notifier<DevOpsLightConfig>(config) {

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
			updateSerialPorts(devices)
		}
	}

	private val usbPortList = FXCollections.observableArrayList(listOf(""))
	private val usbPortConnectedMap = mutableMapOf<String, Boolean>()
	private var lastState = emptyList<RgbLightConfiguration>()

	private val connectedIcon: Icon
		get() = if (bluetoothCommunicator.isConnected || usbCommunicator.isConnected) DevOpsLightIcon.CONNECTED
		else DevOpsLightIcon.DISCONNECTED

	override val navigationRoot: NavigationItem
		get() = super.navigationRoot.apply {
			sub?.add(NavigationItem(
					title = "Reconnect",
					icon = connectedIcon,
					action = {
						bluetoothCommunicator.connect(BluetoothCommunicator.Descriptor(config.deviceAddress, 6))
						usbCommunicator.connect(USBCommunicator.Descriptor(config.usbPort))
					}))
			sub?.add(NavigationItem(title = null))

			config.items.sortedBy { it.id }.forEach { itemConfig ->
				val title = itemConfig.id.takeIf { it.isNotEmpty() }
				sub?.add(NavigationItem(
						title = title ?: "Default",
						icon = title?.let { DevOpsLightIcon.ITEM_NON_DEFAULT } ?: DevOpsLightIcon.ITEM_DEFAULT,
						sub = itemConfig.getSubMenu()))
			}
		}

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
					textProperty().addListener { _, _, address -> config.deviceAddress = address }
					focusedProperty().addListener { _, _, hasFocus -> if (!hasFocus) controller.saveConfig() }
				},
				Label("USB Port").apply {
					maxWidth = Double.MAX_VALUE
					maxHeight = Double.MAX_VALUE
					alignment = Pos.CENTER_RIGHT
				} to ChoiceBox(usbPortList.sorted()).apply {
					selectionModel.select(config.usbPort)
					converter = object : StringConverter<String>() {
						override fun toString(port: String?): String? = usbPortConnectedMap.getOrDefault(port, false)
								?.let { connected -> "${port}${if (!connected) " (disconnected)" else ""}" }

						override fun fromString(string: String?): String? = string
								?.let { "(.*)( \\(disconnected\\))?".toRegex() }
								?.matchEntire(string)
								?.groupValues
								?.takeIf { it.isNotEmpty() }
								?.get(0)
					}
					selectionModel.selectedItemProperty().addListener { _, _, usbPort ->
						config.usbPort = usbPort
						controller.saveConfig()
					}
				},
				Label("On demand connection").apply {
					maxWidth = Double.MAX_VALUE
					maxHeight = Double.MAX_VALUE
					alignment = Pos.CENTER_RIGHT
				} to CheckBox().apply {
					selectedProperty().bindBidirectional(config.onDemandConnectionProperty)
					selectedProperty().addListener { _, _, _ -> controller.saveConfig() }
				},
				Label("Color Ordering").apply {
					maxWidth = Double.MAX_VALUE
					maxHeight = Double.MAX_VALUE
					alignment = Pos.CENTER_RIGHT
				} to HBox(
						*ToggleGroup().let { group ->
							listOf(
									RadioButton("RGB").apply {
										maxHeight = Double.MAX_VALUE
										toggleGroup = group
										isSelected = !config.grbColors
									},
									RadioButton("GRB").apply {
										maxHeight = Double.MAX_VALUE
										toggleGroup = group
										isSelected = config.grbColors
										selectedProperty().addListener { _, _, selected ->
											config.grbColors = selected
											controller.saveConfig()
										}
									})
						}.toTypedArray()
				))

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
		StatusCollector.status.sample(10, TimeUnit.SECONDS, true).subscribe { collector ->
			Platform.runLater {
				val lights = if (config.combineMultipleServices) collector
						.topStatuses(controller.applicationConfiguration.silencedMap.keys, config.minPriority,
								config.minStatus, config.services)
						.also { LOGGER.debug("${if (config.enabled) "Changing" else "Skipping"}: ${it}") }
						.mapNotNull { it.toLightConfig() }
						.flatten()
						.takeIf { it.isNotEmpty() }
						?: config.items.firstOrNull { it.id == "" }?.statusOk
				else collector
						.topStatus(controller.applicationConfiguration.silencedMap.keys, config.minPriority,
								config.minStatus, config.services)
						.also { LOGGER.debug("${if (config.enabled) "Changing" else "Skipping"}: ${it}") }
						?.toLightConfig()
						?: config.items.firstOrNull { it.id == "" }?.statusOk

				lights?.also { lastState = it }
						?.takeIf { config.enabled }
						?.also { changeLights(it) }
			}
		}

		updateSerialPorts(SerialPortList.getPortNames().map { USBCommunicator.Descriptor(it) })
		USBScanner.register(usbScannerListener)
		bluetoothCommunicator.connectionDescriptor = BluetoothCommunicator.Descriptor(config.deviceAddress, 6)
		usbCommunicator.connectionDescriptor = USBCommunicator.Descriptor(config.usbPort)
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
	}

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

	override fun execute(action: NotifierAction): Unit = when (action) {
		NotifierAction.ENABLE -> {
			config.enabled = true
			if (!config.onDemandConnection) {
				bluetoothCommunicator.connect(BluetoothCommunicator.Descriptor(config.deviceAddress, 6))
				usbCommunicator.connect(USBCommunicator.Descriptor(config.usbPort))
			}
			changeLights(lastState)
			controller.saveConfig()
		}
		NotifierAction.DISABLE -> {
			config.enabled = false
			bluetoothCommunicator.disconnect()
			usbCommunicator.disconnect()
			controller.saveConfig()
		}
		NotifierAction.TOGGLE -> execute(if (config.enabled) NotifierAction.DISABLE else NotifierAction.ENABLE)
		NotifierAction.SHUTDOWN -> changeLights(listOf(RgbLightConfiguration()))
	}

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

	private fun StatusItem?.toLightConfig(): List<RgbLightConfiguration>? {
		val lightConfig = config.items
				.map { it.id to it }
				.toMap()
				.let { it[this?.serviceId ?: ""] ?: it[""] }

		return when (this?.status) {
			Status.NONE, Status.OFF -> lightConfig?.statusNone
			Status.UNKNOWN -> lightConfig?.statusUnknown
			Status.OK -> lightConfig?.statusOk
			Status.INFO -> lightConfig?.statusInfo
			Status.NOTIFICATION -> lightConfig?.statusNotification
			Status.CONNECTION_ERROR -> lightConfig?.statusConnectionError
			Status.SERVICE_ERROR -> lightConfig?.statusServiceError
			Status.WARNING -> lightConfig?.statusWarning
			Status.ERROR -> lightConfig?.statusError
			Status.FATAL -> lightConfig?.statusFatal
			else -> lightConfig?.statusNone
		}
	}
}