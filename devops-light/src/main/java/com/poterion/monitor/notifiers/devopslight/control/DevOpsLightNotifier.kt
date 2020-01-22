package com.poterion.monitor.notifiers.devopslight.control

import com.poterion.communication.serial.*
import com.poterion.monitor.api.CommonIcon
import com.poterion.monitor.api.StatusCollector
import com.poterion.monitor.api.controllers.ControllerInterface
import com.poterion.monitor.api.controllers.ModuleInstanceInterface
import com.poterion.monitor.api.controllers.Notifier
import com.poterion.monitor.api.data.RGBColor
import com.poterion.monitor.api.modules.Module
import com.poterion.monitor.api.ui.Icon
import com.poterion.monitor.api.ui.NavigationItem
import com.poterion.monitor.api.utils.noop
import com.poterion.monitor.data.Status
import com.poterion.monitor.data.StatusItem
import com.poterion.monitor.data.notifiers.NotifierAction
import com.poterion.monitor.notifiers.devopslight.DevOpsLight
import com.poterion.monitor.notifiers.devopslight.DevOpsLightIcon
import com.poterion.monitor.notifiers.devopslight.data.DevOpsLightConfig
import com.poterion.monitor.notifiers.devopslight.data.DevOpsLightItemConfig
import com.poterion.monitor.notifiers.devopslight.data.LightConfig
import com.poterion.monitor.notifiers.devopslight.ui.ConfigWindowController
import javafx.application.Platform
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.control.Label
import javafx.scene.control.RadioButton
import javafx.scene.control.TextField
import javafx.scene.control.ToggleGroup
import javafx.scene.layout.HBox
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
class DevOpsLightNotifier(override val controller: ControllerInterface, config: DevOpsLightConfig) :
		Notifier<DevOpsLightConfig>(config), CommunicatorListener {

	companion object {
		val LOGGER: Logger = LoggerFactory.getLogger(DevOpsLightNotifier::class.java)
	}

	override val definition: Module<DevOpsLightConfig, ModuleInstanceInterface<DevOpsLightConfig>> = DevOpsLight
	val bluetoothCommunicator: BluetoothCommunicator = BluetoothCommunicator()
	val usbCommunicator: USBCommunicator = USBCommunicator()
	private var lastState = emptyList<LightConfig>()
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
						sub = itemConfig.getSubMenu().toMutableList()))
			}
		}

	private fun DevOpsLightItemConfig.getSubMenu() = listOf(
			NavigationItem(title = "None", icon = CommonIcon.PRIORITY_NONE) to statusNone,
			NavigationItem(title = "Unknown", icon = CommonIcon.STATUS_UNKNOWN) to statusUnknown,
			NavigationItem(title = "OK", icon = CommonIcon.STATUS_OK) to statusOk,
			NavigationItem(title = "Info", icon = CommonIcon.STATUS_INFO) to statusInfo,
			NavigationItem(title = "Notification", icon = CommonIcon.STATUS_NOTIFICATION) to statusNotification,
			NavigationItem(title = "Connection Error", icon = CommonIcon.BROKEN_LINK) to statusConnectionError,
			NavigationItem(title = "Service Error", icon = CommonIcon.UNAVAILABLE) to statusServiceError,
			NavigationItem(title = "Warning", icon = CommonIcon.STATUS_WARNING) to statusWarning,
			NavigationItem(title = "Error", icon = CommonIcon.STATUS_ERROR) to statusError,
			NavigationItem(title = "Fatal", icon = CommonIcon.STATUS_FATAL) to statusFatal)
			.filter { (_, lights) -> lights.isNotEmpty() }
			.map { (item, lights) -> item.also { it.action = { changeLights(lights) } } }

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
				} to TextField(config.usbPort).apply {
					textProperty().addListener { _, _, address -> config.usbPort = address }
					focusedProperty().addListener { _, _, hasFocus -> if (!hasFocus) controller.saveConfig() }
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

	override val configurationTab: Parent?
		get() = ConfigWindowController.getRoot(config, this)

	override fun initialize() {
		super.initialize()
		StatusCollector.status.sample(10, TimeUnit.SECONDS).subscribe { collector ->
			Platform.runLater {
				val lights = if (config.combineMultipleServices) collector
						.topStatuses(controller.applicationConfiguration.silenced.keys, config.minPriority,
								config.minStatus, config.services)
						.also { LOGGER.debug("${if (config.enabled) "Changing" else "Skipping"}: ${it}") }
						.mapNotNull { it.toLightConfig() }
						.flatten()
						.takeIf { it.isNotEmpty() }
						?: config.items.firstOrNull { it.id == "" }?.statusOk
				else collector
						.topStatus(controller.applicationConfiguration.silenced.keys, config.minPriority,
								config.minStatus, config.services)
						.also { LOGGER.debug("${if (config.enabled) "Changing" else "Skipping"}: ${it}") }
						?.toLightConfig()
						?: config.items.firstOrNull { it.id == "" }?.statusOk

				lights?.also { lastState = it }
						?.takeIf { config.enabled }
						?.also { changeLights(it) }
			}
		}
		bluetoothCommunicator.register(this)
		usbCommunicator.register(this)
		if (config.enabled) {
			bluetoothCommunicator.connect(BluetoothCommunicator.Descriptor(config.deviceAddress, 6))
			usbCommunicator.connect(USBCommunicator.Descriptor(config.usbPort))
		}
	}

	/**
	 * |===================================================================|
	 * | (C) Configuration for concrete strip NUM                          |
	 * |-------------------------------------------------------------------|
	 * | 0 |  1 | 2 |   3   | 4  ... 24 |25 26|  27 |  28  | 29| 30|   31  |
	 * |CRC|KIND|NUM|PATTERN|RGB0...RGB6|DELAY|WIDTH|FADING|MIN|MAX|TIMEOUT|
	 * |===================================================================|
	 */
	internal fun changeLights(lightConfiguration: List<LightConfig>?) {
		if (lightConfiguration != null) {
			lightConfiguration
					.takeIf { it.isNotEmpty() }
					?.mapIndexed { index, lightConfig ->
						listOf(0, // Num. - only one implemented
								lightConfig.pattern.code or (if (index == 0) 0x80 else 0x00),
								*lightConfig.color1.components(),
								*lightConfig.color2.components(),
								*lightConfig.color3.components(),
								*lightConfig.color4.components(),
								*lightConfig.color5.components(),
								*lightConfig.color6.components(),
								*lightConfig.color7.components(),
								lightConfig.delay / 256, lightConfig.delay % 256,
								lightConfig.width,
								lightConfig.fading,
								lightConfig.min, lightConfig.max,
								lightConfig.timeout).map(Int::toByte).toByteArray()
					}
					?.forEach {
						if (usbCommunicator.isConnected) usbCommunicator.send(MessageKind.WS281xLIGHT, it)
						else bluetoothCommunicator.send(MessageKind.WS281xLIGHT, it)
					}
		}
	}

	private fun RGBColor.components(): Array<Int> =
			if (config.grbColors) arrayOf(green, red, blue) else arrayOf(red, green, blue)

	override fun execute(action: NotifierAction): Unit = when (action) {
		NotifierAction.ENABLE -> {
			config.enabled = true
			bluetoothCommunicator.connect(BluetoothCommunicator.Descriptor(config.deviceAddress, 6))
			usbCommunicator.connect(USBCommunicator.Descriptor(config.usbPort))
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
		NotifierAction.SHUTDOWN -> changeLights(listOf(LightConfig()))
	}

	override fun onConnecting(channel: Channel) = noop()

	override fun onConnect(channel: Channel) = noop()

	override fun onDisconnect(channel: Channel) = noop()

	override fun onMessageReceived(channel: Channel, message: IntArray) = noop()

	override fun onMessageSent(channel: Channel, message: IntArray, remaining: Int) = noop()

	private fun StatusItem?.toLightConfig(): List<LightConfig>? {
		val lightConfig = config.items
				.map { it.id to it }
				.toMap()
				.let { it[this?.id ?: ""] ?: it[""] }

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