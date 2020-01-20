package com.poterion.monitor.notifiers.notifications.control

import com.poterion.monitor.api.StatusCollector
import com.poterion.monitor.api.controllers.ControllerInterface
import com.poterion.monitor.api.controllers.ModuleInstanceInterface
import com.poterion.monitor.api.controllers.Notifier
import com.poterion.monitor.api.lib.toIcon
import com.poterion.monitor.api.lib.toImageView
import com.poterion.monitor.api.modules.Module
import com.poterion.monitor.api.utils.toUriOrNull
import com.poterion.monitor.data.Status
import com.poterion.monitor.data.StatusItem
import com.poterion.monitor.data.notifiers.NotifierAction
import com.poterion.monitor.data.serviceName
import com.poterion.monitor.notifiers.notifications.NotificationsModule
import com.poterion.monitor.notifiers.notifications.data.LastUpdatedConfig
import com.poterion.monitor.notifiers.notifications.data.NotificationsConfig
import javafx.application.Platform
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import javafx.stage.Screen
import javafx.stage.Stage
import javafx.stage.StageStyle
import javafx.util.Duration
import org.controlsfx.control.Notifications
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.awt.Desktop
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoField
import java.util.concurrent.TimeUnit


/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
class NotificationsNotifier(override val controller: ControllerInterface, config: NotificationsConfig) : Notifier<NotificationsConfig>(config) {
	companion object {
		val LOGGER: Logger = LoggerFactory.getLogger(NotificationsNotifier::class.java)
	}

	override val definition: Module<NotificationsConfig, ModuleInstanceInterface<NotificationsConfig>> = NotificationsModule
	private val owner: Stage? = Stage(StageStyle.TRANSPARENT)
			.apply {
				val root = StackPane().apply {
					style = "-fx-background-color: TRANSPARENT"
				}
				scene = Scene(root, 1.0, 1.0).apply {
					fill = Color.TRANSPARENT
				}

				val bounds = Screen.getPrimary().visualBounds
				x = bounds.width
				y = bounds.height
				width = 1.0
				height = 1.0
				toBack()
			}
			.also { it.show() }

	override val configurationRows: List<Pair<Node, Node>>?
		get() = listOf(
				Label("Repeat after").apply {
					maxWidth = Double.MAX_VALUE
					maxHeight = Double.MAX_VALUE
					alignment = Pos.CENTER_RIGHT
				} to HBox(TextField(config.repeatAfter?.let { it / 1000 }?.toInt()?.toString() ?: "")
						.apply {
							maxHeight = Double.MAX_VALUE
							promptText = "Every occurrence"
							focusedProperty().addListener { _, _, focused ->
								if (!focused) {
									config.repeatAfter = text.toLongOrNull()?.let { it * 1000 }
									controller.saveConfig()
								}
							}
						}, Label("sec").apply { maxHeight = Double.MAX_VALUE }).apply {
					spacing = 5.0
				},
				Label("Durations:").apply {
					maxWidth = Double.MAX_VALUE
					maxHeight = Double.MAX_VALUE
					alignment = Pos.CENTER_RIGHT
				} to Pane()) + Status.values().map { status ->
			HBox(
					status.toIcon().toImageView(24, 24),
					Label(status.name).apply {
						maxWidth = Double.MAX_VALUE
						maxHeight = Double.MAX_VALUE
					}).apply {
				spacing = 5.0
			} to HBox(TextField(config.durations[status.name]?.let { it / 1000 }?.toInt()?.toString() ?: "")
					.apply {
						maxHeight = Double.MAX_VALUE
						promptText = "Indefinite"
						focusedProperty().addListener { _, _, focused ->
							if (!focused) {
								config.durations[status.name] = text.toLongOrNull()?.let { it * 1000 }
								controller.saveConfig()
							}
						}
					}, Label("sec").apply { maxHeight = Double.MAX_VALUE }).apply {
				spacing = 5.0
			}
		}

	override fun initialize() {
		super.initialize()
		StatusCollector.status.sample(10, TimeUnit.SECONDS).subscribe {
			Platform.runLater {
				update(it.filter(controller.applicationConfiguration.silenced.keys, config.minPriority, config.minStatus, config.services))
			}
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
		statusItems.groupBy { it.serviceId } // Remove missing from last updated cache
				.map { (serviceId, statusItems) -> serviceId to statusItems.map { it.id } }
				.map { (serviceId, itemIds) -> serviceId to config.lastUpdated[serviceId]?.keys?.filterNot { itemIds.contains(it) } }
				.mapNotNull { (serviceId, removedIds) -> removedIds?.let { serviceId to it } }
				.flatMap { (serviceId, removedIds) -> removedIds.map { serviceId to it } }
				.forEach { (serviceId, removedId) -> config.lastUpdated[serviceId]?.remove(removedId) }

		if (config.enabled) for (statusItem in statusItems) {
			val now = Instant.now()
			val last = config.lastUpdated[statusItem.serviceId]
					?.get(statusItem.id)
					?: LastUpdatedConfig(null, null, now.toEpochMilli())
			val repeatAfter = config.repeatAfter

			if (last.status != statusItem.status
					|| (statusItem.isRepeatable && (repeatAfter == null || Instant.ofEpochMilli(last.updatedAt).isBefore(now.minusMillis(repeatAfter))))
					|| last.startedAt != statusItem.startedAt?.toEpochMilli()) {

				config.lastUpdated.getOrPut(statusItem.serviceId, { mutableMapOf() })[statusItem.id] =
						LastUpdatedConfig(statusItem.status, statusItem.startedAt?.toEpochMilli(), now.toEpochMilli())
				controller.saveConfig()

				val formatter = listOf(statusItem.startedAt ?: now, now)
						.map { it.atOffset(ZoneOffset.UTC) }
						.map { it.get(ChronoField.DAY_OF_MONTH) }
						.let { (a, b) -> if (a != b) "d.M.YYYY HH:mm:ss" else "HH:mm:ss" }
						.let { DateTimeFormatter.ofPattern(it).withZone(ZoneId.systemDefault()) }

				val duration = config.durations[statusItem.status.name]?.let { Duration(it.toDouble()) }
						?: Duration.INDEFINITE
				val title = statusItem.serviceName(controller.applicationConfiguration)
				val notification = Notifications.create()
						.owner(owner)
						.graphic(statusItem.status.toIcon().toImageView(48, 48))
						.title("${title.cut(30)} since ${formatter.format(statusItem.startedAt)}")
						.text(statusItem.title.cut(40))
						//.threshold(5)
						.position(Pos.BOTTOM_RIGHT)
						.hideAfter(duration)
				val link = statusItem.link?.toUriOrNull()
				if (link != null) {
					notification.onAction { Desktop.getDesktop().browse(link) }
					//notification.action(Action("Goto") { Desktop.getDesktop().browse(link) })
				}
				notification.show()
			}
		}
	}

	fun String.cut(maxLength: Int = 30): String {
		var value = this
		while (value.length > maxLength) {
			value = value.substringBeforeLast(" ", value.substring(0, value.length - 1))
		}
		if (value != this) value += "..."
		return value
	}
}