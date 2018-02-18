package com.poterion.monitor.notifiers.raspiw2812.ui

import com.poterion.monitor.data.notifiers.NotifierAction
import com.poterion.monitor.notifiers.raspiw2812.control.RaspiW2812NotifierController
import com.poterion.monitor.notifiers.raspiw2812.data.LightConfig
import com.poterion.monitor.notifiers.raspiw2812.data.RaspiW2812Config
import com.poterion.monitor.notifiers.raspiw2812.data.RaspiW2812ItemConfig
import com.poterion.monitor.notifiers.raspiw2812.data.StateConfig
import com.poterion.monitor.ui.Icon
import javafx.collections.FXCollections
import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.geometry.Insets
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.*
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
import kotlin.math.roundToInt


class ConfigWindowController {
	companion object {
		fun create(stage: Stage, config: RaspiW2812Config, controller: RaspiW2812NotifierController) {
			val loader = FXMLLoader(ConfigWindowController::class.java.getResource("config-window.fxml"))
			val root = loader.load<Parent>()
			loader.getController<ConfigWindowController>().apply {
				this.config = config
				this.controller = controller
			}
			val scene = Scene(root, 850.0, 600.0)
			stage.title = "Raspi W2812 Controller Config"
			stage.isResizable = false
			stage.scene = scene
			stage.show()
		}
	}

	@FXML private lateinit var treeConfigs: TreeView<StateConfig>
	@FXML private lateinit var textConfigName: TextField
	@FXML private lateinit var buttonAddConfig: Button
	@FXML private lateinit var buttonDeleteConfig: Button
	@FXML private lateinit var labelColors: Label
	@FXML private lateinit var comboBoxColor1: ColorPicker
	@FXML private lateinit var comboBoxColor2: ColorPicker
	@FXML private lateinit var labelWait: Label
	@FXML private lateinit var labelWidth: Label
	@FXML private lateinit var labelFade: Label
	@FXML private lateinit var labelMin: Label
	@FXML private lateinit var labelMax: Label
	@FXML private lateinit var labelPattern: Label
	@FXML private lateinit var comboBoxPattern: ComboBox<Pair<String, String>>
	@FXML private lateinit var textWait: TextField
	@FXML private lateinit var textWidth: TextField
	@FXML private lateinit var textFade: TextField
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
	@FXML private lateinit var columnLightColor1: TableColumn<LightConfig, Color>
	@FXML private lateinit var columnLightColor2: TableColumn<LightConfig, Color>
	@FXML private lateinit var columnLightWait: TableColumn<LightConfig, Long>
	@FXML private lateinit var columnLightWidth: TableColumn<LightConfig, Int>
	@FXML private lateinit var columnLightFading: TableColumn<LightConfig, Int>
	@FXML private lateinit var columnLightMinimum: TableColumn<LightConfig, Int>
	@FXML private lateinit var columnLightMaximum: TableColumn<LightConfig, Int>

	private var config: RaspiW2812Config = RaspiW2812Config()
		set(value) {
			field = value
			treeConfigs.root = TreeItem(StateConfig("Configurations")).apply {
				children.addAll(config.items.sortedBy { it.id }.map { item ->
					TreeItem(StateConfig(item.id)).apply {
						children.addAll(
								TreeItem(StateConfig("None", Icon.UNKNOWN, item.statusNone)),
								TreeItem(StateConfig("Unknown", Icon.UNKNOWN, item.statusUnknown)),
								TreeItem(StateConfig("OK", Icon.OK, item.statusOk)),
								TreeItem(StateConfig("Info", Icon.INFO, item.statusInfo)),
								TreeItem(StateConfig("Notification", Icon.NOTIFICATION, item.statusNotification)),
								TreeItem(StateConfig("Connectio Error", Icon.INACTIVE, item.statusConnectionError)),
								TreeItem(StateConfig("Service Error", Icon.INACTIVE, item.statusServiceError)),
								TreeItem(StateConfig("Warning", Icon.WARNING, item.statusWarning)),
								TreeItem(StateConfig("Error", Icon.ERROR, item.statusError)),
								TreeItem(StateConfig("Fatal", Icon.FATAL, item.statusFatal)))
					}
				})
			}
		}
	private var controller: RaspiW2812NotifierController? = null

	private val patterns = FXCollections.observableArrayList(
			"light" to "Light",
			"blink" to "Blink",
			"rotation" to "Rotation",
			"wipe" to "Wipe",
			//"spin" to "Spin",
			"chaise" to "Chaise",
			"lighthouse" to "Lighthouse",
			"fade" to "Fade",
			"fadeToggle" to "Fade Toggle")

	@FXML
	fun initialize() {
		comboBoxPattern.apply {
			items?.addAll(patterns)
			selectionModel.select(0)
			converter = object : StringConverter<Pair<String, String>>() {
				override fun toString(obj: Pair<String, String>?): String = obj?.second ?: ""
				override fun fromString(string: String?): Pair<String, String> = patterns
						.firstOrNull { (id, _) -> id == string }
						?: Pair("", "")
			}
			setCellFactory {
				object : ListCell<Pair<String, String>>() {
					public override fun updateItem(item: Pair<String, String>?, empty: Boolean) {
						super.updateItem(item, empty)
						text = item?.second
					}
				}
			}
		}

		val customColors = listOf(Color.RED, Color.GREEN, Color.BLUE,
				Color.MAGENTA, Color.LIME, Color.CYAN,
				Color.AZURE, Color.AQUA, Color.PINK, Color.OLIVE, Color.NAVY, Color.MAROON)
		comboBoxColor1.customColors.addAll(customColors)
		comboBoxColor2.customColors.addAll(customColors)

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
			selectionModel.selectedItemProperty().addListener { _, _, newValue ->
				selectStateConfig(newValue?.value)
				buttonDeleteConfig.isDisable = newValue == null
						|| newValue.value.title.isEmpty()
						|| newValue.parent.value.title.isEmpty()
			}
		}

		tableLightConfigs.apply {
			selectionModel.selectionMode = SelectionMode.SINGLE
			selectionModel.selectedItemProperty().addListener { _, _, newValue -> selectLightConfig(newValue) }
		}

		columnLightPattern.apply {
			setCellValueFactory(PropertyValueFactory<LightConfig, String>("pattern"))
		}
		columnLightColor1.apply {
			cellValueFactory = PropertyValueFactory<LightConfig, Color>("color1")
			setCellFactory {
				object : TableCell<LightConfig, Color>() {
					override fun updateItem(item: Color?, empty: Boolean) {
						super.updateItem(item, empty)
						graphic = Pane().apply {
							background = Background(BackgroundFill(item ?: Color.TRANSPARENT,
									CornerRadii.EMPTY, Insets.EMPTY))
						}
					}
				}
			}
		}
		columnLightColor2.apply {
			cellValueFactory = PropertyValueFactory<LightConfig, Color>("color2")
			setCellFactory {
				object : TableCell<LightConfig, Color>() {
					override fun updateItem(item: Color?, empty: Boolean) {
						super.updateItem(item, empty)
						graphic = Pane().apply {
							background = Background(BackgroundFill(item
									?: Color.TRANSPARENT, CornerRadii.EMPTY, Insets.EMPTY))
						}
					}
				}
			}
		}
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

	@FXML
	fun onAddConfig(event: ActionEvent) {
		textConfigName.text?.trim()?.takeIf { it.isNotEmpty() }?.also { serviceName ->
			treeConfigs.root.children.add(TreeItem(StateConfig(serviceName)).apply {
				children.addAll(
						TreeItem(StateConfig("None", Icon.UNKNOWN, emptyList())),
						TreeItem(StateConfig("Unknown", Icon.UNKNOWN, emptyList())),
						TreeItem(StateConfig("OK", Icon.OK, emptyList())),
						TreeItem(StateConfig("Info", Icon.INFO, emptyList())),
						TreeItem(StateConfig("Notification", Icon.NOTIFICATION, emptyList())),
						TreeItem(StateConfig("Connection Error", Icon.INACTIVE, emptyList())),
						TreeItem(StateConfig("Service Error", Icon.INACTIVE, emptyList())),
						TreeItem(StateConfig("Warning", Icon.WARNING, emptyList())),
						TreeItem(StateConfig("Error", Icon.ERROR, emptyList())),
						TreeItem(StateConfig("Fatal", Icon.FATAL, emptyList())))
			})
			textConfigName.text = ""
			saveConfig()
		}
	}

	@FXML
	fun onDeleteSelectedConfig(event: ActionEvent?) {
		var selected = treeConfigs.selectionModel.selectedItem
		while (selected != null && selected.value.lightConigs != null) selected = selected.parent
		if (selected != null && selected.value.title.isNotEmpty()) selected.parent?.children?.remove(selected)
		saveConfig()
	}

	@FXML
	fun onKeyPressedInTree(keyEvent: KeyEvent) = when (keyEvent.code) {
		KeyCode.DELETE -> onDeleteSelectedConfig(null)
		else -> {
		}
	}

	@FXML
	fun onTestLight(event: ActionEvent) {
		controller?.apply {
			createLightConfig()?.also {
				execute(NotifierAction.DISABLE)
				execute(NotifierAction.NOTIFY, it.toString())
			}
		}
	}

	@FXML
	fun onTestLightSequence(event: ActionEvent) {
		controller?.apply {
			tableLightConfigs.items.map { it.toString() }.takeIf { it.isNotEmpty() }?.also {
				execute(NotifierAction.DISABLE)
				execute(NotifierAction.NOTIFY, *it.toTypedArray())
			}
		}
	}

	@FXML
	fun onTurnOffLight(event: ActionEvent) {
		controller?.apply {
			execute(NotifierAction.DISABLE)
			execute(NotifierAction.NOTIFY, LightConfig(
					pattern = "light",
					color1 = Color.BLACK,
					color2 = Color.BLACK,
					wait = 50L,
					width = 3,
					fading = 0,
					min = 0,
					max = 100).toString())
		}
	}

	@FXML
	fun onMoveUpLight(event: ActionEvent?) {
		val selectedLight = tableLightConfigs.selectionModel.selectedItem
		tableLightConfigs.selectionModel.selectedIndex
				.takeIf { it > 0 && it < tableLightConfigs.items.size }
				?.also {
					tableLightConfigs.items.removeAt(it)
					tableLightConfigs.items.add(it - 1, selectedLight)
					tableLightConfigs.selectionModel.select(it - 1)
				}
		treeConfigs.selectionModel.selectedItem.value.lightConigs = tableLightConfigs.items.map { it.toString() }
		saveConfig()
	}

	@FXML
	fun onMoveDownLight(event: ActionEvent?) {
		val selectedLight = tableLightConfigs.selectionModel.selectedItem
		tableLightConfigs.selectionModel.selectedIndex
				.takeIf { it >= 0 && it < tableLightConfigs.items.size - 1 }
				?.also {
					tableLightConfigs.items.removeAt(it)
					tableLightConfigs.items.add(it + 1, selectedLight)
					tableLightConfigs.selectionModel.select(it + 1)
				}
		treeConfigs.selectionModel.selectedItem.value.lightConigs = tableLightConfigs.items.map { it.toString() }
		saveConfig()
	}

	@FXML
	fun onSaveLight(event: ActionEvent) {
		val selectedLight = tableLightConfigs.selectionModel.selectedItem
		val configuredLight = createLightConfig()
		if (selectedLight == null && configuredLight != null) {
			tableLightConfigs.items.add(configuredLight)
		} else if (selectedLight != null && configuredLight != null) {
			selectedLight.pattern = configuredLight.pattern
			selectedLight.color1 = configuredLight.color1
			selectedLight.color2 = configuredLight.color2
			selectedLight.wait = configuredLight.wait
			selectedLight.width = configuredLight.width
			selectedLight.fading = configuredLight.fading
			selectedLight.min = configuredLight.min
			selectedLight.max = configuredLight.max
		}
		tableLightConfigs.refresh()

		if (configuredLight != null) {
			treeConfigs.selectionModel.selectedItem.value.lightConigs = tableLightConfigs.items.map { it.toString() }
			saveConfig()
			tableLightConfigs.selectionModel.clearSelection()
		}
	}

	@FXML
	fun onClearLight(event: ActionEvent) {
		tableLightConfigs.selectionModel.clearSelection()
	}

	@FXML
	fun onDeleteLight(event: ActionEvent?) {
		tableLightConfigs.selectionModel.selectedIndex
				.takeIf { it >= 0 }
				?.also { tableLightConfigs.items.removeAt(it) }
		treeConfigs.selectionModel.selectedItem.value.lightConigs = tableLightConfigs.items.map { it.toString() }
		saveConfig()
	}

	@FXML
	fun onKeyPressedInTable(keyEvent: KeyEvent) = when (keyEvent.code) {
		KeyCode.DELETE -> onDeleteLight(null)
		KeyCode.UP -> if (keyEvent.isAltDown) onMoveUpLight(null) else null
		KeyCode.DOWN -> if (keyEvent.isAltDown) onMoveDownLight(null) else null
		else -> {
		}
	}

	private fun selectStateConfig(stateConfig: StateConfig?) {
		val lightConfigs = stateConfig?.lightConigs
		buttonTestLightSequence.isDisable = stateConfig?.lightConigs == null
		tableLightConfigs.items.clear()
		lightConfigs?.map { it.toLightConfig() }?.also { tableLightConfigs.items.addAll(it) }
	}

	private fun selectLightConfig(lightConfig: LightConfig?) {
		buttonSaveLight.text = if (lightConfig == null) "Add" else "Save"
		comboBoxPattern.selectionModel.select(patterns.firstOrNull { it.first == lightConfig?.pattern }
				?: patterns.firstOrNull())
		comboBoxColor1.value = lightConfig?.color1 ?: Color.WHITE
		comboBoxColor2.value = lightConfig?.color2 ?: Color.WHITE
		textWait.text = "${lightConfig?.wait ?: 50}"
		textWidth.text = "${lightConfig?.width ?: 3}"
		textFade.text = "${lightConfig?.fading ?: 0}"
		sliderMin.value = lightConfig?.min?.toDouble() ?: 0.0
		sliderMax.value = lightConfig?.max?.toDouble() ?: 100.0
		setLightConfigButtonsEnabled(lightConfig != null)
	}

	/*private fun setConfigEnabled(enabled: Boolean) {
		comboBoxPattern.isDisable = !enabled
		comboBoxColor1.isDisable = !enabled
		comboBoxColor2.isDisable = !enabled
		textWait.isDisable = !enabled
		textWidth.isDisable = !enabled
		textFade.isDisable = !enabled
		sliderMin.isDisable = !enabled
		sliderMax.isDisable = !enabled
		//setConfigButtonsEnabled(enabled)
	}*/

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
		val pattern = comboBoxPattern.selectionModel.selectedItem.first
		val color1 = comboBoxColor1.value
		val color2 = comboBoxColor2.value
		val wait = textWait.text?.toLongOrNull()
		val width = textWidth.text?.toIntOrNull()
		val fade = textFade.text?.toIntOrNull()
		val min = sliderMin.value.roundToInt()
		val max = sliderMax.value.roundToInt()

		return if (color1 != null && color2 != null && wait != null && width != null && fade != null)
			LightConfig(pattern, color1, color2, wait, width, fade, min, max) else null
	}

	private fun String.toLightConfig(): LightConfig? = split(" ")
			.takeIf { it.size == 8 }
			?.let {
				val pattern = it[0].takeIf { patterns.map { (id, _) -> id }.contains(it) }
				val color1 = it[1].split(",")
						.mapNotNull { it.toIntOrNull() }
						.map { it.toDouble() / 255.0 }
						.takeIf { it.size == 3 }
						?.let { Color(it[0], it[1], it[2], 1.0) }
				val color2 = it[2].split(",")
						.mapNotNull { it.toIntOrNull() }
						.map { it.toDouble() / 255.0 }
						.takeIf { it.size == 3 }
						?.let { Color(it[0], it[1], it[2], 1.0) }
				val wait = it[3].toLongOrNull()
				val width = it[4].toIntOrNull()
				val fade = it[5].toIntOrNull()
				val min = it[6].toIntOrNull()
				val max = it[7].toIntOrNull()
				if (pattern != null
						&& color1 != null
						&& color2 != null
						&& wait != null
						&& width != null
						&& fade != null
						&& min != null
						&& max != null)
					LightConfig(pattern, color1, color2, wait, width, fade, min, max)
				else null
			}

	private fun saveConfig() {
		this.config.items = treeConfigs.root.children
				.map { it.value to it.children.map { it.value } }
				.filter { (_, children) -> children.size == 10 }
				.filter { (_, children) -> children.all { it.lightConigs != null } }
				.map { (node, children) ->
					RaspiW2812ItemConfig(node.title,
							statusNone = children[0].lightConigs ?: emptyList(),
							statusUnknown = children[1].lightConigs ?: emptyList(),
							statusOk = children[2].lightConigs ?: emptyList(),
							statusInfo = children[3].lightConigs ?: emptyList(),
							statusNotification = children[4].lightConigs ?: emptyList(),
							statusConnectionError = children[5].lightConigs ?: emptyList(),
							statusServiceError = children[6].lightConigs ?: emptyList(),
							statusWarning = children[7].lightConigs ?: emptyList(),
							statusError = children[8].lightConigs ?: emptyList(),
							statusFatal = children[9].lightConigs ?: emptyList())
				}
		controller?.controller?.saveConfig()
	}
}
