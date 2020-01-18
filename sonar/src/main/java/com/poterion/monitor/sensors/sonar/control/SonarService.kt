package com.poterion.monitor.sensors.sonar.control

import com.poterion.monitor.api.controllers.ControllerInterface
import com.poterion.monitor.api.controllers.ModuleInstanceInterface
import com.poterion.monitor.api.controllers.Service
import com.poterion.monitor.api.lib.toIcon
import com.poterion.monitor.api.lib.toImageView
import com.poterion.monitor.api.modules.Module
import com.poterion.monitor.api.utils.cell
import com.poterion.monitor.api.utils.factory
import com.poterion.monitor.data.Priority
import com.poterion.monitor.data.Status
import com.poterion.monitor.data.StatusItem
import com.poterion.monitor.sensors.sonar.SonarModule
import com.poterion.monitor.sensors.sonar.data.SonarConfig
import com.poterion.monitor.sensors.sonar.data.SonarProjectConfig
import com.poterion.monitor.sensors.sonar.data.SonarProjectResponse
import javafx.collections.FXCollections
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.control.*
import javafx.scene.control.cell.PropertyValueFactory
import javafx.scene.input.KeyCode
import javafx.scene.layout.HBox
import javafx.scene.layout.Region
import javafx.scene.layout.VBox
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
class SonarService(override val controller: ControllerInterface, config: SonarConfig) : Service<SonarConfig>(config) {
	companion object {
		val LOGGER: Logger = LoggerFactory.getLogger(SonarService::class.java)
	}

	override val definition: Module<SonarConfig, ModuleInstanceInterface<SonarConfig>> = SonarModule
	private val service
		get() = retrofit?.create(SonarRestService::class.java)
	private val projects = config.projects.map { it.name to it }.toMap()
	private var lastFoundProjectNames: Collection<String> = projects.keys

	override val configurationRows: List<Pair<Node, Node>>?
		get() = listOf(
				Label("Filter").apply {
					maxWidth = Double.MAX_VALUE
					maxHeight = Double.MAX_VALUE
					alignment = Pos.CENTER_RIGHT
				} to TextField(config.filter).apply {
					textProperty().addListener { _, _, filter -> config.filter = filter }
					focusedProperty().addListener { _, _, hasFocus -> if (!hasFocus) controller.saveConfig() }
				})
	override val configurationAddition: List<Parent>?
		get() = listOf(projectTable, HBox(newProjectId, newProjectName, Button("Add").apply {
			setOnAction { addJob() }
		}))

	private val newProjectId = TextField("").apply {
		HBox.setHgrow(this, javafx.scene.layout.Priority.SOMETIMES)
		setOnKeyReleased { event -> if (event.code == KeyCode.ENTER) addJob() }
	}

	private val newProjectName = TextField("").apply {
		HBox.setHgrow(this, javafx.scene.layout.Priority.ALWAYS)
		setOnKeyReleased { event -> if (event.code == KeyCode.ENTER) addJob() }
	}

	private val projectTable = TableView<SonarProjectConfig>().apply {
		VBox.setVgrow(this, javafx.scene.layout.Priority.ALWAYS)
		setOnKeyReleased { event ->
			when (event.code) {
				KeyCode.HELP, // MacOS mapping of INSERT key
				KeyCode.INSERT -> newProjectName.requestFocus()
				KeyCode.DELETE -> selectionModel.selectedItem?.also { remoteJob(it) }
				else -> {
				}
			}
		}
	}

	private val projectTableIdColumn = TableColumn<SonarProjectConfig, Int>("ID").apply {
		minWidth = 75.0
		prefWidth = Region.USE_COMPUTED_SIZE
		maxWidth = Double.MAX_VALUE
	}

	private val projectTableNameColumn = TableColumn<SonarProjectConfig, String>("Project Name").apply {
		sortType = TableColumn.SortType.ASCENDING
		minWidth = 400.0
		prefWidth = Region.USE_COMPUTED_SIZE
		maxWidth = Double.MAX_VALUE
	}

	private val projectTablePriorityColumn = TableColumn<SonarProjectConfig, Priority>("Priority").apply {
		minWidth = 100.0
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
						valueProperty().bindBidirectional(itemProperty())
						valueProperty().addListener { _, _, priority ->
							item?.priority = priority
							controller.saveConfig()
						}
					}
		}
	}

	init {
		projectTableIdColumn.apply {
			cellValueFactory = PropertyValueFactory<SonarProjectConfig, Int>("id")
		}
		projectTableNameColumn.apply {
			cellValueFactory = PropertyValueFactory<SonarProjectConfig, String>("name")
		}
		projectTablePriorityColumn.apply {
			cellValueFactory = PropertyValueFactory<SonarProjectConfig, Priority>("priority")
		}
		projectTable.columns.addAll(projectTableIdColumn, projectTableNameColumn, projectTablePriorityColumn)
		projectTable.items.addAll(config.projects.sortedBy { it.name })
	}

	override fun check(updater: (Collection<StatusItem>) -> Unit) {
		try {
			service?.check()?.enqueue(object : Callback<Collection<SonarProjectResponse>> {
				override fun onResponse(call: Call<Collection<SonarProjectResponse>>?, response: Response<Collection<SonarProjectResponse>>?) {
					LOGGER.info("${call?.request()?.method()} ${call?.request()?.url()}: ${response?.code()} ${response?.message()}")

					if (response?.isSuccessful == true) {
						val foundProjects = response.body()
								?.filter { job -> config.filter?.let { job.name.matches(it.toRegex()) } ?: true }

						lastFoundProjectNames = foundProjects?.map { it.name } ?: projects.keys

						foundProjects
								?.map { StatusItem(config.name, it.priority, it.severity, it.name, link = "${config.url}dashboard/index/${it.id}") }
								?.also(updater)
					} else {
						lastFoundProjectNames
								.mapNotNull { projects[it] }
								.map { StatusItem(config.name, it.priority, Status.SERVICE_ERROR, it.name, link = config.url) }
								.also(updater)
					}
				}

				override fun onFailure(call: Call<Collection<SonarProjectResponse>>?, response: Throwable?) {
					call?.request()?.also { LOGGER.warn("${it.method()} ${it.url()}: ${response?.message}", response) }
							?: LOGGER.warn(response?.message, response)
					lastFoundProjectNames
							.mapNotNull { projects[it] }
							.map { StatusItem(config.name, it.priority, Status.CONNECTION_ERROR, it.name, link = config.url) }
							.also(updater)
				}
			})
		} catch (e: IOException) {
			LOGGER.error(e.message, e)
			lastFoundProjectNames
					.mapNotNull { projects[it] }
					.map { StatusItem(config.name, it.priority, Status.CONNECTION_ERROR, it.name, link = config.url) }
					.also(updater)
		}
	}

	private val SonarProjectResponse.priority
		get() = projects[name]?.priority ?: config.priority

	private val SonarProjectResponse.severity
		get() = (msr.firstOrNull { it.key == "alert_status" }?.data?.toUpperCase() ?: "UNKNOWN").let {
			when (it) {
				"OK" -> Status.OK
				"WARNING" -> Status.WARNING
				"ERROR" -> Status.ERROR
				else -> Status.UNKNOWN
			}
		}

	private fun addJob() {
		newProjectId.text.toIntOrNull()?.also { projectId ->
			newProjectName.text.takeIf { it.isNotEmpty() && it.isNotBlank() }?.also { projectName ->
				val newJob = SonarProjectConfig(projectId, projectName, config.priority)
				val jobList = projectTable.items.toMutableList()
						.apply { add(newJob) }
						.sortedBy { it.name }
				projectTable.items.clear()
				projectTable.items.addAll(jobList.sortedBy { it.name })
				config.projects.add(newJob)
				controller.saveConfig()

				newProjectId.text = ""
				newProjectName.text = ""
			}
		}

	}

	private fun remoteJob(jenkinsJobConfig: SonarProjectConfig) {
		Alert(Alert.AlertType.CONFIRMATION).apply {
			title = "Delete confirmation"
			headerText = "Delete confirmation"
			contentText = "Do you really want to delete \"${jenkinsJobConfig.name}\"?"
			buttonTypes.setAll(ButtonType.YES, ButtonType.NO)
		}.showAndWait().ifPresent {
			it.takeIf { it == ButtonType.YES }?.also {
				//val selected = jobTable.selectionModel.selectedItem
				projectTable.items.remove(jenkinsJobConfig)
				config.projects.remove(jenkinsJobConfig)
				controller.saveConfig()
			}
		}
	}
}