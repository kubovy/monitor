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
import com.poterion.monitor.api.utils.toIcon
import com.poterion.monitor.data.Priority
import com.poterion.monitor.data.Status
import com.poterion.monitor.data.StatusItem
import com.poterion.monitor.data.data.SilencedStatusItem
import com.poterion.monitor.data.services.ServiceSubConfig
import com.poterion.monitor.notifiers.tabs.NotificationTabsIcon
import com.poterion.monitor.notifiers.tabs.control.NotificationTabsNotifier
import com.poterion.monitor.notifiers.tabs.data.NotificationTabsConfig
import com.poterion.utils.javafx.*
import com.poterion.utils.kotlin.containsExactly
import com.poterion.utils.kotlin.noop
import com.poterion.utils.kotlin.toUriOrNull
import javafx.application.Platform
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.collections.ListChangeListener
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.control.*
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

	@FXML private lateinit var comboboxService: ComboBox<String?>
	@FXML private lateinit var comboboxConfiguration: ComboBox<String?>
	@FXML private lateinit var comboboxStatus: ComboBox<Status>
	@FXML private lateinit var comboboxPriority: ComboBox<Priority>
	@FXML private lateinit var checkboxShowWatched: CheckBox
	@FXML private lateinit var checkboxShowSilenced: CheckBox
	@FXML private lateinit var buttonRefresh: Button
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

	private val serviceSubConfigChangeListener = ListChangeListener<ServiceSubConfig> { change ->
		while (change.next()) if (change.wasRemoved()) change.removed.forEach { removed ->
			if (config.selectedConfiguration == removed.configTitle) {
				config.selectedConfiguration = null
			}
			comboboxConfiguration.items.removeIf { it == removed.configTitle }
		}
	}

	private var cleared = false

	@FXML
	fun initialize() {
		buttonRefresh.graphic = CommonIcon.REFRESH.toImageView()
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
			val service = item?.takeUnless { empty }?.let { uuid -> controller.services.find { it.config.uuid == uuid } }
			graphic = service?.definition?.icon?.toImageView()
			text = service?.config?.name ?: "All services".takeUnless { empty }
		}
		comboboxService.valueProperty().bindBidirectional(config.selectedServiceIdProperty)
		comboboxService.selectionModel.selectedItemProperty().addListener { _, _, _ ->
			config.selectedConfiguration = null
			refreshUI()
			refreshTable()
			controller.saveConfig()
		}

		comboboxConfiguration.factory { item, empty ->
			text = (item?.takeUnless { it.isEmpty() } ?: "All configurations").takeUnless { empty }
		}
		comboboxConfiguration.valueProperty().bindBidirectional(config.selectedConfigurationProperty)
		comboboxConfiguration.selectionModel.selectedItemProperty().addListener { _, _, _ ->
			refreshTable()
			controller.saveConfig()
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

		controller.applicationConfiguration.services.forEach { it.subConfig.addListener(serviceSubConfigChangeListener) }
		controller.applicationConfiguration.services.addListener(ListChangeListener { change ->
			while (change.next()) when {
				change.wasAdded() -> change.addedSubList.forEach { service ->
					service.subConfig.addListener(serviceSubConfigChangeListener)
				}
				change.wasRemoved() -> change.removed.forEach { service ->
					service.subConfig.removeListener(serviceSubConfigChangeListener)
					if (config.selectedServiceId == service.uuid) {
						comboboxService.selectionModel.select(null)
						config.selectedServiceId = null
						config.selectedConfiguration = null
					}
					comboboxService.items.removeIf { it == service?.uuid }
				}
			}
		})

		refreshUI()
		refreshTable()
	}

	private fun refreshUI() {
		val servicesIds = listOf(null) + config.services.map { it.uuid }
		if (!comboboxService.items.containsExactly(servicesIds)) comboboxService.items.setAll(servicesIds)

		val configs = listOf(null) + (comboboxService.selectionModel.selectedItem
				?.let { controller.applicationConfiguration.serviceMap[it] }
				?.subConfig
				?.map { it.configTitle }
				?.distinct()
				?.sorted()
				?: emptyList())
		if (!comboboxConfiguration.items.containsExactly(configs)) comboboxConfiguration.items.setAll(configs)
		config.selectedConfiguration = config.selectedConfiguration?.takeIf { configs.contains(it) }
				?: configs.firstOrNull()

		val statuses = Status.values().filter { it.ordinal >= config.minStatus.ordinal }
		if (!comboboxStatus.items.containsExactly(statuses)) comboboxStatus.items.setAll(statuses)
		config.selectedStatus = config.selectedStatus?.takeIf { statuses.contains(it) }
				?: config.minStatus.takeIf { statuses.contains(it) }
						?: statuses.firstOrNull()

		val priorities = Priority.values().filter { it.ordinal >= config.minPriority.ordinal }
		if (!comboboxPriority.items.containsExactly(priorities)) comboboxPriority.items.setAll(priorities)
		config.selectedPriority = config.selectedPriority?.takeIf { priorities.contains(it) }
				?: config.minPriority.takeIf { priorities.contains(it) }
						?: priorities.firstOrNull()
	}

	@FXML
	fun onRefresh() {
		val services = notifier.selectedServices.filter { it.config.enabled }
		if (services.isNotEmpty()) {
			buttonRefresh.text = "Refreshing ..."
			buttonRefresh.isDisable = true
			var count = 0
			services.forEach { service ->
				count++
				service.refreshProperty.addListener(object : ChangeListener<Boolean> {
					override fun changed(observable: ObservableValue<out Boolean>, previous: Boolean, value: Boolean) {
						if (!value) {
							count--
							observable.removeListener(this)
						}
						if (count <= 0) Platform.runLater {
							buttonRefresh.isDisable = false
							buttonRefresh.text = "Refresh Tab"
						}
					}
				})
				service.refresh = true
			}
		}
	}

	fun clear() {
		treeTableAlerts.selectionModel.clearSelection()
		treeTableAlerts.root.children.clear()
		treeTableAlerts.refresh()
		cleared = true
	}

	fun update(statusItems: Collection<StatusItem>) {
		var changed = cleared
		cleared = false
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
		val ids = treeTableAlerts.selectionModel.selectedItems?.mapNotNull { it?.value?.id }
		if (ids != null) config.watchedItems.also { items ->
			if (isWatched) items.removeAll(ids) else items.addAll(ids.filterNot { items.contains(it) })
		}
		if (refresh) refreshTable()
		if (save) controller.saveConfig()
	}

	private fun StatusItem.toggleSilence(untilChanged: Boolean, refresh: Boolean = true, save: Boolean = true) {
		val ids = treeTableAlerts.selectionModel.selectedItems?.mapNotNull { it?.value?.id }
		if (ids != null) controller.applicationConfiguration.silencedMap.also { map ->
			ids.forEach { id ->
				if (isSilenced) map.remove(id)
				else map[id] = SilencedStatusItem(item = this, lastChange = startedAt, untilChanged = untilChanged)
			}
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
