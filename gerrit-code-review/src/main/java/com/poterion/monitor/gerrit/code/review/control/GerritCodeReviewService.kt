package com.poterion.monitor.gerrit.code.review.control

import com.poterion.monitor.api.controllers.ControllerInterface
import com.poterion.monitor.api.controllers.ModuleInstanceInterface
import com.poterion.monitor.api.controllers.Service
import com.poterion.monitor.api.lib.toIcon
import com.poterion.monitor.api.lib.toImageView
import com.poterion.monitor.api.modules.Module
import com.poterion.monitor.api.ui.CommonIcon
import com.poterion.monitor.api.utils.cell
import com.poterion.monitor.api.utils.factory
import com.poterion.monitor.api.utils.toUriOrNull
import com.poterion.monitor.data.Priority
import com.poterion.monitor.data.Status
import com.poterion.monitor.data.StatusItem
import com.poterion.monitor.gerrit.code.review.GerritCodeReviewModule
import com.poterion.monitor.gerrit.code.review.data.GerritCodeReviewConfig
import com.poterion.monitor.gerrit.code.review.data.GerritCodeReviewQueryConfig
import com.poterion.monitor.gerrit.code.review.data.GerritCodeReviewQueryResponse
import javafx.collections.FXCollections
import javafx.scene.Parent
import javafx.scene.control.*
import javafx.scene.input.KeyCode
import javafx.scene.layout.HBox
import javafx.scene.layout.Region
import javafx.scene.layout.VBox
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.awt.Desktop
import java.io.IOException
import java.net.URLEncoder
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatterBuilder
import java.time.format.DateTimeParseException

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
	private val responseType = objectMapper.typeFactory.constructCollectionType(MutableList::class.java, GerritCodeReviewQueryResponse::class.java)
	private var lastFound = listOf<StatusItem>()

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
				textProperty().bindBidirectional(itemProperty())
				textProperty().addListener { _, _, value ->
					if (value != null) {
						item?.name = value
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
				textProperty().bindBidirectional(itemProperty())
				textProperty().addListener { _, _, value ->
					if (value != null) {
						item?.query = value
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
						valueProperty().bindBidirectional(itemProperty())
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
						valueProperty().bindBidirectional(itemProperty())
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
		if (config.enabled && config.url.isNotEmpty()) try {
			config.queries.mapNotNull { q -> service?.check(q.query)?.let { q to it } }.forEach { (query, call) ->
				call.enqueue(object : Callback<String> {
					override fun onResponse(call: Call<String>?, response: Response<String>?) {
						LOGGER.info("${call?.request()?.method()} ${call?.request()?.url()}: ${response?.code()} ${response?.message()}")

						if (response?.isSuccessful == true) {
							val body = response.body() ?: ""
							val startIndex = body.indexOf('[')
							if (startIndex >= 0) {
								val alerts = objectMapper.readValue<List<GerritCodeReviewQueryResponse>>(
										body.substring(startIndex), responseType)
										.map { item ->
											StatusItem(serviceId = config.uuid,
													priority = query.priority,
													status = query.status,
													title = "[${query.name}] ${item.subject ?: ""}",
													//detail = item.subject,
													labels = listOf(
															"Project" to item.project,
															"Branch" to item.branch,
															"Topic" to item.topic,
															"Status" to item.status,
															"Mergeable" to item.mergeable?.let { if (it) "Yes" else "No" })
															.mapNotNull { (k, v) -> v?.let { k to v } }
															.toMap()
															.toMutableMap()
															.also { m -> m.putAll(item.labels.flatMap { (k, v) -> v.keys.map { k to it } }.toMap()) },
													link = "${config.url}/#/q/${item.changeId}",
													startedAt = try {
														DateTimeFormatterBuilder()
																.appendPattern("yyyy-MM-dd HH:mm:ss.000000000")
																.toFormatter()
																.withZone(ZoneId.of("UTC"))
																.parse(item.updated)
																.let { Instant.from(it) }
													} catch (e: DateTimeParseException) {
														LOGGER.error(e.message, e)
														Instant.now()
													})
										}
										.also { lastFound = it }
										.takeIf { it.isNotEmpty() }
										?: listOf(StatusItem(config.name, config.priority, Status.OK, "${query.name}: No Items"))
								updater(alerts)
							}
						} else {
							updater(getStatusItems("Service error", query.priority, Status.SERVICE_ERROR))
						}
					}

					override fun onFailure(call: Call<String>?, response: Throwable?) {
						call?.request()
								?.also { LOGGER.warn("${it.method()} ${it.url()}: ${response?.message}", response) }
								?: LOGGER.warn(response?.message)
						updater(getStatusItems("Connection error", query.priority, Status.CONNECTION_ERROR))
					}
				})
			}
		} catch (e: IOException) {
			LOGGER.error(e.message, e)
			updater(getStatusItems("Connection error", config.priority, Status.CONNECTION_ERROR))
		}
	}

	private fun getStatusItems(error: String,
							   priority: Priority,
							   status: Status): Collection<StatusItem> =
			lastFound + listOf(StatusItem(config.name, priority, status, error))

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
}