package com.poterion.monitor.notification.tabs.control

import com.poterion.monitor.api.StatusCollector
import com.poterion.monitor.api.controllers.ControllerInterface
import com.poterion.monitor.api.controllers.ModuleInstanceInterface
import com.poterion.monitor.api.controllers.Notifier
import com.poterion.monitor.api.lib.toIcon
import com.poterion.monitor.api.lib.toImageView
import com.poterion.monitor.api.modules.Module
import com.poterion.monitor.api.utils.factory
import com.poterion.monitor.data.Status
import com.poterion.monitor.data.StatusItem
import com.poterion.monitor.data.notifiers.NotifierAction
import com.poterion.monitor.notification.tabs.NotificationTabsModule
import com.poterion.monitor.notification.tabs.data.NotificationTabsConfig
import com.poterion.monitor.notification.tabs.ui.TabController
import javafx.application.Platform
import javafx.collections.FXCollections
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.control.ComboBox
import javafx.scene.control.Label
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
class NotificationTabsNotifier(override val controller: ControllerInterface, config: NotificationTabsConfig) : Notifier<NotificationTabsConfig>(config) {
	companion object {
		val LOGGER: Logger = LoggerFactory.getLogger(NotificationTabsNotifier::class.java)
	}

	override val definition: Module<NotificationTabsConfig, ModuleInstanceInterface<NotificationTabsConfig>> = NotificationTabsModule

	override val configurationRows: List<Pair<Node, Node>>?
		get() = listOf(
				Label("Minimum status").apply {
					maxWidth = Double.MAX_VALUE
					maxHeight = Double.MAX_VALUE
					alignment = Pos.CENTER_RIGHT
				} to ComboBox<Status>(FXCollections.observableList(Status.values().toList())).apply {
					factory { item, empty ->
						text = item?.takeUnless { empty }?.name
						graphic = item?.takeUnless { empty }?.toIcon()?.toImageView()
					}
					maxHeight = Double.MAX_VALUE
					selectionModel.select(config.minStatus)
					selectionModel.selectedItemProperty().addListener { _, _, value ->
						config.minStatus = value
						controller.saveConfig()
					}
				})

	override val configurationTab: Parent?
		get() = TabController.getRoot(controller, config)

	override fun initialize() {
		StatusCollector.status.sample(10, TimeUnit.SECONDS).subscribe {
			Platform.runLater { update(it.items) }
		}
	}

	override fun execute(action: NotifierAction): Unit = when (action) {
		NotifierAction.ENABLE -> {
			config.enabled = true
			controller.saveConfig()
		}
		NotifierAction.DISABLE -> {
			config.enabled = false
			controller.saveConfig()
		}
		NotifierAction.TOGGLE -> execute(if (config.enabled) NotifierAction.DISABLE else NotifierAction.ENABLE)
		else -> LOGGER.debug("Executing action ${action}")
	}

	private fun update(statusItems: Collection<StatusItem>) {
		val items = statusItems
				.filter { it.priority >= config.minPriority }
				.filter { it.status >= config.minStatus }
				.filter { config.services.isEmpty() || config.services.contains(it.serviceName) }
		(configurationTab?.userData as? TabController)?.update(items)
	}
}