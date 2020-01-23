package com.poterion.monitor.sensors.jira.control

import com.poterion.monitor.api.CommonIcon
import com.poterion.monitor.api.controllers.ControllerInterface
import com.poterion.monitor.api.controllers.ModuleInstanceInterface
import com.poterion.monitor.api.controllers.Service
import com.poterion.monitor.api.modules.Module
import com.poterion.monitor.api.ui.CollectionSettingsPlugin
import com.poterion.monitor.api.ui.TableSettingsPlugin
import com.poterion.monitor.api.utils.toIcon
import com.poterion.utils.javafx.toImageView
import com.poterion.utils.kotlin.toUriOrNull
import com.poterion.monitor.data.Priority
import com.poterion.monitor.data.Status
import com.poterion.monitor.data.StatusItem
import com.poterion.monitor.sensors.jira.JiraModule
import com.poterion.monitor.sensors.jira.data.*
import com.poterion.utils.javafx.openInExternalApplication
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.control.Button
import javafx.scene.layout.Region
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.awt.Desktop
import java.io.IOException
import java.net.URLEncoder
import java.time.Instant
import java.time.format.DateTimeFormatterBuilder
import java.time.format.DateTimeParseException
import java.util.concurrent.Executors
import kotlin.math.ceil
import kotlin.math.min
import kotlin.math.round

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
class JiraService(override val controller: ControllerInterface, config: JiraConfig) : Service<JiraConfig>(config) {

	companion object {
		val LOGGER: Logger = LoggerFactory.getLogger(JiraService::class.java)
	}

	override val definition: Module<JiraConfig, ModuleInstanceInterface<JiraConfig>> = JiraModule
	private val service
		get() = retrofit?.create(JiraRestService::class.java)
	private val executor = Executors.newSingleThreadExecutor()
	private var lastUpdate: Long? = null
	private var cache: MutableMap<String, JiraIssue> = mutableMapOf()

	private val queryTableSettingsPlugin = TableSettingsPlugin(
			tableName = "queryTable",
			newLabel = "Query name",
			buttonText = "Add query",
			controller = controller,
			config = config,
			createItem = { JiraQueryConfig() },
			items = config.queries,
			displayName = { name },
			columnDefinitions = listOf(
					TableSettingsPlugin.ColumnDefinition(
							name = "Name",
							getter = { name },
							setter = { name = it },
							initialValue = "",
							isEditable = true),
					TableSettingsPlugin.ColumnDefinition(
							name = "Query",
							getter = { jql },
							setter = { jql = it },
							initialValue = "",
							isEditable = true)),
			comparator = compareBy { it.name },
			actions = listOf { item ->
				item.let { URLEncoder.encode("${it.jql} ORDER BY updated DESC", Charsets.UTF_8.name()) }
						.let { "/issues/?jql=${it}" }
						.let { path -> config.url.toUriOrNull()?.resolve(path) }
						?.let { uri -> Button("", CommonIcon.LINK.toImageView()) to uri }
						?.also { (btn, uri) -> btn.setOnAction { uri.openInExternalApplication() } }
						?.first
			})

	override val configurationRows: List<Pair<Node, Node>>
		get() = super.configurationRows +
				CollectionSettingsPlugin(
						subject = "Status mapping:",
						items = Status.values().toList(),
						value = { config.statusMapping.filterValues { it == this }.keys.joinToString(",") },
						promptText = "No mapping",
						icon = { toIcon() },
						setter = { text ->
							config.statusMapping
									.filterValues { it == this }
									.keys
									.forEach { config.statusMapping.remove(it) }
							config.statusMapping.putAll(text.trim()
									.split(",")
									.map { it.trim() }
									.filter { it.isNotBlank() }
									.map { it to this }
									.toMap())
							controller.saveConfig()
						}
				).rowItems +
				CollectionSettingsPlugin(
						subject = "Priority mapping:",
						items = Priority.values().toList(),
						value = { config.priorityMapping.filterValues { it == this }.keys.joinToString(",") },
						promptText = "No mapping",
						icon = { toIcon() },
						width = Region.USE_COMPUTED_SIZE,
						setter = { text ->
							config.priorityMapping
									.filterValues { it == this }
									.keys
									.forEach { config.priorityMapping.remove(it) }
							config.priorityMapping.putAll(text.trim()
									.split(",")
									.map { it.trim() }
									.map { it to this }
									.toMap())
							controller.saveConfig()
						}
				).rowItems +
				listOf(queryTableSettingsPlugin.rowNewItem)

	override val configurationAddition: List<Parent>
		get() = super.configurationAddition + listOf(queryTableSettingsPlugin.vbox)


	override fun check(updater: (Collection<StatusItem>) -> Unit) {
		if (config.enabled && config.url.isNotEmpty()) executor.submit {
			val alerts = cache.map { (k, v) -> k to v.toStatusItem() }.toMap().toMutableMap()
			for (query in config.queries) {
				var count = 0
				var total = 1
				var error: String? = null
				var offset = 0
				val limit = 100
				while (count < min(1000, total) && error == null && config.enabled) try {

					val jql = lastUpdate
							?.let { Instant.now().toEpochMilli() - it }
							?.let { ceil(it.toDouble() / 1000.0 / 60.0).toInt() }
							?.let { "${query.jql} AND updated >= '-${it}m' ORDER BY updated DESC" }
							?: "${query.jql} ORDER BY updated DESC"
					val call = service?.search(JiraSearchRequestBody(jql = jql, startAt = offset, maxResults = limit))
					val queryResponse = call?.execute()

					if (queryResponse?.isSuccessful == true) {
						val body = queryResponse.body()
						total = body?.total ?: 0
						offset += limit

						val issues = body?.issues ?: emptyList()
						cache.putAll(issues.mapNotNull { it.key?.let { key -> key to it } }.toMap())
						alerts.putAll(issues.mapNotNull { it.key?.let { key -> key to it.toStatusItem() } }.toMap())
						count += issues.size
					} else try {
						error = http.objectMapper
								.readValue(queryResponse?.errorBody()?.string(), JiraErrorResponse::class.java)
								.errorMessages
								.firstOrNull()
								?.let { "[${query.name}] ${it}" }
								?: "Failed retrieving ${query.name} query"
					} catch (e: Throwable) {
						error = "Failed retrieving ${query.name} query"
					}

					if (error != null) alerts.addErrorStatus(query, "Service error: ${error}", Status.SERVICE_ERROR)
				} catch (e: IOException) {
					LOGGER.error(e.message, e)
					alerts.addErrorStatus(query, "Connection error", Status.CONNECTION_ERROR)
				}
			}
			lastUpdate = Instant.now().toEpochMilli()
			updater(alerts.values)
		}
	}

	private fun MutableMap<String, StatusItem>.addErrorStatus(query: JiraQueryConfig,
															  error: String,
															  status: Status) {
		put("${query.name}-error", StatusItem(
				id = "${config.uuid}-error",
				serviceId = config.uuid,
				priority = config.priority,
				status = status,
				title = "[${query.name}] ${error}",
				isRepeatable = false))
	}

	private val JiraIssue.priority: Priority
		get() = fields?.issuetype?.name?.let { config.priorityMapping[it] }
				?: fields?.priority?.name?.let { config.priorityMapping[it] }
				?: Priority.NONE

	private val JiraIssue.status: Status
		get() = fields?.issuetype?.name?.let { config.statusMapping[it] }
				?: fields?.status
						?.let { listOf(it.name) + it.statusCategory.let { c -> listOf(c?.key, c?.name, c?.colorName) } }
						?.filterNotNull()
						?.mapNotNull { config.statusMapping[it] }
						?.maxBy { it.ordinal }
				?: Status.UNKNOWN

	private val JiraIssueFieldProgress.percent: Int?
		get() = progress?.let { p -> total?.let { t -> round(p.toDouble() * 100.0 / t.toDouble()).toInt() } }

	private fun JiraIssue.toStatusItem() = StatusItem(
			id = "${config.uuid}-${this.key}",
			serviceId = config.uuid,
			priority = this.priority,
			status = this.status,
			title = "[${this.key}] ${this.fields?.summary}",
			group = null,
			detail = this.fields?.description,
			labels = (
					(this.fields?.components?.map { it.name to "" } ?: emptyList()) +
							(this.fields?.labels?.map { it to "" } ?: emptyList()) +
							listOf("Type" to this.fields?.issuetype?.name,
									"Resolution" to this.fields?.resolution?.name,
									"Watches" to this.fields?.watches?.watchCount?.takeIf { it > 0 }?.toString(),
									"Votes" to this.fields?.votes?.votes?.takeIf { it > 0 }?.toString(),
									"Progress" to this.fields?.progress?.percent?.takeIf { it > 0 }?.let { "${it}%" },
									"Assigned" to "".takeIf { this.fields?.assignee != null },
									"Resolved" to "".takeIf { this.fields?.resolution != null },
									"Priority" to this.fields?.priority?.name,
									"Status" to this.fields?.status?.name,
									"StatusCategory" to this.fields?.status?.statusCategory?.name))
					.mapNotNull { (key, value) -> key?.let { k -> value?.let { v -> k to v } } }
					.toMap(),
			link = "${config.url}/browse/${this.key}",
			isRepeatable = false,
			children = this.fields?.issuelinks
					?.mapNotNull { it.outwardIssue?.key }
					?.map { "${config.uuid}-${it}" }
					?: emptyList(),
			startedAt = (this.fields?.updated ?: this.fields?.created)?.let { date ->
				try {
					DateTimeFormatterBuilder()
							.appendPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
							.toFormatter()
							//.withZone(ZoneId.of("UTC"))
							.parse(date)
							.let { Instant.from(it) }
				} catch (e: DateTimeParseException) {
					LOGGER.error(e.message, e)
					Instant.now()
				}
			})
}