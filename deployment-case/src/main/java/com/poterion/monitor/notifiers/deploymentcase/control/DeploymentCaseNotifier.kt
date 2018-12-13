package com.poterion.monitor.notifiers.deploymentcase.control

import com.poterion.monitor.api.communication.BluetoothCommunicator
import com.poterion.monitor.api.communication.BluetoothListener
import com.poterion.monitor.api.controllers.ControllerInterface
import com.poterion.monitor.api.controllers.Notifier
import com.poterion.monitor.api.ui.Icon
import com.poterion.monitor.api.ui.NavigationItem
import com.poterion.monitor.data.notifiers.NotifierAction
import com.poterion.monitor.notifiers.deploymentcase.DeploymentCaseIcon
import com.poterion.monitor.notifiers.deploymentcase.data.DeploymentCaseConfig
import com.poterion.monitor.notifiers.deploymentcase.ui.ConfigWindowController
import dorkbox.systemTray.MenuItem
import javafx.scene.Parent

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
class DeploymentCaseNotifier(override val controller: ControllerInterface, config: DeploymentCaseConfig) :
		Notifier<DeploymentCaseConfig>(config), BluetoothListener {
	val communicator: BluetoothCommunicator = BluetoothCommunicator("TBC", config.deviceAddress, 3, 4)

	override val icon: Icon = DeploymentCaseIcon.NUCLEAR_FOOTBALL
	private val connectedIcon: Icon
		get() = if (communicator.isInboundConnected && communicator.isOutboundConnected)
			DeploymentCaseIcon.CONNECTED else DeploymentCaseIcon.DISCONNECTED

	override val navigationRoot: NavigationItem
		get() = super.navigationRoot.apply {
			sub?.add(NavigationItem(
					title = "Reconnect",
					icon = connectedIcon,
					action = { communicator.connect() },
					update = { entry, _ ->
						(entry as? MenuItem)?.enabled = config.enabled
						connectedIcon.inputStream.use { (entry as? MenuItem)?.setImage(it) }
					}
			))
		}

	override val configurationTab: Parent?
		get() = ConfigWindowController.getRoot(config, this)

	override fun initialize() {
		super.initialize()
		communicator.register(this)
	}

	override fun execute(action: NotifierAction) {
		when (action) {
			NotifierAction.ENABLE -> {
				config.enabled = true
				communicator.shouldConnect = true
				communicator.connect()
				controller.saveConfig()
			}
			NotifierAction.DISABLE -> {
				config.enabled = false
				communicator.shouldConnect = false
				communicator.disconnect()
				controller.saveConfig()
			}
			NotifierAction.TOGGLE -> execute(if (config.enabled) NotifierAction.DISABLE else NotifierAction.ENABLE)
			NotifierAction.SHUTDOWN -> communicator.disconnect()
		}
	}

	override fun onInboundConnect() {
		super.onInboundConnect()
		controller.check(true)
		controller.triggerUpdate()
	}

	override fun onInboundDisconnect() {
		super.onInboundDisconnect()
		controller.check(true)
		controller.triggerUpdate()
	}

	override fun onOutboundConnect() {
		super.onOutboundConnect()
		controller.check(true)
		controller.triggerUpdate()
	}

	override fun onOutboundDisconnect() {
		super.onOutboundDisconnect()
		controller.check(true)
		controller.triggerUpdate()
	}
}