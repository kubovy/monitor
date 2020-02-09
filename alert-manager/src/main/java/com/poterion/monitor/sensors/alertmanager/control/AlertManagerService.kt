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
class AlertManagerService(override val controller: ControllerInterface, config: AlertManagerConfig) : Service<AlertManagerConfig>(config) {

	companion object {
		val LOGGER: Logger = LoggerFactory.getLogger(AlertManagerService::class.java)
	}

	override val definition: Module<AlertManagerConfig, ModuleInstanceInterface<AlertManagerConfig>> = AlertManagerModule
	private val service
		get() = retrofit?.create(AlertManagerRestService::class.java)

	private var lastFound = listOf<Triple<String, AlertManagerLabelConfig, AlertManagerResponse>>()

	private val labelTableSettingsPlugin = TableSettingsPlugin(
			tableName = "labelTable",
			buttonText = "Add label",
			controller = controller,
			config = config,
			createItem = { AlertManagerLabelConfig() },
			items = config.labels,
			displayName = { name },
			columnDefinitions = listOf(
					TableSettingsPlugin.ColumnDefinition(
							name = "Label",
							getter = { name },
							setter = { name = it },
							initialValue = "",
							isEditable = true),
					TableSettingsPlugin.ColumnDefinition(
							name = "Value",
							getter = { value },
							setter = { value = it },
							initialValue = "",
							isEditable = true),
					TableSettingsPlugin.ColumnDefinition(
							name = "Priority",
							getter = { priority },
							setter = { priority = it },
							initialValue = Priority.NONE,
							isEditable = true,
							icon = { toIcon() },
							options = { Priority.values().toList() }),
					TableSettingsPlugin.ColumnDefinition(
							name = "Status",
							getter = { status },
							setter = { status = it },
							initialValue = Status.NONE,
							isEditable = true,
							icon = { toIcon() },
							options = { Status.values().toList() })),
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
			})

	override val configurationRows: List<Pair<Node, Node>>
		get() = super.configurationRows + listOf(
				Label("Name annotation/label").apply {
					maxWidth = Double.MAX_VALUE
					maxHeight = Double.MAX_VALUE
					alignment = Pos.CENTER_RIGHT
				} to TextField(config.nameRefs.joinToString(",")).apply {
					textProperty().addListener { _, _, value -> config.nameRefs = value.toSet(",") }
					focusedProperty().addListener { _, _, hasFocus -> if (!hasFocus) controller.saveConfig() }
				},
				Pane() to Label("Comma separated list of annotations or labels. First found will be used."),
				Label("Description").apply {
					maxWidth = Double.MAX_VALUE
					maxHeight = Double.MAX_VALUE
					alignment = Pos.CENTER_RIGHT
				} to TextField(config.descriptionRefs.joinToString(",")).apply {
					textProperty().addListener { _, _, value -> config.descriptionRefs = value.toSet(",") }
					focusedProperty().addListener { _, _, hasFocus -> if (!hasFocus) controller.saveConfig() }
				},
				Pane() to Label("Comma separated list of annotations or labels. First found will be used."),
				Label("Receivers").apply {
					maxWidth = Double.MAX_VALUE
					maxHeight = Double.MAX_VALUE
					alignment = Pos.CENTER_RIGHT
				} to TextField(config.receivers.joinToString(",")).apply {
					promptText = "All receivers"
					textProperty().addListener { _, _, value -> config.receivers = value.toSet(",") }
					focusedProperty().addListener { _, _, hasFocus -> if (!hasFocus) controller.saveConfig() }
				},
				Pane() to Label("Comma separated list of receivers to take into account."),
				Label("Labels").apply {
					maxWidth = Double.MAX_VALUE
					maxHeight = Double.MAX_VALUE
					alignment = Pos.CENTER_RIGHT
				} to TextArea(config.labelFilter.joinToString(",")).apply {
					promptText = "All labels and annotations"
					prefHeight = 60.0
					textProperty().addListener { _, _, value ->
						config.labelFilter = value.replace("[\\n\\r\\t]".toRegex(), "").toSet(",")
					}
					focusedProperty().addListener { _, _, hasFocus -> if (!hasFocus) controller.saveConfig() }
				},
				Pane() to Label("Comma separated list labels or annotation to be used in status items. Use ! for negation."),
				labelTableSettingsPlugin.rowNewItem)

	override val configurationAddition: List<Parent>
		get() = super.configurationAddition + listOf(labelTableSettingsPlugin.vbox)

	override fun check(updater: (Collection<StatusItem>) -> Unit) {
		if (config.enabled && config.url.isNotEmpty()) try {
			val call = service?.check()
			try {
				val response = call?.execute()
				LOGGER.info("${call?.request()?.method()} ${call?.request()?.url()}:" +
						" ${response?.code()} ${response?.message()}")
				if (response?.isSuccessful == true) {
					val configPairs = config.labels.map { "${it.name}:${it.value}" to it }.toMap()
					val alerts = response.body()
						?.filter { it.status?.silencedBy?.isEmpty() != false }
						?.filter { it.status?.inhibitedBy?.isEmpty() != false }
						?.filter { a ->
							config.receivers.isEmpty() || a.receivers.map { it.name }.any {
								config.receivers.contains(it)
							}
						}
						?.flatMap { item -> item.labels.map { "${it.key}:${it.value}" to item } }
						?.filter { (pair, _) -> configPairs[pair] != null }
						?.map { (pair, item) -> configPairs.getValue(pair) to item }
						?.sortedWith(compareBy({ (c, _) -> c.status.ordinal }, { (p, _) -> p.priority.ordinal }))
						?.associateBy { (_, i) ->
							config.nameRefs.mapNotNull {
								i.annotations[it] ?: i.labels[it]
							}.firstOrNull() ?: ""
						}
						?.map { (name, p) -> Triple(name, p.first, p.second) }
						?.also { lastFound = it }
						?.map { (n, c, i) -> createStatusItem(n, c, i) }
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
					updater(getStatusItems("Service error", Status.SERVICE_ERROR))
				}
			} catch (e: Exception) {
				call?.request()
					?.also { LOGGER.warn("${it.method()} ${it.url()}: ${e.message}", e) }
					?: LOGGER.warn(e.message)
				updater(getStatusItems("Connection error", Status.CONNECTION_ERROR))
			}
		} catch (e: IOException) {
			LOGGER.error(e.message, e)
			updater(getStatusItems("Connection error", Status.CONNECTION_ERROR))
		}
	}

	private fun getStatusItems(defaultTitle: String,
							   rewriteStatus: Status? = null): Collection<StatusItem> = lastFound
			.map { (n, c, i) -> createStatusItem(n, c, i, rewriteStatus) }
			.takeIf { it.isNotEmpty() }
			?: listOf(StatusItem(
					id = "${config.uuid}-${rewriteStatus?.name ?: Status.OK.name}",
					serviceId = config.uuid,
					priority = config.priority,
					status = rewriteStatus ?: Status.OK,
					title = defaultTitle,
					isRepeatable = false))

	private fun createStatusItem(title: String,
								 labelConfig: AlertManagerLabelConfig,
								 response: AlertManagerResponse,
								 status: Status? = null) = StatusItem(
			id = "${config.uuid}-${response.fingerprint}",
			serviceId = config.uuid,
			priority = labelConfig.priority,
			status = status ?: labelConfig.status,
			title = title,
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