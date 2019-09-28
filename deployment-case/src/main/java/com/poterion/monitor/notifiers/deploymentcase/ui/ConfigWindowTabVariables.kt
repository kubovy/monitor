package com.poterion.monitor.notifiers.deploymentcase.ui

import com.poterion.monitor.notifiers.deploymentcase.api.ConfigurationContributer
import com.poterion.monitor.notifiers.deploymentcase.data.*
import com.poterion.monitor.notifiers.deploymentcase.getDisplayString
import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.control.*
import javafx.scene.control.cell.PropertyValueFactory
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.layout.HBox
import javafx.scene.paint.Color
import javafx.util.Callback
import javafx.util.StringConverter
import org.slf4j.Logger
import org.slf4j.LoggerFactory


class ConfigWindowTabVariables : ConfigurationContributer {
    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(ConfigWindowTabVariables::class.java)

        internal fun getRoot(saveConfig: () -> Unit): Pair<ConfigWindowTabVariables, Parent> =
                FXMLLoader(ConfigWindowTabVariables::class.java.getResource("config-window-tab-variables.fxml"))
                        .let { it.load<Parent>() to it.getController<ConfigWindowTabVariables>() }
                        .let { (root, ctrl) ->
                            ctrl.saveConfig = saveConfig
                            ctrl to root
                        }

    }


    @FXML private lateinit var tableVariables: TableView<Variable>
    @FXML private lateinit var columnVariableName: TableColumn<Variable, String>
    @FXML private lateinit var columnVariableType: TableColumn<Variable, VariableType>
    @FXML private lateinit var columnVariableValue: TableColumn<Variable, String>

    private lateinit var saveConfig: () -> Unit

    @FXML
    fun initialize() {
        columnVariableName.initEditableText("name",
                { variable ->
                    when {
                        SharedUiData.jobColors.find { it.name == variable?.name } != null -> "-fx-font-weight: bold; -fx-font-style: italic;"
                        SharedUiData.findInStateMachine { it is Action && it.value?.name == variable?.name }.isNotEmpty() -> "-fx-font-weight: bold;"
                        else -> null
                    }
                },
                { variable, name -> variable.name = name },
                { saveConfig() })
        columnVariableType.initEditableCombo("type",
                { VariableType.values() },
                { it?.description ?: "" },
                { str -> VariableType.values().find { it.description == str } },
                { variable -> SharedUiData.jobColors.find { it.name == variable.name } == null },
                { variable, type ->
                    if (variable.type != type) {
                        variable.value = when (type) {
                            VariableType.BOOLEAN -> "false"
                            VariableType.STRING -> ""
                            VariableType.COLOR_PATTERN -> "0,0,0,0"
                            VariableType.STATE -> ""
                            VariableType.ENTER -> "0|Enter Version|##.##.##"
                        }
                        tableVariables.refresh()
                    }
                    variable.type = type
                })
        columnVariableValue.initEditableVariableValue("value")
        //tableVariables.focusedProperty().addListener { _, _, focused -> if (!focused) saveConfig() }
        tableVariables.sortOrder.setAll(columnVariableName)
        tableVariables.isEditable = true
        SharedUiData.variablesProperty.bindContent(tableVariables.items)
    }

    override fun notifyNewConfiguration(configuration: Configuration) {
        tableVariables.items.clear()
        tableVariables.items.addAll(configuration.variables)
    }

    override fun updateConfiguration(config: DeploymentCaseConfig, configuration: Configuration?) {
        configuration?.variables = tableVariables.items.filterNotNull()
        tableVariables.refresh()
    }


    @FXML
    fun onVariableTableKeyPressed(keyEvent: KeyEvent) {
        when (keyEvent.code) {
            KeyCode.HELP, // MacOS mapping of INSERT key
            KeyCode.INSERT -> {
                tableVariables.items.add(Variable())
                saveConfig()
            }
            KeyCode.DELETE -> tableVariables.selectionModel.selectedItem
                    .takeIf { it != null }
                    ?.takeIf { variable -> SharedUiData.findInStateMachine { it is Action && it.value?.name == variable.name }.isEmpty() }
                    ?.takeIf { variable -> SharedUiData.jobColors.find { it.name == variable.name } == null }
                    ?.takeIf { _ ->
                        Alert(Alert.AlertType.CONFIRMATION).apply {
                            title = "Delete confirmation"
                            headerText = "Do you want to delete selected row?"
                        }.showAndWait().filter { it === ButtonType.OK }.isPresent
                    }
                    ?.also { tableVariables.items.remove(it) }
                    ?.also { saveConfig() }
            else -> {
            }
        }
    }

    private fun TableColumn<Variable, String>.initEditableVariableValue(propertyName: String) {
        cellValueFactory = PropertyValueFactory<Variable, String>(propertyName)
        cellFactory = Callback<TableColumn<Variable, String>, TableCell<Variable, String>> { _ ->
            object : TableCell<Variable, String>() {
                private var checkBox: CheckBox? = null
                private var textField: TextField? = null
                private var patternCombo: ComboBox<LightPattern>? = null
                private val colorPicker: ColorPicker? = null

                private val itemText: String?
                    get() = tableView.items[index]?.type?.let { type -> getDisplayString(item, type) }

                private val itemStyle: String?
                    get() = tableView.items[index]?.type?.let { type ->
                        when (type) {
                            VariableType.COLOR_PATTERN -> item
                                    ?.split(",")
                                    ?.takeIf { it.size == 4 }
                                    ?.let { it.subList(1, it.size) }
                                    ?.map { it.toInt().adjustColor(SharedUiData.maxColorComponent) }
                                    ?.joinToString("") { "%02X".format(it) }
                                    ?.let {
                                        "-fx-background-color: #%s".format(it)
                                    }
                            else -> null
                        }
                    }

                override fun startEdit() {
                    super.startEdit()
                    val entry = tableView.items[index]
                    if (entry?.type != null && !isEmpty) {
                        text = null
                        graphic = when (entry.type) {
                            VariableType.BOOLEAN -> createCheckBox()
                            VariableType.STRING -> createTextField()
                            VariableType.COLOR_PATTERN -> createColorPicker()
                            VariableType.STATE -> null
                            VariableType.ENTER -> createTextField()
                        }
                        style = null
                        textField?.selectAll()
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
                            textField?.text = itemText
                            text = null
                            graphic = textField
                            style = null
                        } else {
                            text = itemText
                            graphic = null
                            style = itemStyle
                        }
                    }
                }

                private fun createTextField(): TextField? {
                    textField = TextField(itemText)
                    textField?.minWidth = width - graphicTextGap * 2
                    textField?.focusedProperty()?.addListener { _, _, newValue -> if (!newValue) commitEdit(save()) }
                    textField?.setOnAction { commitEdit(save()) }
                    return textField
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


                    patternCombo = ComboBox(FXCollections.observableArrayList(LightPattern.values().asList()))
                    patternCombo?.converter = object : StringConverter<LightPattern>() {
                        override fun toString(pattern: LightPattern?) = pattern?.description
                        override fun fromString(string: String?) = LightPattern.values()
                                .find { it.description == string }
                                ?: LightPattern.LIGHT
                    }
                    patternCombo?.selectionModel?.select(pattern)

                    val color = item?.split(",")
                            ?.mapNotNull { it.toIntOrNull() }
                            ?.map { it.toDouble() / 255.0 }
                            ?.takeIf { it.size == 4 }
                            ?.let { Color(it[1], it[2], it[3], 1.0) }
                            ?: Color.BLACK

                    val colorPicker = ColorPicker(color)

                    val submitButton = Button("Submit").apply {
                        setOnAction { commitEdit(save()) }
                    }

                    val box = HBox(patternCombo, colorPicker, submitButton)
                    box.minWidth = width - graphicTextGap * 2

                    return box
                }

                private fun save(): String? = tableView.items[index]?.let { entry ->
                    when (entry.type) {
                        VariableType.BOOLEAN -> if (checkBox?.isSelected == true) "true" else "false"
                        VariableType.STRING -> textField?.text ?: ""
                        VariableType.COLOR_PATTERN -> {
                            val p = patternCombo?.selectionModel?.selectedIndex
                            val c = colorPicker?.value
                                    ?.let { listOf(it.red, it.green, it.blue) }
                                    ?.map { it * 255 }
                                    ?.map { it.toInt() }
                                    ?.joinToString(",")
                            p?.let { c?.let { "${p},${c}" } } ?: ""
                        }
                        VariableType.STATE -> null
                        VariableType.ENTER -> textField?.text ?: "0|Enter Version|##.##.##"
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
                                                                         itemsProvider: (Entry?) -> Array<Type>,
                                                                         toString: (Type?) -> String,
                                                                         fromString: (String?) -> Type?,
                                                                         updateable: (Entry) -> Boolean,
                                                                         itemUpdater: (Entry, Type) -> Unit) {
        cellValueFactory = PropertyValueFactory<Entry, Type>(propertyName)
        cellFactory = Callback<TableColumn<Entry, Type>, TableCell<Entry, Type>> {
            object : TableCell<Entry, Type>() {
                private var comboBox: ComboBox<Type>? = null

                override fun startEdit() {
                    super.startEdit()
                    if (!isEmpty) {
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
                        isDisable = !updateable(tableRow.item as Entry)
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
}