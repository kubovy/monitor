package com.poterion.monitor.notifiers.deploymentcase.ui

import com.poterion.utils.javafx.autoFitTable
import com.poterion.utils.kotlin.noop
import com.poterion.monitor.api.utils.toColor
import com.poterion.monitor.api.utils.toHex
import com.poterion.monitor.notifiers.deploymentcase.control.findInStateMachine
import com.poterion.monitor.notifiers.deploymentcase.data.*
import com.poterion.monitor.notifiers.deploymentcase.getDisplayString
import javafx.collections.FXCollections
import javafx.collections.ListChangeListener
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.control.*
import javafx.scene.control.cell.PropertyValueFactory
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.paint.Color
import javafx.util.Callback
import javafx.util.StringConverter
import kotlin.math.pow

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
class ConfigWindowTabVariables {
	companion object {
		internal fun getRoot(config: DeploymentCaseConfig, saveConfig: () -> Unit): Pair<ConfigWindowTabVariables, Parent> =
				FXMLLoader(ConfigWindowTabVariables::class.java.getResource("config-window-tab-variables.fxml"))
						.let { it.load<Parent>() to it.getController<ConfigWindowTabVariables>() }
						.let { (root, ctrl) ->
							ctrl.config = config
							ctrl.saveConfig = saveConfig
							ctrl to root
						}

	}

	@FXML private lateinit var tableVariables: TableView<Variable>
	@FXML private lateinit var columnVariableName: TableColumn<Variable, String>
	@FXML private lateinit var columnVariableType: TableColumn<Variable, VariableType>
	@FXML private lateinit var columnVariableValue: TableColumn<Variable, String>

	private lateinit var config: DeploymentCaseConfig
	private lateinit var saveConfig: () -> Unit
	private var variableToCopy: Variable? = null

	@FXML
	fun initialize() {
		columnVariableName.initEditableText(
				propertyName = "name",
				styler = { variable ->
					when {
						SharedUiData.jobStatus.values.find { it == variable?.name } != null -> "-fx-font-weight: bold; -fx-font-style: italic;"
						findInStateMachine { it is Action && it.value == variable?.name }.isNotEmpty() -> "-fx-font-weight: bold;"
						else -> null
					}
				},
				isReadOnly = { variable -> variable?.type == VariableType.BOOLEAN || variable?.isUsed() != false },
				itemUpdater = { variable, name -> variable.name = name },
				saveConfig = { saveConfig() })
		columnVariableType.initEditableCombo(
				propertyName = "type",
				itemsProvider = { VariableType.values().filterNot { it == VariableType.BOOLEAN || it == VariableType.STATE } },
				toString = { it?.description ?: "" },
				fromString = { str -> VariableType.values().find { it.description == str } },
				isReadOnly = { variable -> variable?.type == VariableType.STATE || variable?.type == VariableType.BOOLEAN || variable?.isUsed() != false },
				itemUpdater = { variable, type ->
					if (variable.type != type) {
						variable.value = when (type) {
							//VariableType.BOOLEAN -> "false"
							VariableType.STRING -> ""
							VariableType.COLOR_PATTERN -> "0,#000000,10,0x00,0x20"
							//VariableType.STATE -> ""
							VariableType.ENTER -> "0|Enter Version|##.##.##"
							else -> ""
						}
						tableVariables.refresh()
					}
					variable.type = type
				})
		columnVariableValue.initEditableVariableValue(
				propertyName = "value",
				isReadOnly = { variable -> variable?.type == VariableType.STATE || variable?.type == VariableType.BOOLEAN })
		tableVariables.sortOrder.setAll(columnVariableName)
		tableVariables.isEditable = true
		SharedUiData.variablesProperty.addListener { _, _, variables ->
			tableVariables.items = variables
			tableVariables.items.addListener(ListChangeListener { tableVariables.refresh() })
			tableVariables.refresh()
			tableVariables.autoFitTable()
		}
		tableVariables.items = SharedUiData.variables
		tableVariables.items.addListener(ListChangeListener { tableVariables.refresh() })
		tableVariables.refresh()
		tableVariables.autoFitTable()
	}

	@FXML
	fun onVariableTableKeyPressed(keyEvent: KeyEvent) {
		when (keyEvent.code) {
			KeyCode.HELP, // MacOS mapping of INSERT key
			KeyCode.INSERT -> {
				tableVariables.items.add(Variable(type = VariableType.STRING))
				saveConfig()
			}
			KeyCode.C -> if (keyEvent.isControlDown) {
				variableToCopy = tableVariables.selectionModel.selectedItem.copy()
			}
			KeyCode.V -> if (keyEvent.isControlDown) {
				variableToCopy?.copy()
						?.also { it.name = "${it.name}_copy" }
						?.also { SharedUiData.variables.add(it) }
			}
			KeyCode.DELETE -> tableVariables.selectionModel.selectedItem
					.takeIf { it != null }
					?.takeIf { variable -> !variable.isUsed() }
					?.takeIf { _ ->
						Alert(Alert.AlertType.CONFIRMATION).apply {
							title = "Delete confirmation"
							headerText = "Do you want to delete selected row?"
						}.showAndWait().filter { it === ButtonType.OK }.isPresent
					}
					?.also { tableVariables.items.remove(it) }
					?.also { saveConfig() }
			else -> noop()
		}
	}

	private fun TableColumn<Variable, String>.initEditableVariableValue(propertyName: String,
																		isReadOnly: (Variable?) -> Boolean) {
		cellValueFactory = PropertyValueFactory<Variable, String>(propertyName)
		cellFactory = Callback<TableColumn<Variable, String>, TableCell<Variable, String>> { _ ->
			object : TableCell<Variable, String>() {
				private var checkBox: CheckBox? = null
				private var textField1: TextField? = null
				private var textField2: TextField? = null
				private var combo: ComboBox<String>? = null
				private var patternCombo: ComboBox<LightPattern>? = null
				private var colorPicker: ColorPicker? = null
				private var minSlider: Slider? = null
				private var maxSlider: Slider? = null
				private val setButton = Button("Set").apply {
					minWidth = 40.0
					maxWidth = 40.0
					setOnAction { commitEdit(save()) }
				}

				private val itemText: String?
					get() = tableView.items[index]?.type?.let { type -> getDisplayString(item, type) }

				private val itemStyle: String?
					get() = tableView.items[index]?.type?.let { type ->
						when (type) {
							VariableType.COLOR_PATTERN -> {
								val background = item?.split(",")?.getOrNull(1)?.toColor() ?: Color.WHITE
								val text = background.textColor()
								return "-fx-text-fill: ${text.toHex()}; -fx-background-color: ${background.toHex()};"
							}
							else -> null
						}
					}

				override fun startEdit() {
					super.startEdit()
					val entry = tableView.items[index]
					if (!isEmpty && !isReadOnly(tableRow.item as? Variable)) {
						text = null
						graphic = when (entry.type) {
							VariableType.BOOLEAN -> createCheckBox()
							VariableType.STRING -> createTextField()
							VariableType.COLOR_PATTERN -> createColorPicker()
							VariableType.STATE -> null
							VariableType.ENTER -> createInputControls()
						}
						style = null
						textField1?.selectAll()
					}
				}

				override fun cancelEdit() {
					super.cancelEdit()
					commitEdit(save())
					text = itemText
					graphic = null
					style = itemStyle
				}

				override fun updateItem(item: String?, empty: Boolean) {
					super.updateItem(item, empty)
					if (empty) {
						text = null
						graphic = null
						style = null
					} else {
						if (isEditing) {
							textField1?.text = itemText
							text = null
							graphic = textField1
							style = null
						} else {
							text = itemText
							graphic = null
							style = itemStyle
						}
					}
				}

				private fun createTextField(): TextField? {
					textField1 = TextField(itemText)
					textField1?.minWidth = width - graphicTextGap * 2
					textField1?.focusedProperty()?.addListener { _, _, newValue -> if (!newValue) commitEdit(save()) }
					textField1?.setOnAction { commitEdit(save()) }
					return textField1
				}

				private fun createCheckBox(): CheckBox? {
					checkBox = CheckBox("")
					checkBox?.isSelected = item?.toBoolean() == true
					checkBox?.minWidth = width - graphicTextGap * 2
					checkBox?.focusedProperty()?.addListener { _, _, newValue -> if (!newValue) commitEdit(save()) }
					return checkBox
				}

				private fun createColorPicker(): HBox? {
					val pattern = item?.split(",")
							?.map { it.toIntOrNull() }
							?.firstOrNull()
							?.let { LightPattern.values()[it] }
							?: LightPattern.LIGHT


					patternCombo = ComboBox(FXCollections.observableArrayList(LightPattern.values().asList())).apply {
						minWidth = 130.0
						maxWidth = 130.0
						converter = object : StringConverter<LightPattern>() {
							override fun toString(pattern: LightPattern?) = pattern?.description
							override fun fromString(string: String?) = LightPattern.values()
									.find { it.description == string }
									?: LightPattern.LIGHT
						}
						selectionModel?.select(pattern)
					}

					val color = item?.split(",")?.getOrNull(1)?.toColor() ?: Color.BLACK
					colorPicker = ColorPicker(color).apply {
						minWidth = 110.0
						maxWidth = 110.0
						customColors?.addAll(config.customColors?.mapNotNull { it.toColor() }?.takeIf { it.isNotEmpty() }
								?: listOf(Color.RED, Color.YELLOW, Color.LIME, Color.CYAN, Color.BLUE, Color.MAGENTA))
						customColors?.addListener(ListChangeListener<Color> { change ->
							config.customColors = change.list.map { it.toHex() }
							saveConfig()
						})
					}


					textField1 = TextField(item.split(",").getOrNull(2) ?: "10").apply {
						minWidth = 75.0
						maxWidth = 75.0
					}

					val min = item.split(",").getOrNull(3)?.split("x")?.getOrNull(1)?.toIntOrNull(16)?.toDouble() ?: 0.0
					val minLabel = Label("%.1f%%".format(min * 100 / 256)).apply {
						minWidth = 50.0
						maxWidth = 50.0
						maxHeight = Double.MAX_VALUE
					}

					minSlider = Slider(0.0, 256.0, min).apply {
						maxWidth = Double.MAX_VALUE
						maxHeight = Double.MAX_VALUE
						blockIncrement = 1.0
						majorTickUnit = 1.0
						isSnapToTicks = true
						//isShowTickMarks= true
						valueProperty().addListener { _, _, v -> minLabel.text = "%.1f%%".format(v.toDouble() * 100 / 256) }
					}
					HBox.setHgrow(minSlider, Priority.ALWAYS)

					val max = item.split(",").getOrNull(4)?.split("x")?.getOrNull(1)?.toIntOrNull(16)?.toDouble()
							?: 32.0
					val maxLabel = Label("%.1f%%".format(max * 100 / 256)).apply {
						minWidth = 50.0
						maxWidth = 50.0
						maxHeight = Double.MAX_VALUE
					}

					maxSlider = Slider(0.0, 256.0, max).apply {
						maxWidth = Double.MAX_VALUE
						maxHeight = Double.MAX_VALUE
						blockIncrement = 1.0
						majorTickUnit = 1.0
						isSnapToTicks = true
						//isShowTickMarks= true
						valueProperty().addListener { _, _, v -> maxLabel.text = "%.1f%%".format(v.toDouble() * 100 / 256) }
					}
					HBox.setHgrow(maxSlider, Priority.ALWAYS)

					val box = HBox(patternCombo, colorPicker, textField1, minSlider, minLabel, maxSlider, maxLabel,
							setButton).apply {
						minWidth = width - graphicTextGap * 2
						maxWidth = Double.MAX_VALUE
					}
					HBox.setHgrow(box, Priority.ALWAYS)
					return box
				}

				private fun createInputControls(): HBox? {
					combo = ComboBox(FXCollections.observableArrayList((0..9).map { "${it}" })).apply {
						minWidth = 60.0
						maxWidth = 60.0
						selectionModel?.select(item.split("|").getOrNull(0) ?: "0")
					}

					textField1 = TextField(item.split("|").getOrNull(1) ?: "Enter version").apply {
						minWidth = 75.0
						maxWidth = Double.MAX_VALUE
					}

					textField2 = TextField(item.split("|").getOrNull(2) ?: "##.##.##").apply {
						minWidth = 75.0
						maxWidth = Double.MAX_VALUE
					}

					val box = HBox(combo, textField1, textField2, setButton).apply {
						minWidth = width - graphicTextGap * 2
						maxWidth = Double.MAX_VALUE
					}
					HBox.setHgrow(box, Priority.ALWAYS)
					return box
				}

				private fun save(): String? = tableView.items[index]?.let { entry ->
					when (entry.type) {
						VariableType.BOOLEAN -> if (checkBox?.isSelected == true) "true" else "false"
						VariableType.STRING -> textField1?.text ?: ""
						VariableType.COLOR_PATTERN -> {
							listOf(patternCombo?.selectionModel?.selectedIndex ?: 0,
									colorPicker?.value?.toHex() ?: "#000000",
									textField1?.text?.toIntOrNull() ?: 10,
									minSlider?.value?.toInt()?.let { "0x%02X".format(it) } ?: "0x00",
									maxSlider?.value?.toInt()?.let { "0x%02X".format(it) } ?: "0x20")
									.joinToString(",")

						}
						VariableType.STATE -> null
						VariableType.ENTER -> "${combo?.selectionModel?.selectedItem ?: "0"}|${textField1?.text
								?: ""}|${textField2?.text ?: ""}"
					}
				}
			}
		}
		setOnEditCommit { event ->
			event?.tableView?.items?.also { items ->
				val item = items[event.tablePosition.row]
				if (item != null) {
					item.value = event.newValue
					saveConfig()
				}
			}
		}
	}

	private fun <Entry, Type> TableColumn<Entry, Type>.initEditableCombo(propertyName: String,
																		 itemsProvider: (Entry?) -> List<Type>,
																		 toString: (Type?) -> String,
																		 fromString: (String?) -> Type?,
																		 isReadOnly: (Entry?) -> Boolean,
																		 itemUpdater: (Entry, Type) -> Unit) {
		cellValueFactory = PropertyValueFactory<Entry, Type>(propertyName)
		cellFactory = Callback<TableColumn<Entry, Type>, TableCell<Entry, Type>> {
			object : TableCell<Entry, Type>() {
				private var comboBox: ComboBox<Type>? = null

				override fun startEdit() {
					super.startEdit()
					@Suppress("UNCHECKED_CAST")
					if (!isEmpty && !isReadOnly(tableRow.item as? Entry)) {
						text = null
						graphic = createComboBox()
					}
				}

				override fun cancelEdit() {
					super.cancelEdit()
					commitEdit(save())
					text = toString(item)
					graphic = null
				}

				override fun updateItem(item: Type?, empty: Boolean) {
					super.updateItem(item, empty)
					if (empty) {
						text = null
						graphic = null
					} else {
						if (isEditing) {
							comboBox?.selectionModel?.select(item)
							text = null
							graphic = comboBox
						} else {
							text = toString(item)
							graphic = null
						}
					}
				}

				private fun createComboBox(): ComboBox<Type>? {
					comboBox = ComboBox(FXCollections.observableArrayList(itemsProvider(tableView.items[index]).toList())).apply {
						minWidth = width - graphicTextGap * 2
						selectionModel?.select(item)
						converter = object : StringConverter<Type>() {
							override fun toString(item: Type) = toString(item)
							override fun fromString(string: String?) = fromString(string)
						}
						focusedProperty()?.addListener { _, _, newValue -> if (!newValue) commitEdit(save()) }
						selectionModel?.selectedItemProperty()?.addListener { _, _, newValue -> commitEdit(newValue) }
					}

					return comboBox
				}

				private fun save() = comboBox?.selectionModel?.selectedItem
			}
		}


		setOnEditCommit { event ->
			event?.tableView?.items?.also { items ->
				val item = items[event.tablePosition.row]
				if (item != null) itemUpdater(item, event.newValue)
				saveConfig()
			}
		}
	}

	private fun Variable.isUsed(): Boolean = SharedUiData.jobStatus.values.find { it == name } != null
			|| findInStateMachine { it is Action && it.value == name }.isNotEmpty()

	private fun Color.textColor(): Color {
		val rg = if (red * 255 <= 10) red * 255 / 3294 else (red * 255 / 269 + 0.0513).pow(2.4)
		val gg = if (green * 255 <= 10) green * 255 / 3294 else (green * 255 / 269 + 0.0513).pow(2.4)
		val bg = if (blue * 255 <= 10) blue * 255 / 3294 else (blue * 255 / 269 + 0.0513).pow(2.4)
		val l = 0.2126 * rg + 0.7152 * gg + 0.0722 * bg
		return if ((1.0 + 0.05) / (l + 0.05) > 7) Color.WHITE else Color.BLACK
	}
}