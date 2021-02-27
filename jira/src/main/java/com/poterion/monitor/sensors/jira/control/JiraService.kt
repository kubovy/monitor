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
package com.poterion.monitor.sensors.jira.control

import com.poterion.monitor.api.CommonIcon
import com.poterion.monitor.api.controllers.ControllerInterface
import com.poterion.monitor.api.controllers.ModuleInstanceInterface
import com.poterion.monitor.api.controllers.Service
import com.poterion.monitor.api.modules.Module
import com.poterion.monitor.api.ui.CollectionSettingsPlugin
import com.poterion.monitor.api.ui.TableSettingsPlugin
import com.poterion.monitor.api.utils.toIcon
import com.poterion.monitor.data.Priority
import com.poterion.monitor.data.Status
import com.poterion.monitor.data.StatusItem
import com.poterion.monitor.sensors.jira.JiraModule
import com.poterion.monitor.sensors.jira.data.*
import com.poterion.utils.javafx.openInExternalApplication
import com.poterion.utils.javafx.toImageView
import com.poterion.utils.kotlin.toUriOrNull
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.control.Button
import javafx.scene.layout.Region
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.URLEncoder
import java.time.Instant
import java.time.format.DateTimeFormatterBuilder
import java.time.format.DateTimeParseException
import kotlin.math.ceil
import kotlin.math.min
import kotlin.math.round

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
class JiraService(override val controller: ControllerInterface, config: JiraConfig) :
		Service<JiraQueryConfig, JiraConfig>(config) {

	companion object {
		val LOGGER: Logger = LoggerFactory.getLogger(JiraService::class.java)
	}

	override val definition: Module<JiraConfig, ModuleInstanceInterface<JiraConfig>> = JiraModule
	private val service
		get() = retrofit?.create(JiraRestService::class.java)
	private var lastUpdate: Long? = null
	private var cache: MutableMap<String, StatusItem> = mutableMapOf()

	private val queryTableSettingsPlugin = TableSettingsPlugin(
			tableName = "queryTable",
			newLabel = "Query name",
			buttonText = "Add query",
			controller = controller,
			config = config,
			createItem = { JiraQueryConfig() },
			items = config.subConfig,
			displayName = { name },
			columnDefinitions = listOf(
					TableSettingsPlugin.ColumnDefinition(
							name = "Name",
							property = { nameProperty },
							initialValue = "",
							isEditable = true),
					TableSettingsPlugin.ColumnDefinition(
							name = "Query",
							property = { jqlProperty },
							initialValue = "",
							isEditable = true)),
			comparator = compareBy { it.name },
			actions = listOf { item ->
				Button("", CommonIcon.LINK.toImageView()).apply { setOnAction { gotoSubConfig(item) } }
			},
			fieldSizes = arrayOf(150.0))

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

	override fun gotoSubConfig(subConfig: JiraQueryConfig) {
		subConfig.let { URLEncoder.encode("${it.jql} ORDER BY updated DESC", Charsets.UTF_8.name()) }
				.let { "/issues/?jql=${it}" }
				.let { path -> config.url.toUriOrNull()?.resolve(path) }
				?.openInExternalApplication()
	}

	override fun doCheck(): List<StatusItem> {
		val alerts = cache.map { (k, v) -> k to v }.toMap().toMutableMap()
		var error: String? = null
		for (query in config.subConfig) {
			var count = 0
			var total = 1
			var offset = 0
			val limit = 100
			while (count < min(1000, total) && error == null && config.enabled) try {

				val jql = lastUpdate
						?.let { Instant.now().toEpochMilli() - it }
						?.let { ceil(it.toDouble() / 1000.0 / 60.0).toInt() }
						?.let { "${query.jql} AND updated >= '-${it}m' ORDER BY updated DESC" }
						?: "${query.jql} ORDER BY updated DESC"
				val call = service?.search(JiraSearchRequestBody(jql = jql, startAt = offset, maxResults = limit))

				checkForInterruptions()
				val response = call?.execute()
				checkForInterruptions()

				LOGGER.info("${call?.request()?.method()} ${call?.request()?.url()}:" +
						" ${response?.code()} ${response?.message()}")

				if (response?.isSuccessful == true) {
					val body = response.body()
					total = body?.total ?: 0
					offset += limit

					val issues = (body?.issues ?: emptyList())
							.mapNotNull { it.key?.let { key -> key to it.toStatusItem(query) } }
							.toMap()
					cache.putAll(issues)
					alerts.putAll(issues)
					count += issues.size
				} else try {
					LOGGER.warn("${call?.request()?.method()} ${call?.request()?.url()}:" +
							" ${response?.code()} ${response?.message()}")
					error = http
							?.objectMapper
							?.readValue(response?.errorBody()?.string(), JiraErrorResponse::class.java)
							?.errorMessages
							?.firstOrNull()
							?: response?.let {
								"Code: ${it.code()} ${it.message()}"
							} ?: "Failed retrieving ${query.name} query"
				} catch (e: Throwable) {
					error = "Failed retrieving ${query.name} query"
				}

				if (error != null) alerts.addErrorStatus(query, "Service error", Status.SERVICE_ERROR, error)
			} catch (e: IOException) {
				LOGGER.error("${config.url} with ${query.jql}: ${e.message}")
				error = e.message ?: "Connection error"
				alerts.addErrorStatus(query, "Connection error", Status.CONNECTION_ERROR, e.message)
				count = total
			}
		}
		lastErrorProperty.set(error)
		lastUpdate = Instant.now().toEpochMilli()
		return alerts.values.toList()
	}

	private fun MutableMap<String, StatusItem>.addErrorStatus(query: JiraQueryConfig,
															  error: String,
															  status: Status,
															  detail: String?) = put(
			"${query.name}-error",
			StatusItem(
					id = "${config.uuid}-error",
					serviceId = config.uuid,
					configIds = mutableListOf(query.configTitle),
					priority = config.priority,
					status = status,
					title = "[${query.name}] ${error}",
					detail = detail,
					isRepeatable = false))

	private val JiraIssue.priority: Priority
		get() = fields?.priority?.name?.let { config.priorityMapping[it] }
				?: fields?.issuetype?.name?.let { config.priorityMapping[it] }
				?: Priority.NONE

	private val JiraIssue.status: Status
		get() = fields?.status
			?.let { listOf(it.name) + it.statusCategory.let { c -> listOf(c?.key, c?.name, c?.colorName) } }
			?.filterNotNull()
			?.mapNotNull { config.statusMapping[it] }
			?.maxByOrNull { it.ordinal }
				?: fields?.issuetype?.name?.let { config.statusMapping[it] }
				?: Status.UNKNOWN

	private val JiraIssueFieldProgress.percent: Int?
		get() = progress?.let { p -> total?.let { t -> round(p.toDouble() * 100.0 / t.toDouble()).toInt() } }

	private fun JiraIssue.toStatusItem(query: JiraQueryConfig) = StatusItem(
			id = "${config.uuid}-${this.key}",
			serviceId = config.uuid,
			configIds = mutableListOf(query.configTitle),
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