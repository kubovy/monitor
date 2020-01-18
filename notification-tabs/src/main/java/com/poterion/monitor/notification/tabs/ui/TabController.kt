package com.poterion.monitor.notification.tabs.ui

import com.poterion.monitor.api.StatusCollector
import com.poterion.monitor.api.controllers.ControllerInterface
import com.poterion.monitor.api.lib.toIcon
import com.poterion.monitor.api.lib.toImageView
import com.poterion.monitor.api.utils.cell
import com.poterion.monitor.api.utils.setOnItemClick
import com.poterion.monitor.api.utils.toUriOrNull
import com.poterion.monitor.data.Priority
import com.poterion.monitor.data.Status
import com.poterion.monitor.data.StatusItem
import com.poterion.monitor.notification.tabs.data.NotificationTabsConfig
import javafx.application.Platform
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
import java.util.concurrent.TimeUnit


class TabController {
	companion object {
		internal fun getRoot(controller: ControllerInterface, config: NotificationTabsConfig): Parent =
				FXMLLoader(TabController::class.java.getResource("tab.fxml"))
						.let { it.load<Parent>() to it.getController<TabController>() }
						.let { (root, ctrl) ->
							ctrl.controller = controller
							ctrl.config = config
							root.userData = ctrl
							root
						}
	}

	@FXML private lateinit var treeTableAlerts: TreeTableView<StatusItem>
	@FXML private lateinit var  columnAlertsTitle: TreeTableColumn<StatusItem, String>
	@FXML private lateinit var  columnAlertsService: TreeTableColumn<StatusItem, String>
	@FXML private lateinit var  columnAlertsPriority: TreeTableColumn<StatusItem, Priority>
	@FXML private lateinit var  columnAlertsLabels: TreeTableColumn<StatusItem, Map<String, String>>
	@FXML private lateinit var  columnAlertsStarted: TreeTableColumn<StatusItem, Instant>

	private lateinit var controller: ControllerInterface
	private lateinit var config: NotificationTabsConfig

	private var labelColors = listOf(
			"#FFCCCC" to "#FF6666",
			"#CCFFCC" to "#66FF66",
			"#CCCCFF" to "#6666FF",
			"#FFFFCC" to "#FFFF66",
			"#FFCCFF" to "#FF66FF",
			"#CCFFFF" to "#66FFFF")
	private var labelColorMap = mutableMapOf<String, Pair<String, String>>()

	private val tableAlertComparator: Comparator<in TreeItem<StatusItem>> = compareBy(
			{ if (it.value.priority == Priority.NONE) 1 else 0 },
			{ it.value.groupOrder },
			{ -it.value.status.ordinal },
			{ -it.value.priority.ordinal },
			{ -it.value.startedAt.epochSecond },
			{ it.value.serviceName },
			{ it.value.title })

	@FXML
	fun initialize() {
		treeTableAlerts.root = TreeItem(StatusItem("ROOT", Priority.NONE, Status.NONE, ""))
		treeTableAlerts.isShowRoot = false
		treeTableAlerts.setOnItemClick { item, event ->
			if (event.clickCount == 2 && !isEmpty) item?.link?.toUriOrNull()?.also { Desktop.getDesktop().browse(it) }
		}
		columnAlertsTitle.cell("title") { item, value, empty ->
			text = value?.takeUnless { empty }
			graphic = item?.status?.takeUnless { empty }?.toIcon()?.toImageView()
			tooltip = item?.detail?.takeUnless { empty }?.let { Tooltip(it) }
			style = when {
				index == 0 -> "-fx-font-weight: bold;"
				item?.priority == Priority.NONE -> "-fx-text-fill: #999; -fx-font-style: italic;"
				else -> null
			}
		}
		columnAlertsService.cell("serviceName") { item, value, empty ->
			text = value?.takeUnless { empty }
			style = when {
				index == 0 -> "-fx-font-weight: bold;"
				item?.priority == Priority.NONE -> "-fx-text-fill: #999; -fx-font-style: italic;"
				else -> null
			}
		}
		columnAlertsPriority.cell("priority") { item, value, empty ->
			text = null
			graphic = value?.takeUnless { empty }?.toIcon()?.toImageView()
			tooltip = Tooltip(value?.name)
			style = when {
				index == 0 -> "-fx-font-weight: bold;"
				item?.priority == Priority.NONE -> "-fx-text-fill: #999; -fx-font-style: italic;"
				else -> null
			}
		}
		columnAlertsLabels.cell("labels") { _, value, _ ->
			//text = value?.takeUnless { empty }?.map { (k, v) -> "${k}: ${v}" }?.joinToString(", ")
			val labels = value?.entries?.sortedBy { (k, _) -> k }?.map { (k, v) ->
				Label("${k}: ${v}").apply {
					val (background, border) = labelColorMap
							.getOrPut(k, { labelColors[labelColorMap.size % labelColors.size] })
					style = " -fx-text-fill: #000;" +
							" -fx-border-color: ${border};" +
							" -fx-background-color: ${background};" +
							" -fx-border-radius: 5px;" +
							" -fx-background-radius: 5px;" +
							" -fx-padding: 0px 2px;"
					tooltip = Tooltip("${k}: ${v}")
				}
			} ?: emptyList()
			graphic = HBox(2.0, *labels.toTypedArray())
					.apply {
						minWidth = Region.USE_COMPUTED_SIZE
						minHeight = Region.USE_COMPUTED_SIZE
						prefWidth = Region.USE_COMPUTED_SIZE
						prefHeight = Region.USE_COMPUTED_SIZE
						maxWidth = Double.MAX_VALUE
						maxHeight = Double.MAX_VALUE
						//style = "-fx-border-color: red"
					}
		}
		columnAlertsStarted.cell("startedAt") { item, value, empty ->
			text = if (empty) null else DateTimeFormatter
					.ofPattern("YYYY-MM-dd HH:mm:ss")
					.withZone(ZoneId.systemDefault())
					.format(value)
			style = when {
				index == 0 -> "-fx-font-weight: bold;"
				item?.priority == Priority.NONE -> "-fx-text-fill: #999; -fx-font-style: italic;"
				else -> null
			}
		}
	}

	fun update(statusItems: Collection<StatusItem>) {
		Platform.runLater {
			treeTableAlerts.root.children.setAll(statusItems.map { TreeItem(it) })
			treeTableAlerts.root.children.sortWith(tableAlertComparator)
		}
	}

	private val StatusItem.groupOrder: Int
		get() = if (serviceName == "" && priority == Priority.NONE && status == Status.NONE && title == "") 0 else 1
}
