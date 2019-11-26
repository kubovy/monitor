package com.poterion.monitor.sensors.alertmanager.control

import com.poterion.monitor.api.controllers.ControllerInterface
import com.poterion.monitor.api.controllers.ModuleInstanceInterface
import com.poterion.monitor.api.controllers.Service
import com.poterion.monitor.api.modules.Module
import com.poterion.monitor.data.Priority
import com.poterion.monitor.data.StatusItem
import com.poterion.monitor.sensors.alertmanager.AlertManagerModule
import com.poterion.monitor.sensors.alertmanager.data.AlertManagerConfig
import com.poterion.monitor.sensors.alertmanager.data.AlertManagerLabelConfig
import com.poterion.monitor.sensors.alertmanager.data.AlertManagerResponse
import javafx.beans.binding.Bindings
import javafx.collections.FXCollections
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
class AlertManagerService(override val controller: ControllerInterface, config: AlertManagerConfig) : Service<AlertManagerConfig>(config) {
	companion object {
		val LOGGER: Logger = LoggerFactory.getLogger(AlertManagerService::class.java)
	}

	override val definition: Module<AlertManagerConfig, ModuleInstanceInterface<AlertManagerConfig>> = AlertManagerModule
	private val service
		get() = retrofit?.create(AlertManagerRestService::class.java)

//	private val labels = config.labels.map { it.label to it }.toMap()

//	private var lastFoundJobNames: Collection<String> = labels.keys

//	override val configurationRows: List<Pair<Node, Node>>?
//		get() = listOf(Label("Filter") to TextField(config.filter).apply {
//			textProperty().addListener { _, _, filter -> config.filter = filter }
//			focusedProperty().addListener { _, _, hasFocus -> if (!hasFocus) controller.saveConfig() }
//		})

	override val configurationAddition: List<Parent>?
		get() = listOf(labelTable, HBox(newLabelName, Button("Add").apply {
			setOnAction { addLabel() }
		}))

	private val newLabelName = TextField("").apply {
		HBox.setHgrow(this, javafx.scene.layout.Priority.ALWAYS)
		setOnKeyReleased { event -> if (event.code == KeyCode.ENTER) addLabel() }
	}

	private val labelTable = TableView<AlertManagerLabelConfig>().apply {
		VBox.setVgrow(this, javafx.scene.layout.Priority.ALWAYS)
		setOnKeyReleased { event ->
			when (event.code) {
				KeyCode.HELP, // MacOS mapping of INSERT key
				KeyCode.INSERT -> newLabelName.requestFocus()
				KeyCode.DELETE -> selectionModel.selectedItem?.also { removeLabel(it) }
				else -> {
				}
			}
		}
	}

	private val labelTableNameColumn = TableColumn<AlertManagerLabelConfig, String>("Label").apply {
		sortType = TableColumn.SortType.ASCENDING
		minWidth = 400.0
		prefWidth = Region.USE_COMPUTED_SIZE
		maxWidth = Double.MAX_VALUE
	}

	private val labelTablePriorityColumn = TableColumn<AlertManagerLabelConfig, Priority>("Priority").apply {
		minWidth = 100.0
		prefWidth = Region.USE_COMPUTED_SIZE
		maxWidth = Region.USE_PREF_SIZE
		setCellFactory {
			val cell = TableCell<AlertManagerLabelConfig, Priority>()
			val comboBox = ComboBox<Priority>(FXCollections.observableList(Priority.values().toList()))
			comboBox.valueProperty().bindBidirectional(cell.itemProperty())

			comboBox.valueProperty().addListener { _, _, priority ->
				cell.tableRow.item.let { it as? AlertManagerLabelConfig }?.also { it.priority = priority }
				labelTable.items.sortWith(compareBy({-it.priority.ordinal}, {it.label}))
				controller.saveConfig()
			}
			cell.graphicProperty().bind(Bindings.`when`(cell.emptyProperty()).then(null as Node?).otherwise(comboBox))
			cell
		}
	}

	init {
		labelTableNameColumn.apply {
			cellValueFactory = PropertyValueFactory<AlertManagerLabelConfig, String>("label")
		}
		labelTablePriorityColumn.apply {
			cellValueFactory = PropertyValueFactory<AlertManagerLabelConfig, Priority>("priority")
		}
		labelTable.columns.addAll(labelTableNameColumn, labelTablePriorityColumn)
		labelTable.items.addAll(config.labels.sortedWith(compareBy({ -it.priority.ordinal }, {it.label})))
	}


	override fun check(updater: (Collection<StatusItem>) -> Unit) {
		if (config.url.isNotEmpty()) try {
			service?.check()?.enqueue(object : Callback<List<AlertManagerResponse>> {
				override fun onResponse(call: Call<List<AlertManagerResponse>>?, response: Response<List<AlertManagerResponse>>?) {
					LOGGER.info("${call?.request()?.method()} ${call?.request()?.url()}: ${response?.code()} ${response?.message()}")

					if (response?.isSuccessful == true) {
//						val foundLabels = response.body()
//								?.map { label -> config.labels.find { label.annotations.val  } } }
//								?.filter { job -> config.labels?.let { job.name.matches(it.toRegex()) } ?: true }
//
//						lastFoundJobNames = foundJobs?.map { it.name } ?: jobs.keys
//
//						foundJobs
//								?.map { StatusItem(config.name, it.priority, it.severity, it.name, link = it.uri) }
//								?.also(updater)
					} else {
//						lastFoundJobNames
//								.mapNotNull { jobs[it] }
//								.map { StatusItem(config.name, it.priority, Status.SERVICE_ERROR, it.name, link = URI(config.url)) }
//								.also(updater)
					}
				}

				override fun onFailure(call: Call<List<AlertManagerResponse>>?, response: Throwable?) {
					call?.request()
							?.also { LOGGER.warn("${it.method()} ${it.url()}: ${response?.message}", response) }
							?: LOGGER.warn(response?.message)
//					lastFoundJobNames
//							.mapNotNull { jobs[it] }
//							.map { StatusItem(config.name, it.priority, Status.CONNECTION_ERROR, it.name, link = URI(config.url)) }
//							.also(updater)
				}
			})
		} catch (e: IOException) {
			LOGGER.error(e.message, e)
//			lastFoundJobNames
//					.mapNotNull { jobs[it] }
//					.map { StatusItem(config.name, it.priority, Status.CONNECTION_ERROR, it.name, link = URI(config.url)) }
//					.also(updater)
		}
	}

//	private val AlertManagerStatusResponse.priority
//		get() = jobs[name]?.priority ?: config.priority

//	private val AlertManagerStatusResponse.severity
//		get() = (color?.toLowerCase() ?: "").let {
//			when {
//				it.startsWith("disabled") -> Status.OFF
//				it.startsWith("notbuild") -> Status.NONE
//				it.startsWith("aborted") -> Status.NOTIFICATION
//				it.startsWith("blue") -> Status.OK
//				it.startsWith("green") -> Status.OK
//				it.startsWith("yellow") -> Status.WARNING
//				it.startsWith("red") -> Status.ERROR
//				else -> Status.UNKNOWN
//			}
//		}

//	private val AlertManagerStatusResponse.uri
//		get() = url?.let { URI(it) }

	private fun addLabel() {
		newLabelName.text.takeIf { it.isNotEmpty() && it.isNotBlank() }?.also { jobName ->
			val newJob = AlertManagerLabelConfig(jobName, config.priority)
			labelTable.items
					.apply { add(newJob) }
					.sortWith(compareBy({-it.priority.ordinal}, { it.label }))
			config.labels.add(newJob)
			controller.saveConfig()
		}
		newLabelName.text = ""
	}

	private fun removeLabel(jenkinsJobConfig: AlertManagerLabelConfig) {
		Alert(Alert.AlertType.CONFIRMATION).apply {
			title = "Delete confirmation"
			headerText = "Delete confirmation"
			contentText = "Do you really want to delete \"${jenkinsJobConfig.label}\"?"
			buttonTypes.setAll(ButtonType.YES, ButtonType.NO)
		}.showAndWait().ifPresent {
			it.takeIf { it == ButtonType.YES }?.also {
				//val selected = jobTable.selectionModel.selectedItem
				labelTable.items.remove(jenkinsJobConfig)
				config.labels.remove(jenkinsJobConfig)
				controller.saveConfig()
			}
		}
	}
}