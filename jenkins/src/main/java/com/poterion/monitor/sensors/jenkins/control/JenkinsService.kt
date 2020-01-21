package com.poterion.monitor.sensors.jenkins.control

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
import com.poterion.monitor.sensors.jenkins.JenkinsModule
import com.poterion.monitor.sensors.jenkins.data.JenkinsConfig
import com.poterion.monitor.sensors.jenkins.data.JenkinsJobConfig
import com.poterion.monitor.sensors.jenkins.data.JenkinsJobResponse
import com.poterion.monitor.sensors.jenkins.data.JenkinsResponse
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
class JenkinsService(override val controller: ControllerInterface, config: JenkinsConfig) : Service<JenkinsConfig>(config) {
	companion object {
		val LOGGER: Logger = LoggerFactory.getLogger(JenkinsService::class.java)
	}

	override val definition: Module<JenkinsConfig, ModuleInstanceInterface<JenkinsConfig>> = JenkinsModule
	private val service
		get() = retrofit?.create(JenkinsRestService::class.java)
	private val jobs = config.jobs.map { it.name to it }.toMap()
	private var lastFoundJobNames: Collection<String> = jobs.keys
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
		get() = listOf(jobTable, HBox(newJobName, Button("Add").apply {
			setOnAction { addJob() }
		}))

	private val newJobName = TextField("").apply {
		HBox.setHgrow(this, javafx.scene.layout.Priority.ALWAYS)
		setOnKeyReleased { event -> if (event.code == KeyCode.ENTER) addJob() }
	}

	private val jobTable = TableView<JenkinsJobConfig>().apply {
		VBox.setVgrow(this, javafx.scene.layout.Priority.ALWAYS)
		setOnKeyReleased { event ->
			when (event.code) {
				KeyCode.HELP, // MacOS mapping of INSERT key
				KeyCode.INSERT -> newJobName.requestFocus()
				KeyCode.DELETE -> selectionModel.selectedItem?.also { removeJob(it) }
				else -> {
				}
			}
		}
	}

	private val jobTableNameColumn = TableColumn<JenkinsJobConfig, String>("Job Name").apply {
		sortType = TableColumn.SortType.ASCENDING
		minWidth = 400.0
		prefWidth = Region.USE_COMPUTED_SIZE
		maxWidth = Double.MAX_VALUE
	}

	private val jobTablePriorityColumn = TableColumn<JenkinsJobConfig, Priority>("Priority").apply {
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
		jobTableNameColumn.apply {
			cellValueFactory = PropertyValueFactory<JenkinsJobConfig, String>("name")
		}
		jobTablePriorityColumn.apply {
			cellValueFactory = PropertyValueFactory<JenkinsJobConfig, Priority>("priority")
		}
		jobTable.columns.addAll(jobTableNameColumn, jobTablePriorityColumn)
		jobTable.items.addAll(config.jobs.sortedBy { it.name })
	}


	override fun check(updater: (Collection<StatusItem>) -> Unit) {
		try {
			service?.check()?.enqueue(object : Callback<JenkinsResponse> {
				override fun onResponse(call: Call<JenkinsResponse>?, response: Response<JenkinsResponse>?) {
					LOGGER.info("${call?.request()?.method()} ${call?.request()?.url()}: ${response?.code()} ${response?.message()}")

					if (response?.isSuccessful == true) {
						val foundJobs = response.body()
								?.jobs
								?.filter { job -> config.filter?.let { job.name.matches(it.toRegex()) } ?: true }

						lastFoundJobNames = foundJobs?.map { it.name } ?: jobs.keys

						foundJobs
								?.map {
									StatusItem(
											id = "${config.uuid}-${it.name}",
											serviceId = config.uuid,
											priority = it.priority,
											status = it.severity,
											title = it.name,
											link = it.url,
											isRepeatable = false)
								}
								?.also(updater)
					} else {
						lastFoundJobNames
								.mapNotNull { jobs[it] }
								.map {
									StatusItem(
											id = "${config.uuid}-${it.name}",
											serviceId = config.uuid,
											priority = it.priority,
											status = Status.SERVICE_ERROR,
											title = it.name,
											link = config.url,
											isRepeatable = false)
								}
								.also(updater)
					}
				}

				override fun onFailure(call: Call<JenkinsResponse>?, response: Throwable?) {
					call?.request()?.also { LOGGER.warn("${it.method()} ${it.url()}: ${response?.message}", response) }
							?: LOGGER.warn(response?.message, response)
					lastFoundJobNames
							.mapNotNull { jobs[it] }
							.map {
								StatusItem(
										id = "${config.uuid}-${it.name}",
										serviceId = config.uuid,
										priority = it.priority,
										status = Status.CONNECTION_ERROR,
										title = it.name,
										link = config.url,
										isRepeatable = false)
							}
							.also(updater)
				}
			})
		} catch (e: IOException) {
			LOGGER.error(e.message, e)
			lastFoundJobNames
					.mapNotNull { jobs[it] }
					.map {
						StatusItem(
								id = "${config.uuid}-${it.name}",
								serviceId = config.uuid,
								priority = it.priority,
								status = Status.CONNECTION_ERROR,
								title = it.name,
								link = config.url,
								isRepeatable = false)
					}
					.also(updater)
		}
	}

	private val JenkinsJobResponse.priority
		get() = jobs[name]?.priority ?: config.priority

	private val JenkinsJobResponse.severity
		get() = (color?.toLowerCase() ?: "").let {
			when {
				it.startsWith("disabled") -> Status.OFF
				it.startsWith("notbuild") -> Status.NONE
				it.startsWith("aborted") -> Status.NOTIFICATION
				it.startsWith("blue") -> Status.OK
				it.startsWith("green") -> Status.OK
				it.startsWith("yellow") -> Status.WARNING
				it.startsWith("red") -> Status.ERROR
				else -> Status.UNKNOWN
			}
		}

	private fun addJob() {
		newJobName.text.takeIf { it.isNotEmpty() && it.isNotBlank() }?.also { jobName ->
			val newJob = JenkinsJobConfig(jobName, config.priority)
			val jobList = jobTable.items.toMutableList()
					.apply { add(newJob) }
					.sortedBy { it.name }
			jobTable.items.clear()
			jobTable.items.addAll(jobList.sortedBy { it.name })
			config.jobs.add(newJob)
			controller.saveConfig()
		}
		newJobName.text = ""
	}

	private fun removeJob(jenkinsJobConfig: JenkinsJobConfig) {
		Alert(Alert.AlertType.CONFIRMATION).apply {
			title = "Delete confirmation"
			headerText = "Delete confirmation"
			contentText = "Do you really want to delete \"${jenkinsJobConfig.name}\"?"
			buttonTypes.setAll(ButtonType.YES, ButtonType.NO)
		}.showAndWait().ifPresent {
			it.takeIf { it == ButtonType.YES }?.also {
				//val selected = jobTable.selectionModel.selectedItem
				jobTable.items.remove(jenkinsJobConfig)
				config.jobs.remove(jenkinsJobConfig)
				controller.saveConfig()
			}
		}
	}
}