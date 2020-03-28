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
package com.poterion.monitor.sensors.gerritcodereview.control

import com.poterion.monitor.api.CommonIcon
import com.poterion.monitor.api.controllers.ControllerInterface
import com.poterion.monitor.api.controllers.ModuleInstanceInterface
import com.poterion.monitor.api.controllers.Service
import com.poterion.monitor.api.modules.Module
import com.poterion.monitor.api.ui.TableSettingsPlugin
import com.poterion.monitor.api.utils.toIcon
import com.poterion.monitor.data.Priority
import com.poterion.monitor.data.Status
import com.poterion.monitor.data.StatusItem
import com.poterion.monitor.sensors.gerritcodereview.GerritCodeReviewModule
import com.poterion.monitor.sensors.gerritcodereview.data.GerritCodeReviewConfig
import com.poterion.monitor.sensors.gerritcodereview.data.GerritCodeReviewQueryConfig
import com.poterion.monitor.sensors.gerritcodereview.data.GerritCodeReviewQueryResponse
import com.poterion.utils.javafx.openInExternalApplication
import com.poterion.utils.javafx.toImageView
import com.poterion.utils.javafx.toObservableList
import com.poterion.utils.kotlin.toUriOrNull
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.control.Button
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.URLEncoder
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatterBuilder
import java.time.format.DateTimeParseException

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
class GerritCodeReviewService(override val controller: ControllerInterface, config: GerritCodeReviewConfig):
		Service<GerritCodeReviewConfig>(config) {

	companion object {
		val LOGGER: Logger = LoggerFactory.getLogger(GerritCodeReviewService::class.java)
	}

	override val definition: Module<GerritCodeReviewConfig, ModuleInstanceInterface<GerritCodeReviewConfig>> =
			GerritCodeReviewModule
	private val service
		get() = retrofit?.create(GerritCodeReviewRestService::class.java)
	private val responseType = http
			?.objectMapper
			?.typeFactory
			?.constructCollectionType(MutableList::class.java, GerritCodeReviewQueryResponse::class.java)
	private var lastFound: MutableMap<String?, MutableCollection<StatusItem>> = mutableMapOf()

	private val queryTableSettingsPlugin = TableSettingsPlugin(
			tableName = "queryTable",
			newLabel = "Query name",
			buttonText = "Add query",
			controller = controller,
			config = config,
			createItem = { GerritCodeReviewQueryConfig() },
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
							property = { queryProperty },
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
			comparator = compareBy({ -it.priority.ordinal }, { -it.status.ordinal }, { it.name }),
			actions = listOf { item ->
				item.let { URLEncoder.encode(it.query, Charsets.UTF_8.name()) }
						.let { "/#/q/${it}" }
						.let { path -> config.url.toUriOrNull()?.resolve(path) }
						?.let { uri -> Button("", CommonIcon.LINK.toImageView()) to uri }
						?.also { (btn, uri) -> btn.setOnAction { uri.openInExternalApplication() } }
						?.first
			},
			fieldSizes = arrayOf(150.0))

	override val configurationRows: List<Pair<Node, Node>>
		get() = super.configurationRows + listOf(queryTableSettingsPlugin.rowNewItem)

	override val configurationAddition: List<Parent>
		get() = super.configurationAddition + listOf(queryTableSettingsPlugin.vbox)

	override fun check(updater: (Collection<StatusItem>) -> Unit) {
		lastFound.keys
				.filterNot { key -> config.subConfig.map { it.name }.contains(key) }
				.forEach { lastFound.remove(it) }
		var error: String? = null
		if (config.enabled && config.url.isNotBlank()) try {
			val queries = config.subConfig.mapNotNull { q -> service?.check(q.query)?.let { q to it } }
			val statuses = mutableMapOf<String, StatusItem>()
			for ((query, call) in queries) try {
				val response = call.execute()
				LOGGER.info("${call.request().method()} ${call.request().url()}:" +
						" ${response.code()} ${response.message()}")
				if (response.isSuccessful) {
					val body = response.body() ?: ""
					val startIndex = body.indexOf('[')
					if (startIndex >= 0) {
						val queryAlerts = http?.objectMapper
								?.readValue<List<GerritCodeReviewQueryResponse>>(body.substring(startIndex),
										responseType)
								?.map { entity ->
									statuses[entity.id]
											?.apply {
												priority = maxOf(priority, query.priority)
												status = maxOf(status, query.status)
												if (!configIds.contains(query.configTitle)) configIds.add(query.configTitle)
											}
											?: entity.toStatusItem(query).also { statuses[it.id] = it }
								}
								?.takeIf { it.isNotEmpty() }
						if (queryAlerts == null) lastFound.remove(query.name)
						else lastFound[query.name] = queryAlerts.toMutableList()
					}
				} else {
					LOGGER.warn("${call.request().method()} ${call.request().url()}:" +
							" ${response.code()} ${response.message()}")
					error = "Code: ${response.code()} ${response.message() ?: "Service error"}"
					addErrorStatus("Service error", Status.SERVICE_ERROR, error, query)
				}
			} catch (e: IOException) {
				LOGGER.error(e.message)
				error = e.message ?: "Connection error"
				addErrorStatus("Connection error", Status.CONNECTION_ERROR, e.message, query)
			}
			updater(lastFound.values.flatten())
		} catch (e: IOException) {
			LOGGER.error(e.message)
			error = e.message ?: "Connection error"
			addErrorStatus("Connection error", Status.CONNECTION_ERROR, e.message)
		}
		lastErrorProperty.set(error)
	}

	private fun addErrorStatus(error: String,
							   status: Status,
							   detail: String?,
							   query: GerritCodeReviewQueryConfig? = null) {
		val id = "${config.uuid}-${query?.name ?: ""}-error"
		val cache = lastFound.getOrPut(query?.name, { mutableListOf() })
		cache.add(StatusItem(
				id = id,
				serviceId = config.uuid,
				configIds = listOfNotNull(query?.configTitle).toMutableList(),
				priority = query?.priority ?: config.priority,
				status = status,
				title = "${query?.name?.let { "[${it}] " }}${error}",
				detail = detail,
				isRepeatable = false))
	}

	private fun GerritCodeReviewQueryResponse.toStatusItem(query: GerritCodeReviewQueryConfig) = StatusItem(
			id = "${config.uuid}-${this.changeId}",
			serviceId = config.uuid,
			configIds = mutableListOf(query.configTitle),
			priority = query.priority,
			status = query.status,
			title = subject ?: changeId ?: id ?: "",
			//detail = item.subject,
			labels = listOf(
					"Project" to this.project,
					"Branch" to this.branch,
					"Topic" to this.topic,
					"Status" to this.status,
					"Mergeable" to this.mergeable?.let { if (it) "Yes" else "No" },
					"Submit" to this.submitType?.toLowerCase())
					.mapNotNull { (k, v) -> v?.let { k to v } }
					.toMap()
					.toMutableMap()
					.also { m -> m.putAll(this.labels.flatMap { (k, v) -> v.keys.map { k to it } }.toMap()) },
			link = "${config.url}/#/q/${this.changeId}",
			isRepeatable = false,
			startedAt = try {
				DateTimeFormatterBuilder()
						.appendPattern("yyyy-MM-dd HH:mm:ss.000000000")
						.toFormatter()
						.withZone(ZoneId.of("UTC"))
						.parse(this.updated)
						.let { Instant.from(it) }
			} catch (e: DateTimeParseException) {
				LOGGER.error(e.message, e)
				Instant.now()
			})
}