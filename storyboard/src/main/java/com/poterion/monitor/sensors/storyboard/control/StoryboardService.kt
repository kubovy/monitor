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
package com.poterion.monitor.sensors.storyboard.control

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
import com.poterion.monitor.sensors.storyboard.StoryboardModule
import com.poterion.monitor.sensors.storyboard.data.Story
import com.poterion.monitor.sensors.storyboard.data.StoryboardConfig
import com.poterion.monitor.sensors.storyboard.data.StoryboardProjectConfig
import com.poterion.monitor.sensors.storyboard.data.Task
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
import java.time.format.DateTimeFormatterBuilder
import java.time.format.DateTimeParseException

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
class StoryboardService(override val controller: ControllerInterface, config: StoryboardConfig) :
		Service<StoryboardProjectConfig, StoryboardConfig>(config) {

	companion object {
		val LOGGER: Logger = LoggerFactory.getLogger(StoryboardService::class.java)
	}

	override val definition: Module<StoryboardConfig, ModuleInstanceInterface<StoryboardConfig>> = StoryboardModule
	private val service
		get() = retrofit?.create(StoryboardRestService::class.java)
	private var lastFound: MutableMap<String, MutableCollection<StatusItem>> = mutableMapOf()

	private val projectTableSettingsPlugin = TableSettingsPlugin(
			tableName = "projectTable",
			newLabel = "Project name",
			buttonText = "Add project",
			controller = controller,
			config = config,
			createItem = { StoryboardProjectConfig() },
			items = config.subConfig,
			displayName = { name },
			columnDefinitions = listOf(
					TableSettingsPlugin.ColumnDefinition(
							name = "Name",
							property = { nameProperty },
							initialValue = "",
							isEditable = true),
					TableSettingsPlugin.ColumnDefinition(
							name = "Priority",
							property = { priorityProperty },
							initialValue = Priority.NONE,
							isEditable = true,
							icon = { toIcon() },
							options = Priority.values().toObservableList())),
			comparator = compareBy({ -it.priority.ordinal }, { it.name }),
			actions = listOf { item ->
				Button("", CommonIcon.LINK.toImageView()).apply { setOnAction { gotoSubConfig(item) } }
			},
			fieldSizes = arrayOf(200.0))

	override val configurationRows: List<Pair<Node, Node>>
		get() = super.configurationRows + listOf(projectTableSettingsPlugin.rowNewItem)

	override val configurationAddition: List<Parent>
		get() = super.configurationAddition + listOf(projectTableSettingsPlugin.vbox)

	override fun gotoSubConfig(subConfig: StoryboardProjectConfig) {
		subConfig.let { URLEncoder.encode(it.name, Charsets.UTF_8.name()) }
				.let { "/#!/project/${it}" }
				.let { path -> config.url.toUriOrNull()?.resolve(path) }
				?.openInExternalApplication()
	}

	override fun doCheck(): List<StatusItem> {
		lastFound.keys
				.filterNot { key -> config.subConfig.map { it.name }.contains(key) }
				.forEach { lastFound.remove(it) }
		var error: String? = null
		for (project in config.subConfig) try {
			val alerts = mutableListOf<StatusItem>()
			val call = service?.projects(project.name)

			checkForInterruptions()
			val response = call?.execute()
			checkForInterruptions()

			LOGGER.info("${call?.request()?.method()} ${call?.request()?.url()}:" +
					" ${response?.code()} ${response?.message()}")

			if (response?.isSuccessful == true) {
				val projectId = response.body()?.find { it.name == project.name }?.id
				if (projectId != null) {

					checkForInterruptions()
					val storiesResponse = service?.stories(projectId)?.execute()
					checkForInterruptions()

					if (storiesResponse?.isSuccessful == true) {
						val stories = storiesResponse.body() ?: emptyList()
						for (story in stories) if (story.id != null) {
							alerts.add(story.toStatusItem(project))

							checkForInterruptions()
							val taskAlerts = service
									?.tasks(story.id!!)
									?.execute()
									?.takeIf { it.isSuccessful }
									?.body()
									?.map { task -> task.toStatusItem(project, story) }
									?: emptyList()
							checkForInterruptions()

							alerts.addAll(taskAlerts)
						}
					} else error = "Failed retrieving ${project.name} stories"
				}
				lastFound[project.name] = alerts
			} else {
				LOGGER.warn("${call?.request()?.method()} ${call?.request()?.url()}:" +
						" ${response?.code()} ${response?.message()}")
				error = response
						?.let {
							"Code: ${response.code()} ${response.message()}"
						}
						?: "Failed retrieving ${project.name}"
			}

			if (error != null) addErrorStatus(project, "Service error", Status.SERVICE_ERROR, error)
		} catch (e: IOException) {
			LOGGER.error(e.message)
			error = e.message ?: "Connection error"
			addErrorStatus(project, "Connection error", Status.CONNECTION_ERROR, e.message)
		}
		lastErrorProperty.set(error)
		return lastFound.values.flatten()
	}

	private fun addErrorStatus(project: StoryboardProjectConfig,
							   error: String,
							   status: Status,
							   detail: String?) {
		val id = "${config.uuid}-${project.name}-error"
		val cache = lastFound.getOrPut(project.name, { mutableListOf() })
		cache.add(StatusItem(
				id = id,
				serviceId = config.uuid,
				configIds = mutableListOf(project.configTitle),
				priority = project.priority,
				status = status,
				title = "[${project.name}] ${error}",
				detail = detail,
				isRepeatable = false))
	}

	private fun Story.toStatusItem(project: StoryboardProjectConfig) = StatusItem(
			id = "${config.uuid}-${project.name}-${this.id}",
			serviceId = config.uuid,
			configIds = mutableListOf(project.configTitle),
			priority = when (this.status) {
				"invalid" -> Priority.NONE
				else -> project.priority
			},
			status = when (this.status) {
				"todo" -> when {
					//this.isBug -> Status.ERROR
					this.tags.map { it.toLowerCase() }.contains("bug") -> Status.ERROR
					else -> Status.NOTIFICATION
				}
				"active" -> when {
					//this.isBug -> Status.ERROR
					this.tags.map { it.toLowerCase() }.contains("bug") -> Status.ERROR
					else -> Status.INFO
				}
				"invalid" -> Status.NONE
				"inprogress" -> Status.INFO
				"review" -> Status.OK
				"merged" -> Status.OFF
				else -> Status.UNKNOWN
			},
			title = "${this.title}",
			group = null,
			detail = this.description,
			labels = (listOf(
					"Status" to this.status,
					//"Bug" to "".takeIf { this.isBug },
					"Private" to "".takeIf { this.isPrivate })
					+ this.tags.map { it to "" }
					+ listOf("Todo" to "todo",
					"InProgress" to "inprogress",
					"Invalid" to "invalid",
					"Review" to "review",
					"Merged" to "merged")
					.map { (label, key) -> label to this.taskStatuses.find { it.key == key } }
					.map { (label, st) -> label to st?.count?.takeIf { it > 0 }?.toString() })
					.mapNotNull { (k, v) -> v?.let { k to it } }
					.toMap(),
			link = "${config.url}/#!/story/${this.id}",
			isRepeatable = false,
			startedAt = try {
				DateTimeFormatterBuilder()
						.appendPattern("yyyy-MM-dd'T'HH:mm:ssZZZZZ")
						.toFormatter()
						//.withZone(ZoneId.of("UTC"))
						.parse(this.updatedAt ?: this.createdAt)
						.let { Instant.from(it) }
			} catch (e: DateTimeParseException) {
				LOGGER.error(e.message, e)
				Instant.now()
			})

	private fun Task.toStatusItem(project: StoryboardProjectConfig, story: Story) = StatusItem(
			id = "${config.uuid}-${project.name}-${story.id}-${this.id}",
			serviceId = config.uuid,
			configIds = mutableListOf(project.configTitle),
			parentId = "${config.uuid}-${project.name}-${story.id}",
			parentRequired = true,
			priority = when (this.status) {
				"invalid" -> Priority.NONE
				else -> project.priority
			},
			status = when (this.status) {
				"todo" -> Status.NOTIFICATION
				"active" -> Status.INFO
				"invalid" -> Status.NONE
				"inprogress" -> Status.INFO
				"review" -> Status.OK
				"merged" -> Status.OFF
				else -> Status.UNKNOWN
			},
			title = this.title ?: "",
			group = null,
			detail = null,
			labels = listOf(
					"Status" to this.status,
					"Assigned" to "".takeIf { this.assigneeId != null })
					.mapNotNull { (k, v) -> v?.let { k to it } }
					.toMap(),
			link = this.link ?: "${config.url}/#!/task/${this.id}",
			isRepeatable = false,
			startedAt = try {
				DateTimeFormatterBuilder()
						.appendPattern("yyyy-MM-dd'T'HH:mm:ssZZZZZ")
						.toFormatter()
						//.withZone(ZoneId.of("UTC"))
						.parse(this.updatedAt ?: story.updatedAt ?: this.createdAt ?: story.createdAt)
						.let { Instant.from(it) }
			} catch (e: DateTimeParseException) {
				LOGGER.error(e.message, e)
				Instant.now()
			})
}