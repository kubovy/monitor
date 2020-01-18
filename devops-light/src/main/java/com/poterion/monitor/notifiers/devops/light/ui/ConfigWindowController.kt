package com.poterion.monitor.notifiers.devops.light.ui

import com.poterion.communication.serial.BluetoothCommunicator
import com.poterion.communication.serial.Channel
import com.poterion.communication.serial.CommunicatorListener
import com.poterion.communication.serial.USBCommunicator
import com.poterion.monitor.api.lib.toImage
import com.poterion.monitor.api.lib.toImageView
import com.poterion.monitor.api.ui.CommonIcon
import com.poterion.monitor.api.utils.cell
import com.poterion.monitor.api.utils.factory
import com.poterion.monitor.notifiers.devops.light.DevOpsLightIcon
import com.poterion.monitor.notifiers.devops.light.control.DevOpsLightNotifier
import com.poterion.monitor.notifiers.devops.light.data.*
import com.poterion.monitor.notifiers.devops.light.deepCopy
import com.poterion.monitor.notifiers.devops.light.toColor
import com.poterion.monitor.notifiers.devops.light.toLightColor
import javafx.collections.FXCollections
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
 * @author Jan Kubovy <jan@kubovy.eu>
 */
class ConfigWindowController : CommunicatorListener {
	companion object {
		internal fun getRoot(config: DevOpsLightConfig, controller: DevOpsLightNotifier): Parent =
				FXMLLoader(ConfigWindowController::class.java.getResource("config-window.fxml"))
						.let { it.load<Parent>() to it.getController<ConfigWindowController>() }
						.let { (root, ctrl) ->
							ctrl.config = config
							ctrl.notifier = controller
							ctrl.load()
							root
						}
	}

	@FXML private lateinit var splitPane: SplitPane
	@FXML private lateinit var treeConfigs: TreeView<StateConfig>
	@FXML private lateinit var comboConfigName: ComboBox<String>
	@FXML private lateinit var buttonAddConfig: Button
	@FXML private lateinit var buttonDeleteConfig: Button

	@FXML private lateinit var textServiceName: TextField
	@FXML private lateinit var comboBoxPattern: ComboBox<LightPattern>
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

	@FXML private lateinit var tableLightConfigs: TableView<LightConfig>
	@FXML private lateinit var columnLightPattern: TableColumn<LightConfig, String>
	@FXML private lateinit var columnLightColor1: TableColumn<LightConfig, LightColor>
	@FXML private lateinit var columnLightColor2: TableColumn<LightConfig, LightColor>
	@FXML private lateinit var columnLightColor3: TableColumn<LightConfig, LightColor>
	@FXML private lateinit var columnLightColor4: TableColumn<LightConfig, LightColor>
	@FXML private lateinit var columnLightColor5: TableColumn<LightConfig, LightColor>
	@FXML private lateinit var columnLightColor6: TableColumn<LightConfig, LightColor>
	@FXML private lateinit var columnLightDelay: TableColumn<LightConfig, Int>
	@FXML private lateinit var columnLightWidth: TableColumn<LightConfig, Int>
	@FXML private lateinit var columnLightFading: TableColumn<LightConfig, Int>
	@FXML private lateinit var columnLightMinimum: TableColumn<LightConfig, Int>
	@FXML private lateinit var columnLightMaximum: TableColumn<LightConfig, Int>
	@FXML private lateinit var columnLightTimeout: TableColumn<LightConfig, Int>

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
	private val clipboard = mutableListOf<LightConfig>()

	private val patterns = FXCollections.observableArrayList(*LightPattern.values())

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
			items?.addAll(patterns)
			selectionModel.select(0)
			selectionModel.selectedItemProperty().addListener { _, _, value -> selectPattern(value) }
			converter = object : StringConverter<LightPattern>() {
				override fun toString(obj: LightPattern?): String = obj?.title ?: ""
				override fun fromString(string: String?): LightPattern = patterns
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
			factory { item, empty ->
				text = item?.takeUnless { empty }?.title?.let { if (it.isEmpty()) "Default" else it }
				graphic = item?.takeUnless { empty }?.icon?.toImageView()
			}
			selectionModel.selectedItemProperty().addListener { _, _, item ->
				selectStateConfig(item)
				val nothingOrDefaultSelected = item == null
						|| item.value.title.isEmpty()
						|| item.parent.value.title.isEmpty()

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
		columnLightTimeout.cell("timeout") { _, value, empty -> text = value?.takeUnless { empty }.toString() }
		treeConfigs.selectionModel.clearSelection()
		selectStateConfig(null)
		selectLightConfig(null)
	}

	private fun load() {
		splitPane.setDividerPosition(0, config.split)
		splitPane.dividers.first().positionProperty().addListener { _, _, value ->
			config.split = value.toDouble()
			notifier.controller.saveConfig()
		}

		treeConfigs.root = TreeItem(StateConfig("Configurations")).apply {
			config.items
					.sortedBy { it.id }
					.map { item ->
						TreeItem(StateConfig(item.id)).apply {
							children.addAll(
									TreeItem(StateConfig("None", CommonIcon.UNKNOWN, item.statusNone)),
									TreeItem(StateConfig("Unknown", CommonIcon.UNKNOWN, item.statusUnknown)),
									TreeItem(StateConfig("OK", CommonIcon.OK, item.statusOk)),
									TreeItem(StateConfig("Info", CommonIcon.INFO, item.statusInfo)),
									TreeItem(StateConfig("Notification", CommonIcon.NOTIFICATION, item.statusNotification)),
									TreeItem(StateConfig("Connection Error", CommonIcon.INACTIVE, item.statusConnectionError)),
									TreeItem(StateConfig("Service Error", CommonIcon.INACTIVE, item.statusServiceError)),
									TreeItem(StateConfig("Warning", CommonIcon.WARNING, item.statusWarning)),
									TreeItem(StateConfig("Error", CommonIcon.ERROR, item.statusError)),
									TreeItem(StateConfig("Fatal", CommonIcon.FATAL, item.statusFatal)))
							isExpanded = true
						}
					}
					.also { children.addAll(it) }
		}
		comboConfigName.apply {
			notifier.controller
					.applicationConfiguration
					.services
					.values
					.map { it.name }
					.filter { !config.items.map { i -> i.id }.contains(it) }
					.distinct()
					.sorted()
					.takeIf { it.isNotEmpty() }
					?.also { items?.addAll(it) }
			selectionModel.clearSelection()
			this.value = ""
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
	fun onAddConfig() = comboConfigName.value?.trim()
			?.takeIf { it.isNotEmpty() }?.also { serviceName ->
				treeConfigs.root.children.add(TreeItem(StateConfig(serviceName)).apply {
					children.addAll(
							TreeItem(StateConfig("None", CommonIcon.NONE, emptyList())),
							TreeItem(StateConfig("Unknown", CommonIcon.UNKNOWN, emptyList())),
							TreeItem(StateConfig("OK", CommonIcon.OK, emptyList())),
							TreeItem(StateConfig("Info", CommonIcon.INFO, emptyList())),
							TreeItem(StateConfig("Notification", CommonIcon.NOTIFICATION, emptyList())),
							TreeItem(StateConfig("Connection Error", CommonIcon.BROKEN_LINK, emptyList())),
							TreeItem(StateConfig("Service Error", CommonIcon.UNAVAILABLE, emptyList())),
							TreeItem(StateConfig("Warning", CommonIcon.WARNING, emptyList())),
							TreeItem(StateConfig("Error", CommonIcon.ERROR, emptyList())),
							TreeItem(StateConfig("Fatal", CommonIcon.FATAL, emptyList())))
				})
				comboConfigName.apply {
					items.remove(serviceName)
					selectionModel.clearSelection()
					value = ""
				}
				saveConfig()
			}

	@FXML
	fun onDeleteSelectedConfig() {
		Alert(AlertType.CONFIRMATION).apply {
			title = "Delete confirmation"
			headerText = "Delete confirmation"
			contentText = "Do you really want to delete this whole configuration?"
			buttonTypes.setAll(ButtonType.YES, ButtonType.NO)
		}.showAndWait().ifPresent { btnType ->
			btnType.takeIf { it == ButtonType.YES }?.also {
				var selected = treeConfigs.selectionModel.selectedItem
				while (selected != null && selected.value.lightConfigs != null) selected = selected.parent
				if (selected != null && selected.value.title.isNotEmpty()) selected.parent?.children?.remove(selected)
				saveConfig()
			}
		}
	}

	@FXML
	fun onKeyPressedInTree(keyEvent: KeyEvent) = when (keyEvent.code) {
		KeyCode.DELETE -> onDeleteSelectedConfig()
		KeyCode.C -> if (keyEvent.isControlDown) {
			clipboard.clear()
			clipboard.addAll(tableLightConfigs.items.deepCopy())
			null
		} else null
		KeyCode.V -> if (keyEvent.isControlDown) {
			tableLightConfigs.items.addAll(clipboard.deepCopy())
			null
		} else null
		else -> null
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
		notifier.changeLights(listOf(LightConfig()))
	}

	@FXML
	fun onMoveUpLight() {
		val selectedLight = tableLightConfigs.selectionModel.selectedItem?.deepCopy()
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
		val selectedLight = tableLightConfigs.selectionModel.selectedItem?.deepCopy()
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
			tableLightConfigs.selectionModel.selectedItem?.deepCopy()?.also { clipboard.add(it) }
			null
		} else null
		KeyCode.V -> if (keyEvent.isControlDown) {
			tableLightConfigs.items.addAll(tableLightConfigs.selectionModel.selectedIndex, clipboard.deepCopy())
			null
		} else null
		else -> null
	}

	override fun onConnecting(channel: Channel) {
		btnConnect.text = "Cancel [F5]"
	}

	override fun onConnect(channel: Channel) {
		btnConnect.text = "Disconnect [F5]"
		when (channel) {
			Channel.BLUETOOTH -> iconBluetooth.image = DevOpsLightIcon.BLUETOOTH_CONNECTED.toImage()
			Channel.USB -> iconUSB.image = DevOpsLightIcon.USB_CONNECTED.toImage()
		}
	}

	override fun onDisconnect(channel: Channel) {
		btnConnect.text = "Connect [F5]"
		when (channel) {
			Channel.BLUETOOTH -> iconBluetooth.image = DevOpsLightIcon.BLUETOOTH_DISCONNECTED.toImage()
			Channel.USB -> iconUSB.image = DevOpsLightIcon.USB_DISCONNECTED.toImage()
		}
	}

	override fun onMessageReceived(channel: Channel, message: IntArray) {
	}

	override fun onMessageSent(channel: Channel, message: IntArray, remaining: Int) {
	}

	private fun selectStateConfig(treeItem: TreeItem<StateConfig>?) {
		val lightConfigs = treeItem?.value?.lightConfigs?.deepCopy()
		buttonTestLightSequence.isDisable = treeItem?.value?.lightConfigs == null
		tableLightConfigs.items.clear()
		lightConfigs?.also { tableLightConfigs.items.addAll(it) }
		if (tableLightConfigs.items.size > 0) tableLightConfigs.selectionModel.select(0)
	}

	private fun selectLightConfig(lightConfig: LightConfig?) {
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
			sliderMin.value = lightConfig?.min?.toDouble() ?: 0.0
			sliderMax.value = lightConfig?.max?.toDouble() ?: 100.0
			setLightConfigButtonsEnabled(lightConfig != null)
		}
	}

	private fun selectPattern(pattern: LightPattern) {
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

	private fun createLightConfig(): LightConfig? {
		val pattern = comboBoxPattern.selectionModel.selectedItem
		val color1 = comboBoxColor1.value?.toLightColor()
		val color2 = comboBoxColor2.value?.toLightColor()
		val color3 = comboBoxColor3.value?.toLightColor()
		val color4 = comboBoxColor4.value?.toLightColor()
		val color5 = comboBoxColor5.value?.toLightColor()
		val color6 = comboBoxColor6.value?.toLightColor()
		val color7 = LightColor()
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
			LightConfig(pattern,
					color1, color2, color3, color4, color5, color6, color7,
					delay, width, fade, min, max, timeout)
		else null
	}

	private fun saveConfig() {
		treeConfigs.selectionModel.selectedItem?.value?.lightConfigs = tableLightConfigs.items.deepCopy()
		config.items = treeConfigs.root.children
				.map { it.value to it.children.map { c -> c.value } }
				.filter { (_, children) -> children.size == 10 }
				.filter { (_, children) -> children.all { it.lightConfigs != null } }
				.map { (node, children) ->
					DevOpsLightItemConfig(node.title,
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
				}
		notifier.controller.saveConfig()
		notifier.bluetoothCommunicator.connect(BluetoothCommunicator.Descriptor(config.deviceAddress, 6))
		notifier.usbCommunicator.connect(USBCommunicator.Descriptor(config.usbPort))
	}

	private fun TableColumn<LightConfig, LightColor>.init(propertyName: String) = cell(propertyName) { _, value, empty ->
		graphic = Pane().takeUnless { empty }?.apply {
			background = Background(BackgroundFill(value?.toColor() ?: Color.TRANSPARENT,
					CornerRadii.EMPTY,
					Insets.EMPTY))
		}
	}
}
