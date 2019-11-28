package com.poterion.monitor.sensors.alertmanager.control

import com.poterion.monitor.api.controllers.ControllerInterface
import com.poterion.monitor.api.controllers.ModuleInstanceInterface
import com.poterion.monitor.api.controllers.Service
import com.poterion.monitor.api.modules.Module
import com.poterion.monitor.data.Priority
import com.poterion.monitor.data.Status
import com.poterion.monitor.data.StatusItem
import com.poterion.monitor.sensors.alertmanager.AlertManagerModule
import com.poterion.monitor.sensors.alertmanager.data.AlertManagerConfig
import com.poterion.monitor.sensors.alertmanager.data.AlertManagerLabelConfig
import com.poterion.monitor.sensors.alertmanager.data.AlertManagerResponse
import javafx.application.Platform
import javafx.beans.binding.Bindings
import javafx.collections.FXCollections
import javafx.geometry.Orientation
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
import java.time.LocalDateTime


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

	private var lastFound = listOf<Triple<String, AlertManagerLabelConfig, AlertManagerResponse>>()

	override val configurationRows: List<Pair<Node, Node>>?
		get() = listOf(Label("Name label") to TextField(config.nameLabel).apply {
			textProperty().addListener { _, _, value -> config.nameLabel = value }
			focusedProperty().addListener { _, _, hasFocus -> if (!hasFocus) controller.saveConfig() }
		})

	override val configurationAddition: List<Parent>?
		get() = listOf(
				SplitPane(
						VBox(
								HBox(
										Label("Name"), newLabelName,
										Label("Value"), newLabelValue,
										Button("Add").apply { setOnAction { addLabel() } }),
								labelTable),
						logArea).apply {
					orientation = Orientation.VERTICAL
					setDividerPositions(0.8, 0.2)
					VBox.setVgrow(this, javafx.scene.layout.Priority.ALWAYS)
				})

	private val newLabelName = TextField("").apply {
		HBox.setHgrow(this, javafx.scene.layout.Priority.ALWAYS)
		setOnKeyReleased { event -> if (event.code == KeyCode.ENTER) addLabel() }
	}

	private val newLabelValue = TextField("").apply {
		HBox.setHgrow(this, javafx.scene.layout.Priority.ALWAYS)
		setOnKeyReleased { event -> if (event.code == KeyCode.ENTER) addLabel() }
	}

	private val logArea = TextArea("").apply {
		isEditable = false
		text = ""
		textProperty().addListener { _, _, text ->
			if (!isFocused) scrollTop = Double.MAX_VALUE
		}
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
		//sortType = TableColumn.SortType.ASCENDING
		minWidth = 150.0
		prefWidth = Region.USE_COMPUTED_SIZE
		maxWidth = Double.MAX_VALUE
		setCellFactory {
			val cell = TableCell<AlertManagerLabelConfig, String>()
			val textField = TextField(cell.item)
			textField.textProperty().bindBidirectional(cell.itemProperty())
			textField.textProperty().addListener { _, _, value ->
				cell.tableRow.item.let { it as? AlertManagerLabelConfig }?.also { it.name = value }
				controller.saveConfig()
			}
			cell.graphicProperty().bind(Bindings.`when`(cell.emptyProperty()).then(null as Node?).otherwise(textField))
			cell
		}
	}

	private val labelTableValueColumn = TableColumn<AlertManagerLabelConfig, String>("Value").apply {
		//sortType = TableColumn.SortType.ASCENDING
		minWidth = 200.0
		prefWidth = Region.USE_COMPUTED_SIZE
		maxWidth = Double.MAX_VALUE
		setCellFactory {
			val cell = TableCell<AlertManagerLabelConfig, String>()
			val textField = TextField(cell.item)
			textField.textProperty().bindBidirectional(cell.itemProperty())
			textField.textProperty().addListener { _, _, value ->
				cell.tableRow.item.let { it as? AlertManagerLabelConfig }?.also { it.value = value }
				controller.saveConfig()
			}
			cell.graphicProperty().bind(Bindings.`when`(cell.emptyProperty()).then(null as Node?).otherwise(textField))
			cell
		}

	}

	private val labelTablePriorityColumn = TableColumn<AlertManagerLabelConfig, Priority>("Priority").apply {
		sortType = TableColumn.SortType.ASCENDING
		minWidth = 150.0
		prefWidth = Region.USE_COMPUTED_SIZE
		maxWidth = Region.USE_PREF_SIZE
		setCellFactory {
			val cell = TableCell<AlertManagerLabelConfig, Priority>()
			val comboBox = ComboBox<Priority>(FXCollections.observableList(Priority.values().toList()))
			comboBox.valueProperty().bindBidirectional(cell.itemProperty())

			comboBox.valueProperty().addListener { _, _, priority ->
				cell.tableRow.item.let { it as? AlertManagerLabelConfig }?.also { it.priority = priority }
				labelTable.items.sortWith(compareBy({ -it.priority.ordinal }, { it.name }, { it.value }))
				controller.saveConfig()
			}
			cell.graphicProperty().bind(Bindings.`when`(cell.emptyProperty()).then(null as Node?).otherwise(comboBox))
			cell
		}
	}

	private val labelTableStatusColumn = TableColumn<AlertManagerLabelConfig, Status>("Status").apply {
		minWidth = 200.0
		prefWidth = Region.USE_COMPUTED_SIZE
		maxWidth = Region.USE_PREF_SIZE
		setCellFactory {
			val cell = TableCell<AlertManagerLabelConfig, Status>()
			val comboBox = ComboBox<Status>(FXCollections.observableList(Status.values().toList()))
			comboBox.valueProperty().bindBidirectional(cell.itemProperty())

			comboBox.valueProperty().addListener { _, _, status ->
				cell.tableRow.item.let { it as? AlertManagerLabelConfig }?.also { it.status = status }
				controller.saveConfig()
			}
			cell.graphicProperty().bind(Bindings.`when`(cell.emptyProperty()).then(null as Node?).otherwise(comboBox))
			cell
		}
	}

	init {
		labelTableNameColumn.apply {
			cellValueFactory = PropertyValueFactory<AlertManagerLabelConfig, String>("name")
		}
		labelTableValueColumn.apply {
			cellValueFactory = PropertyValueFactory<AlertManagerLabelConfig, String>("value")
		}
		labelTablePriorityColumn.apply {
			cellValueFactory = PropertyValueFactory<AlertManagerLabelConfig, Priority>("priority")
		}
		labelTableStatusColumn.apply {
			cellValueFactory = PropertyValueFactory<AlertManagerLabelConfig, Status>("status")
		}
		labelTable.columns.addAll(labelTablePriorityColumn, labelTableNameColumn, labelTableValueColumn, labelTableStatusColumn)
		labelTable.items.addAll(config.labels.sortedWith(compareBy({ -it.priority.ordinal }, { it.name }, { it.value })))
	}


	override fun check(updater: (Collection<StatusItem>) -> Unit) {
		if (config.enabled && config.url.isNotEmpty()) try {
			service?.check()?.enqueue(object : Callback<List<AlertManagerResponse>> {
				override fun onResponse(call: Call<List<AlertManagerResponse>>?, response: Response<List<AlertManagerResponse>>?) {
					LOGGER.info("${call?.request()?.method()} ${call?.request()?.url()}: ${response?.code()} ${response?.message()}")

					if (response?.isSuccessful == true) {
						val configPairs = config.labels.map { "${it.name}:${it.value}" to it }.toMap()
						val alerts = response.body()
								?.filter { it.status?.silencedBy?.isEmpty() != false }
								?.filter { it.status?.inhibitedBy?.isEmpty() != false }
								?.flatMap { item -> item.labels.map { "${it.key}:${it.value}" to item } }
								?.filter { (pair, _) -> configPairs[pair] != null }
								?.map { (pair, item) -> configPairs[pair]!! to item }
								?.sortedWith(compareBy({ (c, _) -> c.status.ordinal }, { (p, _) -> p.priority.ordinal }))
								?.associateBy { (_, i) -> i.labels[config.nameLabel] ?: "" }
								?.map { (name, p) -> Triple(name, p.first, p.second) }
								?.also { lastFound = it }
								?.map { (n, c, i) -> StatusItem(config.name, c.priority, c.status, n, link = i.generatorURL) }
								?.takeIf { it.isNotEmpty() }
								?: listOf(StatusItem(config.name, Priority.MAXIMUM, Status.OK, ""))
						alerts.also(updater)

						Platform.runLater {
							logArea.text = alerts.joinToString("\n") {
								"${LocalDateTime.now()} [${it.priority}]" +
										" ${it.label.takeIf { it.isNotEmpty() } ?: "Default"}: ${it.status}"
							}
						}
					} else {
						lastFound
								.map { (n, c, i) -> StatusItem(config.name, c.priority, Status.SERVICE_ERROR, n, link = i.generatorURL) }
								.also(updater)
					}
				}

				override fun onFailure(call: Call<List<AlertManagerResponse>>?, response: Throwable?) {
					call?.request()
							?.also { LOGGER.warn("${it.method()} ${it.url()}: ${response?.message}", response) }
							?: LOGGER.warn(response?.message)
					lastFound
							.map { (n, c, i) -> StatusItem(config.name, c.priority, Status.CONNECTION_ERROR, n, link = i.generatorURL) }
							.also(updater)
				}
			})
		} catch (e: IOException) {
			LOGGER.error(e.message, e)
			lastFound
					.map { (n, c, i) -> StatusItem(config.name, c.priority, Status.CONNECTION_ERROR, n, link = i.generatorURL) }
					.also(updater)
		}
	}

	private fun addLabel() {
		newLabelName.text.takeIf { it.isNotEmpty() && it.isNotBlank() }?.also { name ->
			val newLabel = AlertManagerLabelConfig(
					name = name,
					value = newLabelValue.text,
					priority = config.priority,
					status = Status.NONE)
			labelTable.items
					.apply { add(newLabel) }
			//.sortWith(compareBy({-it.priority.ordinal}, { it.name }, {it.value}))
			config.labels.add(newLabel)
			controller.saveConfig()
		}
		newLabelName.text = ""
	}

	private fun removeLabel(jenkinsJobConfig: AlertManagerLabelConfig) {
		Alert(Alert.AlertType.CONFIRMATION).apply {
			title = "Delete confirmation"
			headerText = "Delete confirmation"
			contentText = "Do you really want to delete label ${jenkinsJobConfig.name}=\"${jenkinsJobConfig.value}\"?"
			buttonTypes.setAll(ButtonType.YES, ButtonType.NO)
		}.showAndWait().ifPresent {
			it.takeIf { it == ButtonType.YES }?.also {
				labelTable.items.remove(jenkinsJobConfig)
				config.labels.remove(jenkinsJobConfig)
				controller.saveConfig()
			}
		}
	}
}