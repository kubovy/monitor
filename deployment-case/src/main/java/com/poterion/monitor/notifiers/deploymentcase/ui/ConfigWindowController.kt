package com.poterion.monitor.notifiers.deploymentcase.ui

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.poterion.monitor.api.communication.BluetoothListener
import com.poterion.monitor.notifiers.deploymentcase.DeploymentCaseIcon
import com.poterion.monitor.notifiers.deploymentcase.control.DeploymentCaseNotifier
import com.poterion.monitor.notifiers.deploymentcase.data.Configuration
import com.poterion.monitor.notifiers.deploymentcase.data.DeploymentCaseConfig
import com.poterion.monitor.notifiers.deploymentcase.data.Device
import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.control.*
import javafx.scene.control.cell.PropertyValueFactory
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
class ConfigWindowController : BluetoothListener {

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

	@FXML private lateinit var textBluetoothAddress: TextField
	@FXML private lateinit var listConfigurations: ListView<Configuration>

	@FXML private lateinit var checkboxActive: CheckBox
	@FXML private lateinit var textName: TextField
	@FXML private lateinit var comboboxMethod: ComboBox<String>
	@FXML private lateinit var textURL: TextField
	@FXML private lateinit var textUsername: TextField
	@FXML private lateinit var textPassword: PasswordField

	@FXML private lateinit var tableVariables: TableView<Pair<String, String>>
	@FXML private lateinit var columnVariableName: TableColumn<Pair<String, String>, String>
	@FXML private lateinit var columnVariableValue: TableColumn<Pair<String, String>, String>


	@FXML private lateinit var tableDevices: TableView<Device>
	@FXML private lateinit var columnDevicesName: TableColumn<Device, String>
	@FXML private lateinit var columnDevicesKind: TableColumn<Device, String>
	@FXML private lateinit var columnDevicesKey: TableColumn<Device, String>

	@FXML private lateinit var textStateMachine: TextArea
	@FXML private lateinit var textLog: TextArea

	@FXML private lateinit var comboboxType: ComboBox<String>
	@FXML private lateinit var comboboxName: ComboBox<String>
	@FXML private lateinit var comboboxValue: ComboBox<String>

	@FXML private lateinit var iconInbound: ImageView
	@FXML private lateinit var iconOutbound: ImageView

	private var config: DeploymentCaseConfig? = null
	private var controller: DeploymentCaseNotifier? = null
	private val jsonMapper = ObjectMapper()
	private val yamlMapper = ObjectMapper(YAMLFactory())

	init {
		yamlMapper.enable(SerializationFeature.INDENT_OUTPUT)
	}

	@FXML
	fun initialize() {
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
				tableVariables.items.clear()
				tableVariables.items.addAll(configuration.variables.entries.map { it.key to it.value })
				tableDevices.items.clear()
				tableDevices.items.addAll(configuration.devices)
				textStateMachine.text = configuration.stateMachine
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

		columnVariableName.init("first")
		columnVariableValue.init("second")
		tableVariables.focusedProperty().addListener { _, _, focused -> if (!focused) saveConfig() }
		tableVariables.sortOrder.setAll(columnVariableName)

		columnDevicesName.init("name")
		columnDevicesKind.init("kind")
		columnDevicesKey.init("key")
		tableDevices.focusedProperty().addListener { _, _, focused -> if (!focused) saveConfig() }
		tableDevices.sortOrder.setAll(columnDevicesName)

		textStateMachine.focusedProperty().addListener { _, _, focused -> if (!focused) saveConfig() }

		// Test
		comboboxType.items.addAll("State", "Action")
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
		iconInbound.image = Image(
				if (controller?.communicator?.isInboundConnected == true) DeploymentCaseIcon.CONNECTED.inputStream
				else DeploymentCaseIcon.DISCONNECTED.inputStream)
		iconOutbound.image = Image(
				if (controller?.communicator?.isOutboundConnected == true) DeploymentCaseIcon.CONNECTED.inputStream
				else DeploymentCaseIcon.DISCONNECTED.inputStream)

		controller?.communicator?.register(this)
	}

	@FXML
	fun onAddConfig(event: ActionEvent) {
		var index = 1
		while (listConfigurations.items.map { it.name }.contains("${NEW_NAME} ${index}")) index++
		listConfigurations.items.add(Configuration(name = "${NEW_NAME} ${index}"))
		saveConfig()
	}

	@FXML
	fun onDeleteSelectedConfig(event: ActionEvent) {
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
	fun onPull(event: ActionEvent?) {
		controller?.communicator?.send("PULL")
	}

	@FXML
	fun onPush(event: ActionEvent?) {

	}

	@FXML
	fun onClear(event: ActionEvent?) {
		textLog.clear()
	}

	@FXML
	fun onReconnect(event: ActionEvent?) {
		controller?.communicator?.connect()
	}

	@FXML
	fun onTest(event: ActionEvent?) {
		val type = comboboxType.selectionModel.selectedItem?.toLowerCase()
		val name = comboboxName.value
		val value = comboboxValue.value

		if (comboboxName.items.contains(name).not()) comboboxName.items.add(0, name)
		if (comboboxValue.items.contains(value).not()) comboboxValue.items.add(0, value)

		while (comboboxName.items.size > 20) comboboxName.items.removeAt(comboboxName.items.size - 1)
		while (comboboxValue.items.size > 20) comboboxValue.items.removeAt(comboboxValue.items.size - 1)

		LOGGER.debug("Message: ${type},${name},${value}")
		controller?.communicator?.send("${type},${name},${value}")
		saveConfig()
	}

	@FXML
	fun onKeyPressed(keyEvent: KeyEvent) = when (keyEvent.code) {
		KeyCode.F2 -> onPull(null)
		KeyCode.F3 -> onPush(null)
		KeyCode.F4 -> onTest(null)
		KeyCode.F5 -> onReconnect(null)
		KeyCode.F8 -> onClear(null)
		else -> null
	}

	override fun onInboundConnect() {
		iconInbound.image = Image(DeploymentCaseIcon.CONNECTED.inputStream)
	}

	override fun onInboundDisconnect() {
		iconInbound.image = Image(DeploymentCaseIcon.DISCONNECTED.inputStream)
	}

	override fun onOutboundConnect() {
		iconOutbound.image = Image(DeploymentCaseIcon.CONNECTED.inputStream)
	}

	override fun onOutboundDisconnect() {
		iconOutbound.image = Image(DeploymentCaseIcon.DISCONNECTED.inputStream)
	}

	override fun onMessage(message: String) {
		textLog.appendText("${message}\n")
		textLog.scrollTop = Double.MAX_VALUE

		val json = jsonMapper.readTree(message)
		if (json.has("states") && json.has("devices") && json.has("vars")) {
			textStateMachine.text = yamlMapper.writer().withDefaultPrettyPrinter().writeValueAsString(json.get("states"))

			tableVariables.items.clear()
			tableVariables.items.addAll(json.get("vars").fields()
					.asSequence()
					.map { (k, v) -> k to v.toString() }
					.toList())
			tableVariables.sortOrder.setAll(columnVariableName)

			tableDevices.items.clear()
			tableDevices.items.addAll(json.get("devices").fields()
					.asSequence()
					.map { (k, v) -> Device(k, v.get("kind").asText(), v.get("key").asText()) }
					.toList())
			tableDevices.sortOrder.setAll(columnDevicesName)
		}
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
			variables = tableVariables.items.toMap()
			devices = tableDevices.items
			stateMachine = textStateMachine.text
		}

		// Save config
		config?.deviceAddress = textBluetoothAddress.text ?: ""
		config?.testNameHistory = comboboxName.items
		config?.testValueHistory = comboboxValue.items
		config?.configurations = listConfigurations.items

		controller?.controller?.saveConfig()
	}

	private fun <Entry> TableColumn<Entry, String>.init(propertyName: String) {
		cellValueFactory = PropertyValueFactory<Entry, String>(propertyName)
		setCellFactory {
			object : TableCell<Entry, String>() {
				override fun updateItem(item: String?, empty: Boolean) {
					super.updateItem(item, empty)
					text = item
				}
			}
		}
	}
}
