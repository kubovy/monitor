/******************************************************************************
 * Copyright (C) 2020 Jan Kubovy (jan@kubovy.eu)                              *
 *                                                                            *
 * This program is free software: you can redistribute it and/or modify       *
 * it under the terms of the GNU General Public License as published by       *
 * the Free Software Foundation, either version 3 of the License, or          *
 * (at your option) any later version.                                        *
 *                                                                            *
 * This program is distributed in the hope that it will be useful,            *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of             *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the              *
 * GNU General Public License for more details.                               *
 *                                                                            *
 * You should have received a copy of the GNU General Public License          *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.      *
 ******************************************************************************/
package com.poterion.monitor.notifiers.tabs.ui

import com.poterion.monitor.api.CommonIcon
import com.poterion.monitor.api.controllers.ControllerInterface
import com.poterion.monitor.api.controllers.Service
import com.poterion.monitor.api.utils.toIcon
import com.poterion.monitor.data.Priority
import com.poterion.monitor.data.Status
import com.poterion.monitor.data.StatusItem
import com.poterion.monitor.data.data.SilencedStatusItem
import com.poterion.monitor.data.services.ServiceConfig
import com.poterion.monitor.data.services.ServiceSubConfig
import com.poterion.monitor.notifiers.tabs.NotificationTabsIcon
import com.poterion.monitor.notifiers.tabs.control.NotificationTabsNotifier
import com.poterion.monitor.notifiers.tabs.data.NotificationTabsConfig
import com.poterion.utils.javafx.cell
import com.poterion.utils.javafx.factory
import com.poterion.utils.javafx.monitorExpansion
import com.poterion.utils.javafx.openInExternalApplication
import com.poterion.utils.javafx.setOnItemClick
import com.poterion.utils.javafx.toImageView
import com.poterion.utils.kotlin.containsExactly
import com.poterion.utils.kotlin.noop
import com.poterion.utils.kotlin.toUriOrNull
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.control.Button
import javafx.scene.control.CheckBox
import javafx.scene.control.ComboBox
import javafx.scene.control.ContextMenu
import javafx.scene.control.Label
import javafx.scene.control.MenuItem
import javafx.scene.control.SelectionMode
import javafx.scene.control.Tooltip
import javafx.scene.control.TreeItem
import javafx.scene.control.TreeTableColumn
import javafx.scene.control.TreeTableView
import javafx.scene.input.KeyCode
import javafx.scene.layout.HBox
import javafx.scene.layout.Region
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
class TabController {
	companion object {
		internal fun getRoot(notifier: NotificationTabsNotifier): Pair<Parent, TabController> =
				FXMLLoader(TabController::class.java.getResource("tab.fxml"))
						.let { it.load<Parent>() to it.getController<TabController>() }
						.let { (root, ctrl) ->
							ctrl.notifier = notifier
							root.userData = ctrl
							ctrl.start()
							root to ctrl
						}
	}

	@FXML private lateinit var comboboxService: ComboBox<Service<ServiceConfig<out ServiceSubConfig>>?>
	@FXML private lateinit var comboboxConfiguration: ComboBox<String?>
	@FXML private lateinit var comboboxStatus: ComboBox<Status>
	@FXML private lateinit var comboboxPriority: ComboBox<Priority>
	@FXML private lateinit var checkboxShowWatched: CheckBox
	@FXML private lateinit var checkboxShowSilenced: CheckBox
	@FXML private lateinit var btnRefresh: Button
	@FXML private lateinit var treeTableAlerts: TreeTableView<StatusItem>
	@FXML private lateinit var columnAlertsTitle: TreeTableColumn<StatusItem, String>
	@FXML private lateinit var columnAlertsService: TreeTableColumn<StatusItem, String>
	@FXML private lateinit var columnAlertsConfig: TreeTableColumn<StatusItem, List<String>>
	@FXML private lateinit var columnAlertsPriority: TreeTableColumn<StatusItem, Priority>
	@FXML private lateinit var columnAlertsLabels: TreeTableColumn<StatusItem, Map<String, String>>
	@FXML private lateinit var columnAlertsStarted: TreeTableColumn<StatusItem, Instant>

	private lateinit var notifier: NotificationTabsNotifier
	private val controller: ControllerInterface
		get() = notifier.controller
	private val config: NotificationTabsConfig
		get() = notifier.config
	private val expandedCache: MutableMap<String, Boolean> = mutableMapOf()
	private val statusItemCache: MutableMap<String?, MutableCollection<StatusItem>> = mutableMapOf()

	private val StatusItem.isWatched: Boolean
		get() = config.watchedItems.contains(id)

	private val StatusItem.isSilenced: Boolean
		get() = controller.applicationConfiguration.silencedMap.keys.contains(id)

	private val filtered: Collection<StatusItem>
		get() = statusItemCache
				.getOrDefault(null, mutableListOf())
				.asSequence()
				.filter { item -> notifier.selectedServices.map { it.config.uuid }.contains(item.serviceId) }
				.filter { item ->
					config.showWatched && item.isWatched
							|| (config.showSilenced || !item.isSilenced)
							&& config.selectedStatus?.ordinal?.let { it <= item.status.ordinal } != false
							&& config.selectedPriority?.ordinal?.let { it <= item.priority.ordinal } != false
							&& config.selectedServiceId?.let { it == item.serviceId } != false
							&& config.selectedConfiguration?.let { item.configIds.contains(it) } != false
				}
				.map { if (it.isSilenced) it.copy(priority = Priority.NONE) else it }
				.toList()


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
			{ controller.applicationConfiguration.serviceMap[it.value.serviceId]?.name ?: "" },
			{ it.value.title })

	@FXML
	fun initialize() {
		btnRefresh.graphic = CommonIcon.REFRESH.toImageView()
		btnRefresh.text = ""
		treeTableAlerts.root = TreeItem(StatusItem("", "ROOT", Priority.NONE, Status.NONE, ""))
		treeTableAlerts.isShowRoot = false
		treeTableAlerts.selectionModel.selectionMode = SelectionMode.MULTIPLE
		treeTableAlerts.setOnItemClick { item, event ->
			if (event.clickCount == 2 && !isEmpty) item?.link?.toUriOrNull()?.also { it.openInExternalApplication() }
		}
		treeTableAlerts.setOnKeyPressed { event ->
			if (event.isControlDown) when (event.code) {
				KeyCode.S -> {
					val untilChanged = !event.isAltDown
					treeTableAlerts.selectionModel.selectedItems
							.forEach { it.value?.toggleSilence(untilChanged, refresh = false, save = false) }
					refreshTable()
					controller.saveConfig()
				}
				KeyCode.W -> {
					treeTableAlerts.selectionModel.selectedItems
							.forEach { it.value?.toggleWatch(refresh = false, save = false) }
					refreshTable()
					controller.saveConfig()
				}
				else -> noop()
			}
		}
	}

	private fun start() {
		comboboxService.factory { item, empty ->
			graphic = item?.takeUnless { empty }?.definition?.icon?.toImageView()
			text = item?.takeUnless { empty }?.config?.name ?: "All services".takeUnless { empty }
		}
		comboboxService.selectionModel.selectedItemProperty().addListener { _, _, value ->
			if (config.selectedServiceId != value?.config?.uuid) {
				config.selectedServiceId = value?.config?.uuid
				comboboxConfiguration.items.clear()
				comboboxConfiguration.items.add(null)
				value?.config?.subConfig?.map { it.configTitle }
						?.distinct()
						?.sorted()
						?.also { comboboxConfiguration.items.addAll(it) }
				refreshTable()
				controller.saveConfig()
			}
		}

		comboboxConfiguration.factory { item, empty ->
			text = (item?.takeUnless { it.isEmpty() } ?: "All configurations").takeUnless { empty }
		}
		comboboxConfiguration.valueProperty().bindBidirectional(config.selectedConfigurationProperty)
		comboboxConfiguration.selectionModel.selectedItemProperty().addListener { _, _, value ->
			refreshTable()
		}

		comboboxStatus.factory { item, empty ->
			graphic = item?.takeUnless { empty }?.toIcon()?.toImageView()
			text = item?.takeUnless { empty }?.name
		}
		comboboxStatus.valueProperty().bindBidirectional(config.selectedStatusProperty)
		comboboxStatus.selectionModel.selectedItemProperty().addListener { _, _, _ ->
			refreshTable()
			controller.saveConfig()
		}

		comboboxPriority.factory { item, empty ->
			graphic = item?.takeUnless { empty }?.toIcon()?.toImageView()
			text = item?.takeUnless { empty }?.name
		}
		comboboxPriority.valueProperty().bindBidirectional(config.selectedPriorityProperty)
		comboboxPriority.selectionModel.selectedItemProperty().addListener { _, _, _ ->
			refreshTable()
			controller.saveConfig()
		}

		checkboxShowWatched.selectedProperty().bindBidirectional(config.showWatchedProperty)
		checkboxShowWatched.selectedProperty().addListener { _, _, _ ->
			refreshTable()
			controller.saveConfig()
		}

		checkboxShowSilenced.selectedProperty().bindBidirectional(config.showSilencedProperty)
		checkboxShowSilenced.selectedProperty().addListener { _, _, _ ->
			refreshTable()
			controller.saveConfig()
		}

		columnAlertsTitle.prefWidthProperty().bindBidirectional(config.alertTitleWidthProperty)
		columnAlertsTitle.widthProperty().addListener { _, _, _ -> controller.saveConfig() }

		columnAlertsService.prefWidthProperty().bindBidirectional(config.alertServiceWidthProperty)
		columnAlertsService.widthProperty().addListener { _, _, value ->
			controller.saveConfig()
		}

		columnAlertsConfig.prefWidthProperty().bindBidirectional(config.alertConfigWidthProperty)
		columnAlertsConfig.widthProperty().addListener { _, _, _ -> controller.saveConfig() }

		columnAlertsLabels.prefWidthProperty().bindBidirectional(config.alertLabelsWidthProperty)
		columnAlertsLabels.widthProperty().addListener { _, _, _ -> controller.saveConfig() }

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
			val service = item.takeUnless { empty }?.let { controller.applicationConfiguration.serviceMap[it.serviceId] }
			if (service != null) {
				textProperty().bind(service.nameProperty)
			} else {
				if (textProperty().isBound) textProperty().unbind()
				text = null
			}
			style = when {
				config.watchedItems.contains(item?.id) -> "-fx-font-weight: bold; -fx-text-fill: #600;"
				index == 0 -> "-fx-font-weight: bold;"
				item?.priority == Priority.NONE -> "-fx-text-fill: #999; -fx-font-style: italic;"
				else -> null
			}
			contextMenu = item?.takeUnless { empty }?.contextMenu()
		}

		columnAlertsConfig.cell("configIds") { item, value, empty ->
			text = value?.takeUnless { empty }?.joinToString(", ")
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
			text = if (value == null || empty) null else DateTimeFormatter
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
		refreshUI()
		refreshTable()
	}

	private fun refreshUI() {
		val selectedServiceId = config.selectedServiceId
		val services = listOf(null) + notifier.selectedServices
		if (!comboboxService.items.containsExactly(services) { it?.config }) {
			comboboxService.items.setAll(listOf(null) + notifier.selectedServices)
		}
		notifier.selectedServices
				.find { it.config.uuid == selectedServiceId }
				.also { service ->
					if (comboboxService.selectionModel?.selectedItem?.config?.uuid != service?.config?.uuid) {
						if (service == null) comboboxService.selectionModel.clearSelection()
						else comboboxService.selectionModel.select(service)
					}
				}

		val selectedStatus = config.selectedStatus ?: config.minStatus
		val statuses = Status.values().filter { it.ordinal >= config.minStatus.ordinal }
		if (!comboboxStatus.items.containsExactly(statuses)) {
			comboboxStatus.items.setAll(statuses)
		}
		if (comboboxStatus.selectionModel.selectedItem != selectedStatus) {
			comboboxStatus.selectionModel.select(selectedStatus)
		}

		val selectedPriority = config.selectedPriority ?: config.minPriority
		val priorities = Priority.values().filter { it.ordinal >= config.minPriority.ordinal }
		if (!comboboxPriority.items.containsExactly(priorities)) {
			comboboxPriority.items.setAll(priorities)
		}
		if (comboboxPriority.selectionModel.selectedItem != selectedPriority) {
			comboboxPriority.selectionModel.select(selectedPriority)
		}

		if (checkboxShowWatched.isSelected != config.showWatched) {
			checkboxShowWatched.isSelected = config.showWatched
		}
		if (checkboxShowSilenced.isSelected != config.showSilenced) {
			checkboxShowSilenced.isSelected = config.showSilenced
		}
	}

	@FXML
	fun onRefresh() {
		notifier.selectedServices.forEach { it.refresh = true }
	}

	fun update(statusItems: Collection<StatusItem>) {
		var changed = false
		for ((parentId, children) in statusItems.groupBy { it.parentId }.entries) {
			if (statusItemCache.containsKey(parentId)) {
				for (child in children) {
					if (!statusItemCache.getOrDefault(parentId, mutableListOf()).contains(child)) {
						statusItemCache.getOrPut(parentId, { mutableListOf() }).also { cache ->
							if (!cache.contains(child)) {
								cache.removeIf { it.id == child.id }
								changed = child.buildChildren(statusItems) || changed
								cache.add(child)
								changed = true
							}
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
				children.forEach { it.buildChildren(statusItems) }
				changed = true
			}
		}

		refreshUI()
		if (changed) refreshTable()
	}

	private fun StatusItem.buildChildren(statusItems: Collection<StatusItem>): Boolean {
		var changed = false
		for (child in children.mapNotNull { id -> statusItems.find { it.id == id } }) {
			val cache = statusItemCache.getOrPut(id, { mutableListOf() })
			if (!cache.contains(child)) {
				cache.removeIf { it.id == child.id }
				cache.add(child)
				changed = true
			}
		}
		return changed
	}

	private fun refreshTable() {
		val selectedStatusItem = treeTableAlerts.selectionModel.selectedItem
		val treeItems = filtered.map { TreeItem(it).monitorExpansion() }
		treeItems.forEach { it.buildChildren() }
		treeTableAlerts.root.children.setAll(treeItems.toList())
		treeTableAlerts.root.children.sortWith(tableAlertComparator)
		treeTableAlerts.refresh()
		treeTableAlerts.selectionModel.select(selectedStatusItem)
	}

	private fun TreeItem<StatusItem>.buildChildren(depth: Int = 0) {
		if (depth < 5) { // TODO Hardcoded limit
			val items = statusItemCache[value.id]
			if (items != null) {
				children.setAll(items.map { TreeItem(it).monitorExpansion() })
				children.sortWith(tableAlertComparator)
				children.forEach { it.buildChildren(depth + 1) }
			}
		}
	}

	private fun StatusItem.contextMenu() = ContextMenu(*listOfNotNull(
			MenuItem(if (isWatched) "Unwatch [Ctrl+W]" else "Watch [Ctrl+W]",
					(if (isWatched) NotificationTabsIcon.UNWATCH else NotificationTabsIcon.WATCH).toImageView())
					.also { it.setOnAction { toggleWatch() } },
			MenuItem(if (isSilenced) "Unsilence [Ctrl+S]" else "Silence [Ctrl+Alt+S]",
					(if (isSilenced) NotificationTabsIcon.UNSILENCE else NotificationTabsIcon.SILENCE).toImageView())
					.also { it.setOnAction { toggleSilence(false) } },
			MenuItem("Silence until changed [Ctrl+S]", NotificationTabsIcon.SILENCE.toImageView())
					.takeIf { !isSilenced && startedAt != null }
					?.also { it.setOnAction { toggleSilence(true) } }).toTypedArray())

	private fun StatusItem.toggleWatch(refresh: Boolean = true, save: Boolean = true) {
		config.watchedItems.also { if (isWatched) it.remove(id) else it.add(id) }
		if (refresh) refreshTable()
		if (save) controller.saveConfig()
	}

	private fun StatusItem.toggleSilence(untilChanged: Boolean, refresh: Boolean = true, save: Boolean = true) {
		controller.applicationConfiguration.silencedMap.also {
			if (isSilenced) it.remove(id)
			else it[id] = SilencedStatusItem(item = this, lastChange = startedAt, untilChanged = untilChanged)
		}
		if (refresh) refreshTable()
		if (save) controller.saveConfig()
	}

	private val StatusItem.groupOrder: Int
		get() = if ((controller.applicationConfiguration.serviceMap[serviceId]?.name ?: "") == ""
				&& priority == Priority.NONE
				&& status == Status.NONE
				&& title == "")
			0 else 1

	private fun TreeItem<StatusItem>.monitorExpansion(): TreeItem<StatusItem> = monitorExpansion(
			{ value -> expandedCache.getOrDefault(value?.id, false) },
			{ value, expanded -> value?.id?.also { expandedCache[it] = expanded } })
}
