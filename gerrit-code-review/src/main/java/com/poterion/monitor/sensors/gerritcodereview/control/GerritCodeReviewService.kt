package com.poterion.monitor.sensors.gerritcodereview.control

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
import com.poterion.monitor.sensors.gerritcodereview.GerritCodeReviewModule
import com.poterion.monitor.sensors.gerritcodereview.data.GerritCodeReviewConfig
import com.poterion.monitor.sensors.gerritcodereview.data.GerritCodeReviewQueryConfig
import com.poterion.monitor.sensors.gerritcodereview.data.GerritCodeReviewQueryResponse
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
import java.time.ZoneId
import java.time.format.DateTimeFormatterBuilder
import java.time.format.DateTimeParseException
import java.util.concurrent.Executors

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
class GerritCodeReviewService(override val controller: ControllerInterface, config: GerritCodeReviewConfig) : Service<GerritCodeReviewConfig>(config) {

	companion object {
		val LOGGER: Logger = LoggerFactory.getLogger(GerritCodeReviewService::class.java)
	}

	override val definition: Module<GerritCodeReviewConfig, ModuleInstanceInterface<GerritCodeReviewConfig>> = GerritCodeReviewModule
	private val service
		get() = retrofit?.create(GerritCodeReviewRestService::class.java)
	private val responseType = http.objectMapper.typeFactory.constructCollectionType(MutableList::class.java, GerritCodeReviewQueryResponse::class.java)
	private var lastFound: MutableMap<String, MutableCollection<StatusItem>> = mutableMapOf()
	private val executor = Executors.newSingleThreadExecutor()

	override val configurationAddition: List<Parent>?
		get() = listOf(
				VBox(
						HBox(
								Label("Name").apply { maxHeight = Double.MAX_VALUE }, textNewName,
								Label("Query").apply { maxHeight = Double.MAX_VALUE }, textNewQuery,
								Button("Add").apply { setOnAction { addQuery() } }),
						labelTable).apply {
					VBox.setVgrow(this, javafx.scene.layout.Priority.ALWAYS)
				})

	private val textNewName = TextField("").apply {
		HBox.setHgrow(this, javafx.scene.layout.Priority.ALWAYS)
		setOnKeyReleased { event -> if (event.code == KeyCode.ENTER) addQuery() }
	}

	private val textNewQuery = TextField("").apply {
		HBox.setHgrow(this, javafx.scene.layout.Priority.ALWAYS)
		setOnKeyReleased { event -> if (event.code == KeyCode.ENTER) addQuery() }
	}

	private val labelTable = TableView<GerritCodeReviewQueryConfig>().apply {
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
				KeyCode.INSERT -> textNewName.requestFocus()
				KeyCode.DELETE -> selectionModel.selectedItem?.also { removeQuery(it) }
				else -> {
					// Nothing to do
				}
			}
		}
	}

	private val labelTableNameColumn = TableColumn<GerritCodeReviewQueryConfig, String>("Name").apply {
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

	private val labelTableQueryColumn = TableColumn<GerritCodeReviewQueryConfig, String>("Query").apply {
		isSortable = false
		minWidth = 200.0
		prefWidth = Region.USE_COMPUTED_SIZE
		maxWidth = Double.MAX_VALUE
		cell("query") { item, value, empty ->
			graphic = TextField(value).takeUnless { empty }?.apply {
				text = value
				focusedProperty().addListener { _, _, value ->
					if (!value) {
						item?.query = text
						sortLabelTable()
						controller.saveConfig()
					}
				}
			}
		}
	}

	private val labelTablePriorityColumn = TableColumn<GerritCodeReviewQueryConfig, Priority>("Priority").apply {
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

	private val labelTableStatusColumn = TableColumn<GerritCodeReviewQueryConfig, Status>("Status").apply {
		isSortable = false
		minWidth = 200.0
		prefWidth = Region.USE_COMPUTED_SIZE
		maxWidth = Region.USE_PREF_SIZE
		cell("status") { item, value, empty ->
			graphic = ComboBox<Status>(FXCollections.observableList(Status.values().toList())).takeUnless { empty }
					?.apply {
						factory { item, empty ->
							text = item?.takeUnless { empty }?.name
							graphic = item?.takeUnless { empty }?.toIcon()?.toImageView()
						}
						selectionModel.select(value)
						valueProperty().addListener { _, _, status ->
							if (status != null) {
								item?.status = status
								sortLabelTable()
								controller.saveConfig()
							}
						}
					}
		}
	}

	private val labelTableActionColumn = TableColumn<GerritCodeReviewQueryConfig, Status>("").apply {
		isSortable = false
		minWidth = 96.0
		prefWidth = Region.USE_COMPUTED_SIZE
		maxWidth = Region.USE_PREF_SIZE
		cell { item, _, empty ->
			graphic = if (!empty && item != null) HBox(
					Button("", CommonIcon.LINK.toImageView()).apply {
						setOnAction {
							val url = item
									.let { URLEncoder.encode(it.query, Charsets.UTF_8.name()) }
									.let { "${config.url}/#/q/${it}" }
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
		labelTable.items.addAll(config.queries)
		labelTable.columns.addAll(labelTablePriorityColumn, labelTableNameColumn, labelTableQueryColumn,
				labelTableStatusColumn, labelTableActionColumn)
		sortLabelTable()
	}

	override fun check(updater: (Collection<StatusItem>) -> Unit) {
		lastFound.keys
				.filterNot { key -> config.queries.map { it.name }.contains(key) }
				.forEach { lastFound.remove(it) }
		if (config.enabled && config.url.isNotEmpty()) executor.submit {
			val queries = config.queries.mapNotNull { q -> service?.check(q.query)?.let { q to it } }
			for ((query, call) in queries) try {
				val response = call.execute()
				if (response?.isSuccessful == true) {
					val body = response.body() ?: ""
					val startIndex = body.indexOf('[')
					if (startIndex >= 0) {
						val queryAlerts = http.objectMapper.readValue<List<GerritCodeReviewQueryResponse>>(
								body.substring(startIndex), responseType)
								.map { item -> item.toStatusItem(query) }
								.takeIf { it.isNotEmpty() }
						if (queryAlerts == null) lastFound.remove(query.name)
						else lastFound[query.name] = queryAlerts.toMutableList()
					}
				} else addErrorStatus(query, "Service error", Status.SERVICE_ERROR)
			} catch (e: IOException) {
				LOGGER.error(e.message, e)
				addErrorStatus(query, "Connection error", Status.CONNECTION_ERROR)
			}
			updater(lastFound.values.flatten())
		}
	}

	private fun addErrorStatus(query: GerritCodeReviewQueryConfig,
							   error: String,
							   status: Status) {
		val id = "${config.uuid}-${query.name}-error"
		val cache = lastFound.getOrPut(query.name, { mutableListOf() })
		cache.add(StatusItem(
				id = id,
				serviceId = config.uuid,
				priority = query.priority,
				status = status,
				title = "[${query.name}] ${error}",
				isRepeatable = false))
	}

	private fun addQuery() {
		textNewName.text.takeIf { it.isNotEmpty() && it.isNotBlank() }?.also { name ->
			val newLabel = GerritCodeReviewQueryConfig(
					name = name,
					query = textNewQuery.text,
					priority = config.priority,
					status = Status.NONE)
			labelTable.items
					.apply { add(newLabel) }
			//.sortWith(compareBy({-it.priority.ordinal}, { it.name }, {it.value}))
			config.queries.add(newLabel)
			controller.saveConfig()
		}
		textNewName.text = ""
		textNewQuery.text = ""
	}

	private fun removeQuery(queryConfig: GerritCodeReviewQueryConfig) {
		Alert(Alert.AlertType.CONFIRMATION).apply {
			title = "Delete confirmation"
			headerText = "Delete confirmation"
			contentText = "Do you really want to delete query ${queryConfig.name}?"
			buttonTypes.setAll(ButtonType.YES, ButtonType.NO)
		}.showAndWait().ifPresent {
			it.takeIf { it == ButtonType.YES }?.also {
				labelTable.items.remove(queryConfig)
				config.queries.remove(queryConfig)
				controller.saveConfig()
			}
		}
	}

	private fun sortLabelTable() {
		labelTable.items.sortWith(compareBy(
				{ -it.priority.ordinal },
				{ -it.status.ordinal },
				{ it.name }))
	}

	private fun GerritCodeReviewQueryResponse.toStatusItem(query: GerritCodeReviewQueryConfig) = StatusItem(
			id = "${config.uuid}-${this.changeId}",
			serviceId = config.uuid,
			priority = query.priority,
			status = query.status,
			title = "[${query.name}] ${this.subject ?: ""}",
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