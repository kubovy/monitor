/******************************************************************************
 * Copyright (C) 2020 Jan Kubovy (jan@kubovy.eu)                              *
 *                                                                            *
 * This program is free software: you can redistribute it and/or modify       *
 * it under the terms of the GNU General Public License as published by       *
 * the Free Software Foundation, either version 3 of the License, or          *
 * (at your option) any later version.                                        *
 *                                                                            *
 * This program is distributed in the hope that it will be useful,            *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of             *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the              *
 * GNU General Public License for more details.                               *
 *                                                                            *
 * You should have received a copy of the GNU General Public License          *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.      *
 ******************************************************************************/
package com.poterion.monitor.notifiers.deploymentcase.ui

import com.poterion.communication.serial.communicator.BluetoothCommunicator
import com.poterion.communication.serial.communicator.Channel
import com.poterion.communication.serial.communicator.State
import com.poterion.communication.serial.listeners.CommunicatorListener
import com.poterion.communication.serial.payload.DeviceCapabilities
import com.poterion.communication.serial.payload.LcdCommand
import com.poterion.utils.kotlin.noop
import com.poterion.utils.javafx.toImage
import com.poterion.monitor.notifiers.deploymentcase.DeploymentCaseIcon
import com.poterion.monitor.notifiers.deploymentcase.api.ConfigurationWindowActionListener
import com.poterion.monitor.notifiers.deploymentcase.api.DeploymentCaseMessageListener
import com.poterion.monitor.notifiers.deploymentcase.control.DeploymentCaseNotifier
import com.poterion.monitor.notifiers.deploymentcase.data.Configuration
import com.poterion.monitor.notifiers.deploymentcase.data.DeploymentCaseConfig
import com.poterion.monitor.notifiers.deploymentcase.data.Device
import com.poterion.monitor.notifiers.deploymentcase.data.SharedUiData
import javafx.application.Platform
import javafx.beans.Observable
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
 * @author Jan Kubovy [jan@kubovy.eu]
 */
class ConfigWindowController : DeploymentCaseMessageListener,
	CommunicatorListener {

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
	@FXML private lateinit var btnClear: Button
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

	private var tabsEnabled: Boolean = true
		set(value) {
			tabConfiguration.isDisable = !value
			tabVariables.isDisable = !value
			tabDevices.isDisable = !value
			tabStateMachine.isDisable = !value
			field = value
		}

	private var controlsEnabled: Boolean = true
		set(value) {
			tabLayout.isDisable = !value
					|| !notifier.bluetoothCommunicator.isConnected
			btnDownload.isDisable = !value
					|| !notifier.bluetoothCommunicator.isConnected
					|| !SharedUiData.isActiveProperty.get()
			btnUpload.isDisable = !value
					|| !notifier.bluetoothCommunicator.isConnected
					|| !SharedUiData.isActiveProperty.get()
			btnSynchronize.isDisable = !value
					|| !notifier.bluetoothCommunicator.isConnected
					|| !SharedUiData.isActiveProperty.get()
			btnClear.isDisable = !value
			btnLcdReset.isDisable = !value
					|| !notifier.bluetoothCommunicator.isConnected
			field = value
		}

	private var activationChangeListener = { _: Observable, _: Boolean, _: Boolean -> listConfigurations.refresh() }

	@FXML
	fun initialize() {
		// TODO
		(btnDownload.parent as? HBox)?.children?.remove(btnDownload)
		(btnLcdReset.parent as? HBox)?.children?.remove(btnLcdReset)

		additionalButtons.children.clear()
		listConfigurations.apply {
			setCellFactory {
				object : ListCell<Configuration>() {
					override fun updateItem(item: Configuration?, empty: Boolean) {
						super.updateItem(item, empty)
						if (item != null && !empty) {
							text = item.name
							graphic = null
							style = item.takeIf { it.isActive }?.let { "-fx-font-weight: bold;" } ?: ""
						} else {
							text = null
							graphic = null
							style = null
						}
					}
				}
			}
			selectionModel.selectedItemProperty().addListener { _, _, configuration ->
				SharedUiData.isActiveProperty.removeListener(activationChangeListener)
				SharedUiData.configurationProperty.set(configuration)
				SharedUiData.isActiveProperty.addListener(activationChangeListener)
				tabsEnabled = configuration != null
				controlsEnabled = true
			}
			tabsEnabled = false
			SharedUiData.nameProperty.addListener { _, _, _ -> listConfigurations.refresh() }
		}
		tabPane.selectionModel.select(1)
		listConfigurations.selectionModel.select(null)
	}

	private fun load() {
		rootPane.setDividerPosition(0, config.split)
		rootPane.dividers.first().positionProperty().addListener { _, _, value ->
			config.split = value.toDouble()
			notifier.controller.saveConfig()
		}

		listOf(tabLayout to ConfigWindowTabLayoutController.getRoot(notifier),
				tabConfiguration to ConfigWindowTabConfigurationController.getRoot(config, notifier, this@ConfigWindowController::saveConfig),
				tabVariables to ConfigWindowTabVariables.getRoot(config, this@ConfigWindowController::saveConfig),
				tabDevices to ConfigWindowTabDevices.getRoot(this@ConfigWindowController::saveConfig),
				tabStateMachine to ConfigWindowTabStateMachine.getRoot(this@ConfigWindowController::saveConfig))
				.forEach { (tab, result) ->
					val (ctrl, node) = result
					tab.content = node
					tabControllers.add(ctrl)
				}

		config.configurations.also { listConfigurations.items.addAll(it) }
		//if (listConfigurations.items.isNotEmpty()) listConfigurations.selectionModel.select(0)

		// Status
		updateConnected()
		notifier.bluetoothCommunicator.register(this)
		notifier.register(this)
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
		notifier.lcdCommunicator.sendLcdCommand(0, LcdCommand.RESET)
		// TODO notifier.bluetoothCommunicator.send(MessageKind.SM_SET_STATE,
		//		Action(device = Device(kind = DeviceKind.LCD, key = "${LcdKey.RESET.key}").toData(),
		//				value = Variable(type = VariableType.BOOLEAN, value = true.toString()).name) // FIXME will not woe
		//				.toData(SharedUiData.stateMachine, SharedUiData.devices, SharedUiData.variables)
		//				.toByteArray()
		//				.let { byteArrayOf(1).plus(it) })
	}

	@FXML
	fun onReconnect() {
		when {
			notifier.bluetoothCommunicator.isConnected -> notifier.bluetoothCommunicator.disconnect()
			notifier.bluetoothCommunicator.isConnecting -> notifier.bluetoothCommunicator.disconnect()
			else -> notifier.bluetoothCommunicator.connect(BluetoothCommunicator.Descriptor(config.deviceAddress, 6))
		}
	}

	@FXML
	fun onKeyPressed(keyEvent: KeyEvent) = when (keyEvent.code) {
		//TODO KeyCode.F2 -> onDownload()
		KeyCode.F3 -> onUpload()
		KeyCode.F5 -> onReconnect()
		KeyCode.F8 -> onClear()
		else -> tabControllers.mapNotNull { it as? ConfigurationWindowActionListener }.forEach { it.onKeyPressed(keyEvent) }
	}

	override fun onConnecting(channel: Channel) = Platform.runLater { updateConnected() }

	override fun onConnect(channel: Channel) = Platform.runLater { updateConnected() }

	override fun onConnectionReady(channel: Channel) = noop()

	override fun onDisconnect(channel: Channel) = Platform.runLater { updateConnected() }

	override fun onMessageReceived(channel: Channel, message: IntArray) = Platform.runLater {
		textLog.appendText("[${channel.name}] ${message.joinToString(" ") { "0x%02X ".format(it) }}\n")
		textLog.scrollTop = Double.MAX_VALUE
	}

	override fun onMessagePrepare(channel: Channel) = noop()

	override fun onMessageSent(channel: Channel, message: IntArray, remaining: Int) = noop()

	override fun onDeviceCapabilitiesChanged(channel: Channel, capabilities: DeviceCapabilities) = noop()

	override fun onDeviceNameChanged(channel: Channel, name: String) = noop()

	override fun onProgress(progress: Int, count: Int, disable: Boolean) {
		super.onProgress(progress, count, disable)
		when {
			progress >= count -> { // Finished
				this.progress.progress = 0.0
				if (listConfigurations.selectionModel.selectedIndex != -1) tabsEnabled = true
				controlsEnabled = when (notifier.bluetoothCommunicator.state) {
					State.CONNECTING,
					State.DISCONNECTING,
					State.DISCONNECTED -> false
					State.CONNECTED -> true
				}
			}
			progress < 0 -> { // Started indeterminate
				if (disable) {
					tabsEnabled = false
					controlsEnabled = false
				}
				this.progress.progress = ProgressIndicator.INDETERMINATE_PROGRESS
			}
			else -> { // Started determinate
				if (disable) {
					tabsEnabled = false
					controlsEnabled = false
				}
				this.progress.progress = progress.toDouble() / count.toDouble()
			}
		}
		tabControllers.mapNotNull { it as? DeploymentCaseMessageListener }.forEach { it.onProgress(progress, count, disable) }
	}

	override fun onAction(device: Device, value: String) {
		super.onAction(device, value)
		tabControllers.mapNotNull { it as? DeploymentCaseMessageListener }.forEach { it.onAction(device, value) }
	}

	override fun onVerification(verified: Boolean) {
		super.onVerification(verified)
		iconVerified.image = (if (verified) DeploymentCaseIcon.VERIFIED else DeploymentCaseIcon.MISMATCH).toImage()
		tabControllers.mapNotNull { it as? DeploymentCaseMessageListener }.forEach { it.onVerification(verified) }
	}

	private fun updateConnected() {
		val icon: DeploymentCaseIcon
		when (notifier.bluetoothCommunicator.state) {
			State.CONNECTING,
			State.DISCONNECTING -> {
				onProgress(-1, 1, false)
				controlsEnabled = false
				//if (tabPane.selectionModel.selectedIndex == 0) tabPane.selectionModel.select(1)
				icon = DeploymentCaseIcon.DISCONNECTED
				btnReconnect.text = "Cancel [F5]"
			}
			State.CONNECTED -> {
				onProgress(0, 0, false)
				icon = DeploymentCaseIcon.CONNECTED
				btnReconnect.text = "Disconnect [F5]"
			}
			State.DISCONNECTED -> {
				onProgress(0, 0, false)
				controlsEnabled = false
				//if (tabPane.selectionModel.selectedIndex == 0) tabPane.selectionModel.select(1)
				icon = DeploymentCaseIcon.DISCONNECTED
				btnReconnect.text = "Connect [F5]"
			}
		}

		iconConnected.image = icon.toImage()
		iconVerified.image = DeploymentCaseIcon.UNVERIFIED.toImage()
	}

	private fun saveConfig() {
		// Save config
		config.configurations.clear()
		config.configurations.addAll(listConfigurations.items)

		notifier.controller.saveConfig()
		notifier.bluetoothCommunicator
				.takeIf(BluetoothCommunicator::isConnected)
				?.connect(BluetoothCommunicator.Descriptor(config.deviceAddress, 6))
	}
}
