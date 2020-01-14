package com.poterion.monitor.notifications.control

import com.poterion.monitor.api.StatusCollector
import com.poterion.monitor.api.controllers.ControllerInterface
import com.poterion.monitor.api.controllers.ModuleInstanceInterface
import com.poterion.monitor.api.controllers.Notifier
import com.poterion.monitor.api.lib.toIcon
import com.poterion.monitor.api.lib.toImageView
import com.poterion.monitor.api.modules.Module
import com.poterion.monitor.api.utils.factory
import com.poterion.monitor.api.utils.toUriOrNull
import com.poterion.monitor.data.Status
import com.poterion.monitor.data.StatusItem
import com.poterion.monitor.data.notifiers.NotifierAction
import com.poterion.monitor.notifications.NotificationsModule
import com.poterion.monitor.notifications.data.NotificationsConfig
import javafx.application.Platform
import javafx.collections.FXCollections
import javafx.geometry.HPos
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.layout.GridPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.util.Duration
import org.controlsfx.control.Notifications
import org.controlsfx.control.action.Action
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

	private val statusItemCache = mutableMapOf<String, StatusItem>()
	private val statusItemLastShownAtCache = mutableMapOf<String, Instant>()

	override val definition: Module<NotificationsConfig, ModuleInstanceInterface<NotificationsConfig>> = NotificationsModule

	override val configurationRows: List<Pair<Node, Node>>?
		get() = listOf(
				Label("Min. status").apply {
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
				},
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
		for (statusItem in statusItems
				.filter { it.priority >= config.minPriority }
				.filter { it.status >= config.minStatus }) {
			val now = Instant.now()
			val key = "${statusItem.serviceName}-${statusItem.title}"
			//val lastStarted = statusItemCache[key]?.startedAt
			val lastStatus = statusItemCache[key]?.status
			val lastShown = statusItemLastShownAtCache[key] ?: Instant.EPOCH
			val repeatAfter = config.repeatAfter

			if (lastStatus != statusItem.status
					|| repeatAfter == null
					|| lastShown.isBefore(now.minusMillis(repeatAfter))) {
				statusItemCache[key] = statusItem
				statusItemLastShownAtCache[key] = now

				val formatter = listOf(statusItem.startedAt, now)
						.map { it.atOffset(ZoneOffset.UTC) }
						.map { it.get(ChronoField.DAY_OF_MONTH) }
						.let { (a, b) -> if (a != b) "d.M.YYYY HH:mm:ss" else "HH:mm:ss" }
						.let { DateTimeFormatter.ofPattern(it).withZone(ZoneId.systemDefault()) }

				val duration = config.durations[statusItem.status.name]?.let { Duration(it.toDouble()) }
						?: Duration.INDEFINITE

				val notification = Notifications.create()
						.graphic(statusItem.status.toIcon().toImageView(48, 48))
						.title("${statusItem.title} since ${formatter.format(statusItem.startedAt)}")
						.text(statusItem.detail)
						.hideAfter(duration)
				val link = statusItem.link?.toUriOrNull()
				if (link != null) {
					notification.action(Action("Goto") { Desktop.getDesktop().browse(link) })
				}
				notification.show()
			}
		}
	}
}