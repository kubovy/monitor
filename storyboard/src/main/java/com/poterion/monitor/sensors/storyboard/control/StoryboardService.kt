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
class StoryboardService(override val controller: ControllerInterface, config: StoryboardConfig):
		Service<StoryboardConfig>(config) {

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
			items = config.projects,
			displayName = { name },
			columnDefinitions = listOf(
					TableSettingsPlugin.ColumnDefinition(
							name = "Name",
							getter = { name },
							setter = { name = it },
							initialValue = "",
							isEditable = true),
					TableSettingsPlugin.ColumnDefinition(
							name = "Priority",
							getter = { priority },
							setter = { priority = it },
							initialValue = Priority.NONE,
							isEditable = true,
							icon = { toIcon() },
							options = { Priority.values().toList() })),
			comparator = compareBy({ -it.priority.ordinal }, { it.name }),
			actions = listOf { item ->
				item.let { URLEncoder.encode(it.name, Charsets.UTF_8.name()) }
						.let { "/#!/project/${it}" }
						.let { path -> config.url.toUriOrNull()?.resolve(path) }
						?.let { uri -> Button("", CommonIcon.LINK.toImageView()) to uri }
						?.also { (btn, uri) -> btn.setOnAction { uri.openInExternalApplication() } }
						?.first
			},
			fieldSizes = arrayOf(200.0))

	override val configurationRows: List<Pair<Node, Node>>
		get() = super.configurationRows + listOf(projectTableSettingsPlugin.rowNewItem)

	override val configurationAddition: List<Parent>
		get() = super.configurationAddition + listOf(projectTableSettingsPlugin.vbox)

	override fun check(updater: (Collection<StatusItem>) -> Unit) {
		lastFound.keys
				.filterNot { key -> config.projects.map { it.name }.contains(key) }
				.forEach { lastFound.remove(it) }
		if (config.enabled && config.url.isNotEmpty()) {
			for (project in config.projects) try {
				val alerts = mutableListOf<StatusItem>()
				val call = service?.projects(project.name)
				val response = call?.execute()
				LOGGER.info("${call?.request()?.method()} ${call?.request()?.url()}:" +
						" ${response?.code()} ${response?.message()}")

				var error: String? = null
				if (response?.isSuccessful == true) {
					val projectId = response.body()?.find { it.name == project.name }?.id
					if (projectId != null) {
						val storiesResponse = service?.stories(projectId)?.execute()
						if (storiesResponse?.isSuccessful == true) {
							val stories = storiesResponse.body() ?: emptyList()
							for (story in stories) if (story.id != null) {
								alerts.add(story.toStatusItem(project))
								val taskAlerts = service
										?.tasks(story.id!!)
										?.execute()
										?.takeIf { it.isSuccessful }
										?.body()
										?.map { task -> task.toStatusItem(project, story) }
										?: emptyList()
								alerts.addAll(taskAlerts)
							}
						} else error = "Failed retrieving ${project.name} stories"
					}
					lastFound[project.name] = alerts
				} else error = "Failed retrieving ${project.name} project ID"

				if (error != null) addErrorStatus(project, "Service error: ${error}", Status.SERVICE_ERROR)
			} catch (e: IOException) {
				LOGGER.error(e.message, e)
				addErrorStatus(project, "Connection error", Status.CONNECTION_ERROR)
			}
			updater(lastFound.values.flatten())
		}
	}

	private fun addErrorStatus(project: StoryboardProjectConfig,
							   error: String,
							   status: Status) {
		val id = "${config.uuid}-${project.name}-error"
		val cache = lastFound.getOrPut(project.name, { mutableListOf() })
		cache.add(StatusItem(
				id = id,
				serviceId = config.uuid,
				priority = project.priority,
				status = status,
				title = "[${project.name}] ${error}",
				isRepeatable = false))
	}

	private fun Story.toStatusItem(project: StoryboardProjectConfig) = StatusItem(
			id = "${config.uuid}-${project.name}-${this.id}",
			serviceId = config.uuid,
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