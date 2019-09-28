package com.poterion.monitor.notifiers.deploymentcase.ui

import com.poterion.monitor.notifiers.deploymentcase.*
import com.poterion.monitor.notifiers.deploymentcase.api.ConfigurationContributer
import com.poterion.monitor.notifiers.deploymentcase.control.setStateMachine
import com.poterion.monitor.notifiers.deploymentcase.control.toStateMachine
import com.poterion.monitor.notifiers.deploymentcase.data.*
import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.geometry.Pos
import javafx.scene.Parent
import javafx.scene.control.*
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.util.Callback
import javafx.util.StringConverter
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class ConfigWindowTabStateMachine : ConfigurationContributer {
    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(ConfigWindowTabStateMachine::class.java)

        internal fun getRoot(saveConfig: () -> Unit): Pair<ConfigWindowTabStateMachine, Parent> =
                FXMLLoader(ConfigWindowTabStateMachine::class.java.getResource("config-window-tab-state-machine.fxml"))
                        .let { it.load<Parent>() to it.getController<ConfigWindowTabStateMachine>() }
                        .let { (root, ctrl) ->
                            ctrl.saveConfig = saveConfig
                            ctrl to root
                        }

    }

    @FXML
    private lateinit var treeStateMachine: TreeView<StateMachineItem>

    private lateinit var saveConfig: () -> Unit

    private var clipboardStateMachineItem: TreeItem<StateMachineItem>? = null

    @FXML
    fun initialize() {
        treeStateMachine.initEditable()
        treeStateMachine.selectionModel.selectedItemProperty().addListener { _, _, _ -> treeStateMachine.refresh() }
        treeStateMachine.root = TreeItem<StateMachineItem>(Placeholder())
        SharedUiData.stateMachineProperty.bindContent(treeStateMachine.root.children)
    }

    override fun notifyNewConfiguration(configuration: Configuration) {
        treeStateMachine.isShowRoot = false
        treeStateMachine.setStateMachine(configuration.stateMachine)
    }

    override fun updateConfiguration(config: DeploymentCaseConfig, configuration: Configuration?) {
        configuration?.stateMachine = treeStateMachine.toStateMachine()
        treeStateMachine.refresh()
    }

    @FXML
    fun onStateMachineTreeKeyPressed(keyEvent: KeyEvent) {
        val selected = treeStateMachine.selectionModel.selectedItem
        when (keyEvent.code) {
            KeyCode.UP -> if (keyEvent.isControlDown || keyEvent.isMetaDown) {
                val parent = selected.parent
                val index = parent.children.indexOf(selected)
                if (index > 0) {
                    parent.children[index] = parent.children[index - 1]
                    parent.children[index - 1] = selected
                    treeStateMachine.selectionModel.select(selected)
                    treeStateMachine.refresh()
                    saveConfig()
                }
            }
            KeyCode.DOWN -> if (keyEvent.isControlDown || keyEvent.isMetaDown) {
                val parent = selected.parent
                val index = parent.children.indexOf(selected)
                if (index < parent.children.size - 1) {
                    parent.children[index] = parent.children[index + 1]
                    parent.children[index + 1] = selected
                    treeStateMachine.selectionModel.select(selected)
                    treeStateMachine.refresh()
                    saveConfig()
                }
            }
            KeyCode.C -> if (keyEvent.isControlDown) {
                clipboardStateMachineItem = selected
            }
            KeyCode.D,
            KeyCode.V -> {
                if (keyEvent.isControlDown) {
                    val item = if (keyEvent.code == KeyCode.D) selected else clipboardStateMachineItem
                    when (item?.value) {
                        is Evaluation -> {
                            val conditions = item.children[0].children.mapNotNull { (it.value as? Condition)?.copy() }
                            val actions = item.children[1].children.mapNotNull { (it.value as? Action)?.copy() }
                            val evaluation = TreeItem<StateMachineItem>(Evaluation()).apply {
                                children.addAll(
                                        TreeItem<StateMachineItem>(Placeholder("Conditions", DeploymentCaseIcon.CONDITIONS)
                                                .apply { isExpanded = true })
                                                .apply { children.addAll(conditions.map { TreeItem<StateMachineItem>(it) }) },
                                        TreeItem<StateMachineItem>(Placeholder("Actions", DeploymentCaseIcon.ACTIONS)
                                                .apply { isExpanded = true })
                                                .apply { children.addAll(actions.map { TreeItem<StateMachineItem>(it) }) })
                            }
                            var parent: TreeItem<StateMachineItem>? = selected
                            while (parent != null && parent.value !is State) parent = parent.parent
                            parent?.children?.add(evaluation)
                            evaluation.expandAll()
                        }
                        is Condition -> {
                            var parent: TreeItem<StateMachineItem>? = selected
                            while (parent != null && parent.value !is Evaluation) parent = parent.parent
                            (item.value as? Condition)?.copy()
                                    ?.let { TreeItem<StateMachineItem>(it) }
                                    ?.also { parent?.children?.get(0)?.children?.add(it) }
                            parent?.expandAll()
                        }
                        is Action -> {
                            var parent: TreeItem<StateMachineItem>? = selected
                            while (parent != null && parent.value !is Evaluation) parent = parent.parent
                            (item.value as? Action)?.copy()
                                    ?.let { TreeItem<StateMachineItem>(it) }
                                    ?.also { parent?.children?.get(1)?.children?.add(it) }
                            parent?.expandAll()
                        }
                    }
                    saveConfig()
                }
            }
            KeyCode.HELP, // MacOS mapping of INSERT key
            KeyCode.INSERT -> {
                when (selected?.value) {
                    null -> {
                        val state = TreeItem<StateMachineItem>(State("${treeStateMachine.root.children.size}"))
                        treeStateMachine.root.children.add(state)
                        treeStateMachine.root.isExpanded = true
                        state.expandAll()
                    }
                    is State -> {
                        val evaluation = TreeItem<StateMachineItem>(Evaluation()).apply {
                            children.addAll(
                                    TreeItem<StateMachineItem>(Placeholder("Conditions", DeploymentCaseIcon.CONDITIONS)),
                                    TreeItem<StateMachineItem>(Placeholder("Actions", DeploymentCaseIcon.ACTIONS)))
                        }
                        selected.children.add(evaluation)
                        selected.isExpanded = true
                        evaluation.expandAll()
                    }
                    is Evaluation -> {
                        val evaluation = TreeItem<StateMachineItem>(Evaluation()).apply {
                            children.addAll(
                                    TreeItem<StateMachineItem>(Placeholder("Conditions", DeploymentCaseIcon.CONDITIONS)),
                                    TreeItem<StateMachineItem>(Placeholder("Actions", DeploymentCaseIcon.ACTIONS)))
                        }
                        selected.parent.children.add(evaluation)
                        evaluation.expandAll()
                    }
                    is Placeholder -> {
                        if (selected.value?.title == "Conditions") {
                            selected.children.add(TreeItem<StateMachineItem>(Condition()))
                        } else {
                            selected.children.add(TreeItem<StateMachineItem>(Action()))
                        }
                        selected.isExpanded = true
                    }
                    is Condition -> {
                        selected.parent.children.add(TreeItem<StateMachineItem>(Condition()))
                    }
                    is Action -> {
                        selected.parent.children.add(TreeItem<StateMachineItem>(Action()))
                        selected.parent.isExpanded = true
                    }
                }
                selected?.isExpanded = true
                saveConfig()
            }
            KeyCode.DELETE -> {
                treeStateMachine.selectionModel.selectedItem
                        ?.takeUnless { it.value is Placeholder }
                        ?.takeIf { item -> item.value.let { it !is State || findStateReferences(it).isEmpty() } }
                        ?.takeIf { item -> SharedUiData.pipelineTargetStates.find { it.name == (item.value as? State)?.name } == null }
                        ?.takeIf { _ ->
                            Alert(Alert.AlertType.CONFIRMATION).apply {
                                title = "Delete confirmation"
                                headerText = "Do you want to delete selected node?"
                            }.showAndWait().filter { it === ButtonType.OK }.isPresent
                        }
                        ?.also { it.parent.children.remove(it) }
                        ?.also { saveConfig() }
            }
            else -> {
            }
        }
    }

    private fun TreeView<StateMachineItem>.initEditable() {
        cellFactory = Callback<TreeView<StateMachineItem>, TreeCell<StateMachineItem>> { _ ->
            object : TreeCell<StateMachineItem>() {
                private var textField: TextField? = null
                private var vBox: VBox? = null
                private var comboBox1: ComboBox<Device>? = null
                private var comboBox2: ComboBox<Variable>? = null
                private var checkbox: CheckBox? = null

                override fun startEdit() {
                    super.startEdit()

                    item?.also { item ->
                        when (item) {
                            is State -> {
                                text = null
                                graphic = createTextField(item)
                            }
                            is Condition,
                            is Action -> {
                                text = null
                                graphic = createDoubleComboBox()
                            }
                        }
                    }
                }

                override fun cancelEdit() {
                    super.cancelEdit()
                    commitEdit(save())
                    text = item?.title
                    graphic = item?.getImageView
                }

                override fun updateItem(item: StateMachineItem?, empty: Boolean) {
                    super.updateItem(item, empty)

                    when {
                        empty -> {
                            text = null
                            graphic = null
                        }
                        isEditing -> {
                            text = null
                            when (item) {
                                is State -> {
                                    textField?.text = item.name
                                    graphic = textField
                                }
                                is Condition -> {
                                    comboBox1?.selectionModel?.select(item.device)
                                    comboBox2?.selectionModel?.select(item.value)
                                    graphic = vBox
                                }
                                is Action -> {
                                    comboBox1?.selectionModel?.select(item.device)
                                    comboBox2?.selectionModel?.select(item.value)
                                    checkbox?.isSelected = item.includingEnteringState
                                    graphic = vBox
                                }
                            }
                        }
                        else -> {
                            text = item?.title
                            graphic = item?.getImageView
                        }
                    }
                    style = when {
                        treeView.selectionModel.selectedItem?.value == item -> null
                        treeView.selectionModel.selectedIndex == index -> null
                        item is State && SharedUiData.pipelineTargetStates.find { it.name == item.name } != null -> "-fx-background-color: #FFCCFF"
                        item is State && findStateReferences(item).isEmpty() -> "-fx-background-color: #CCCCCC"
                        item is State && findStateReferences(item).isNotEmpty() -> "-fx-background-color: #CCCCFF"
                        item is Condition -> "-fx-background-color: #CCFFCC"
                        item is Action && treeItem?.parent?.parent?.children?.getOrNull(0)?.children?.isEmpty() == true -> "-fx-background-color: #FFEECC"
                        item is Action && item.includingEnteringState -> "-fx-background-color: #FFEECC"
                        item is Action && !item.includingEnteringState -> "-fx-background-color: #CCFFFF"
                        else -> null
                    }
                }

                private fun createTextField(state: State): TextField? {
                    textField = TextField(state.name)
                    textField?.minWidth = width - graphicTextGap * 2
                    textField?.focusedProperty()?.addListener { _, _, newValue ->
                        if (!newValue) commitEdit(save())
                    }
                    textField?.setOnAction { commitEdit(save()) }
                    textField?.selectedText
                    return textField
                }

                private fun createDoubleComboBox(): VBox? {
                    item?.also { item ->
                        when (item) {
                            is Condition -> {
                                comboBox1 = ComboBox(FXCollections.observableArrayList(SharedUiData
                                        .devices
                                        .filtered {
                                            // it.type == VariableType.BOOLEAN
                                            it.kind == DeviceKind.MCP23017 && it.key.toInt() < 32
                                                    || it.kind == DeviceKind.BLUETOOTH && it.key == "connected"
                                        }))
                                        .apply { selectionModel.select(item.device) }
                                comboBox2 = ComboBox(SharedUiData.variables.filtered { it.type == VariableType.BOOLEAN })
                                        .apply { selectionModel.select(item.value) }
                                checkbox = null
                            }
                            is Action -> {
                                comboBox2 = ComboBox()
                                comboBox1 = ComboBox(FXCollections.observableArrayList(SharedUiData
                                        .devices
                                        .toMutableList()
                                        .apply { add(Device("GOTO", DeviceKind.VIRTUAL, VirtualKey.GOTO.key)) }
                                        .apply { add(Device("ENTER", DeviceKind.VIRTUAL, VirtualKey.ENTER.key)) }
                                        .filter { it.kind != DeviceKind.MCP23017 || it.key.toInt() >= 32 }))
                                        .apply {
                                            selectionModel.selectedItemProperty().addListener { _, oldValue, newValue ->
                                                if (oldValue == null || oldValue.type != newValue?.type) {
                                                    comboBox2?.items?.clear()
                                                    comboBox2?.items?.addAll(SharedUiData
                                                            .variables
                                                            .toMutableList()
                                                            .apply {
                                                                addAll(treeStateMachine.root.children
                                                                        .mapNotNull { it.value as? State }
                                                                        .map { Variable("state_${it.name}", VariableType.STATE, it.name) })
                                                            }
                                                            .filter { it.type == newValue.type })
                                                    comboBox2?.selectionModel?.select(item.value)
                                                }
                                            }
                                        }
                                        .apply { selectionModel.select(item.device) }
                                checkbox = CheckBox("Including entering state").apply {
                                    isSelected = item.includingEnteringState
                                }
                            }
                        }
                    }

                    comboBox1?.converter = object : StringConverter<Device>() {
                        override fun toString(device: Device?) = device?.getDisplayName()
                        override fun fromString(string: String?) = string?.toDevice(SharedUiData.devices)
                    }
                    comboBox2?.converter = object : StringConverter<Variable>() {
                        override fun toString(device: Variable?) = device?.getDisplayNameValue()
                        override fun fromString(string: String?) = string?.toVariable(SharedUiData.variables)
                    }

                    comboBox1?.focusedProperty()?.addListener { _, _, newValue -> if (!newValue) save() }
                    comboBox2?.focusedProperty()?.addListener { _, _, newValue -> if (!newValue) save() }
                    checkbox?.selectedProperty()?.addListener { _, _, _ -> save() }

                    val submitButton = Button("Submit").apply {
                        setOnAction {
                            commitEdit(save())
                        }
                    }

                    val hBox1 = HBox(1.0).apply {
                        minWidth = width - graphicTextGap * 2
                        alignment = Pos.CENTER_LEFT

                        children.addAll(comboBox1, Label("="), comboBox2)
                    }

                    val hBox2 = HBox(1.0).apply {
                        minWidth = width - graphicTextGap * 2
                        alignment = Pos.CENTER_LEFT

                        if (item is Action && checkbox != null) children.add(checkbox)
                        children.add(submitButton)
                    }

                    vBox = VBox(1.0).apply {
                        minWidth = width - graphicTextGap * 2
                        alignment = Pos.CENTER_LEFT
                        children.addAll(hBox1, hBox2)
                    }

                    return vBox
                }

                private fun save() = item?.also { item ->
                    when (item) {
                        is State -> {
                            item.name = textField?.text ?: ""
                        }
                        is Condition -> {
                            item.device = comboBox1?.selectionModel?.selectedItem
                            item.value = comboBox2?.selectionModel?.selectedItem
                        }
                        is Action -> {
                            item.device = comboBox1?.selectionModel?.selectedItem
                            item.value = comboBox2?.selectionModel?.selectedItem
                            item.includingEnteringState = checkbox?.isSelected ?: false
                        }
                    }
                }
            }
        }

        setOnEditCommit { event ->
            editingItem?.value = event?.newValue
            saveConfig()
        }
    }

    private fun findStateReferences(state: State): List<Action> = SharedUiData.findInStateMachine {
        it is Action
                && it.device?.kind == DeviceKind.VIRTUAL
                && it.device?.key == VirtualKey.GOTO.key
                && (it.value?.value == state.name || it.value?.value == "${treeStateMachine.selectionModel.selectedIndex}")
    }.map { it as Action }
}