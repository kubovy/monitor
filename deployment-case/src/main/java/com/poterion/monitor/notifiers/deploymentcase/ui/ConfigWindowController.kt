package com.poterion.monitor.notifiers.deploymentcase.ui

import com.poterion.monitor.api.communication.BluetoothCommunicatorEmbedded
import com.poterion.monitor.api.communication.BluetoothEmbeddedListener
import com.poterion.monitor.api.lib.toImage
import com.poterion.monitor.notifiers.deploymentcase.DeploymentCaseIcon
import com.poterion.monitor.notifiers.deploymentcase.api.ConfigurationContributer
import com.poterion.monitor.notifiers.deploymentcase.api.ConfigurationWindowActionListener
import com.poterion.monitor.notifiers.deploymentcase.api.DeploymentCaseMessageKind
import com.poterion.monitor.notifiers.deploymentcase.api.DeploymentCaseMessageListener
import com.poterion.monitor.notifiers.deploymentcase.control.DeploymentCaseNotifier
import com.poterion.monitor.notifiers.deploymentcase.control.toByteArray
import com.poterion.monitor.notifiers.deploymentcase.control.toData
import com.poterion.monitor.notifiers.deploymentcase.data.*
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.control.*
import javafx.scene.image.ImageView
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.layout.HBox

/**
 * Deployment case configuration window notifier.
 *
 * @author Jan Kubovy <jan@kubovy.eu>
 */
class ConfigWindowController : DeploymentCaseMessageListener, BluetoothEmbeddedListener {

	companion object {
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
	@FXML private lateinit var listConfigurations: ListView<Configuration>

	@FXML private lateinit var tabPane: TabPane
	@FXML private lateinit var tabLayout: Tab
	@FXML private lateinit var tabConfiguration: Tab
	@FXML private lateinit var tabVariables: Tab
	@FXML private lateinit var tabDevices: Tab
	@FXML private lateinit var tabStateMachine: Tab
	@FXML private lateinit var tabLog: Tab

	@FXML private lateinit var btnDownload: Button
	@FXML private lateinit var btnUpload: Button
	@FXML private lateinit var btnSynchronize: Button
	@FXML private lateinit var btnLcdReset: Button
	@FXML private lateinit var btnReconnect: Button

	@FXML private lateinit var textLog: TextArea

	@FXML private lateinit var iconConnected: ImageView
	@FXML private lateinit var iconVerified: ImageView
	@FXML private lateinit var progress: ProgressBar
	@FXML private lateinit var additionalButtons: HBox

	private lateinit var config: DeploymentCaseConfig
	private lateinit var notifier: DeploymentCaseNotifier

	private val tabControllers = mutableListOf<Any>()

	@FXML
	fun initialize() {
		additionalButtons.children.clear()
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
				tabControllers.mapNotNull { it as? ConfigurationContributer }.forEach { it.notifyNewConfiguration(configuration) }
			}
		}
	}

	private fun load() {
		listOf(tabLayout to ConfigWindowTabLayoutController.getRoot(notifier),
				tabConfiguration to ConfigWindowTabConfigurationController.getRoot(config, notifier, this@ConfigWindowController::saveConfig) { rootPane.isDisable = !it },
				tabVariables to ConfigWindowTabVariables.getRoot(this@ConfigWindowController::saveConfig),
				tabDevices to ConfigWindowTabDevices.getRoot(this@ConfigWindowController::saveConfig),
				tabStateMachine to ConfigWindowTabStateMachine.getRoot(this@ConfigWindowController::saveConfig))
				.forEach { (tab, result) ->
					val (ctrl, node) = result
					tab.content = node
					tabControllers.add(ctrl)
				}

		listConfigurations.items.clear()
		config.configurations.also { listConfigurations.items.addAll(it) }
		if (listConfigurations.items.isNotEmpty()) listConfigurations.selectionModel.select(0)

		// Status
		updateConnected()
		notifier.communicator.register(this)
		notifier.register(this)

		//notifier?.config?.configurations?.find { it.isActive }?.also { config ->
		//	FileInputStream("sm.bin").use { input ->
		//		StateCompareWindowController.popup(config.stateMachine,
		//				input.readBytes().toIntList().toStateMachine(config.stateMachine, config.devices, config.variables))
		//	}
		//}
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
	fun onDownload() {
		notifier.pullStateMachine()
	}

	@FXML
	fun onUpload() {
		tabControllers.mapNotNull { it as? ConfigurationWindowActionListener }.forEach(ConfigurationWindowActionListener::onUpload)
	}

	@FXML
	fun onSynchronize() {
		iconVerified.image = DeploymentCaseIcon.UNVERIFIED.toImage()
		notifier.synchronizeStateMachine()
	}

	@FXML
	fun onClear() {
		textLog.clear()
	}

	@FXML
	fun onResetLCD() {
		notifier.communicator.send(DeploymentCaseMessageKind.SET_STATE,
				Action(device = Device(kind = DeviceKind.LCD, key = LcdKey.RESET.key),
						value = Variable(type = VariableType.BOOLEAN, value = true.toString()))
						.toData(SharedUiData.stateMachine)
						.toByteArray()
						.let { byteArrayOf(1).plus(it) })
	}

	@FXML
	fun onReconnect() {
		notifier.communicator.also { communicator ->
			when {
				communicator.isConnected -> communicator.disconnect()
				communicator.isConnecting -> communicator.cancel()
				else -> communicator.connect(config.deviceAddress)
			}
		}
	}

	@FXML
	fun onKeyPressed(keyEvent: KeyEvent) = when (keyEvent.code) {
		KeyCode.F2 -> onDownload()
		KeyCode.F3 -> onUpload()
		KeyCode.F5 -> onReconnect()
		KeyCode.F8 -> onClear()
		else -> tabControllers.mapNotNull { it as? ConfigurationWindowActionListener }.forEach { it.onKeyPressed(keyEvent) }
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
		textLog.appendText("${message.joinToString(" ") { "0x%02X ".format(it) }}\n")
		textLog.scrollTop = Double.MAX_VALUE
	}

	override fun onProgress(progress: Int, count: Int, disable: Boolean) {
		super.onProgress(progress, count, disable)
		when {
			progress >= count -> {
				this.progress.progress = 0.0
				rootPane.isDisable = false
			}
			progress < 0 -> {
				if (disable) rootPane.isDisable = true
				this.progress.progress = ProgressIndicator.INDETERMINATE_PROGRESS
			}
			else -> {
				if (disable) rootPane.isDisable = true
				this.progress.progress = progress.toDouble() / count.toDouble()
			}
		}
		tabControllers.mapNotNull { it as? DeploymentCaseMessageListener }.forEach { it.onProgress(progress, count, disable) }
	}

	override fun onAction(action: Action) {
		super.onAction(action)
		tabControllers.mapNotNull { it as? DeploymentCaseMessageListener }.forEach { it.onAction(action) }
	}

	override fun onVerification(verified: Boolean) {
		super.onVerification(verified)
		iconVerified.image = (if (verified) DeploymentCaseIcon.VERIFIED else DeploymentCaseIcon.MISMATCH).toImage()
		tabControllers.mapNotNull { it as? DeploymentCaseMessageListener }.forEach { it.onVerification(verified) }
	}

	private fun updateConnected() {
		val connected = notifier.communicator.isConnected
		val connecting = notifier.communicator.isConnecting
		progress.progress = 0.0
		rootPane.isDisable = false

		val icon = if (connected) DeploymentCaseIcon.CONNECTED else DeploymentCaseIcon.DISCONNECTED
		iconConnected.image = icon.toImage()
		iconVerified.image = DeploymentCaseIcon.UNVERIFIED.toImage()

		progress.progress = 0.0
		rootPane.isDisable = false

		tabLayout.isDisable = !connected
		if (tabPane.selectionModel.selectedIndex == 0 && !connected) tabPane.selectionModel.select(1)
		btnDownload.isDisable = !connected
		btnUpload.isDisable = !connected
		btnSynchronize.isDisable = !connected
		btnLcdReset.isDisable = !connected

		btnReconnect.text = if (connected) "Disconnect [F5]" else if (connecting) "Cancel [F5]" else "Connect [F5]"
	}

	private fun saveConfig() {
		// Save selected configuration
		tabControllers.mapNotNull { it as? ConfigurationContributer }
				.forEach { it.updateConfiguration(config, listConfigurations.selectionModel.selectedItem) }

		// Save config
		config.configurations = listConfigurations.items

		notifier.controller.saveConfig()
		notifier.communicator.takeIf(BluetoothCommunicatorEmbedded::isConnected)?.connect(config.deviceAddress)
	}
}
