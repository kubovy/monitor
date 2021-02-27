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
package com.poterion.monitor.notifiers.notifications.control

import com.poterion.monitor.api.StatusCollector
import com.poterion.monitor.api.controllers.ControllerInterface
import com.poterion.monitor.api.controllers.ModuleInstanceInterface
import com.poterion.monitor.api.controllers.Notifier
import com.poterion.monitor.api.filter
import com.poterion.monitor.api.modules.Module
import com.poterion.monitor.api.ui.CollectionSettingsPlugin
import com.poterion.monitor.api.utils.toIcon
import com.poterion.monitor.data.Status
import com.poterion.monitor.notifiers.notifications.NotificationsModule
import com.poterion.monitor.notifiers.notifications.data.LastUpdatedConfig
import com.poterion.monitor.notifiers.notifications.data.NotificationsConfig
import com.poterion.utils.javafx.openInExternalApplication
import com.poterion.utils.javafx.toImageView
import com.poterion.utils.kotlin.cutLastWords
import com.poterion.utils.kotlin.noop
import com.poterion.utils.kotlin.toUriOrNull
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.control.Button
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
import javafx.util.StringConverter
import org.controlsfx.control.Notifications
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoField


/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
class NotificationsNotifier(override val controller: ControllerInterface, config: NotificationsConfig) : Notifier<NotificationsConfig>(config) {
	companion object {
		val LOGGER: Logger = LoggerFactory.getLogger(NotificationsNotifier::class.java)
	}

	override val definition: Module<NotificationsConfig, ModuleInstanceInterface<NotificationsConfig>> = NotificationsModule
	private val owner: Stage? = Stage(StageStyle.TRANSPARENT)
			.apply {
				val size = 1.0
				val root = StackPane().apply {
					style = "-fx-background-color: TRANSPARENT"
				}
				scene = Scene(root, size, size).apply {
					fill = Color.TRANSPARENT
				}

				val bounds = Screen.getPrimary().visualBounds
				x = bounds.width - size
				y = bounds.height - size
				width = size
				height = size
				toBack()
			}
			.also { it.show() }

	override val configurationRows: List<Pair<Node, Node>>
		get() = super.configurationRows + listOf(
				Label("Repeat after").apply {
					maxWidth = Double.MAX_VALUE
					maxHeight = Double.MAX_VALUE
					alignment = Pos.CENTER_RIGHT
				} to HBox(TextField()
						.apply {
							prefWidth = 130.0
							promptText = "Every occurrence"
							textProperty().bindBidirectional(config.repeatAfterProperty, object : StringConverter<Long?>() {
								override fun fromString(string: String?): Long? = string
										?.toLongOrNull()
										?.let { it * 1000 }

								override fun toString(value: Long?): String = value
										?.let { it / 1000 }?.toInt()
										?.toString()
										?: ""
							})
							focusedProperty().addListener { _, _, focused -> if (!focused) controller.saveConfig() }
						}, Label("seconds").apply { maxHeight = Double.MAX_VALUE }).apply {
					spacing = 5.0
				}) +
				CollectionSettingsPlugin(
						subject = "Durations:",
						items = Status.values().toList(),
						value = { config.durations[name]?.let { it / 1000 }?.toInt()?.toString() ?: "" },
						promptText = "Indefinite",
						width = 130.0,
						suffix = "seconds",
						icon = { toIcon() },
						setter = { text ->
							val value = text.toLongOrNull()?.let { it * 1000 }
							if (value == null) config.durations.remove(name) else config.durations[name] = value
							controller.saveConfig()
						}
				).rowItems +
				listOf(
						Pane() to Button("Test")
								.apply {
									prefWidth = 130.0
									setOnAction {
										Status.values().forEach { status ->
											Notifications.create()
													.owner(owner)
													.graphic(status.toIcon().toImageView(48, 48))
													.title("${status} test")
													.text("Suspendisse facilisis dolor odio, nec lobortis ligula pharetra in.")
													//.threshold(5)
													//.position(Pos.BOTTOM_RIGHT)
													.hideAfter(Duration.seconds(5.0))
													.show()
										}
									}
								}
				)

	override fun update() {
		val statusItems = StatusCollector.items.filter(controller.applicationConfiguration.silencedMap.keys,
				config.minPriority,
				config.minStatus,
				config.services)
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
				val title = controller.applicationConfiguration.serviceMap[statusItem.serviceId]?.name ?: ""
				val since = formatter.format(statusItem.startedAt ?: Instant.now())
				val notification = Notifications.create()
						.owner(owner)
						.graphic(statusItem.status.toIcon().toImageView(48, 48))
						.title("${title.cutLastWords(30)} since ${since}")
						.text(statusItem.title.cutLastWords(40))
						//.threshold(5)
						.position(Pos.BOTTOM_RIGHT)
						.hideAfter(duration)
				val link = statusItem.link?.toUriOrNull()
				if (link != null) {
					notification.onAction { link.openInExternalApplication() }
					//notification.action(Action("Goto") { Desktop.getDesktop().browse(link) })
				}
				notification.show()
			}
		}
	}

	override fun shutdown() = noop()
}