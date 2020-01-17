package com.poterion.monitor.sensors.alertmanager.control

import com.poterion.monitor.api.controllers.ControllerInterface
import com.poterion.monitor.api.controllers.ModuleInstanceInterface
import com.poterion.monitor.api.controllers.Service
import com.poterion.monitor.api.lib.toIcon
import com.poterion.monitor.api.lib.toImageView
import com.poterion.monitor.api.modules.Module
import com.poterion.monitor.api.ui.CommonIcon
import com.poterion.monitor.api.utils.factory
import com.poterion.monitor.api.utils.toSet
import com.poterion.monitor.data.Priority
import com.poterion.monitor.data.Status
import com.poterion.monitor.data.StatusItem
import com.poterion.monitor.sensors.alertmanager.AlertManagerModule
import com.poterion.monitor.sensors.alertmanager.data.AlertManagerConfig
import com.poterion.monitor.sensors.alertmanager.data.AlertManagerLabelConfig
import com.poterion.monitor.sensors.alertmanager.data.AlertManagerResponse
import javafx.beans.binding.Bindings
import javafx.collections.FXCollections
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.control.*
import javafx.scene.control.cell.PropertyValueFactory
import javafx.scene.input.KeyCode
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.layout.Region
import javafx.scene.layout.VBox
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException
import java.time.Instant
import java.time.format.DateTimeParseException

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
		get() = listOf(
				Label("Name annotation/label").apply {
					maxWidth = Double.MAX_VALUE
					maxHeight = Double.MAX_VALUE
					alignment = Pos.CENTER_RIGHT
				} to TextField(config.nameRefs.joinToString(",")).apply {
					textProperty().addListener { _, _, value -> config.nameRefs = value.toSet(",") }
					focusedProperty().addListener { _, _, hasFocus -> if (!hasFocus) controller.saveConfig() }
				},
				Pane() to Label("Comma separated list of annotations and/or labels. First found will be used."),
				Label("Description annotation/label").apply {
					maxWidth = Double.MAX_VALUE
					maxHeight = Double.MAX_VALUE
					alignment = Pos.CENTER_RIGHT
				} to TextField(config.descriptionRefs.joinToString(",")).apply {
					textProperty().addListener { _, _, value -> config.descriptionRefs = value.toSet(",") }
					focusedProperty().addListener { _, _, hasFocus -> if (!hasFocus) controller.saveConfig() }
				},
				Pane() to Label("Comma separated list of annotations and/or labels. First found will be used."),
				Label("Receivers").apply {
					maxWidth = Double.MAX_VALUE
					maxHeight = Double.MAX_VALUE
					alignment = Pos.CENTER_RIGHT
				} to TextField(config.receivers.joinToString(",")).apply {
					promptText = "All receivers"
					textProperty().addListener { _, _, value -> config.receivers = value.toSet(",") }
					focusedProperty().addListener { _, _, hasFocus -> if (!hasFocus) controller.saveConfig() }
				},
				Pane() to Label("Comma separated list of receivers to take into account."),
				Label("Labels/annotations").apply {
					maxWidth = Double.MAX_VALUE
					maxHeight = Double.MAX_VALUE
					alignment = Pos.CENTER_RIGHT
				} to TextField(config.labelFilter.joinToString(",")).apply {
					promptText = "All labels and annotations"
					textProperty().addListener { _, _, value -> config.labelFilter = value.toSet(",") }
					focusedProperty().addListener { _, _, hasFocus -> if (!hasFocus) controller.saveConfig() }
				},
				Pane() to Label("Comma separated list labels and annotation to be used in status items. Use ! for negation."))

	override val configurationAddition: List<Parent>?
		get() = listOf(
				VBox(
						HBox(
								Label("Name").apply { maxHeight = Double.MAX_VALUE }, newLabelName,
								Label("Value").apply { maxHeight = Double.MAX_VALUE }, newLabelValue,
								Button("Add").apply { setOnAction { addLabel() } }),
						labelTable).apply {
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

	private val labelTable = TableView<AlertManagerLabelConfig>().apply {
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
				KeyCode.INSERT -> newLabelName.requestFocus()
				KeyCode.DELETE -> selectionModel.selectedItem?.also { removeLabel(it) }
				else -> {
					// Nothing to do
				}
			}
		}
	}

	private val labelTableNameColumn = TableColumn<AlertManagerLabelConfig, String>("Label").apply {
		isSortable = false
		minWidth = 150.0
		prefWidth = Region.USE_COMPUTED_SIZE
		maxWidth = Double.MAX_VALUE
		setCellFactory {
			val cell = TableCell<AlertManagerLabelConfig, String>()
			val textField = TextField(cell.item)
			textField.textProperty().bindBidirectional(cell.itemProperty())
			textField.textProperty().addListener { _, _, value ->
				cell.tableRow.item.let { it as? AlertManagerLabelConfig }?.also { it.name = value }
				labelTable.items.sortWith(compareBy({ -it.priority.ordinal }, { -it.status.ordinal }, { it.name }, { it.value }))
				controller.saveConfig()
			}
			cell.graphicProperty().bind(Bindings.`when`(cell.emptyProperty()).then(null as Node?).otherwise(textField))
			cell
		}
	}

	private val labelTableValueColumn = TableColumn<AlertManagerLabelConfig, String>("Value").apply {
		isSortable = false
		minWidth = 200.0
		prefWidth = Region.USE_COMPUTED_SIZE
		maxWidth = Double.MAX_VALUE
		setCellFactory {
			val cell = TableCell<AlertManagerLabelConfig, String>()
			val textField = TextField(cell.item)
			textField.textProperty().bindBidirectional(cell.itemProperty())
			textField.textProperty().addListener { _, _, value ->
				cell.tableRow.item.let { it as? AlertManagerLabelConfig }?.also { it.value = value }
				labelTable.items.sortWith(compareBy({ -it.priority.ordinal }, { -it.status.ordinal }, { it.name }, { it.value }))
				controller.saveConfig()
			}
			cell.graphicProperty().bind(Bindings.`when`(cell.emptyProperty()).then(null as Node?).otherwise(textField))
			cell
		}
	}

	private val labelTablePriorityColumn = TableColumn<AlertManagerLabelConfig, Priority>("Priority").apply {
		isSortable = false
		minWidth = 150.0
		prefWidth = Region.USE_COMPUTED_SIZE
		maxWidth = Region.USE_PREF_SIZE
		setCellFactory {
			val cell = TableCell<AlertManagerLabelConfig, Priority>()
			val comboBox = ComboBox<Priority>(FXCollections.observableList(Priority.values().toList())).apply {
				factory { item, empty ->
					text = item?.takeUnless { empty }?.name
					graphic = item?.takeUnless { empty }?.toIcon()?.toImageView()
				}
				valueProperty().bindBidirectional(cell.itemProperty())
				valueProperty().addListener { _, _, priority ->
					cell.tableRow.item.let { it as? AlertManagerLabelConfig }?.also { it.priority = priority }
					labelTable.items.sortWith(compareBy({ -it.priority.ordinal }, { -it.status.ordinal }, { it.name }, { it.value }))
					controller.saveConfig()
				}
			}
			cell.graphicProperty().bind(Bindings.`when`(cell.emptyProperty()).then(null as Node?).otherwise(comboBox))
			cell
		}
	}

	private val labelTableStatusColumn = TableColumn<AlertManagerLabelConfig, Status>("Status").apply {
		isSortable = false
		minWidth = 200.0
		prefWidth = Region.USE_COMPUTED_SIZE
		maxWidth = Region.USE_PREF_SIZE
		setCellFactory {
			val cell = TableCell<AlertManagerLabelConfig, Status>()
			val comboBox = ComboBox<Status>(FXCollections.observableList(Status.values().toList())).apply {
				factory { item, empty ->
					text = item?.takeUnless { empty }?.name
					graphic = item?.takeUnless { empty }?.toIcon()?.toImageView()
				}
				valueProperty().bindBidirectional(cell.itemProperty())

				valueProperty().addListener { _, _, status ->
					cell.tableRow.item.let { it as? AlertManagerLabelConfig }?.also { it.status = status }
					labelTable.items.sortWith(compareBy({ -it.priority.ordinal }, { -it.status.ordinal }, { it.name }, { it.value }))
					controller.saveConfig()
				}
			}
			cell.graphicProperty().bind(Bindings.`when`(cell.emptyProperty()).then(null as Node?).otherwise(comboBox))
			cell
		}
	}

	private val labelTableActionColumn = TableColumn<AlertManagerLabelConfig, Status>("").apply {
		isSortable = false
		minWidth = 48.0
		prefWidth = Region.USE_COMPUTED_SIZE
		maxWidth = Region.USE_PREF_SIZE
		setCellFactory {
			val cell = TableCell<AlertManagerLabelConfig, Status>()
			val button = Button("", CommonIcon.TRASH.toImageView()).apply {
				setOnAction {
					cell.tableRow.item?.let { it as? AlertManagerLabelConfig }?.also { removeLabel(it) }
				}
			}
			cell.graphicProperty().bind(Bindings.`when`(cell.emptyProperty()).then(null as Node?).otherwise(button))
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
		labelTable.columns.addAll(labelTablePriorityColumn, labelTableNameColumn, labelTableValueColumn,
				labelTableStatusColumn, labelTableActionColumn)
		labelTable.items.addAll(config.labels.sortedWith(compareBy({ -it.priority.ordinal }, { -it.status.ordinal }, { it.name }, { it.value })))
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
								?.filter { a -> config.receivers.isEmpty() || a.receivers.map { it.name }.any { config.receivers.contains(it) } }
								?.flatMap { item -> item.labels.map { "${it.key}:${it.value}" to item } }
								?.filter { (pair, _) -> configPairs[pair] != null }
								?.map { (pair, item) -> configPairs.getValue(pair) to item }
								?.sortedWith(compareBy({ (c, _) -> c.status.ordinal }, { (p, _) -> p.priority.ordinal }))
								?.associateBy { (_, i) ->
									config.nameRefs.mapNotNull {
										i.annotations[it] ?: i.labels[it]
									}.firstOrNull() ?: ""
								}
								?.map { (name, p) -> Triple(name, p.first, p.second) }
								?.also { lastFound = it }
								?.map { (n, c, i) ->
									StatusItem(serviceName = config.name,
											priority = c.priority,
											status = c.status,
											title = n,
											detail = config.descriptionRefs
													.mapNotNull { i.annotations[it] ?: i.labels[it] }
													.firstOrNull(),
											labels = (i.labels + i.annotations)
													.filterNot { (k, _) -> config.nameRefs.contains(k) }
													.filterNot { (k, _) -> config.descriptionRefs.contains(k) }
													.filter { (k, _) ->
														config.labelFilter
																.filterNot { it.startsWith("!") }
																.let { it.isEmpty() || it.contains(k) }
													}
													.filter { (k, _) ->
														config.labelFilter
																.filter { it.startsWith("!") }
																.map { it.removePrefix("!") }
																.let { it.isEmpty() || !it.contains(k) }
													},
											link = i.generatorURL,
											startedAt = try {
												Instant.parse(i.startsAt)
											} catch (e: DateTimeParseException) {
												LOGGER.error(e.message, e)
												Instant.now()
											})
								}
								?.takeIf { it.isNotEmpty() }
								?: listOf(StatusItem(config.name, config.priority, Status.OK, "No alerts"))
						updater(alerts)
					} else {
						updater(getStatusItems("Service error", Status.SERVICE_ERROR))
					}
				}

				override fun onFailure(call: Call<List<AlertManagerResponse>>?, response: Throwable?) {
					call?.request()
							?.also { LOGGER.warn("${it.method()} ${it.url()}: ${response?.message}", response) }
							?: LOGGER.warn(response?.message)
					updater(getStatusItems("Connection error", Status.CONNECTION_ERROR))
				}
			})
		} catch (e: IOException) {
			LOGGER.error(e.message, e)
			updater(getStatusItems("Connection error", Status.CONNECTION_ERROR))
		}
	}

	private fun getStatusItems(defaultTitle: String,
							   rewriteStatus: Status? = null): Collection<StatusItem> = lastFound
			.map { (n, c, i) ->
				StatusItem(config.name, c.priority, rewriteStatus ?: c.status, n, link = i.generatorURL)
			}
			.takeIf { it.isNotEmpty() }
			?: listOf(StatusItem(config.name, config.priority, rewriteStatus ?: Status.OK, defaultTitle))

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

	private fun removeLabel(labelConfig: AlertManagerLabelConfig) {
		Alert(Alert.AlertType.CONFIRMATION).apply {
			title = "Delete confirmation"
			headerText = "Delete confirmation"
			contentText = "Do you really want to delete label ${labelConfig.name}=\"${labelConfig.value}\"?"
			buttonTypes.setAll(ButtonType.YES, ButtonType.NO)
		}.showAndWait().ifPresent {
			it.takeIf { it == ButtonType.YES }?.also {
				labelTable.items.remove(labelConfig)
				config.labels.remove(labelConfig)
				controller.saveConfig()
			}
		}
	}
}