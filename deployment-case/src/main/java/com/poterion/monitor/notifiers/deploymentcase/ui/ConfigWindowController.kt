package com.poterion.monitor.notifiers.deploymentcase.ui

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.poterion.monitor.api.communication.BluetoothEmbeddedListener
import com.poterion.monitor.api.communication.BluetoothMessageKind
import com.poterion.monitor.notifiers.deploymentcase.*
import com.poterion.monitor.notifiers.deploymentcase.control.*
import com.poterion.monitor.notifiers.deploymentcase.data.*
import javafx.application.Platform
import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.control.*
import javafx.scene.control.Alert.AlertType
import javafx.scene.control.ButtonType
import javafx.scene.control.cell.PropertyValueFactory
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.layout.HBox
import javafx.scene.paint.Color
import javafx.util.Callback
import javafx.util.StringConverter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javafx.scene.control.TreeItem


/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
class ConfigWindowController : BluetoothEmbeddedListener {

	companion object {
		private val LOGGER: Logger = LoggerFactory.getLogger(ConfigWindowController::class.java)
		private const val NEW_NAME = "New configuration"

		internal fun getRoot(config: DeploymentCaseConfig, controller: DeploymentCaseNotifier): Parent =
				FXMLLoader(ConfigWindowController::class.java.getResource("config-window.fxml"))
						.let { it.load<Parent>() to it.getController<ConfigWindowController>() }
						.let { (root, ctrl) ->
							ctrl.config = config
							ctrl.controller = controller
							ctrl.load()
							root
						}
	}

	@FXML private lateinit var rootPane: SplitPane
	@FXML private lateinit var textBluetoothAddress: TextField
	@FXML private lateinit var listConfigurations: ListView<Configuration>

	@FXML private lateinit var checkboxActive: CheckBox
	@FXML private lateinit var textName: TextField
	@FXML private lateinit var comboboxMethod: ComboBox<String>
	@FXML private lateinit var textURL: TextField
	@FXML private lateinit var textUsername: TextField
	@FXML private lateinit var textPassword: PasswordField
	@FXML private lateinit var textJobName: TextField
	@FXML private lateinit var textParameters: TextArea

	@FXML private lateinit var tableVariables: TableView<Variable>
	@FXML private lateinit var columnVariableName: TableColumn<Variable, String>
	@FXML private lateinit var columnVariableType: TableColumn<Variable, VariableType>
	@FXML private lateinit var columnVariableValue: TableColumn<Variable, String>


	@FXML private lateinit var tableDevices: TableView<Device>
	@FXML private lateinit var columnDevicesName: TableColumn<Device, String>
	@FXML private lateinit var columnDevicesKind: TableColumn<Device, DeviceKind>
	@FXML private lateinit var columnDevicesKey: TableColumn<Device, String>

	@FXML private lateinit var tableActions: TableView<Action>
	@FXML private lateinit var columnActionsId: TableColumn<Action, Int>
	@FXML private lateinit var columnActionsDevice: TableColumn<Action, Device>
	@FXML private lateinit var columnActionsValue: TableColumn<Action, Variable>

	@FXML private lateinit var treeStateMachine: TreeView<StateMachineItem>

	@FXML private lateinit var textLog: TextArea

	@FXML private lateinit var comboboxType: ComboBox<String>
	@FXML private lateinit var comboboxName: ComboBox<String>
	@FXML private lateinit var comboboxValue: ComboBox<String>

	@FXML private lateinit var iconConnected: ImageView
	@FXML private lateinit var progress: ProgressBar
	@FXML private lateinit var additionalButtons: HBox

	private var config: DeploymentCaseConfig? = null
	private var controller: DeploymentCaseNotifier? = null
	private val jsonMapper = ObjectMapper()
	private val yamlMapper = ObjectMapper(YAMLFactory())
	private var clipboardVariable: Variable? = null
	private var clipboardStateMachineItem: TreeItem<StateMachineItem>? = null
	private var messagesInQueue = 0

	init {
		yamlMapper.enable(SerializationFeature.INDENT_OUTPUT)
	}

	@FXML
	fun initialize() {
		additionalButtons.children.clear()
		textBluetoothAddress.focusedProperty().addListener { _, _, focused -> if (!focused) saveConfig() }
		listConfigurations.apply {
			setCellFactory {
				object : ListCell<Configuration>() {
					override fun updateItem(item: Configuration?, empty: Boolean) {
						super.updateItem(item, empty)
						text = item?.name
						graphic = null
					}
				}
			}
			selectionModel.selectedItemProperty().addListener { _, _, configuration ->
				checkboxActive.isSelected = configuration.isActive
				textName.text = configuration.name
				comboboxMethod.selectionModel.select(configuration.method)
				textURL.text = configuration.url
				textUsername.text = configuration.username
				textPassword.text = configuration.password
				textJobName.text = configuration.jobName
				textParameters.text = configuration.parameters

				tableVariables.items.clear()
				tableVariables.items.addAll(configuration.variables)
				//tableVariables.items.add(null)

				tableDevices.items.clear()
				tableDevices.items.addAll((0..39)
						.map { key -> key to configuration.devices.find { it.kind == DeviceKind.MCP23017 && it.key == "${key}" }?.name }
						.map { (key, name) -> Device(name ?: "", DeviceKind.MCP23017, "${key}") })
				tableDevices.items.addAll((0..31)
						.map { key -> key to configuration.devices.find { it.kind == DeviceKind.WS281x && it.key == "${key}" }?.name }
						.map { (key, name) ->
							Device(name ?: "", DeviceKind.WS281x, "${key}")
						})
				tableDevices.items.addAll(arrayOf("connected", "trigger")
						.map { key -> key to configuration.devices.find { it.kind == DeviceKind.BLUETOOTH && it.key == key }?.name }
						.map { (key, name) -> Device(name ?: "bt_${key}", DeviceKind.BLUETOOTH, key) })
				tableDevices.items.addAll(arrayOf("message", "backlight", "reset", "clear")
						.map { key -> key to configuration.devices.find { it.kind == DeviceKind.LCD && it.key == key }?.name }
						.map { (key, name) -> Device(name ?: "lcd_${key}", DeviceKind.LCD, key) })

				tableActions.items.clear()
				tableActions.items.addAll(configuration.actions)
				//tableActions.items.add(null)

				treeStateMachine.isShowRoot = false
				treeStateMachine.root = TreeItem<StateMachineItem>(Placeholder())
				treeStateMachine.setStateMachine(configuration.stateMachine)
			}
		}

		// Config
		checkboxActive.focusedProperty().addListener { _, _, focused -> if (!focused) saveConfig() }
		textName.focusedProperty().addListener { _, _, focused -> if (!focused) saveConfig() }

		comboboxMethod.items.addAll("GET", "POST")
		comboboxMethod.selectionModel.select(0)
		comboboxMethod.focusedProperty().addListener { _, _, focused -> if (!focused) saveConfig() }

		textURL.focusedProperty().addListener { _, _, focused -> if (!focused) saveConfig() }
		textUsername.focusedProperty().addListener { _, _, focused -> if (!focused) saveConfig() }
		textPassword.focusedProperty().addListener { _, _, focused -> if (!focused) saveConfig() }
		textJobName.focusedProperty().addListener { _, _, focused -> if (!focused) saveConfig() }
		textParameters.focusedProperty().addListener { _, _, focused -> if (!focused) saveConfig() }

		columnVariableName.initEditableText("name",
				{ Variable(it, VariableType.BOOLEAN, "false") },
				{ variable, name ->
					variable.name = name
					tableActions.items.filter { it.value?.name == name }.forEach { it.value = variable }
					tableActions.refresh()
				})
		columnVariableType.initEditableCombo("type",
				{ VariableType.values() },
				{ it?.description ?: "" },
				{ str -> VariableType.values().find { it.description == str } },
				{ type -> Variable("", type, "") },
				{ variable, type ->
					if (variable.type != type) {
						variable.value = when (type) {
							VariableType.BOOLEAN -> "false"
							VariableType.STRING -> ""
							VariableType.COLOR_PATTERN -> "0,0,0,0"
							VariableType.STATE -> ""
						}
						tableActions.items.filter { it.value?.name == variable.name }.forEach { it.value = variable }
						tableVariables.refresh()
					}
					variable.type = type
				})
		columnVariableValue.initEditableVariableValue("value") { variable ->
			tableActions.items.filter { it.value?.name == variable.name }.forEach { it.value = variable }
			tableActions.refresh()
		}
		tableVariables.focusedProperty().addListener { _, _, focused -> if (!focused) saveConfig() }
		tableVariables.sortOrder.setAll(columnVariableName)
		tableVariables.isEditable = true

		columnDevicesName.initEditableText("name",
				{ Device(it, DeviceKind.MCP23017, "") },
				{ device, name ->
					device.name = name
					tableActions.refresh()
				})
		columnDevicesKind.init("kind") { it?.description ?: "" }
		columnDevicesKey.init("key") { it ?: "" }
		tableDevices.focusedProperty().addListener { _, _, focused -> if (!focused) saveConfig() }
		tableDevices.sortOrder.setAll(columnDevicesName)

		columnActionsId.init("id") { value -> value?.let { "%02X".format(it) } ?: "" }
		columnActionsDevice.initEditableCombo("device",
				{ tableDevices.items.toTypedArray() },
				{ value -> value.getDisplayName() },
				{ str -> str?.toDevice(tableDevices.items) },
				{ device -> Action(device, null) },
				{ action, device -> action.device = device })
		columnActionsValue.initEditableCombo("value",
				{ entry -> tableVariables.items.filter { it?.type == entry?.device?.type }.toTypedArray() },
				{ it?.getDisplayNameValue() ?: "" },
				{ str -> str?.toVariable(tableVariables.items) },
				{ variable -> Action(tableDevices.items.first(), variable) },
				{ action, variable -> action.value = variable })
		tableActions.sortOrder.setAll(columnActionsId)

		treeStateMachine.initEditable()

		// Test
		comboboxType.items.addAll("State", "Action", "Transit")
		comboboxType.selectionModel.select(0)
	}

	private fun load() {
		textBluetoothAddress.text = config?.deviceAddress ?: ""
		comboboxName.items.clear()
		comboboxValue.items.clear()
		listConfigurations.items.clear()
		config?.testNameHistory?.also { comboboxName.items.addAll(it) }
		config?.testValueHistory?.also { comboboxValue.items.addAll(it) }
		config?.configurations?.also { listConfigurations.items.addAll(it) }
		if (listConfigurations.items.isNotEmpty()) listConfigurations.selectionModel.select(0)

		// Status
		iconConnected.image = Image(
				if (controller?.communicator?.isConnected == true) DeploymentCaseIcon.CONNECTED.inputStream
				else DeploymentCaseIcon.DISCONNECTED.inputStream)

		controller?.communicator?.register(this)
	}

	@FXML
	fun onAddConfig() {
		var index = 1
		while (listConfigurations.items.map { it.name }.contains("${NEW_NAME} ${index}")) index++
		listConfigurations.items.add(Configuration(name = "${NEW_NAME} ${index}"))
		saveConfig()
	}

	@FXML
	fun onAddVariable() {

	}

	@FXML
	fun onDeleteSelectedConfig() {
		if (listConfigurations.items.size > 1) {
			var selectedIndex = listConfigurations.selectionModel.selectedIndex
			if (selectedIndex >= 0) {
				listConfigurations.items.removeAt(selectedIndex)
				while (selectedIndex >= listConfigurations.items.size) selectedIndex--
				listConfigurations.selectionModel.select(selectedIndex)
			}
		}
	}

	@FXML
	fun onPull() {
		rootPane.isDisable = true
		controller?.communicator?.send(BluetoothMessageKind.PULL_STATE_MACHINE)
	}

	@FXML
	fun onPush() {
		rootPane.isDisable = true
		val result = treeStateMachine.getStateMachine().toByteArray()
		result.toList()
				.chunked(61)
				.map { data -> data.toTypedArray() }
				.mapIndexed { index, data ->
					(index * 61) to data
				}
				.map { (reg, data) ->
					listOf(Math.floor(reg / 256.0).toByte(), (reg % 256).toByte(), *data)
				}
				.map { it.toByteArray() }
				.forEach {
					controller?.communicator?.send(BluetoothMessageKind.PUSH_STATE_MACHINE, it)
				}
	}

	@FXML
	fun onClear() {
		textLog.clear()
	}

	@FXML
	fun onResetLCD() {
		//controller?.communicator?.send("LCD_RESET")
	}

	@FXML
	fun onReconnect() {
		controller?.communicator?.connect()
	}

	@FXML
	fun onTest() {
		val type = comboboxType.selectionModel.selectedItem?.toLowerCase()
		val name = comboboxName.value
		val value = comboboxValue.value

		if (comboboxName.items.contains(name).not()) comboboxName.items.add(0, name)
		if (comboboxValue.items.contains(value).not()) comboboxValue.items.add(0, value)

		while (comboboxName.items.size > 20) comboboxName.items.removeAt(comboboxName.items.size - 1)
		while (comboboxValue.items.size > 20) comboboxValue.items.removeAt(comboboxValue.items.size - 1)

		LOGGER.debug("Message: ${type},${name},${value}")
		//controller?.communicator?.send("${type},${name},${value}")
		saveConfig()
	}

	@FXML
	fun onKeyPressed(keyEvent: KeyEvent) = when (keyEvent.code) {
		KeyCode.F2 -> onPull()
		KeyCode.F3 -> onPush()
		KeyCode.F4 -> onTest()
		KeyCode.F5 -> onReconnect()
		KeyCode.F8 -> onClear()
		else -> null
	}

	@FXML
	fun onVariableTableKeyPressed(keyEvent: KeyEvent) {
		when (keyEvent.code) {
			KeyCode.INSERT -> {
				tableVariables.items.add(Variable())
				saveConfig()
			}
			KeyCode.DELETE -> tableVariables.selectionModel.selectedItem
					.takeIf { it != null }
					.takeIf { variable -> tableActions.items.none { it.value?.name == variable?.name } }
					?.takeIf {
						Alert(AlertType.CONFIRMATION).apply {
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

	@FXML
	fun onActionsTableKeyPressed(keyEvent: KeyEvent) {
		when (keyEvent.code) {
			KeyCode.INSERT -> {
				if (tableActions.items.size < 256) {
					tableActions.items.add(Action())
					saveConfig()
				}
			}
			KeyCode.DELETE -> tableActions.selectionModel.selectedIndex
					.takeIf {
						Alert(AlertType.CONFIRMATION).apply {
							title = "Delete confirmation"
							headerText = "Do you want to delete selected row?"
						}.showAndWait().filter { it === ButtonType.OK }.isPresent
					}
					?.also { tableActions.items.removeAt(it) }
					?.also {
						//tableActions.items.sortBy { it.id }
						//tableActions.items.forEachIndexed { id, action -> action.id = id }
					}
					?.also { saveConfig() }
			KeyCode.UP -> if (keyEvent.isControlDown) {
				val index = tableActions.selectionModel.selectedIndex
				if (index > 0) {
					//tableActions.items.sortBy { it.id }
					val before = tableActions.items[index - 1]
					tableActions.items[index - 1] = tableActions.items[index]
					tableActions.items[index] = before
					//tableActions.items.forEachIndexed { id, action -> action.id = id }
					saveConfig()
				}
			}
			KeyCode.DOWN -> if (keyEvent.isControlDown) {
				val index = tableActions.selectionModel.selectedIndex
				if (index < tableActions.items.size - 2) {
					//tableActions.items.sortBy { it.id }
					val after = tableActions.items[index + 1]
					tableActions.items[index + 1] = tableActions.items[index]
					tableActions.items[index] = after
					//tableActions.items.forEachIndexed { id, action -> action.id = id }
					saveConfig()
				}
			}
			else -> {
			}
		}
	}

	@FXML
	fun onStateMachineTreeKeyPressed(keyEvent: KeyEvent) {
		val selected = treeStateMachine.selectionModel.selectedItem
		when (keyEvent.code) {
			KeyCode.UP -> if (keyEvent.isControlDown) {
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
			KeyCode.DOWN -> if (keyEvent.isControlDown) {
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
						?.takeIf {
							Alert(AlertType.CONFIRMATION).apply {
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

	override fun onConnect() {
		super.onConnect()
		this.progress.progress = 0.0
		rootPane.isDisable = false
		iconConnected.image = Image(DeploymentCaseIcon.CONNECTED.inputStream)
	}

	override fun onDisconnect() {
		super.onDisconnect()
		this.progress.progress = 0.0
		rootPane.isDisable = false
		iconConnected.image = Image(DeploymentCaseIcon.DISCONNECTED.inputStream)
	}

	override fun onMessage(kind: BluetoothMessageKind, message: ByteArray) {
		textLog.appendText("${message.map { "0x%02X ".format(it) } }}\n")
		textLog.scrollTop = Double.MAX_VALUE

		val json = jsonMapper.readTree(message)
		if (json.has("states") && json.has("devices") && json.has("vars")) {
//			textStateMachine.text = yamlMapper.writer().withDefaultPrettyPrinter().writeValueAsString(json.get("states"))
//
//			tableVariables.items.clear()
//			tableVariables.items.addAll(json.get("vars").fields()
//					.asSequence()
//					.map { (k, v) -> k to v.toString() }
//					.toList())
//			tableVariables.sortOrder.setAll(columnVariableName)
//
//			tableDevices.items.clear()
//			tableDevices.items.addAll(json.get("devices").fields()
//					.asSequence()
//					.map { (k, v) -> Device(k, v.get("kind").asText(), v.get("key").asText()) }
//					.toList())
//			tableDevices.sortOrder.setAll(columnDevicesName)
		}
	}

	override fun onProgress(progress: Int, count: Int, disable: Boolean) {
		super.onProgress(progress, count, disable)
		if (progress >= count) {
			this.progress.progress = 0.0
			rootPane.isDisable = false
		} else if (progress < 0) {
			if (disable) rootPane.isDisable = true
			this.progress.progress = ProgressIndicator.INDETERMINATE_PROGRESS
		} else {
			if (disable) rootPane.isDisable = true
			this.progress.progress = progress.toDouble() / count.toDouble()
		}
	}

	override fun onStateMachine(stateMachine: ByteArray) {
		super.onStateMachine(stateMachine)
		StateCompareWindowController.popup(
				treeStateMachine.getStateMachine(),
				stateMachine.toStateMachine(tableDevices.items, tableVariables.items))
	}

	private fun saveConfig() {
		// Save selected configuration
		listConfigurations.selectionModel.selectedItem.apply {
			isActive = checkboxActive.isSelected
			name = textName.text
			method = comboboxMethod.selectionModel.selectedItem
			url = textURL.text
			username = textUsername.text
			password = textPassword.text
			jobName = textJobName.text
			parameters = textParameters.text
			variables = tableVariables.items.filterNotNull()
			devices = tableDevices.items.filterNotNull()
			actions = tableActions.items.filterNotNull()
			stateMachine = treeStateMachine.getStateMachine()
		}

		// Save config
		config?.deviceAddress = textBluetoothAddress.text ?: ""
		config?.testNameHistory = comboboxName.items
		config?.testValueHistory = comboboxValue.items
		config?.configurations = listConfigurations.items

		controller?.controller?.saveConfig()
		controller?.communicator?.connect(config?.deviceAddress)
	}

	private fun <Entry, Type> TableColumn<Entry, Type>.init(propertyName: String, transformer: (Type?) -> String) {
		cellValueFactory = PropertyValueFactory<Entry, Type>(propertyName)
		setCellFactory {
			object : TableCell<Entry, Type>() {
				override fun updateItem(item: Type?, empty: Boolean) {
					super.updateItem(item, empty)
					text = transformer(item)
				}
			}
		}
	}

	private fun <Entry> TableColumn<Entry, String>.initEditableText(propertyName: String,
																	itemProvider: (String) -> Entry,
																	itemUpdater: (Entry, String) -> Unit) {
		cellValueFactory = PropertyValueFactory<Entry, String>(propertyName)
		cellFactory = Callback<TableColumn<Entry, String>, TableCell<Entry, String>> {
			object : TableCell<Entry, String>() {
				private var textField: TextField? = null

				override fun startEdit() {
					super.startEdit()
					if (!isEmpty) {
						text = null
						graphic = createTextField()
						textField?.selectAll()
					}
				}

				override fun cancelEdit() {
					super.cancelEdit()
					commitEdit(save())
					text = item
					graphic = null
				}

				override fun updateItem(item: String?, empty: Boolean) {
					super.updateItem(item, empty)

					if (empty) {
						text = null
						graphic = null
					} else {
						if (isEditing) {
							textField?.text = item
							text = null
							graphic = textField
						} else {
							text = item
							graphic = null
						}
					}
				}

				private fun createTextField(): TextField? {
					textField = TextField(item)
					textField?.minWidth = width - graphicTextGap * 2
					textField?.focusedProperty()?.addListener { _, _, newValue -> if (!newValue) commitEdit(save()) }
					textField?.setOnAction { commitEdit(save()) }
					return textField
				}

				private fun save() = textField?.text ?: ""
			}
		}
		setOnEditCommit { event ->
			event?.tableView?.items?.also { items ->
				val item = items[event.tablePosition.row]
				if (item == null) {
//					while (items.remove(null)) {
//					}
//					event.newValue?.also { items.add(itemProvider(it)) }
//					items.add(null)
				} else {
					itemUpdater(item, event.newValue)
				}
				saveConfig()
			}
		}
	}

	private fun <Entry, Type> TableColumn<Entry, Type>.initEditableCombo(propertyName: String,
																		 itemsProvider: (Entry?) -> Array<Type>,
																		 toString: (Type?) -> String,
																		 fromString: (String?) -> Type?,
																		 itemProvider: (Type) -> Entry,
																		 itemUpdater: (Entry, Type) -> Unit) {
		cellValueFactory = PropertyValueFactory<Entry, Type>(propertyName)
//		cellFactory = ComboBoxTableCell.forTableColumn(object : StringConverter<Type>() {
//			override fun toString(item: Type): String = toString(item)
//			override fun fromString(string: String?): Type? = fromString(string)
//		}, *itemsProvider())

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
					comboBox = ComboBox(FXCollections.observableArrayList(itemsProvider(tableView.items[index]).toList()))
					comboBox?.minWidth = width - graphicTextGap * 2
					comboBox?.selectionModel?.select(item)
					comboBox?.converter = object : StringConverter<Type>() {
						override fun toString(item: Type) = toString(item)
						override fun fromString(string: String?) = fromString(string)
					}
					comboBox?.focusedProperty()?.addListener { _, _, newValue -> if (!newValue) commitEdit(save()) }
					comboBox?.selectionModel?.selectedItemProperty()?.addListener { _, _, newValue -> commitEdit(newValue) }
					return comboBox
				}

				private fun save() = comboBox?.selectionModel?.selectedItem
			}
		}


		setOnEditCommit { event ->
			event?.tableView?.items?.also { items ->
				val item = items[event.tablePosition.row]
				if (item == null) {
//					while (items.remove(null)) {
//					}
//					event.newValue?.also { items.add(itemProvider(it)) }
//					items.add(null)
				} else {
					itemUpdater(item, event.newValue)
				}
				saveConfig()
			}
		}
	}

	private fun TableColumn<Variable, String>.initEditableVariableValue(propertyName: String,
																		itemUpdater: (Variable) -> Unit) {
		cellValueFactory = PropertyValueFactory<Variable, String>(propertyName)
		cellFactory = Callback<TableColumn<Variable, String>, TableCell<Variable, String>> {
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
									?.map { it.toInt() }
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
					}
				}
			}
		}
		setOnEditCommit { event ->
			event?.tableView?.items?.also { items ->
				val item = items[event.tablePosition.row]
				if (item != null) {
					item.value = event.newValue
					itemUpdater(item)
					saveConfig()
				}
			}
		}
	}

	private fun TreeView<StateMachineItem>.initEditable() {
		cellFactory = Callback<TreeView<StateMachineItem>, TreeCell<StateMachineItem>> {
			object : TreeCell<StateMachineItem>() {
				private var textField: TextField? = null
				private var hBox: HBox? = null
				private var comboBox1: ComboBox<Device>? = null
				private var comboBox2: ComboBox<Variable>? = null

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

					if (empty) {
						text = null
						graphic = null
					} else if (isEditing) {
						text = null
						when (item) {
							is State -> {
								textField?.text = item.name
								graphic = textField
							}
							is Condition -> {
								comboBox1?.selectionModel?.select(item.device)
								comboBox2?.selectionModel?.select(item.value)
								graphic = hBox
							}
							is Action -> {
								comboBox1?.selectionModel?.select(item.device)
								comboBox2?.selectionModel?.select(item.value)
								graphic = hBox
							}
						}
					} else {
						text = item?.title
						graphic = item?.getImageView
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

				private fun createDoubleComboBox(): HBox? {
					item?.also { item ->
						when (item) {
							is Condition -> {
								comboBox1 = ComboBox(FXCollections.observableArrayList(tableDevices.items
										.filter {
											// it.type == VariableType.BOOLEAN
											it.kind == DeviceKind.MCP23017 && it.key.toInt() < 32
													|| it.kind == DeviceKind.BLUETOOTH && it.key == "connected"
										}))
										.apply { selectionModel.select(item.device) }
								comboBox2 = ComboBox(FXCollections.observableArrayList(tableVariables.items
										.filter { it?.type == VariableType.BOOLEAN }))
										.apply { selectionModel.select(item.value) }
							}
							is Action -> {
								comboBox2 = ComboBox()
								comboBox1 = ComboBox(FXCollections.observableArrayList(tableDevices.items
										.map { it }
										.toMutableList()
										.apply { add(Device("GOTO", DeviceKind.VIRTUAL, "goto")) }
										.filter { it.kind != DeviceKind.MCP23017 || it.key.toInt() >= 32 }))
										.apply {
											selectionModel.selectedItemProperty().addListener { _, oldValue, newValue ->
												if (oldValue == null || oldValue.type != newValue?.type) {
													comboBox2?.items?.clear()
													comboBox2?.items?.addAll(tableVariables.items
															.map { it }
															.toMutableList()
															.apply {
																addAll(treeStateMachine.root.children
																		.mapNotNull { it.value as? State }
																		.map { Variable("state_${it.name}", VariableType.STATE, it.name) })
															}
															.filter { it?.type == newValue.type })
													comboBox2?.selectionModel?.select(item.value)
												}
											}
										}
										.apply { selectionModel.select(item.device) }
							}
						}
					}

					comboBox1?.converter = object : StringConverter<Device>() {
						override fun toString(device: Device?) = device?.getDisplayName()
						override fun fromString(string: String?) = string?.toDevice(tableDevices.items)
					}
					comboBox2?.converter = object : StringConverter<Variable>() {
						override fun toString(device: Variable?) = device?.getDisplayNameValue()
						override fun fromString(string: String?) = string?.toVariable(tableVariables.items)
					}

					comboBox1?.focusedProperty()?.addListener { _, _, newValue -> if (newValue) save() }
					comboBox2?.focusedProperty()?.addListener { _, _, newValue -> if (!newValue) save() }


					val submitButton = javafx.scene.control.Button("Submit").apply {
						setOnAction {
							commitEdit(save())
						}
					}

					hBox = HBox(comboBox1, Label("="), comboBox2, submitButton)
					hBox?.minWidth = width - graphicTextGap * 2
					return hBox
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
}
