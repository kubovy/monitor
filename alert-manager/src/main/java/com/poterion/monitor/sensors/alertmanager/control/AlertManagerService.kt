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
package com.poterion.monitor.sensors.alertmanager.control

import com.poterion.monitor.api.controllers.ControllerInterface
import com.poterion.monitor.api.controllers.ModuleInstanceInterface
import com.poterion.monitor.api.controllers.Service
import com.poterion.monitor.api.modules.Module
import com.poterion.monitor.api.ui.TableSettingsPlugin
import com.poterion.monitor.api.utils.toIcon
import com.poterion.monitor.data.Priority
import com.poterion.monitor.data.Status
import com.poterion.monitor.data.StatusItem
import com.poterion.monitor.sensors.alertmanager.AlertManagerModule
import com.poterion.monitor.sensors.alertmanager.data.AlertManagerConfig
import com.poterion.monitor.sensors.alertmanager.data.AlertManagerLabelConfig
import com.poterion.monitor.sensors.alertmanager.data.AlertManagerResponse
import com.poterion.utils.javafx.openInExternalApplication
import com.poterion.utils.javafx.toImageView
import com.poterion.utils.javafx.toObservableList
import com.poterion.utils.kotlin.setAll
import com.poterion.utils.kotlin.toSet
import com.poterion.utils.kotlin.toUriOrNull
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.TextArea
import javafx.scene.control.TextField
import javafx.scene.layout.Pane
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.URLEncoder
import java.time.Instant
import java.time.format.DateTimeParseException

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
class AlertManagerService(override val controller: ControllerInterface, config: AlertManagerConfig):
		Service<AlertManagerConfig>(config) {

	companion object {
		val LOGGER: Logger = LoggerFactory.getLogger(AlertManagerService::class.java)
	}

	override val definition: Module<AlertManagerConfig, ModuleInstanceInterface<AlertManagerConfig>> =
			AlertManagerModule
	private val service
		get() = retrofit?.create(AlertManagerRestService::class.java)

	private var lastFound = listOf<Triple<String, AlertManagerResponse, Collection<AlertManagerLabelConfig>>>()

	private val labelTableSettingsPlugin = TableSettingsPlugin(
			tableName = "labelTable",
			buttonText = "Add label",
			controller = controller,
			config = config,
			createItem = { AlertManagerLabelConfig() },
			items = config.subConfig,
			displayName = { name },
			columnDefinitions = listOf(
					TableSettingsPlugin.ColumnDefinition(
							name = "Label",
							property = { nameProperty },
							initialValue = "",
							isEditable = true),
					TableSettingsPlugin.ColumnDefinition(
							name = "Value",
							property = { valueProperty },
							initialValue = "",
							isEditable = true),
					TableSettingsPlugin.ColumnDefinition(
							name = "Priority",
							property = { priorityProperty },
							initialValue = Priority.NONE,
							isEditable = true,
							icon = { toIcon() },
							options = Priority.values().toObservableList()),
					TableSettingsPlugin.ColumnDefinition(
							name = "Status",
							property = { statusProperty },
							initialValue = Status.NONE,
							isEditable = true,
							icon = { toIcon() },
							options = Status.values().toObservableList())),
			comparator = compareBy(
					{ -it.priority.ordinal },
					{ -it.status.ordinal },
					{ it.name },
					{ it.value }),
			actions = listOf { item ->
				item.let { URLEncoder.encode("{${it.name}=\"${it.value}\"}", Charsets.UTF_8.name()) }
						.let { "/#/alerts?silenced=true&inhibited=true&active=true&filter=${it}" }
						.let { path -> config.url.toUriOrNull()?.resolve(path) }
						?.let { uri -> Button("", com.poterion.monitor.api.CommonIcon.LINK.toImageView()) to uri }
						?.also { (btn, uri) -> btn.setOnAction { uri.openInExternalApplication() } }
						?.first
			},
			fieldSizes = arrayOf(150.0))

	override val configurationRows: List<Pair<Node, Node>>
		get() = super.configurationRows + listOf(
				Label("Name annotation/label").apply {
					maxWidth = Double.MAX_VALUE
					maxHeight = Double.MAX_VALUE
					alignment = Pos.CENTER_RIGHT
				} to TextField(config.nameRefs.joinToString(",")).apply {
					focusedProperty().addListener { _, _, focused ->
						if (!focused) {
							config.nameRefs.setAll(text.replace("[\\n\\r\\t]".toRegex(), "").toSet(","))
							controller.saveConfig()
						}
					}
				},
				Pane() to Label("Comma separated list of annotations or labels. First found will be used."),
				Label("Description").apply {
					maxWidth = Double.MAX_VALUE
					maxHeight = Double.MAX_VALUE
					alignment = Pos.CENTER_RIGHT
				} to TextField(config.descriptionRefs.joinToString(",")).apply {
					focusedProperty().addListener { _, _, focused ->
						if (!focused) {
							config.descriptionRefs.setAll(text.replace("[\\n\\r\\t]".toRegex(), "").toSet(","))
							controller.saveConfig()
						}
					}
				},
				Pane() to Label("Comma separated list of annotations or labels. First found will be used."),
				Label("Receivers").apply {
					maxWidth = Double.MAX_VALUE
					maxHeight = Double.MAX_VALUE
					alignment = Pos.CENTER_RIGHT
				} to TextField(config.receivers.joinToString(",")).apply {
					promptText = "All receivers"
					textProperty().addListener { _, _, v -> config.receivers.setAll(v.toSet(",")) }
					focusedProperty().addListener { _, _, focused -> if (!focused) controller.saveConfig() }
				},
				Pane() to Label("Comma separated list of receivers to take into account."),
				Label("Labels").apply {
					maxWidth = Double.MAX_VALUE
					maxHeight = Double.MAX_VALUE
					alignment = Pos.CENTER_RIGHT
				} to TextArea(config.labelFilter.joinToString(",")).apply {
					promptText = "All labels and annotations"
					prefHeight = 60.0
					focusedProperty().addListener { _, _, focused ->
						if (!focused) {
							config.labelFilter.setAll(text.replace("[\\n\\r\\t]".toRegex(), "").toSet(","))
							controller.saveConfig()
						}
					}
				},
				Pane() to Label("Comma separated list labels or annotation to be used in status items. Use ! for negation."),
				labelTableSettingsPlugin.rowNewItem)

	override val configurationAddition: List<Parent>
		get() = super.configurationAddition + listOf(labelTableSettingsPlugin.vbox)

	override fun check(updater: (Collection<StatusItem>) -> Unit) {
		var error: String? = null
		if (config.enabled && config.url.isNotBlank()) try {
			val call = service?.check()
			val response = call?.execute()
			LOGGER.info("${call?.request()?.method()} ${call?.request()?.url()}:" +
					" ${response?.code()} ${response?.message()}")
			if (response?.isSuccessful == true) {
				val configPairs = config.subConfig.map { "${it.name}:${it.value}" to it }.toMap()
				val alerts = response.body()
						?.filter { it.status?.silencedBy?.isEmpty() != false }
						?.filter { it.status?.inhibitedBy?.isEmpty() != false }
						?.filter { a ->
							config.receivers.isEmpty() || a.receivers.map { it.name }.any {
								config.receivers.contains(it)
							}
						}
						?.map { item -> item to item.labels.map { "${it.key}:${it.value}" } }
						?.map { (item, labels) -> item to labels.mapNotNull { configPairs[it] } }
						?.mapNotNull { (item, configs) -> configs.takeIf { it.isNotEmpty() }?.let { item to it } }
						//?.sortedWith(compareBy({ (item, configs) -> configs.status.ordinal }, { (p, _) -> p.priority.ordinal }))
						?.associateBy { (i, _) ->
							config.nameRefs.mapNotNull { i.annotations[it] ?: i.labels[it] }.firstOrNull() ?: ""
						}
						?.map { (name, pair) -> Triple(name, pair.first, pair.second) }
						?.also { lastFound = it }
						?.map { (name, items, configs) -> createStatusItem(name, items, configs) }
						?.takeIf { it.isNotEmpty() }
						?: listOf(StatusItem(
								id = "${config.uuid}-no-alerts",
								serviceId = config.uuid,
								priority = config.priority,
								status = Status.OK,
								title = "No alerts",
								isRepeatable = false))
				updater(alerts)
			} else {
				LOGGER.warn("${call?.request()?.method()} ${call?.request()?.url()}:" +
						" ${response?.code()} ${response?.message()}")
				error = response?.let { "Code: ${it.code()} ${it.message() ?: ""}" } ?: "Service error"
				updater(getStatusItems("Service error", Status.SERVICE_ERROR,
						response?.let { "Code: ${it.code()} ${it.message() ?: ""}" }))
			}
		} catch (e: IOException) {
			LOGGER.error(e.message)
			error = e.message ?: "Connection error"
			updater(getStatusItems("Connection error", Status.CONNECTION_ERROR, e.message))
		}
		lastErrorProperty.set(error)
	}

	private fun getStatusItems(defaultTitle: String,
							   rewriteStatus: Status,
							   detail: String?): Collection<StatusItem> = lastFound
			.map { (name, item, configs) -> createStatusItem(name, item, configs, rewriteStatus) }
			.takeIf { it.isNotEmpty() }
			?: listOf(StatusItem(
					id = "${config.uuid}-${rewriteStatus.name}",
					serviceId = config.uuid,
					priority = config.priority,
					status = rewriteStatus,
					title = defaultTitle,
					detail = detail,
					isRepeatable = false))

	private fun createStatusItem(title: String,
								 response: AlertManagerResponse,
								 labelConfigs: Collection<AlertManagerLabelConfig>,
								 status: Status? = null) = StatusItem(
			id = "${config.uuid}-${response.fingerprint}",
			serviceId = config.uuid,
			configIds = labelConfigs.map { it.configTitle }.toMutableList(),
			priority = labelConfigs.maxBy { it.priority }?.priority ?: config.priority,
			status = status ?: labelConfigs.maxBy { it.status }?.status ?: Status.UNKNOWN,
			title = title,
			detail = config.descriptionRefs.mapNotNull { response.annotations[it] ?: response.labels[it] }.firstOrNull()
					?: "Annotations:"
					+ response.annotations.map { (k, v) -> "\t${k}: ${v}" }.joinToString("\n", "\n", "\n")
					+ "Labels:"
					+ response.labels.map { (k, v) -> "\t${k}: ${v}" }.joinToString("\n", "\n"),
			labels = (response.labels + response.annotations)
					.filterNot { (k, _) -> config.nameRefs.contains(k) }
					.filterNot { (k, _) -> config.descriptionRefs.contains(k) }
					.filter { (k, _) ->
						config.labelFilter
								.filterNot { it.startsWith("!") }
								.let { it.isEmpty() || it.contains(k) }
					}
					.filter { (k, _) ->
						config.labelFilter
								.filter { it.startsWith("!") }
								.map { it.removePrefix("!") }
								.let { it.isEmpty() || !it.contains(k) }
					},
			link = response.generatorURL,
			isRepeatable = true,
			startedAt = try {
				Instant.parse(response.startsAt)
			} catch (e: DateTimeParseException) {
				LOGGER.error(e.message, e)
				Instant.now()
			})
}