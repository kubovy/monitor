package com.poterion.monitor.notifiers.deploymentcase.ui

import com.poterion.monitor.api.communication.BluetoothCommunicatorEmbedded
import com.poterion.monitor.api.communication.BluetoothEmbeddedListener
import com.poterion.monitor.notifiers.deploymentcase.*
import com.poterion.monitor.notifiers.deploymentcase.api.DeploymentCaseMessageKind
import com.poterion.monitor.notifiers.deploymentcase.api.DeploymentCaseMessageListener
import com.poterion.monitor.notifiers.deploymentcase.control.*
import com.poterion.monitor.notifiers.deploymentcase.data.*
import javafx.collections.FXCollections
import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Node
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
import javafx.scene.shape.Circle
import javafx.util.Callback
import javafx.util.StringConverter
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Deployment case configuration window notifier.
 *
 * @author Jan Kubovy <jan@kubovy.eu>
 */
class ConfigWindowController : DeploymentCaseMessageListener, BluetoothEmbeddedListener {

	companion object {
		private val LOGGER: Logger = LoggerFactory.getLogger(ConfigWindowController::class.java)
		private const val NEW_NAME = "New configuration"

		internal fun getRoot(config: DeploymentCaseConfig, controller: DeploymentCaseNotifier): Parent =
				FXMLLoader(ConfigWindowController::class.java.getResource("config-window.fxml"))
						.let { it.load<Parent>() to it.getController<ConfigWindowController>() }
						.let { (root, ctrl) ->
							ctrl.config = config
							ctrl.notifier = controller
							ctrl.load()
							root
						}
	}

	@FXML private lateinit var rootPane: SplitPane
	@FXML private lateinit var comboBluetoothAddress: ComboBox<Pair<String, String>>
	@FXML private lateinit var listConfigurations: ListView<Configuration>

	@FXML private lateinit var tabPane: TabPane
	@FXML private lateinit var tabLayout: Tab
	@FXML private lateinit var tabConfiguration: Tab
	@FXML private lateinit var tabVariables: Tab
	@FXML private lateinit var tabDevices: Tab
	@FXML private lateinit var tabActions: Tab
	@FXML private lateinit var tabStateMachine: Tab
	@FXML private lateinit var tabLog: Tab

	@FXML private lateinit var btnPull: Button
	@FXML private lateinit var btnPush: Button
	@FXML private lateinit var btnClear: Button
	@FXML private lateinit var btnLcdReset: Button
	@FXML private lateinit var btnReconnect: Button

	@FXML private lateinit var lcd: TextArea
	@FXML private lateinit var led0: Circle
	@FXML private lateinit var led1: Circle
	@FXML private lateinit var led2: Circle
	@FXML private lateinit var led3: Circle
	@FXML private lateinit var rgb00: Circle
	@FXML private lateinit var rgb01: Circle
	@FXML private lateinit var rgb02: Circle
	@FXML private lateinit var rgb03: Circle
	@FXML private lateinit var rgb04: Circle
	@FXML private lateinit var rgb05: Circle
	@FXML private lateinit var rgb06: Circle
	@FXML private lateinit var rgb07: Circle
	@FXML private lateinit var rgb08: Circle
	@FXML private lateinit var rgb09: Circle
	@FXML private lateinit var rgb10: Circle
	@FXML private lateinit var rgb11: Circle
	@FXML private lateinit var rgb12: Circle
	@FXML private lateinit var rgb13: Circle
	@FXML private lateinit var rgb14: Circle
	@FXML private lateinit var rgb15: Circle
	@FXML private lateinit var rgb16: Circle
	@FXML private lateinit var rgb17: Circle
	@FXML private lateinit var rgb18: Circle
	@FXML private lateinit var rgb19: Circle
	@FXML private lateinit var rgb20: Circle
	@FXML private lateinit var rgb21: Circle
	@FXML private lateinit var rgb22: Circle
	@FXML private lateinit var rgb23: Circle
	@FXML private lateinit var rgb24: Circle
	@FXML private lateinit var rgb25: Circle
	@FXML private lateinit var rgb26: Circle
	@FXML private lateinit var rgb27: Circle
	@FXML private lateinit var rgb28: Circle
	@FXML private lateinit var rgb29: Circle
	@FXML private lateinit var rgb30: Circle
	@FXML private lateinit var rgb31: Circle
	@FXML private lateinit var btn00: ToggleButton
	@FXML private lateinit var btn01: RadioButton
	@FXML private lateinit var btn0102: RadioButton
	@FXML private lateinit var btn02: RadioButton
	@FXML private lateinit var btn03: ToggleButton
	@FXML private lateinit var btn04: ToggleButton
	@FXML private lateinit var btn05: ToggleButton
	@FXML private lateinit var btn06: ToggleButton
	@FXML private lateinit var btn07: ToggleButton
	@FXML private lateinit var btn08: ToggleButton
	@FXML private lateinit var btn09: ToggleButton
	@FXML private lateinit var btn10: ToggleButton
	@FXML private lateinit var btn11: ToggleButton
	@FXML private lateinit var btn12: ToggleButton
	@FXML private lateinit var btn13: ToggleButton
	@FXML private lateinit var btn14: ToggleButton
	@FXML private lateinit var btn15: ToggleButton
	@FXML private lateinit var btn16: ToggleButton
	@FXML private lateinit var btn17: ToggleButton
	@FXML private lateinit var btn18: ToggleButton
	@FXML private lateinit var btn19: ToggleButton
	@FXML private lateinit var btn20: ToggleButton
	@FXML private lateinit var btn21: ToggleButton
	@FXML private lateinit var btn22: ToggleButton
	@FXML private lateinit var btn23: ToggleButton
	@FXML private lateinit var btn24: ToggleButton
	@FXML private lateinit var btn25: ToggleButton


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
	private var notifier: DeploymentCaseNotifier? = null
	private var clipboardStateMachineItem: TreeItem<StateMachineItem>? = null

	@FXML
	fun initialize() {
		additionalButtons.children.clear()
		comboBluetoothAddress.converter = object : StringConverter<Pair<String, String>>() {
			override fun toString(device: Pair<String, String>?): String = device
					?.let { (name, addr) -> "${name} [${addr}]" }
					?: ""

			override fun fromString(string: String?): Pair<String, String>? = string
					?.let { "(.*) \\[([0-9a-fA-F:]+)]".toRegex().find(it) }
					?.let { it.groupValues[1] to it.groupValues[2] }
		}
		comboBluetoothAddress.focusedProperty().addListener { _, _, focuesed -> if (!focuesed) saveConfig() }
		comboBluetoothAddress.selectionModel.selectedItemProperty().addListener { _, _, selected ->
			config?.deviceAddress = selected.second
			saveConfig()
		}
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
				treeStateMachine.isShowRoot = false
				treeStateMachine.root = TreeItem<StateMachineItem>(Placeholder())
				treeStateMachine.setStateMachine(configuration.stateMachine)

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
			}
		}

		// Config
		checkboxActive.focusedProperty().addListener { _, _, focused -> if (!focused) saveConfig() }
		checkboxActive.selectedProperty().addListener { _, _, selected ->
			val selectedConfig = listConfigurations.selectionModel.selectedItem
			if (selected) {
				listConfigurations.items
						.filter { it != listConfigurations.selectionModel.selectedItem }
						.forEach { it.isActive = false }
				selectedConfig?.also { notifier?.sendStateMachine(it.stateMachine) }
			}
			selectedConfig?.isActive = selected
			saveConfig()
		}
		textName.focusedProperty().addListener { _, _, focused -> if (!focused) saveConfig() }

		comboboxMethod.items.addAll("GET", "POST")
		comboboxMethod.selectionModel.select(0)
		comboboxMethod.focusedProperty().addListener { _, _, focused -> if (!focused) saveConfig() }

		textURL.focusedProperty().addListener { _, _, focused -> if (!focused) saveConfig() }
		textUsername.focusedProperty().addListener { _, _, focused -> if (!focused) saveConfig() }
		textPassword.focusedProperty().addListener { _, _, focused -> if (!focused) saveConfig() }
		textJobName.focusedProperty().addListener { _, _, focused -> if (!focused) saveConfig() }
		textParameters.focusedProperty().addListener { _, _, focused -> if (!focused) saveConfig() }

		columnVariableName.initEditableText("name") { variable, name ->
			variable.name = name
			tableActions.items.filter { it.value?.name == name }.forEach { it.value = variable }
			tableActions.refresh()
		}
		columnVariableType.initEditableCombo("type",
				{ VariableType.values() },
				{ it?.description ?: "" },
				{ str -> VariableType.values().find { it.description == str } },
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

		columnDevicesName.initEditableText("name") { device, name ->
			device.name = name
			tableActions.refresh()
		}
		columnDevicesKind.init("kind") { it?.description ?: "" }
		columnDevicesKey.init("key") { it ?: "" }
		tableDevices.focusedProperty().addListener { _, _, focused -> if (!focused) saveConfig() }
		tableDevices.sortOrder.setAll(columnDevicesName)

		columnActionsId.init("id") { value -> value?.let { "%02X".format(it) } ?: "" }
		columnActionsDevice.initEditableCombo("device",
				{ tableDevices.items.toTypedArray() },
				{ value -> value.getDisplayName() },
				{ str -> str?.toDevice(tableDevices.items) },
				{ action, device -> action.device = device })
		columnActionsValue.initEditableCombo("value",
				{ entry -> tableVariables.items.filter { it?.type == entry?.device?.type }.toTypedArray() },
				{ it?.getDisplayNameValue() ?: "" },
				{ str -> str?.toVariable(tableVariables.items) },
				{ action, variable -> action.value = variable })
		tableActions.sortOrder.setAll(columnActionsId)

		treeStateMachine.initEditable()

		// Test
		comboboxType.items.addAll("State", "Action", "Transit")
		comboboxType.selectionModel.select(0)
	}

	private fun load() {
		val remoteDevices = notifier?.communicator
				?.takeIf { notifier?.controller?.config?.btDiscovery == true }
				?.devices()
				?.toMutableList()
				?: mutableListOf()
		config?.deviceAddress
				?.takeIf { addr -> remoteDevices.none { (_, a) -> a == addr } }
				?.also { addr -> remoteDevices.add("" to addr) }
		comboBluetoothAddress.items.clear()
		comboBluetoothAddress.items.addAll(remoteDevices)
		remoteDevices.find { (_, addr) -> addr == config?.deviceAddress }
				?.also { comboBluetoothAddress.selectionModel.select(it) }

		comboboxName.items.clear()
		comboboxValue.items.clear()
		listConfigurations.items.clear()
		config?.testNameHistory?.also { comboboxName.items.addAll(it) }
		config?.testValueHistory?.also { comboboxValue.items.addAll(it) }
		config?.configurations?.also { listConfigurations.items.addAll(it) }
		if (listConfigurations.items.isNotEmpty()) listConfigurations.selectionModel.select(0)

		// Status
		updateConnected()
		notifier?.communicator?.register(this)
		notifier?.register(this)
	}

	@FXML
	fun onAddConfig() {
		var index = 1
		while (listConfigurations.items.map { it.name }.contains("${NEW_NAME} ${index}")) index++
		listConfigurations.items.add(Configuration(name = "${NEW_NAME} ${index}"))
		saveConfig()
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
		notifier?.communicator?.send(DeploymentCaseMessageKind.PULL_STATE_MACHINE)
	}

	@FXML
	fun onPush() {
		rootPane.isDisable = true
		notifier?.sendStateMachine(treeStateMachine.toStateMachine())
	}

	@FXML
	fun onClear() {
		textLog.clear()
	}

	@FXML
	fun onResetLCD() {
		notifier?.communicator?.send(DeploymentCaseMessageKind.SET_STATE,
				Action(device = Device(kind = DeviceKind.LCD, key = LcdKey.RESET.key),
						value = Variable(type = VariableType.BOOLEAN, value = true.toString()))
						.toData(treeStateMachine.toStateMachine())
						.toByteArray()
						.let { byteArrayOf(1).plus(it) })
	}

	@FXML
	fun onReconnect() {
		notifier?.communicator?.also { communicator ->
			when {
				communicator.isConnected -> communicator.disconnect()
				communicator.isConnecting -> communicator.cancel()
				else -> communicator.connect(config?.deviceAddress)
			}
		}
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
		//notifier?.communicator?.send("${type},${name},${value}")
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
	fun onTestButton(event: ActionEvent) {
		LOGGER.info("ID: ${(event.source as? Node)?.id}")
		(event.source as? Node)?.id?.also { id ->
			if (id == "btn0102") {
				val data = (1..2)
						.map { Device(kind = DeviceKind.MCP23017, key = "${it}") }
						.map { device -> device to Variable(type = VariableType.BOOLEAN, value = "false") }
						.map { (device, value) -> Action(device = device, value = value) }
						.map { it.toData(treeStateMachine.toStateMachine()).toByteArray() }
						.reduce { acc, bytes -> acc.plus(bytes) }
						.let { byteArrayOf(2).plus(it) }
				notifier?.communicator?.send(DeploymentCaseMessageKind.SET_STATE, data)
			} else if (id.startsWith("btn")) {
				val data = Device(kind = DeviceKind.MCP23017, key = "${id.substring(3, 5).toInt()}")
						.let { device -> device to (event.source as? ToggleButton)?.isSelected }
						.let { (device, value) -> device to Variable(type = VariableType.BOOLEAN, value = value.toString()) }
						.let { (device, value) -> Action(device = device, value = value) }
						.let { it.toData(treeStateMachine.toStateMachine()).toByteArray() }
						.let { byteArrayOf(1).plus(it) }
				notifier?.communicator?.send(DeploymentCaseMessageKind.SET_STATE, data)
			}
		}
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
					?.takeIf { _ ->
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
					.takeIf { _ ->
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
						?.takeIf { _ ->
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

	override fun onConnecting() {
		super.onConnecting()
		updateConnected()
		progress.progress = ProgressIndicator.INDETERMINATE_PROGRESS
	}

	override fun onConnect() {
		super.onConnect()
		updateConnected()
	}

	override fun onDisconnect() {
		super.onDisconnect()
		updateConnected()
	}

	override fun onMessage(message: ByteArray) {
		textLog.appendText("${message.map { "0x%02X ".format(it) }}}\n")
		textLog.scrollTop = Double.MAX_VALUE
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

	override fun onAction(action: Action) {
		super.onAction(action)
		action.let { it.device to it.value }
				.let { (device, variable) -> device?.let { d -> variable?.let { v -> d to v } } }
				//?.takeIf { (device, _) -> device.kind == DeviceKind.MCP23017 }
				?.let { (device, variable) -> device to variable.value }
				?.also { (device, value) ->
					if (device.kind == DeviceKind.MCP23017 && device.key.toInt() < 32) listOf(btn00, btn01, btn02,
							btn03, btn04, btn05, btn06, btn07, btn08, btn09, btn10, btn11, btn12, btn13, btn14, btn15,
							btn16, btn17, btn18, btn19, btn20, btn21, btn22, btn23, btn24, btn25)
							.find { it.id == "btn%02d".format(device.key.toInt()) }
							?.isSelected = value.toBoolean()

					if (device.kind == DeviceKind.MCP23017 && device.key.toInt() >= 32) listOf(led0, led1, led2, led3)
							.find { it.id == "led%d".format(device.key.toInt() - 32) }
							?.fill = if (value.toBoolean()) Color.RED else Color.BLACK

					if (device.kind == DeviceKind.WS281x) listOf(rgb00, rgb01, rgb02, rgb03, rgb04, rgb05, rgb06, rgb07,
							rgb08, rgb09, rgb10, rgb11, rgb12, rgb13, rgb14, rgb15, rgb16, rgb17, rgb18, rgb19, rgb20,
							rgb21, rgb22, rgb23, rgb24, rgb25, rgb26, rgb27, rgb28, rgb29, rgb30, rgb31)
							.find { it.id == "rgb%02d".format(device.key.toInt()) }
							?.fill = value
							.split(",")
							.mapNotNull { it.toIntOrNull() }
							.takeIf { it.size >= 4 }
							?.let { (_, r, g, b) -> Color.rgb(r, g, b) }
							?: Color.BLACK

					if (device.kind == DeviceKind.LCD && device.key == LcdKey.MESSAGE.key) lcd.text = value
							.replace("\\n", "\n")
				}

	}

	private fun updateConnected() {
		val connected = notifier?.communicator?.isConnected == true
		val connecting = notifier?.communicator?.isConnecting == true
		progress.progress = 0.0
		rootPane.isDisable = false

		val icon = if (connected) DeploymentCaseIcon.CONNECTED else DeploymentCaseIcon.DISCONNECTED
		iconConnected.image = Image(icon.inputStream)

		progress.progress = 0.0
		rootPane.isDisable = false
		iconConnected.image = Image(DeploymentCaseIcon.DISCONNECTED.inputStream)

		tabLayout.isDisable = !connected
		if (tabPane.selectionModel.selectedIndex == 0 && !connected) tabPane.selectionModel.select(1)
		tabPane.tabs.remove(tabActions)
		btnPull.isDisable = !connected
		btnPush.isDisable = !connected
		btnLcdReset.isDisable = !connected

		btnReconnect.text = if (connected) "Disconnect [F5]" else if (connecting) "Cancel [F5]" else "Connect [F5]"
	}

	private fun saveConfig() {
		// Save selected configuration
		listConfigurations.selectionModel.selectedItem?.apply {
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
			stateMachine = treeStateMachine.toStateMachine()
		}

		// Save config
		config?.deviceAddress = comboBluetoothAddress.selectionModel.selectedItem?.second ?: ""
		config?.testNameHistory = comboboxName.items
		config?.testValueHistory = comboboxValue.items
		config?.configurations = listConfigurations.items

		notifier?.controller?.saveConfig()
		notifier?.communicator?.takeIf(BluetoothCommunicatorEmbedded::isConnected)?.connect(config?.deviceAddress)
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
				if (item != null) itemUpdater(item, event.newValue)
				saveConfig()
			}
		}
	}

	private fun <Entry, Type> TableColumn<Entry, Type>.initEditableCombo(propertyName: String,
																		 itemsProvider: (Entry?) -> Array<Type>,
																		 toString: (Type?) -> String,
																		 fromString: (String?) -> Type?,
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
				if (item != null) itemUpdater(item, event.newValue)
				saveConfig()
			}
		}
	}

	private fun TableColumn<Variable, String>.initEditableVariableValue(propertyName: String,
																		itemUpdater: (Variable) -> Unit) {
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
		cellFactory = Callback<TreeView<StateMachineItem>, TreeCell<StateMachineItem>> { _ ->
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
										.toMutableList()
										.apply { add(Device("GOTO", DeviceKind.VIRTUAL, "goto")) }
										.filter { it.kind != DeviceKind.MCP23017 || it.key.toInt() >= 32 }))
										.apply {
											selectionModel.selectedItemProperty().addListener { _, oldValue, newValue ->
												if (oldValue == null || oldValue.type != newValue?.type) {
													comboBox2?.items?.clear()
													comboBox2?.items?.addAll(tableVariables.items
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
