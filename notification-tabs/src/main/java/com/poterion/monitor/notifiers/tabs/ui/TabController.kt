package com.poterion.monitor.notifiers.tabs.ui

import com.poterion.monitor.api.controllers.ControllerInterface
import com.poterion.monitor.api.controllers.Service
import com.poterion.monitor.api.lib.toIcon
import com.poterion.monitor.api.lib.toImageView
import com.poterion.monitor.api.CommonIcon
import com.poterion.monitor.api.utils.cell
import com.poterion.monitor.api.utils.factory
import com.poterion.monitor.api.utils.setOnItemClick
import com.poterion.monitor.api.utils.toUriOrNull
import com.poterion.monitor.data.Priority
import com.poterion.monitor.data.Status
import com.poterion.monitor.data.StatusItem
import com.poterion.monitor.data.serviceName
import com.poterion.monitor.notifiers.tabs.NotificationTabsIcon
import com.poterion.monitor.notifiers.tabs.data.NotificationTabsConfig
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.control.*
import javafx.scene.layout.HBox
import javafx.scene.layout.Region
import java.awt.Desktop
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
class TabController {
	companion object {
		internal fun getRoot(controller: ControllerInterface,
							 config: NotificationTabsConfig): Pair<Parent, TabController> =
				FXMLLoader(TabController::class.java.getResource("tab.fxml"))
						.let { it.load<Parent>() to it.getController<TabController>() }
						.let { (root, ctrl) ->
							ctrl.controller = controller
							ctrl.config = config
							root.userData = ctrl
							ctrl.load()
							root to ctrl
						}
	}

	@FXML private lateinit var comboboxService: ComboBox<Service<*>?>
	@FXML private lateinit var comboboxStatus: ComboBox<Status>
	@FXML private lateinit var comboboxPriority: ComboBox<Priority>
	@FXML private lateinit var checkboxShowWatched: CheckBox
	@FXML private lateinit var btnRefresh: Button
	@FXML private lateinit var treeTableAlerts: TreeTableView<StatusItem>
	@FXML private lateinit var columnAlertsTitle: TreeTableColumn<StatusItem, String>
	@FXML private lateinit var columnAlertsService: TreeTableColumn<StatusItem, String>
	@FXML private lateinit var columnAlertsPriority: TreeTableColumn<StatusItem, Priority>
	@FXML private lateinit var columnAlertsLabels: TreeTableColumn<StatusItem, Map<String, String>>
	@FXML private lateinit var columnAlertsStarted: TreeTableColumn<StatusItem, Instant>

	private lateinit var controller: ControllerInterface
	private lateinit var config: NotificationTabsConfig
	private val expandedCache: MutableMap<String, Boolean> = mutableMapOf()
	private val statusItemCache: MutableMap<String?, MutableCollection<StatusItem>> = mutableMapOf()

	private val StatusItem.isWatched: Boolean
		get() = config.showWatched && config.watchedItems.contains(id)

	private val filtered: Collection<StatusItem>
		get() = statusItemCache
				.getOrDefault(null, mutableListOf())
				.filter { item -> item.isWatched || config.selectedStatus?.ordinal?.let { it <= item.status.ordinal } ?: true }
				.filter { item -> item.isWatched || config.selectedPriority?.ordinal?.let { it <= item.priority.ordinal } ?: true }
				.filter { item -> item.isWatched || config.selectedServiceId?.let { it == item.serviceId } ?: true }


	private var labelColors = listOf(
			"#FFCCCC" to "#FF6666",
			"#CCFFCC" to "#66FF66",
			"#CCCCFF" to "#6666FF",
			"#FFFFCC" to "#FFFF66",
			"#FFCCFF" to "#FF66FF",
			"#CCFFFF" to "#66FFFF")
	private var labelColorMap = mutableMapOf<String, Pair<String, String>>()

	private val tableAlertComparator: Comparator<in TreeItem<StatusItem>> = compareBy(
			{ if (config.watchedItems.contains(it.value.id)) 0 else 1 },
			{ if (it.value.priority == Priority.NONE) 1 else 0 },
			{ it.value.groupOrder },
			{ -it.value.status.ordinal },
			{ -it.value.priority.ordinal },
			{ -(it.value.startedAt ?: Instant.now()).epochSecond },
			{ it.value.serviceName(controller.applicationConfiguration) },
			{ it.value.title })

	@FXML
	fun initialize() {
		btnRefresh.graphic = CommonIcon.REFRESH.toImageView()
		btnRefresh.text = ""
		treeTableAlerts.root = TreeItem(StatusItem("", "ROOT", Priority.NONE, Status.NONE, ""))
		treeTableAlerts.isShowRoot = false
		treeTableAlerts.setOnItemClick { item, event ->
			if (event.clickCount == 2 && !isEmpty) item?.link?.toUriOrNull()?.also { Desktop.getDesktop().browse(it) }
		}
	}

	fun load() {
		comboboxService.factory { item, empty ->
			graphic = item?.takeUnless { empty }?.definition?.icon?.toImageView()
			text = item?.takeUnless { empty }?.config?.name ?: "All services".takeUnless { empty }
		}
		comboboxService.items.setAll(listOf(null)
				+ controller.services.filter { config.services.isEmpty() || config.services.contains(it.config.uuid) })
		config.selectedServiceId?.findService().also { service ->
			if (service == null) comboboxService.selectionModel.clearSelection()
			else comboboxService.selectionModel.select(service)
		}
		comboboxService.selectionModel.selectedItemProperty().addListener { _, _, value ->
			config.selectedServiceId = value?.config?.uuid
			refreshTable()
			controller.saveConfig()
		}

		comboboxStatus.factory { item, empty ->
			graphic = item?.takeUnless { empty }?.toIcon()?.toImageView()
			text = item?.takeUnless { empty }?.name
		}
		comboboxStatus.items.setAll(Status.values().toList()) //.filter { it.ordinal >= config.minStatus.ordinal })
		comboboxStatus.selectionModel.select(config.selectedStatus ?: config.minStatus)
		comboboxStatus.selectionModel.selectedItemProperty().addListener { _, _, value ->
			config.selectedStatus = value
			refreshTable()
			controller.saveConfig()
		}

		comboboxPriority.factory { item, empty ->
			graphic = item?.takeUnless { empty }?.toIcon()?.toImageView()
			text = item?.takeUnless { empty }?.name
		}
		comboboxPriority.items.setAll(Priority.values().toList()) //.filter { it.ordinal >= config.minPriority.ordinal })
		comboboxPriority.selectionModel.select(config.selectedPriority ?: config.minPriority)
		comboboxPriority.selectionModel.selectedItemProperty().addListener { _, _, value ->
			config.selectedPriority = value
			refreshTable()
			controller.saveConfig()
		}

		checkboxShowWatched.isSelected = config.showWatched
		checkboxShowWatched.selectedProperty().addListener { _, _, value ->
			config.showWatched = value
			refreshTable()
			controller.saveConfig()
		}

		columnAlertsTitle.prefWidth = config.alertTitleWidth
		columnAlertsTitle.widthProperty().addListener { _, _, value ->
			config.alertTitleWidth = value.toDouble()
			controller.saveConfig()
		}
		columnAlertsService.prefWidth = config.alertServiceWidth
		columnAlertsService.widthProperty().addListener { _, _, value ->
			config.alertServiceWidth = value.toDouble()
			controller.saveConfig()
		}
		columnAlertsLabels.prefWidth = config.alertLabelsWidth
		columnAlertsLabels.widthProperty().addListener { _, _, value ->
			config.alertLabelsWidth = value.toDouble()
			controller.saveConfig()
		}

		columnAlertsTitle.cell("title") { item, value, empty ->
			text = value?.takeUnless { empty }
			graphic = item?.status?.takeUnless { empty }?.toIcon()?.toImageView()
			tooltip = item?.detail?.takeUnless { empty }?.let { Tooltip(it) }
			style = when {
				config.watchedItems.contains(item?.id) -> "-fx-font-weight: bold; -fx-text-fill: #600;"
				index == 0 -> "-fx-font-weight: bold;"
				item?.priority == Priority.NONE -> "-fx-text-fill: #999; -fx-font-style: italic;"
				else -> null
			}
			contextMenu = item?.takeUnless { empty }?.contextMenu()
		}

		columnAlertsService.cell("serviceId") { item, _, empty ->
			text = item?.takeUnless { empty }?.serviceName(controller.applicationConfiguration)
			style = when {
				config.watchedItems.contains(item?.id) -> "-fx-font-weight: bold; -fx-text-fill: #600;"
				index == 0 -> "-fx-font-weight: bold;"
				item?.priority == Priority.NONE -> "-fx-text-fill: #999; -fx-font-style: italic;"
				else -> null
			}
			contextMenu = item?.takeUnless { empty }?.contextMenu()
		}

		columnAlertsPriority.cell("priority") { item, value, empty ->
			text = null
			graphic = value?.takeUnless { empty }?.toIcon()?.toImageView()
			tooltip = Tooltip(value?.name)
			style = when {
				config.watchedItems.contains(item?.id) -> "-fx-font-weight: bold; -fx-text-fill: #600;"
				index == 0 -> "-fx-font-weight: bold;"
				item?.priority == Priority.NONE -> "-fx-text-fill: #999; -fx-font-style: italic;"
				else -> null
			}
			contextMenu = item?.takeUnless { empty }?.contextMenu()
		}

		columnAlertsLabels.cell("labels") { item, value, empty ->
			//text = value?.takeUnless { empty }?.map { (k, v) -> "${k}: ${v}" }?.joinToString(", ")
			val labels = value?.takeUnless { empty }?.entries?.sortedBy { (k, _) -> k }?.map { (k, v) ->
				Label(if (v.isBlank()) k else "${k}: ${v}").apply {
					val (background, border) = labelColorMap
							.getOrPut(k, { labelColors[labelColorMap.size % labelColors.size] })
					style = " -fx-text-fill: #000;" +
							" -fx-border-color: ${border};" +
							" -fx-background-color: ${background};" +
							" -fx-border-radius: 5px;" +
							" -fx-background-radius: 5px;" +
							" -fx-padding: 0px 2px;"
					tooltip = Tooltip(if (v.isBlank()) k else "${k}: ${v}")
				}
			} ?: emptyList()
			graphic = HBox(2.0, *labels.toTypedArray())
					.takeUnless { empty }
					?.apply {
						minWidth = Region.USE_COMPUTED_SIZE
						minHeight = Region.USE_COMPUTED_SIZE
						prefWidth = Region.USE_COMPUTED_SIZE
						prefHeight = Region.USE_COMPUTED_SIZE
						maxWidth = Double.MAX_VALUE
						maxHeight = Double.MAX_VALUE
						//style = "-fx-border-color: red"
					}
			contextMenu = item?.takeUnless { empty }?.contextMenu()
		}

		columnAlertsStarted.cell("startedAt") { item, value, empty ->
			text = if (empty) null else DateTimeFormatter
					.ofPattern("YYYY-MM-dd HH:mm:ss")
					.withZone(ZoneId.systemDefault())
					.format(value)
			style = when {
				config.watchedItems.contains(item?.id) -> "-fx-font-weight: bold; -fx-text-fill: #600;"
				index == 0 -> "-fx-font-weight: bold;"
				item?.priority == Priority.NONE -> "-fx-text-fill: #999; -fx-font-style: italic;"
				else -> null
			}
			contextMenu = item?.takeUnless { empty }?.contextMenu()
		}
	}

	@FXML
	fun onRefresh() {
		controller.services
				.filter { config.services.isEmpty() || config.services.contains(it.config.uuid) }
				.forEach { it.refresh = true }
	}

	fun update(statusItems: Collection<StatusItem>) {
		var changed = false
		for ((parentId, children) in statusItems.groupBy { it.parentId }.entries) {
			if (statusItemCache.containsKey(parentId)) {
				for (child in children) {
					if (!statusItemCache.getOrDefault(parentId, mutableListOf()).contains(child)) {
						statusItemCache.getOrPut(parentId, { mutableListOf() }).also { cache ->
							cache.removeIf { it.id == child.id }
							cache.add(child)
							changed = true
						}
					}
				}
				val orphanIds = statusItemCache.getOrDefault(parentId, mutableListOf())
						.filter { existing -> children.none { it.id == existing.id } }
						.map { it.id }
				if (orphanIds.isNotEmpty()) {
					statusItemCache.getOrDefault(parentId, mutableListOf()).removeIf { orphanIds.contains(it.id) }
					changed = true
				}
			} else {
				statusItemCache[parentId] = children.toMutableList()
				changed = true
			}
		}
		if (changed) refreshTable()
	}

	private fun refreshTable() {
		//val selectedStatus = comboboxStatus.selectionModel.selectedItem
		//comboboxStatus.selectionModel.clearSelection()
		//comboboxStatus.items.setAll(Status.values().filter { it.ordinal >= config.minStatus.ordinal })
		//comboboxStatus.selectionModel.select(selectedStatus?.takeIf { it.ordinal >= config.minStatus.ordinal } ?: config.minStatus)

		//val selectedPriority = comboboxPriority.selectionModel.selectedItem
		//comboboxPriority.selectionModel.clearSelection()
		//comboboxPriority.items.setAll(Priority.values().filter { it.ordinal >= config.minPriority.ordinal })
		//comboboxPriority.selectionModel.select(selectedPriority?.takeIf { it.ordinal >= config.minStatus.ordinal } ?: config.minPriority)

		val treeItems = filtered.map { TreeItem(it).monitorExpanded() }
		treeItems.map { it to statusItemCache.getOrDefault(it.value.id, mutableListOf()) }
				.map { (parent, children) -> parent to children.map { TreeItem(it).monitorExpanded() } }
				.forEach { (parent, children) ->
					parent.children.addAll(children)
					parent.children.sortWith(tableAlertComparator)
				}
		treeTableAlerts.root.children.setAll(treeItems)
		treeTableAlerts.root.children.sortWith(tableAlertComparator)
		treeTableAlerts.refresh()
	}

	private fun StatusItem.contextMenu() = ContextMenu(
			if (config.watchedItems.contains(id)) MenuItem("Unwatch", NotificationTabsIcon.UNWATCH.toImageView()).also {
				it.setOnAction {
					config.watchedItems.remove(id)
					//treeTableAlerts.refresh()
					refreshTable()
					controller.saveConfig()
				}
			} else MenuItem("Watch", NotificationTabsIcon.WATCH.toImageView()).also {
				it.setOnAction {
					config.watchedItems.add(id)
					//treeTableAlerts.refresh()
					refreshTable()
					controller.saveConfig()
				}
			},
			MenuItem("Silence", NotificationTabsIcon.SILENCE.toImageView()).also { menuItem ->
				menuItem.setOnAction {
					controller.applicationConfiguration.silenced[id] = this
					controller.saveConfig()
				}
			})

	private val StatusItem.groupOrder: Int
		get() = if (serviceName(controller.applicationConfiguration.services) == ""
				&& priority == Priority.NONE
				&& status == Status.NONE
				&& title == "")
			0 else 1


	private fun TreeItem<StatusItem>.monitorExpanded(): TreeItem<StatusItem> {
		isExpanded = expandedCache.getOrDefault(value.id, false)
		expandedProperty().addListener { _, _, expanded -> expandedCache[value.id] = expanded }
		return this
	}

	private fun String.findService(): Service<*>? = controller.services.find { it.config.uuid == this }
}
