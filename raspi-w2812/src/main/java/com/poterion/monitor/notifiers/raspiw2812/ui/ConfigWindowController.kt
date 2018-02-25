package com.poterion.monitor.notifiers.raspiw2812.ui

import com.poterion.monitor.api.ui.CommonIcon
import com.poterion.monitor.data.notifiers.NotifierAction
import com.poterion.monitor.notifiers.raspiw2812.RaspiW2812Icon
import com.poterion.monitor.notifiers.raspiw2812.control.RaspiW2812Notifier
import com.poterion.monitor.notifiers.raspiw2812.data.*
import com.poterion.monitor.notifiers.raspiw2812.deepCopy
import com.poterion.monitor.notifiers.raspiw2812.services.DetectPortNameService
import com.poterion.monitor.notifiers.raspiw2812.toColor
import com.poterion.monitor.notifiers.raspiw2812.toLightColor
import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.geometry.Insets
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.control.Alert.AlertType
import javafx.scene.control.ButtonType
import javafx.scene.control.cell.PropertyValueFactory
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.layout.Background
import javafx.scene.layout.BackgroundFill
import javafx.scene.layout.CornerRadii
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import javafx.stage.Stage
import javafx.util.StringConverter
import jssc.SerialPortList
import kotlin.math.roundToInt


/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
class ConfigWindowController {
	companion object {
		private val AUTODETECT: Pair<String?, String> = null to "[Autodetect]"

		fun create(stage: Stage, config: RaspiW2812Config, controller: RaspiW2812Notifier) {
			val root = getRoot(config, controller)
			val scene = Scene(root, 850.0, 600.0)
			RaspiW2812Icon.RASPBERRY_PI.image().also { stage.icons.add(it) }
			stage.title = "Raspi W2812 Controller Config"
			stage.isResizable = false
			stage.scene = scene
			stage.show()
		}

		internal fun getRoot(config: RaspiW2812Config, controller: RaspiW2812Notifier): Parent =
				FXMLLoader(ConfigWindowController::class.java.getResource("config-window.fxml"))
						.let { it.load<Parent>() to it.getController<ConfigWindowController>() }
						.let { (root, ctrl) ->
							ctrl.config = config
							ctrl.controller = controller
							ctrl.load()
							root
						}
	}

	@FXML private lateinit var comboPortName: ComboBox<Pair<String?, String>>
	@FXML private lateinit var treeConfigs: TreeView<StateConfig>
	@FXML private lateinit var comboConfigName: ComboBox<String>
	@FXML private lateinit var buttonAddConfig: Button
	@FXML private lateinit var buttonDeleteConfig: Button
	@FXML private lateinit var labelColors: Label
	@FXML private lateinit var comboBoxColor1: ColorPicker
	@FXML private lateinit var comboBoxColor2: ColorPicker
	@FXML private lateinit var comboBoxColor3: ColorPicker
	@FXML private lateinit var comboBoxColor4: ColorPicker
	@FXML private lateinit var comboBoxColor5: ColorPicker
	@FXML private lateinit var comboBoxColor6: ColorPicker
	@FXML private lateinit var labelWait: Label
	@FXML private lateinit var labelWidth: Label
	@FXML private lateinit var labelFade: Label
	@FXML private lateinit var labelMin: Label
	@FXML private lateinit var labelMax: Label
	@FXML private lateinit var labelPattern: Label
	@FXML private lateinit var textServiceName: TextField
	@FXML private lateinit var comboBoxPattern: ComboBox<LightPattern>
	@FXML private lateinit var textWait: TextField
	@FXML private lateinit var textWidth: TextField
	@FXML private lateinit var textFade: TextField
	@FXML private lateinit var labelMinValue: Label
	@FXML private lateinit var labelMaxValue: Label
	@FXML private lateinit var sliderMin: Slider
	@FXML private lateinit var sliderMax: Slider
	@FXML private lateinit var buttonTestLight: Button
	@FXML private lateinit var buttonTestLightSequence: Button
	@FXML private lateinit var buttonTurnOffLight: Button
	@FXML private lateinit var buttonMoveUpLight: Button
	@FXML private lateinit var buttonMoveDownLight: Button
	@FXML private lateinit var buttonSaveLight: Button
	@FXML private lateinit var buttonClearLight: Button
	@FXML private lateinit var buttonDeleteLight: Button
	@FXML private lateinit var tableLightConfigs: TableView<LightConfig>
	@FXML private lateinit var columnLightPattern: TableColumn<LightConfig, String>
	@FXML private lateinit var columnLightColor1: TableColumn<LightConfig, LightColor>
	@FXML private lateinit var columnLightColor2: TableColumn<LightConfig, LightColor>
	@FXML private lateinit var columnLightColor3: TableColumn<LightConfig, LightColor>
	@FXML private lateinit var columnLightColor4: TableColumn<LightConfig, LightColor>
	@FXML private lateinit var columnLightColor5: TableColumn<LightConfig, LightColor>
	@FXML private lateinit var columnLightColor6: TableColumn<LightConfig, LightColor>
	@FXML private lateinit var columnLightWait: TableColumn<LightConfig, Long>
	@FXML private lateinit var columnLightWidth: TableColumn<LightConfig, Int>
	@FXML private lateinit var columnLightFading: TableColumn<LightConfig, Int>
	@FXML private lateinit var columnLightMinimum: TableColumn<LightConfig, Int>
	@FXML private lateinit var columnLightMaximum: TableColumn<LightConfig, Int>

	private var config: RaspiW2812Config? = null
	private var controller: RaspiW2812Notifier? = null
	private val clipboard = mutableListOf<LightConfig>()

	private val patterns = FXCollections.observableArrayList(
			LightPattern("light", "Light", hasColor1 = true, hasColor2 = true, hasColor5 = true, hasColor6 = true, hasWait = true),
			LightPattern("blink", "Blink", hasColor1 = true, hasColor2 = true, hasColor5 = true, hasColor6 = true, hasWait = true),
			LightPattern("rotation", "Rotation", hasColor1 = true, hasColor2 = true, hasColor5 = true, hasColor6 = true, hasWait = true, hasWidth = true, hasFading = true),
			LightPattern("wipe", "Wipe", hasColor1 = true, hasColor2 = true, hasColor5 = true, hasColor6 = true, hasWait = true, hasFading = true),
			//LightPattern("spin" , "Spin", hasColor1 = true, hasColor2 = true, hasColor5 = true, hasColor6 = true, hasWait = true),
			LightPattern("chaise", "Chaise", hasColor1 = true, hasColor2 = true, hasColor5 = true, hasColor6 = true, hasWait = true, hasWidth = true, hasFading = true),
			LightPattern("lighthouse", "Lighthouse", hasColor1 = true, hasColor2 = true, hasColor3 = true, hasColor4 = true, hasColor5 = true, hasColor6 = true, hasWait = true, hasWidth = true, hasFading = true),
			LightPattern("fade", "Fade", hasColor1 = true, hasColor2 = true, hasColor5 = true, hasColor6 = true, hasWait = true, hasMin = true, hasMax = true),
			LightPattern("fadeToggle", "Fade Toggle", hasColor1 = true, hasColor2 = true, hasColor5 = true, hasColor6 = true, hasWait = true, hasMin = true, hasMax = true),
			LightPattern("theater", "Theater", hasColor1 = true, hasColor2 = true, hasColor5 = true, hasColor6 = true, hasWait = true, hasFading = true, fadingTitle = "Iterations"),
			LightPattern("rainbow", "Rainbow", hasWait = true, hasFading = true, fadingTitle = "Iterations"),
			LightPattern("rainbowCycle", "Rainbow Cycle", hasWait = true, hasFading = true, fadingTitle = "Iterations"),
			LightPattern("wait", "Wait", hasWait = true),
			LightPattern("clear", "Clear"))

	@FXML
	fun initialize() {
		comboPortName.apply {
			converter = object : StringConverter<Pair<String?, String>>() {
				override fun toString(obj: Pair<String?, String>?): String = obj?.second ?: ""

				override fun fromString(string: String?): Pair<String?, String>? = string?.split(" ")
						?.get(0)
						?.let { it to string }
			}
			setCellFactory {
				object : ListCell<Pair<String?, String>>() {
					public override fun updateItem(item: Pair<String?, String>?, empty: Boolean) {
						super.updateItem(item, empty)
						text = item?.second
					}
				}
			}
		}

		textServiceName.isEditable = false
		textServiceName.isDisable = true
//		textServiceName.textProperty().addListener { _, _, value ->
//			value?.takeIf { it.isNotEmpty() }?.also {
//				val item = treeConfigs.selectionModel.selectedItem
//						?.let { it.value.takeIf { it.lightConfigs == null } ?: it.parent.value }
//				item?.title = it
//				treeConfigs.refresh()
//				saveConfig()
//			}
//		}

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
			setCellFactory {
				object : ListCell<LightPattern>() {
					public override fun updateItem(item: LightPattern?, empty: Boolean) {
						super.updateItem(item, empty)
						text = item?.title
					}
				}
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
			setCellFactory {
				object : TreeCell<StateConfig>() {
					override fun updateItem(item: StateConfig?, empty: Boolean) {
						super.updateItem(item, empty)
						text = item?.title?.let { if (it.isEmpty()) "Default" else it }
						graphic = item?.icon?.inputStream?.use { ImageView(Image(it, 16.0, 16.0, false, false)) }
					}
				}
			}
			selectionModel.selectedItemProperty().addListener { _, _, item ->
				selectStateConfig(item?.value)
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

		columnLightPattern.apply {
			setCellValueFactory(PropertyValueFactory<LightConfig, String>("pattern"))
		}

		fun TableColumn<LightConfig, LightColor>.init(propertyName: String) {
			cellValueFactory = PropertyValueFactory<LightConfig, LightColor>(propertyName)
			setCellFactory {
				object : TableCell<LightConfig, LightColor>() {
					override fun updateItem(item: LightColor?, empty: Boolean) {
						super.updateItem(item, empty)
						graphic = Pane().apply {
							background = Background(BackgroundFill(item?.toColor() ?: Color.TRANSPARENT,
									CornerRadii.EMPTY, Insets.EMPTY))
						}
					}
				}
			}
		}
		columnLightColor1.init("color1")
		columnLightColor2.init("color2")
		columnLightColor3.init("color3")
		columnLightColor4.init("color4")
		columnLightColor5.init("color5")
		columnLightColor6.init("color6")

		columnLightWait.apply {
			cellValueFactory = PropertyValueFactory<LightConfig, Long>("wait")
			setCellFactory {
				object : TableCell<LightConfig, Long>() {
					override fun updateItem(item: Long?, empty: Boolean) {
						super.updateItem(item, empty)
						text = item?.let { "${it} ms" }
					}
				}
			}
		}
		columnLightWidth.apply {
			cellValueFactory = PropertyValueFactory<LightConfig, Int>("width")
			setCellFactory {
				object : TableCell<LightConfig, Int>() {
					override fun updateItem(item: Int?, empty: Boolean) {
						super.updateItem(item, empty)
						text = item?.toString()
					}
				}
			}
		}
		columnLightFading.apply {
			cellValueFactory = PropertyValueFactory<LightConfig, Int>("fading")
			setCellFactory {
				object : TableCell<LightConfig, Int>() {
					override fun updateItem(item: Int?, empty: Boolean) {
						super.updateItem(item, empty)
						text = item?.toString()
					}
				}
			}
		}
		columnLightMinimum.apply {
			cellValueFactory = PropertyValueFactory<LightConfig, Int>("min")
			setCellFactory {
				object : TableCell<LightConfig, Int>() {
					override fun updateItem(item: Int?, empty: Boolean) {
						super.updateItem(item, empty)
						text = item?.toString()
					}
				}
			}
		}
		columnLightMaximum.apply {
			cellValueFactory = PropertyValueFactory<LightConfig, Int>("max")
			setCellFactory {
				object : TableCell<LightConfig, Int>() {
					override fun updateItem(item: Int?, empty: Boolean) {
						super.updateItem(item, empty)
						text = item?.toString()
					}
				}
			}
		}
		treeConfigs.selectionModel.clearSelection()
		selectStateConfig(null)
		selectLightConfig(null)
	}

	private fun load() {
		updateComboPortName()
		DetectPortNameService().apply {
			setOnSucceeded { it.source.value?.takeIf { it is String }?.also { updateComboPortName(it as String) } }
		}.start()

		treeConfigs.root = TreeItem(StateConfig("Configurations")).apply {
			config?.items
					?.sortedBy { it.id }
					?.map { item ->
						TreeItem(StateConfig(item.id)).apply {
							children.addAll(
									TreeItem(StateConfig("None", CommonIcon.UNKNOWN, item.statusNone)),
									TreeItem(StateConfig("Unknown", CommonIcon.UNKNOWN, item.statusUnknown)),
									TreeItem(StateConfig("OK", CommonIcon.OK, item.statusOk)),
									TreeItem(StateConfig("Info", CommonIcon.INFO, item.statusInfo)),
									TreeItem(StateConfig("Notification", CommonIcon.NOTIFICATION, item.statusNotification)),
									TreeItem(StateConfig("Connectio Error", CommonIcon.INACTIVE, item.statusConnectionError)),
									TreeItem(StateConfig("Service Error", CommonIcon.INACTIVE, item.statusServiceError)),
									TreeItem(StateConfig("Warning", CommonIcon.WARNING, item.statusWarning)),
									TreeItem(StateConfig("Error", CommonIcon.ERROR, item.statusError)),
									TreeItem(StateConfig("Fatal", CommonIcon.FATAL, item.statusFatal)))
						}
					}
					?.also { children.addAll(it) }
		}
		comboConfigName.apply {
			controller?.controller
					?.config
					?.services
					?.map { it.name }
					?.filter { config?.items?.map { it.id }?.contains(it) == false }
					?.distinct()
					?.sorted()
					?.takeIf { it.isNotEmpty() }
					?.also { items?.addAll(it) }
			selectionModel.clearSelection()
			this.value = ""
		}
	}

	@FXML
	fun onPortNameSelected() {
		config?.portName = comboPortName.selectionModel.selectedItem.takeIf { it != AUTODETECT }?.first
		controller?.reset()
		if (config?.enabled == true) controller?.controller?.check(force = true)
	}

	@FXML
	fun onKeyPressed(keyEvent: KeyEvent) = when (keyEvent.code) {
		KeyCode.F3 -> onTestLight()
		KeyCode.F4 -> onTestLightSequence()
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
			//headerText = "Look, a Confirmation Dialog"
			contentText = "Do you really want to delete this whole configuration?"
			buttonTypes.setAll(ButtonType.YES, ButtonType.NO)
		}.showAndWait().ifPresent {
			it.takeIf { it == ButtonType.YES }?.also {
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
		controller?.apply {
			createLightConfig()?.also {
				execute(NotifierAction.DISABLE)
				//execute(NotifierAction.NOTIFY, objectMapper.writeValueAsString(listOf(it)))
				changeLights(listOf(it))
			}
		}
	}

	@FXML
	fun onTestLightSequence() {
		controller?.apply {
			tableLightConfigs.items.takeIf { it.isNotEmpty() }?.also {
				execute(NotifierAction.DISABLE)
				//execute(NotifierAction.NOTIFY, objectMapper.writeValueAsString(it))
				changeLights(it)
			}
		}
	}

	@FXML
	fun onTurnOffLight() {
		controller?.apply {
			execute(NotifierAction.DISABLE)
			//execute(NotifierAction.NOTIFY, objectMapper.writeValueAsString(listOf(LightConfig())))
			execute(NotifierAction.SHUTDOWN)
		}
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

	private fun updateComboPortName(detected: String? = null) {
		val ports = SerialPortList.getPortNames()
				.map { it to if (detected != null && it == detected) "${it} (Detected)" else it }
				.toMutableList()
		config?.portName
				?.takeIf { !ports.map { (port, _) -> port }.contains(it) }
				?.also { ports.add(it to "${it} (Not Found)") }
		comboPortName.apply {
			val selected = config?.portName
			items.apply {
				clear()
				add(AUTODETECT)
				addAll(ports.sortedBy { it.first })
			}
			config?.portName = selected
			selectionModel.select(ports.firstOrNull { (port, _) -> port == config?.portName } ?: AUTODETECT)
		}
	}

	private fun selectStateConfig(stateConfig: StateConfig?) {
		val lightConfigs = stateConfig?.lightConfigs?.deepCopy()
		buttonTestLightSequence.isDisable = stateConfig?.lightConfigs == null
		tableLightConfigs.items.clear()
		lightConfigs?.also { tableLightConfigs.items.addAll(it) }
	}

	private fun selectLightConfig(lightConfig: LightConfig?) {
		buttonSaveLight.text = if (lightConfig == null) "Add [Ctrl+S]" else "Save [Ctrl+S]"
		(patterns.firstOrNull { it.id == lightConfig?.pattern } ?: patterns.first()).also { pattern ->
			comboBoxPattern.selectionModel.select(pattern)
			selectPattern(pattern)
			comboBoxColor1.value = lightConfig?.color1?.toColor() ?: Color.WHITE
			comboBoxColor2.value = lightConfig?.color2?.toColor() ?: Color.WHITE
			comboBoxColor3.value = lightConfig?.color3?.toColor() ?: Color.WHITE
			comboBoxColor4.value = lightConfig?.color4?.toColor() ?: Color.WHITE
			comboBoxColor5.value = lightConfig?.color5?.toColor() ?: Color.WHITE
			comboBoxColor6.value = lightConfig?.color6?.toColor() ?: Color.WHITE
			textWait.text = "${lightConfig?.wait ?: 50}"
			textWidth.text = "${lightConfig?.width ?: 3}"
			textFade.text = "${lightConfig?.fading ?: 0}"
			sliderMin.value = lightConfig?.min?.toDouble() ?: 0.0
			sliderMax.value = lightConfig?.max?.toDouble() ?: 100.0
			setLightConfigButtonsEnabled(lightConfig != null)
		}
	}

	private fun selectPattern(pattern: LightPattern) {
		comboBoxColor1.isDisable = !pattern.hasColor1
		comboBoxColor2.isDisable = !pattern.hasColor2
		comboBoxColor3.isDisable = !pattern.hasColor3
		comboBoxColor4.isDisable = !pattern.hasColor4
		comboBoxColor5.isDisable = !pattern.hasColor5
		comboBoxColor6.isDisable = !pattern.hasColor6
		textWait.isDisable = !pattern.hasWait
		textWidth.isDisable = !pattern.hasWidth
		textFade.isDisable = !pattern.hasFading
		sliderMin.isDisable = !pattern.hasMin
		sliderMax.isDisable = !pattern.hasMax
		labelFade.text = pattern.fadingTitle
	}

	private fun setLightConfigButtonsEnabled(enabled: Boolean) {
		//buttonSaveLight.isDisable = !enabled
		buttonMoveUpLight.isDisable = !enabled
				|| tableLightConfigs.selectionModel.selectedIndex == 0
		buttonMoveDownLight.isDisable = !enabled
				|| tableLightConfigs.selectionModel.selectedIndex == tableLightConfigs.items.size - 1
		buttonClearLight.isDisable = !enabled
		buttonDeleteLight.isDisable = !enabled
	}

	private fun createLightConfig(): LightConfig? {
		val pattern = comboBoxPattern.selectionModel.selectedItem?.id
		val color1 = comboBoxColor1.value?.toLightColor()
		val color2 = comboBoxColor2.value?.toLightColor()
		val color3 = comboBoxColor3.value?.toLightColor()
		val color4 = comboBoxColor4.value?.toLightColor()
		val color5 = comboBoxColor5.value?.toLightColor()
		val color6 = comboBoxColor6.value?.toLightColor()
		val wait = textWait.text?.toLongOrNull()
		val width = textWidth.text?.toIntOrNull()
		val fade = textFade.text?.toIntOrNull()
		val min = sliderMin.value.roundToInt()
		val max = sliderMax.value.roundToInt()

		return if (pattern != null
				&& color1 != null
				&& color2 != null
				&& color3 != null
				&& color4 != null
				&& color5 != null
				&& color6 != null
				&& wait != null
				&& width != null
				&& fade != null)
			LightConfig(pattern, color1, color2, color3, color4, color5, color6, wait, width, fade, min, max) else null
	}

	private fun saveConfig() {
		treeConfigs.selectionModel.selectedItem?.value?.lightConfigs = tableLightConfigs.items.deepCopy()
		config?.items = treeConfigs.root.children
				.map { it.value to it.children.map { it.value } }
				.filter { (_, children) -> children.size == 10 }
				.filter { (_, children) -> children.all { it.lightConfigs != null } }
				.map { (node, children) ->
					RaspiW2812ItemConfig(node.title,
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
		controller?.controller?.saveConfig()
	}
}
