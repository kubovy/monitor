package com.poterion.monitor.sensors.storyboard.control

import com.poterion.monitor.api.controllers.ControllerInterface
import com.poterion.monitor.api.controllers.ModuleInstanceInterface
import com.poterion.monitor.api.controllers.Service
import com.poterion.monitor.api.lib.toIcon
import com.poterion.monitor.api.lib.toImageView
import com.poterion.monitor.api.modules.Module
import com.poterion.monitor.api.CommonIcon
import com.poterion.monitor.api.utils.cell
import com.poterion.monitor.api.utils.factory
import com.poterion.monitor.api.utils.toUriOrNull
import com.poterion.monitor.data.Priority
import com.poterion.monitor.data.Status
import com.poterion.monitor.data.StatusItem
import com.poterion.monitor.sensors.storyboard.StoryboardModule
import com.poterion.monitor.sensors.storyboard.data.Story
import com.poterion.monitor.sensors.storyboard.data.StoryboardConfig
import com.poterion.monitor.sensors.storyboard.data.StoryboardProjectConfig
import com.poterion.monitor.sensors.storyboard.data.Task
import javafx.collections.FXCollections
import javafx.scene.Parent
import javafx.scene.control.*
import javafx.scene.input.KeyCode
import javafx.scene.layout.HBox
import javafx.scene.layout.Region
import javafx.scene.layout.VBox
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.awt.Desktop
import java.io.IOException
import java.net.URLEncoder
import java.time.Instant
import java.time.format.DateTimeFormatterBuilder
import java.time.format.DateTimeParseException
import java.util.concurrent.Executors

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
class StoryboardService(override val controller: ControllerInterface, config: StoryboardConfig) : Service<StoryboardConfig>(config) {

	companion object {
		val LOGGER: Logger = LoggerFactory.getLogger(StoryboardService::class.java)
	}

	override val definition: Module<StoryboardConfig, ModuleInstanceInterface<StoryboardConfig>> = StoryboardModule
	private val service
		get() = retrofit?.create(StoryboardRestService::class.java)
	private var lastFound: MutableMap<String, MutableCollection<StatusItem>> = mutableMapOf()
	private val executor = Executors.newSingleThreadExecutor()

	override val configurationAddition: List<Parent>?
		get() = listOf(
				VBox(
						HBox(
								Label("Project").apply { maxHeight = Double.MAX_VALUE }, textNewProjectName,
								Button("Add").apply { setOnAction { addProject() } }),
						tableProjects).apply {
					VBox.setVgrow(this, javafx.scene.layout.Priority.ALWAYS)
				})

	private val textNewProjectName = TextField("").apply {
		HBox.setHgrow(this, javafx.scene.layout.Priority.ALWAYS)
		setOnKeyReleased { event -> if (event.code == KeyCode.ENTER) addProject() }
	}

	private val tableProjects = TableView<StoryboardProjectConfig>().apply {
		minWidth = Region.USE_COMPUTED_SIZE
		minHeight = Region.USE_COMPUTED_SIZE
		prefWidth = Region.USE_COMPUTED_SIZE
		prefHeight = Region.USE_COMPUTED_SIZE
		maxWidth = Double.MAX_VALUE
		maxHeight = Double.MAX_VALUE
		VBox.setVgrow(this, javafx.scene.layout.Priority.ALWAYS)
		setOnKeyReleased { event ->
			when (event.code) {
				KeyCode.HELP, // MacOS mapping of INSERT key
				KeyCode.INSERT -> textNewProjectName.requestFocus()
				KeyCode.DELETE -> selectionModel.selectedItem?.also { removeQuery(it) }
				else -> {
					// Nothing to do
				}
			}
		}
	}

	private val tableColumnProjectName = TableColumn<StoryboardProjectConfig, String>("Name").apply {
		isSortable = false
		minWidth = 150.0
		prefWidth = Region.USE_COMPUTED_SIZE
		maxWidth = Double.MAX_VALUE
		cell("name") { item, value, empty ->
			graphic = TextField(value).takeUnless { empty }?.apply {
				text = value
				focusedProperty().addListener { _, _, value ->
					if (!value) {
						item?.name = text
						sortLabelTable()
						controller.saveConfig()
					}
				}
			}
		}
	}

	private val tableColumnProjectPriority = TableColumn<StoryboardProjectConfig, Priority>("Priority").apply {
		isSortable = false
		minWidth = 150.0
		prefWidth = Region.USE_COMPUTED_SIZE
		maxWidth = Region.USE_PREF_SIZE
		cell("priority") { item, value, empty ->
			graphic = ComboBox<Priority>(FXCollections.observableList(Priority.values().toList())).takeUnless { empty }
					?.apply {
						factory { item, empty ->
							text = item?.takeUnless { empty }?.name
							graphic = item?.takeUnless { empty }?.toIcon()?.toImageView()
						}
						selectionModel.select(value)
						valueProperty().addListener { _, _, priority ->
							if (priority != null) {
								item?.priority = priority
								sortLabelTable()
								controller.saveConfig()
							}
						}
					}
		}
	}

	private val tableColumnProjectAction = TableColumn<StoryboardProjectConfig, Status>("").apply {
		isSortable = false
		minWidth = 96.0
		prefWidth = Region.USE_COMPUTED_SIZE
		maxWidth = Region.USE_PREF_SIZE
		cell { item, _, empty ->
			graphic = if (!empty && item != null) HBox(
					Button("", CommonIcon.LINK.toImageView()).apply {
						setOnAction {
							val url = item
									.let { URLEncoder.encode(it.name, Charsets.UTF_8.name()) }
									.let { "${config.url}/#!/project/${it}" }
									.replace("//", "/")
									.toUriOrNull()
							if (url != null) Desktop.getDesktop().browse(url)
						}
					},
					Button("", CommonIcon.TRASH.toImageView()).apply { setOnAction { removeQuery(item) } })
			else null
		}
	}

	init {
		tableProjects.items.addAll(config.projects)
		tableProjects.columns.addAll(tableColumnProjectPriority, tableColumnProjectName, tableColumnProjectAction)
		sortLabelTable()
	}

	override fun check(updater: (Collection<StatusItem>) -> Unit) {
		lastFound.keys
				.filterNot { key -> config.projects.map { it.name }.contains(key) }
				.forEach { lastFound.remove(it) }
		if (config.enabled && config.url.isNotEmpty()) executor.submit {
			for (project in config.projects) try {
				val alerts = mutableListOf<StatusItem>()
				val projectResponse = service
						?.projects(project.name)
						?.execute()

				var error: String? = null
				if (projectResponse?.isSuccessful == true) {
					val projectId = projectResponse.body()?.find { it.name == project.name }?.id
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

	private fun addProject() {
		textNewProjectName.text.takeIf { it.isNotEmpty() && it.isNotBlank() }?.also { name ->
			val newProject = StoryboardProjectConfig(name = name, priority = config.priority)
			tableProjects.items.apply { add(newProject) }
			//.sortWith(compareBy({-it.priority.ordinal}, { it.name }, {it.value}))
			config.projects.add(newProject)
			controller.saveConfig()
		}
		textNewProjectName.text = ""
	}

	private fun removeQuery(projectConfig: StoryboardProjectConfig) {
		Alert(Alert.AlertType.CONFIRMATION).apply {
			title = "Delete confirmation"
			headerText = "Delete confirmation"
			contentText = "Do you really want to delete query ${projectConfig.name}?"
			buttonTypes.setAll(ButtonType.YES, ButtonType.NO)
		}.showAndWait().ifPresent {
			it.takeIf { it == ButtonType.YES }?.also {
				tableProjects.items.remove(projectConfig)
				config.projects.remove(projectConfig)
				controller.saveConfig()
			}
		}
	}

	private fun sortLabelTable() {
		tableProjects.items.sortWith(compareBy(
				{ -it.priority.ordinal },
				{ it.name }))
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