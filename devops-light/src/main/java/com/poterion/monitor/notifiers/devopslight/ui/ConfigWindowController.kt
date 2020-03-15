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
package com.poterion.monitor.notifiers.devopslight.ui

import com.poterion.communication.serial.communicator.BluetoothCommunicator
import com.poterion.communication.serial.communicator.Channel
import com.poterion.communication.serial.communicator.USBCommunicator
import com.poterion.communication.serial.listeners.CommunicatorListener
import com.poterion.communication.serial.payload.DeviceCapabilities
import com.poterion.communication.serial.payload.RgbColor
import com.poterion.communication.serial.payload.RgbLightConfiguration
import com.poterion.communication.serial.payload.RgbLightPattern
import com.poterion.communication.serial.toColor
import com.poterion.communication.serial.toRGBColor
import com.poterion.monitor.api.CommonIcon
import com.poterion.monitor.api.utils.title
import com.poterion.monitor.data.services.ServiceConfig
import com.poterion.monitor.notifiers.devopslight.DevOpsLightIcon
import com.poterion.monitor.notifiers.devopslight.control.DevOpsLightNotifier
import com.poterion.monitor.notifiers.devopslight.data.DevOpsLightConfig
import com.poterion.monitor.notifiers.devopslight.data.DevOpsLightItemConfig
import com.poterion.monitor.notifiers.devopslight.data.StateConfig
import com.poterion.monitor.notifiers.devopslight.deepCopy
import com.poterion.utils.javafx.*
import com.poterion.utils.kotlin.noop
import javafx.application.Platform
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.geometry.Insets
import javafx.scene.Parent
import javafx.scene.control.*
import javafx.scene.control.Alert.AlertType
import javafx.scene.control.ButtonType
import javafx.scene.image.ImageView
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.layout.Background
import javafx.scene.layout.BackgroundFill
import javafx.scene.layout.CornerRadii
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import javafx.util.StringConverter
import kotlin.math.roundToInt


/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
class ConfigWindowController : CommunicatorListener {
	companion object {
		internal fun getRoot(config: DevOpsLightConfig, controller: DevOpsLightNotifier): Pair<Parent, ConfigWindowController> =
				FXMLLoader(ConfigWindowController::class.java.getResource("config-window.fxml"))
						.let { it.load<Parent>() to it.getController<ConfigWindowController>() }
						.let { (root, ctrl) ->
							ctrl.config = config
							ctrl.notifier = controller
							ctrl.load()
							root to ctrl
						}
	}

	@FXML private lateinit var splitPane: SplitPane
	@FXML private lateinit var treeConfigs: TreeView<StateConfig>
	@FXML private lateinit var comboConfigName: ComboBox<ServiceConfig>
	@FXML private lateinit var buttonAddConfig: Button
	@FXML private lateinit var buttonDeleteConfig: Button

	@FXML private lateinit var textServiceName: TextField
	@FXML private lateinit var comboBoxPattern: ComboBox<RgbLightPattern>
	@FXML private lateinit var comboBoxColor1: ColorPicker
	@FXML private lateinit var comboBoxColor2: ColorPicker
	@FXML private lateinit var comboBoxColor3: ColorPicker
	@FXML private lateinit var comboBoxColor4: ColorPicker
	@FXML private lateinit var comboBoxColor5: ColorPicker
	@FXML private lateinit var comboBoxColor6: ColorPicker
	@FXML private lateinit var textDelay: TextField
	@FXML private lateinit var textWidth: TextField
	@FXML private lateinit var labelFade: Label
	@FXML private lateinit var textFade: TextField
	@FXML private lateinit var sliderMin: Slider
	@FXML private lateinit var sliderMax: Slider
	@FXML private lateinit var textTimeout: TextField
	@FXML private lateinit var labelMinValue: Label
	@FXML private lateinit var labelMaxValue: Label
	@FXML private lateinit var buttonTestLight: Button
	@FXML private lateinit var buttonSaveLight: Button
	@FXML private lateinit var buttonClearLight: Button

	@FXML private lateinit var tableLightConfigs: TableView<RgbLightConfiguration>
	@FXML private lateinit var columnLightPattern: TableColumn<RgbLightConfiguration, String>
	@FXML private lateinit var columnLightColor1: TableColumn<RgbLightConfiguration, RgbColor>
	@FXML private lateinit var columnLightColor2: TableColumn<RgbLightConfiguration, RgbColor>
	@FXML private lateinit var columnLightColor3: TableColumn<RgbLightConfiguration, RgbColor>
	@FXML private lateinit var columnLightColor4: TableColumn<RgbLightConfiguration, RgbColor>
	@FXML private lateinit var columnLightColor5: TableColumn<RgbLightConfiguration, RgbColor>
	@FXML private lateinit var columnLightColor6: TableColumn<RgbLightConfiguration, RgbColor>
	@FXML private lateinit var columnLightDelay: TableColumn<RgbLightConfiguration, Int>
	@FXML private lateinit var columnLightWidth: TableColumn<RgbLightConfiguration, Int>
	@FXML private lateinit var columnLightFading: TableColumn<RgbLightConfiguration, Int>
	@FXML private lateinit var columnLightMinimum: TableColumn<RgbLightConfiguration, Int>
	@FXML private lateinit var columnLightMaximum: TableColumn<RgbLightConfiguration, Int>
	@FXML private lateinit var columnLightTimeout: TableColumn<RgbLightConfiguration, Int>

	@FXML private lateinit var buttonTestLightSequence: Button
	@FXML private lateinit var buttonTurnOffLight: Button
	@FXML private lateinit var buttonMoveUpLight: Button
	@FXML private lateinit var buttonMoveDownLight: Button
	@FXML private lateinit var buttonDeleteLight: Button
	@FXML private lateinit var btnConnect: Button

	@FXML private lateinit var iconBluetooth: ImageView
	@FXML private lateinit var iconUSB: ImageView

	private lateinit var config: DevOpsLightConfig
	private lateinit var notifier: DevOpsLightNotifier
	private var currentLightConfiguration: List<RgbLightConfiguration> = emptyList()
	private val clipboard = mutableListOf<RgbLightConfiguration>()

	private val patterns = RgbLightPattern.values().toList()

	private val selectedParent: TreeItem<StateConfig>?
		get() {
			var item = treeConfigs.selectionModel.selectedItem
			while (item != null && item.parent != treeConfigs.root) item = item.parent
			return item
		}


	@FXML
	fun initialize() {
		textServiceName.textProperty().addListener { _, _, value ->
			selectedParent?.value?.title = value
			treeConfigs.refresh()
		}
		textServiceName.focusedProperty().addListener { _, _, focused -> if (!focused) saveConfig() }

		comboBoxPattern.apply {
			items.addAll(patterns)
			selectionModel.select(0)
			selectionModel.selectedItemProperty().addListener { _, _, value -> selectPattern(value) }
			converter = object : StringConverter<RgbLightPattern>() {
				override fun toString(obj: RgbLightPattern?): String = obj?.title ?: ""
				override fun fromString(string: String?): RgbLightPattern = patterns
						.firstOrNull { it.title == string }
						?: patterns.first()
			}
			factory { item, empty ->
				text = item?.takeUnless { empty }?.title
			}
		}

		// Up o 12
		val customColors = listOf(Color.RED, Color.LIME, Color.BLUE, Color.MAGENTA, Color.YELLOW, Color.CYAN)
		// Color.AZURE, Color.AQUA, Color.PINK, Color.OLIVE, Color.NAVY, Color.MAROON
		comboBoxColor1.customColors.addAll(customColors)
		comboBoxColor2.customColors.addAll(customColors)
		comboBoxColor3.customColors.addAll(customColors)
		comboBoxColor4.customColors.addAll(customColors)
		comboBoxColor5.customColors.addAll(customColors)
		comboBoxColor6.customColors.addAll(customColors)

		sliderMin.valueProperty().addListener { _, _, value -> labelMinValue.text = "${value.toInt()}%" }
		sliderMax.valueProperty().addListener { _, _, value -> labelMaxValue.text = "${value.toInt()}%" }

		treeConfigs.apply {
			isShowRoot = false
			selectionModel.selectionMode = SelectionMode.SINGLE
			cell { _, item, empty ->
				text = item?.takeUnless { empty }?.title?.let { if (it.isEmpty()) "Default" else it }
				graphic = item?.takeUnless { empty }?.icon?.toImageView()
			}
			selectionModel.selectedItemProperty().addListener { _, _, item ->
				selectStateConfig(item)
				val nothingOrDefaultSelected = item == null
						|| item.value.serviceId.isEmpty()
						|| item.parent.value.serviceId.isEmpty()

				textServiceName.text = (item.value.takeIf { it.lightConfigs == null } ?: item.parent.value).title
				textServiceName.isDisable = nothingOrDefaultSelected
				buttonDeleteConfig.isDisable = nothingOrDefaultSelected
			}
		}

		tableLightConfigs.apply {
			selectionModel.selectionMode = SelectionMode.SINGLE
			selectionModel.selectedItemProperty().addListener { _, _, newValue -> selectLightConfig(newValue) }
		}

		columnLightPattern.cell("pattern")

		columnLightColor1.init("color1")
		columnLightColor2.init("color2")
		columnLightColor3.init("color3")
		columnLightColor4.init("color4")
		columnLightColor5.init("color5")
		columnLightColor6.init("color6")

		columnLightDelay.cell("delay") { _, value, empty -> text = value?.takeUnless { empty }?.let { "${it} ms" } }
		columnLightWidth.cell("width") { _, value, empty -> text = value?.takeUnless { empty }?.toString() }
		columnLightFading.cell("fading") { _, value, empty -> text = value?.takeUnless { empty }?.toString() }
		columnLightMinimum.cell("min") { _, value, empty -> text = value?.takeUnless { empty }?.toString() }
		columnLightMaximum.cell("max") { _, value, empty -> text = value?.takeUnless { empty }?.toString() }
		columnLightTimeout.cell("timeout") { _, value, empty -> text = value?.takeUnless { empty }?.toString() }
		treeConfigs.selectionModel.clearSelection()
		selectStateConfig(null)
		selectLightConfig(null)
	}

	private val ServiceConfig.icon: Icon?
		get() = notifier.controller.modules.find { it.configClass == this::class }?.icon

	private val configComparator: Comparator<DevOpsLightItemConfig> = Comparator { c1, c2 ->
		val n1 = notifier.controller.applicationConfiguration.serviceMap[c1.id]?.name ?: "Default"
		val n2 = notifier.controller.applicationConfiguration.serviceMap[c2.id]?.name ?: "Default"

		when {
			n1 == "Default" && n2 != "Default" -> 1
			n1 != "Default" && n2 == "Default" -> -1
			else -> compareValues(n1, n2)
		}
	}

	private fun load() {
		splitPane.setDividerPosition(0, config.split)
		splitPane.dividers.first().positionProperty().addListener { _, _, value ->
			config.split = value.toDouble()
			notifier.controller.saveConfig()
		}

		if (config.items.none { it.id.isBlank() }) config.items.add(DevOpsLightItemConfig())

		treeConfigs.root = TreeItem(StateConfig("Configurations")).apply {
			config.items
					.sortedWith(configComparator)
					.map { it to notifier.controller.applicationConfiguration.serviceMap[it.id] }
					.map { (item, service) ->
						TreeItem(StateConfig(service?.name ?: "Default", serviceId = item.id)).apply {
							children.addAll(
									TreeItem(StateConfig("None", CommonIcon.STATUS_UNKNOWN, item.statusNone)),
									TreeItem(StateConfig("Unknown", CommonIcon.STATUS_UNKNOWN, item.statusUnknown)),
									TreeItem(StateConfig("OK", CommonIcon.STATUS_OK, item.statusOk)),
									TreeItem(StateConfig("Info", CommonIcon.STATUS_INFO, item.statusInfo)),
									TreeItem(StateConfig("Notification", CommonIcon.STATUS_NOTIFICATION, item.statusNotification)),
									TreeItem(StateConfig("Connection Error", CommonIcon.INACTIVE, item.statusConnectionError)),
									TreeItem(StateConfig("Service Error", CommonIcon.INACTIVE, item.statusServiceError)),
									TreeItem(StateConfig("Warning", CommonIcon.STATUS_WARNING, item.statusWarning)),
									TreeItem(StateConfig("Error", CommonIcon.STATUS_ERROR, item.statusError)),
									TreeItem(StateConfig("Fatal", CommonIcon.STATUS_FATAL, item.statusFatal)))
							isExpanded = true
						}
					}
					.also { children.addAll(it) }
		}
		comboConfigName.apply {
			factory { item, empty ->
				item?.takeUnless { empty }?.nameProperty?.also { textProperty().bindBidirectional(it) }
				graphic = item?.takeUnless { empty }?.icon?.toImageView()
			}
			items = notifier.controller
					.applicationConfiguration
					.services
					.bindFiltered(config.items) { !config.items.map { i -> i.id }.contains(it.uuid) }
					//.distinctBy { it.uuid }
					.sorted(compareBy { it.name })
			//.takeIf { it.isNotEmpty() }
			//?.also { items?.addAll(it) }
			selectionModel.clearSelection()
			value = null
		}

		// Status
		iconBluetooth.image =
				if (notifier.bluetoothCommunicator.isConnected) DevOpsLightIcon.BLUETOOTH_CONNECTED.toImage()
				else DevOpsLightIcon.BLUETOOTH_DISCONNECTED.toImage()
		iconUSB.image =
				if (notifier.usbCommunicator.isConnected) DevOpsLightIcon.USB_CONNECTED.toImage()
				else DevOpsLightIcon.USB_DISCONNECTED.toImage()

		notifier.bluetoothCommunicator.register(this)
		notifier.usbCommunicator.register(this)
	}

	@FXML
	fun onKeyPressed(keyEvent: KeyEvent) = when (keyEvent.code) {
		KeyCode.F3 -> onTestLight()
		KeyCode.F4 -> onTestLightSequence()
		KeyCode.F5 -> onReconnect()
		KeyCode.F12 -> onTurnOffLight()
		KeyCode.S -> if (keyEvent.isControlDown) onSaveLight() else null
		KeyCode.ESCAPE -> onClearLight()
		else -> null
	}

	@FXML
	fun onAddConfig() = comboConfigName.value
			.also { service ->
				treeConfigs.root.children.add(TreeItem(StateConfig(service?.name ?: "Default", service.icon)).apply {
					children.addAll(
							TreeItem(StateConfig("None", CommonIcon.PRIORITY_NONE, emptyList())),
							TreeItem(StateConfig("Unknown", CommonIcon.STATUS_UNKNOWN, emptyList())),
							TreeItem(StateConfig("OK", CommonIcon.STATUS_OK, emptyList())),
							TreeItem(StateConfig("Info", CommonIcon.STATUS_INFO, emptyList())),
							TreeItem(StateConfig("Notification", CommonIcon.STATUS_NOTIFICATION, emptyList())),
							TreeItem(StateConfig("Connection Error", CommonIcon.BROKEN_LINK, emptyList())),
							TreeItem(StateConfig("Service Error", CommonIcon.UNAVAILABLE, emptyList())),
							TreeItem(StateConfig("Warning", CommonIcon.STATUS_WARNING, emptyList())),
							TreeItem(StateConfig("Error", CommonIcon.STATUS_ERROR, emptyList())),
							TreeItem(StateConfig("Fatal", CommonIcon.STATUS_FATAL, emptyList())))
				})
				comboConfigName.apply {
					items.remove(service)
					selectionModel.clearSelection()
					value = null
				}
				saveConfig()
			}

	@FXML
	fun onDeleteSelectedConfig() {
		var selected = treeConfigs.selectionModel.selectedItem
		while (selected != null && selected.value.lightConfigs != null) selected = selected.parent

		if (selected != null && selected.value.serviceId.isNotEmpty()) Alert(AlertType.CONFIRMATION).apply {
			title = "Delete confirmation"
			headerText = "Delete confirmation"
			contentText = "Do you really want to delete this whole configuration?"
			buttonTypes.setAll(ButtonType.YES, ButtonType.NO)
		}.showAndWait().ifPresent { btnType ->
			btnType.takeIf { it == ButtonType.YES }?.also {
				if (selected.value.title.isNotEmpty()) selected.parent?.children?.remove(selected)
				saveConfig()
			}
		}
	}

	@FXML
	fun onKeyPressedInTree(keyEvent: KeyEvent) {
		if (treeConfigs.selectionModel.selectedItem != null) when (keyEvent.code) {
			KeyCode.DELETE -> onDeleteSelectedConfig()
			KeyCode.C -> if (keyEvent.isControlDown) {
				clipboard.clear()
				clipboard.addAll(tableLightConfigs.items.deepCopy())
			}
			KeyCode.V -> if (keyEvent.isControlDown) {
				tableLightConfigs.items.addAll(clipboard.deepCopy())
			}
			else -> noop()
		}
	}

	@FXML
	fun onTestLight() {
		createLightConfig()?.also { notifier.changeLights(listOf(it)) }
	}

	@FXML
	fun onTestLightSequence() {
		tableLightConfigs.items.takeIf { it.isNotEmpty() }?.also { notifier.changeLights(it) }
	}

	@FXML
	fun onTurnOffLight() {
		notifier.changeLights(listOf(RgbLightConfiguration()))
	}

	@FXML
	fun onMoveUpLight() {
		val selectedLight = tableLightConfigs.selectionModel.selectedItem?.copy()
		tableLightConfigs.selectionModel.selectedIndex
				.takeIf { it > 0 && it < tableLightConfigs.items.size }
				?.also {
					tableLightConfigs.items.removeAt(it)
					tableLightConfigs.items.add(it - 1, selectedLight)
					tableLightConfigs.selectionModel.select(it - 1)
					saveConfig()
				}
	}

	@FXML
	fun onMoveDownLight() {
		val selectedLight = tableLightConfigs.selectionModel.selectedItem?.copy()
		tableLightConfigs.selectionModel.selectedIndex
				.takeIf { it >= 0 && it < tableLightConfigs.items.size - 1 }
				?.also {
					tableLightConfigs.items.removeAt(it)
					tableLightConfigs.items.add(it + 1, selectedLight)
					tableLightConfigs.selectionModel.select(it + 1)
					saveConfig()
				}
	}

	@FXML
	fun onSaveLight() {
		val selectedIndex = tableLightConfigs.selectionModel.selectedIndex
		val configuredLight = createLightConfig()
		if (selectedIndex < 0 && configuredLight != null) {
			tableLightConfigs.items.add(configuredLight)
		} else if (selectedIndex >= 0 && configuredLight != null) {
			tableLightConfigs.items[tableLightConfigs.selectionModel.selectedIndex] = configuredLight
		}
		tableLightConfigs.refresh()

		if (configuredLight != null) {
			saveConfig()
			tableLightConfigs.selectionModel.clearSelection()
		}
	}

	@FXML
	fun onClearLight() = tableLightConfigs.selectionModel.clearSelection()

	@FXML
	fun onDeleteLight() {
		tableLightConfigs.selectionModel.selectedIndex.takeIf { it >= 0 }?.also {
			tableLightConfigs.items.removeAt(it)
			saveConfig()
		}
	}

	@FXML
	fun onReconnect() {
		if (notifier.bluetoothCommunicator.isConnected || notifier.usbCommunicator.isConnected) {
			if (notifier.bluetoothCommunicator.isConnected) notifier.bluetoothCommunicator.disconnect()
			if (notifier.usbCommunicator.isConnected) notifier.usbCommunicator.disconnect()
		} else if (notifier.bluetoothCommunicator.isConnecting || notifier.usbCommunicator.isConnecting) {
			if (notifier.bluetoothCommunicator.isConnecting) notifier.bluetoothCommunicator.disconnect()
			if (notifier.usbCommunicator.isConnecting) notifier.usbCommunicator.disconnect()
		} else {
			notifier.bluetoothCommunicator.connect(BluetoothCommunicator.Descriptor(config.deviceAddress, 6))
			notifier.usbCommunicator.connect(USBCommunicator.Descriptor(config.usbPort))
		}
	}

	@FXML
	fun onKeyPressedInTable(keyEvent: KeyEvent) = when (keyEvent.code) {
		KeyCode.DELETE -> onDeleteLight()
		KeyCode.UP -> if (keyEvent.isAltDown) onMoveUpLight() else null
		KeyCode.DOWN -> if (keyEvent.isAltDown) onMoveDownLight() else null
		KeyCode.C -> if (keyEvent.isControlDown) {
			clipboard.clear()
			tableLightConfigs.selectionModel.selectedItem?.copy()?.also { clipboard.add(it) }
			null
		} else null
		KeyCode.V -> if (keyEvent.isControlDown) {
			tableLightConfigs.items.addAll(tableLightConfigs.selectionModel.selectedIndex, clipboard.deepCopy())
			null
		} else null
		else -> null
	}

	override fun onConnecting(channel: Channel) = Platform.runLater {
		btnConnect.text = "Cancel [F5]"
	}

	override fun onConnect(channel: Channel) = Platform.runLater {
		btnConnect.text = "Disconnect [F5]"
		when (channel) {
			Channel.BLUETOOTH -> iconBluetooth.image = DevOpsLightIcon.BLUETOOTH_CONNECTED.toImage()
			Channel.USB -> iconUSB.image = DevOpsLightIcon.USB_CONNECTED.toImage()
		}
	}

	override fun onConnectionReady(channel: Channel) = noop()

	override fun onDisconnect(channel: Channel) = Platform.runLater {
		btnConnect.text = "Connect [F5]"
		when (channel) {
			Channel.BLUETOOTH -> iconBluetooth.image = DevOpsLightIcon.BLUETOOTH_DISCONNECTED.toImage()
			Channel.USB -> iconUSB.image = DevOpsLightIcon.USB_DISCONNECTED.toImage()
		}
	}

	override fun onMessageReceived(channel: Channel, message: IntArray) = noop()

	override fun onMessagePrepare(channel: Channel) = noop()

	override fun onMessageSent(channel: Channel, message: IntArray, remaining: Int) = noop()

	override fun onDeviceCapabilitiesChanged(channel: Channel, capabilities: DeviceCapabilities) = noop()

	override fun onDeviceNameChanged(channel: Channel, name: String) = noop()

	internal fun changeLights(lightConfiguration: List<RgbLightConfiguration>?) {
		currentLightConfiguration = lightConfiguration ?: emptyList()
	}

	private fun selectStateConfig(treeItem: TreeItem<StateConfig>?) {
		val lightConfigs = treeItem?.value?.lightConfigs?.deepCopy()

		textServiceName.isDisable = treeItem?.value?.lightConfigs == null
		comboBoxPattern.isDisable = treeItem?.value?.lightConfigs == null
		comboBoxColor1.isDisable = treeItem?.value?.lightConfigs == null
		comboBoxColor2.isDisable = treeItem?.value?.lightConfigs == null
		comboBoxColor3.isDisable = treeItem?.value?.lightConfigs == null
		comboBoxColor4.isDisable = treeItem?.value?.lightConfigs == null
		comboBoxColor5.isDisable = treeItem?.value?.lightConfigs == null
		comboBoxColor6.isDisable = treeItem?.value?.lightConfigs == null
		textDelay.isDisable = treeItem?.value?.lightConfigs == null
		textWidth.isDisable = treeItem?.value?.lightConfigs == null
		textFade.isDisable = treeItem?.value?.lightConfigs == null
		sliderMin.isDisable = treeItem?.value?.lightConfigs == null
		sliderMax.isDisable = treeItem?.value?.lightConfigs == null
		textTimeout.isDisable = treeItem?.value?.lightConfigs == null

		buttonTestLightSequence.isDisable = treeItem?.value?.lightConfigs == null
		tableLightConfigs.items.clear()
		tableLightConfigs.items.addAll(lightConfigs ?: currentLightConfiguration)
		if (tableLightConfigs.items.size > 0) tableLightConfigs.selectionModel.select(0)
	}

	private fun selectLightConfig(lightConfig: RgbLightConfiguration?) {
		buttonSaveLight.text = if (lightConfig == null) "Add [Ctrl+S]" else "Save [Ctrl+S]"
		(patterns.firstOrNull { it == lightConfig?.pattern } ?: patterns.first()).also { pattern ->
			comboBoxPattern.selectionModel.select(pattern)
			selectPattern(pattern)
			comboBoxColor1.value = lightConfig?.color1?.toColor() ?: Color.WHITE
			comboBoxColor2.value = lightConfig?.color2?.toColor() ?: Color.WHITE
			comboBoxColor3.value = lightConfig?.color3?.toColor() ?: Color.WHITE
			comboBoxColor4.value = lightConfig?.color4?.toColor() ?: Color.WHITE
			comboBoxColor5.value = lightConfig?.color5?.toColor() ?: Color.WHITE
			comboBoxColor6.value = lightConfig?.color6?.toColor() ?: Color.WHITE
			textDelay.text = "${lightConfig?.delay ?: 50}"
			textWidth.text = "${lightConfig?.width ?: 3}"
			textFade.text = "${lightConfig?.fading ?: 0}"
			sliderMin.value = lightConfig?.minimum?.toDouble() ?: 0.0
			sliderMax.value = lightConfig?.maximum?.toDouble() ?: 100.0
			setLightConfigButtonsEnabled(lightConfig != null)
		}
	}

	private fun selectPattern(pattern: RgbLightPattern) {
		textDelay.isDisable = pattern.delay == null
		textWidth.isDisable = pattern.width == null
		textFade.isDisable = pattern.fading == null
		sliderMin.isDisable = pattern.min == null
		sliderMax.isDisable = pattern.max == null
		//labelFade.text = pattern.fadingTitle
		textTimeout.isDisable = pattern.timeout == null
	}

	private fun setLightConfigButtonsEnabled(enabled: Boolean) {
		buttonMoveUpLight.isDisable = !enabled
				|| tableLightConfigs.selectionModel.selectedIndex == 0
		buttonMoveDownLight.isDisable = !enabled
				|| tableLightConfigs.selectionModel.selectedIndex == tableLightConfigs.items.size - 1
		buttonClearLight.isDisable = !enabled
		buttonDeleteLight.isDisable = !enabled
	}

	private fun createLightConfig(): RgbLightConfiguration? {
		val pattern = comboBoxPattern.selectionModel.selectedItem
		val color1 = comboBoxColor1.value?.toRGBColor()
		val color2 = comboBoxColor2.value?.toRGBColor()
		val color3 = comboBoxColor3.value?.toRGBColor()
		val color4 = comboBoxColor4.value?.toRGBColor()
		val color5 = comboBoxColor5.value?.toRGBColor()
		val color6 = comboBoxColor6.value?.toRGBColor()
		val color7 = RgbColor()
		val delay = textDelay.text?.toIntOrNull() ?: 1000
		val width = textWidth.text?.toIntOrNull() ?: 3
		val fade = textFade.text?.toIntOrNull() ?: 0
		val min = sliderMin.value.roundToInt()
		val max = sliderMax.value.roundToInt()
		val timeout = textTimeout.text.toIntOrNull() ?: 10

		return if (pattern != null
				&& color1 != null
				&& color2 != null
				&& color3 != null
				&& color4 != null
				&& color5 != null
				&& color6 != null)
			RgbLightConfiguration(pattern, color1, color2, color3, color4, color5, color6, color7, delay, width, fade,
					min, max, timeout)
		else null
	}

	private fun saveConfig() {
		treeConfigs.selectionModel.selectedItem?.value?.lightConfigs = tableLightConfigs.items.deepCopy()
		config.items.clear()
		config.items.addAll(treeConfigs.root.children
				.map { it.value to it.children.map { c -> c.value } }
				.filter { (_, children) -> children.size == 10 }
				.filter { (_, children) -> children.all { it.lightConfigs != null } }
				.map { (node, children) ->
					DevOpsLightItemConfig(
							id = node.serviceId,
							statusNone = children[0].lightConfigs ?: emptyList(),
							statusUnknown = children[1].lightConfigs ?: emptyList(),
							statusOk = children[2].lightConfigs ?: emptyList(),
							statusInfo = children[3].lightConfigs ?: emptyList(),
							statusNotification = children[4].lightConfigs ?: emptyList(),
							statusConnectionError = children[5].lightConfigs ?: emptyList(),
							statusServiceError = children[6].lightConfigs ?: emptyList(),
							statusWarning = children[7].lightConfigs ?: emptyList(),
							statusError = children[8].lightConfigs ?: emptyList(),
							statusFatal = children[9].lightConfigs ?: emptyList())
				})
		notifier.controller.saveConfig()
		notifier.bluetoothCommunicator.connect(BluetoothCommunicator.Descriptor(config.deviceAddress, 6))
		notifier.usbCommunicator.connect(USBCommunicator.Descriptor(config.usbPort))
	}

	private fun TableColumn<RgbLightConfiguration, RgbColor>.init(propertyName: String) = cell(propertyName) { _, value, empty ->
		graphic = Pane().takeUnless { empty }?.apply {
			background = Background(BackgroundFill(value?.toColor() ?: Color.TRANSPARENT,
					CornerRadii.EMPTY,
					Insets.EMPTY))
		}
	}
}
