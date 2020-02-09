package com.poterion.monitor.sensors.feed.control

import com.poterion.monitor.api.controllers.ControllerInterface
import com.poterion.monitor.api.controllers.ModuleInstanceInterface
import com.poterion.monitor.api.controllers.Service
import com.poterion.monitor.api.modules.Module
import com.poterion.monitor.api.ui.TableSettingsPlugin
import com.poterion.monitor.api.utils.toIcon
import com.poterion.monitor.data.Priority
import com.poterion.monitor.data.Status
import com.poterion.monitor.data.StatusItem
import com.poterion.monitor.data.toHeaderString
import com.poterion.monitor.sensors.feed.SyndicationFeedModule
import com.poterion.monitor.sensors.feed.data.SyndicationFeedConfig
import com.poterion.monitor.sensors.feed.data.SyndicationFeedFilterConfig
import com.poterion.utils.javafx.factory
import com.poterion.utils.javafx.toImageView
import com.poterion.utils.kotlin.toUriOrNull
import com.rometools.rome.io.SyndFeedInput
import com.rometools.rome.io.XmlReader
import javafx.collections.FXCollections
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.control.ComboBox
import javafx.scene.control.Label
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.Executors


/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
class SyndicationFeedService(override val controller: ControllerInterface, config: SyndicationFeedConfig) :
	Service<SyndicationFeedConfig>(config) {

	companion object {
		val LOGGER: Logger = LoggerFactory.getLogger(SyndicationFeedService::class.java)
	}

	override val definition: Module<SyndicationFeedConfig, ModuleInstanceInterface<SyndicationFeedConfig>> =
		SyndicationFeedModule
	private val lastFound = mutableListOf<StatusItem>()
	private val executor = Executors.newSingleThreadExecutor()

	private val queryTableSettingsPlugin = TableSettingsPlugin(
		tableName = "filterTable",
		newLabel = "Filter name",
		buttonText = "Add filter",
		controller = controller,
		config = config,
		createItem = { SyndicationFeedFilterConfig() },
		items = config.filters,
		displayName = { name },
		columnDefinitions = listOf(
			TableSettingsPlugin.ColumnDefinition(
				name = "Name",
				getter = { name },
				setter = { name = it },
				initialValue = "",
				isEditable = true),
			TableSettingsPlugin.ColumnDefinition(
				name = "Title Filter",
				getter = { titleFilter },
				setter = { titleFilter = it },
				initialValue = "",
				isEditable = true),
			TableSettingsPlugin.ColumnDefinition(
				name = "Summary Filter",
				getter = { summaryFilter },
				setter = { summaryFilter = it },
				initialValue = "",
				isEditable = true),
			TableSettingsPlugin.ColumnDefinition(
				name = "Priority",
				getter = { priority },
				setter = { priority = it },
				initialValue = Priority.NONE,
				isEditable = true,
				icon = { toIcon() },
				options = { Priority.values().toList() }),
			TableSettingsPlugin.ColumnDefinition(
				name = "Status",
				getter = { status },
				setter = { status = it },
				initialValue = Status.NONE,
				isEditable = true,
				icon = { toIcon() },
				options = { Status.values().toList() })),
		comparator = compareBy({ -it.priority.ordinal }, { -it.status.ordinal }, { it.name }))

	override val configurationRows: List<Pair<Node, Node>>
		get() = super.configurationRows + listOf(
			Label("Default status").apply {
				maxWidth = Double.MAX_VALUE
				maxHeight = Double.MAX_VALUE
				alignment = Pos.CENTER_RIGHT
			} to ComboBox<Status>(FXCollections.observableList(Status.values().toList())).apply {
				factory { item, empty ->
					text = item?.takeUnless { empty }?.name
					graphic = item?.takeUnless { empty }?.toIcon()?.toImageView()
				}
				selectionModel.select(config.status)
				selectionModel.selectedItemProperty().addListener { _, _, value ->
					config.status = value
					controller.saveConfig()
				}
			},
			queryTableSettingsPlugin.rowNewItem)

	override val configurationAddition: List<Parent>
		get() = super.configurationAddition + listOf(queryTableSettingsPlugin.vbox)

	override fun check(updater: (Collection<StatusItem>) -> Unit) {
		if (config.enabled && config.url.isNotEmpty()) executor.submit {
			var errorStatus: StatusItem? = null
			val proxy = config.proxy
				?.let { it.address?.let { address -> address to (it.port ?: 80) } }
				?.let { (address, port) -> Proxy(Proxy.Type.HTTP, InetSocketAddress(address, port)) }

			val feed = config.url
				.toUriOrNull()
				?.toURL()
				?.let {
					try {
						if (proxy != null) it.openConnection(proxy) else it.openConnection()
					} catch (e: Exception) {
						LOGGER.error(e.message, e)
						errorStatus = getErrorStatus(Status.CONNECTION_ERROR, e.message ?: "Connection error")
						null
					}
				}
				?.also { connection ->
					try {
						if (proxy != null) connection.setRequestProperty("Proxy-Connection", "Keep-Alive")
						config.proxy?.auth?.toHeaderString()
							?.also { connection.setRequestProperty("Proxy-Authorization", it) }
						config.auth?.toHeaderString()?.also { connection.setRequestProperty("Authorization", it) }
					} catch (e: Exception) {
						LOGGER.error(e.message, e)
						errorStatus = getErrorStatus(Status.CONNECTION_ERROR, e.message ?: "Header error")
					}
				}
				?.let {
					try {
						SyndFeedInput().build(XmlReader(it))
					} catch (e: Exception) {
						LOGGER.error(e.message, e)
						errorStatus = getErrorStatus(Status.SERVICE_ERROR, e.message ?: "Parsing error")
						null
					}
				}

			if (feed != null) {
				lastFound.clear()
				for (entry in feed.entries) {
					val (priority, status) = config.filters
						.asSequence()
						.filter { it.titleFilter.toRegex().containsMatchIn(entry.title) }
						.filter { it.summaryFilter.toRegex().containsMatchIn(entry.description.value) }
						.firstOrNull()
						?.let { it.priority to it.status }
						?: (config.priority to config.status)

					val statusItem = StatusItem(
						id = "${config.uuid}|${entry.title}",
						serviceId = config.uuid,
						priority = priority,
						status = status,
						title = "[${feed.title ?: config.name}] ${entry.title}",
						group = null,
						detail = entry.description?.value ?: entry.contents?.mapNotNull { it.value }?.firstOrNull()
						?: "",
						labels = (entry.categories.map { it.name to "" } +
								entry.authors.map { a -> "Author" to "${a.name}${a.email?.let { " (${it})" }}" } +
								entry.contributors.map { a -> "Contributor" to "${a.name}${a.email?.let { " (${it})" }}" })
							.toMap(),
						link = entry.link,
						startedAt = entry.updatedDate?.toInstant() ?: entry.publishedDate?.toInstant())
					lastFound.add(statusItem)
				}
				updater(lastFound + listOfNotNull(errorStatus))
			}
		}
	}

	private fun getErrorStatus(status: Status, error: String) = StatusItem(
		id = "${config.uuid}-error",
		serviceId = config.uuid,
		priority = config.priority,
		status = status,
		title = "[${config.name}] ${error}",
		isRepeatable = false)
}