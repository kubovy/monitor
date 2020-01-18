package com.poterion.monitor.notification.tabs.control

import com.poterion.monitor.api.StatusCollector
import com.poterion.monitor.api.controllers.ControllerInterface
import com.poterion.monitor.api.controllers.ModuleInstanceInterface
import com.poterion.monitor.api.controllers.Notifier
import com.poterion.monitor.api.lib.toIcon
import com.poterion.monitor.api.lib.toImageView
import com.poterion.monitor.api.modules.Module
import com.poterion.monitor.api.ui.CommonIcon
import com.poterion.monitor.api.utils.cell
import com.poterion.monitor.api.utils.factory
import com.poterion.monitor.data.Status
import com.poterion.monitor.data.StatusItem
import com.poterion.monitor.data.notifiers.NotifierAction
import com.poterion.monitor.data.services.ServiceConfig
import com.poterion.monitor.notification.tabs.NotificationTabsModule
import com.poterion.monitor.notification.tabs.data.NotificationTabsConfig
import com.poterion.monitor.notification.tabs.ui.TabController
import javafx.application.Platform
import javafx.collections.FXCollections
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.control.*
import javafx.scene.input.KeyCode
import javafx.scene.layout.HBox
import javafx.scene.layout.Region
import javafx.scene.layout.VBox
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
class NotificationTabsNotifier(override val controller: ControllerInterface, config: NotificationTabsConfig) : Notifier<NotificationTabsConfig>(config) {
	companion object {
		val LOGGER: Logger = LoggerFactory.getLogger(NotificationTabsNotifier::class.java)
	}

	override val definition: Module<NotificationTabsConfig, ModuleInstanceInterface<NotificationTabsConfig>> = NotificationTabsModule

	override val configurationRows: List<Pair<Node, Node>>?
		get() = listOf(
				Label("Minimum status").apply {
					maxWidth = Double.MAX_VALUE
					maxHeight = Double.MAX_VALUE
					alignment = Pos.CENTER_RIGHT
				} to ComboBox<Status>(FXCollections.observableList(Status.values().toList())).apply {
					factory { item, empty ->
						text = item?.takeUnless { empty }?.name
						graphic = item?.takeUnless { empty }?.toIcon()?.toImageView()
					}
					maxHeight = Double.MAX_VALUE
					selectionModel.select(config.minStatus)
					selectionModel.selectedItemProperty().addListener { _, _, value ->
						config.minStatus = value
						controller.saveConfig()
					}
				},
				Label("Service").apply {
					maxWidth = Double.MAX_VALUE
					maxHeight = Double.MAX_VALUE
					alignment = Pos.CENTER_RIGHT
				} to HBox(comboboxNewServiceConfig, Button("Add").apply { setOnAction { addService() } }))

	override val configurationAddition: List<Parent>?
		get() = listOf(VBox(tableServiceConfigs).apply { VBox.setVgrow(this, javafx.scene.layout.Priority.ALWAYS) })


	private var tabController: TabController? = null

	override var configurationTab: Parent? = null
		get() {
			if (field == null || tabController == null) {
				val (parent, ctrl) = TabController.getRoot(controller, config)
				field = parent
				tabController = ctrl
			}
			return field
		}
		private set

	private val unusedServiceConfigs: Collection<ServiceConfig>
		get() = controller
				.applicationConfiguration
				.services
				.filterKeys { !config.services.contains(it) }
				.values
				.sortedBy { it.name }

	private val comboboxNewServiceConfig = ComboBox<ServiceConfig>().apply {
		HBox.setHgrow(this, javafx.scene.layout.Priority.ALWAYS)
		items.setAll(unusedServiceConfigs)
		factory { item, empty ->
			graphic = controller
					.takeUnless { empty }
					?.modules
					?.find { module -> module.configClass == item?.let { it::class } }
					?.icon
					?.toImageView()
			text = item?.takeUnless { empty }?.name
		}
	}

	private val tableServiceConfigs = TableView<ServiceConfig>().apply {
		minWidth = Region.USE_COMPUTED_SIZE
		minHeight = Region.USE_COMPUTED_SIZE
		prefWidth = Region.USE_COMPUTED_SIZE
		prefHeight = Region.USE_COMPUTED_SIZE
		maxWidth = Double.MAX_VALUE
		maxHeight = Double.MAX_VALUE
		VBox.setVgrow(this, javafx.scene.layout.Priority.ALWAYS)
		setOnKeyReleased { event ->
			when (event.code) {
				KeyCode.DELETE -> selectionModel.selectedItem?.also { removeLabel(it) }
				else -> {
					// Nothing to do
				}
			}
		}
	}

	private val tableColumnServiceName = TableColumn<ServiceConfig, String>("Service Name").apply {
		isSortable = false
		minWidth = 150.0
		prefWidth = Region.USE_COMPUTED_SIZE
		maxWidth = Double.MAX_VALUE
		cell("name") { item, value, empty ->
			graphic = controller
					.takeUnless { empty }
					?.modules
					?.find { module -> module.configClass == item?.let { it::class } }
					?.icon
					?.toImageView()
			text = value?.takeUnless { empty }
		}
	}

	private val tableColumnAction = TableColumn<ServiceConfig, Status>("").apply {
		isSortable = false
		minWidth = 48.0
		prefWidth = Region.USE_COMPUTED_SIZE
		maxWidth = Region.USE_PREF_SIZE
		cell { item, _, empty ->
			graphic = Button("", CommonIcon.TRASH.toImageView()).takeUnless { empty }?.apply {
				setOnAction { item?.also { removeLabel(it) } }
			}
		}
	}

	init {
		tableServiceConfigs.columns.addAll(tableColumnServiceName, tableColumnAction)
		tableServiceConfigs.items.addAll(controller.applicationConfiguration.services.filterKeys { config.services.contains(it) }.values)
	}


	override fun initialize() {
		StatusCollector.status.sample(10, TimeUnit.SECONDS).subscribe {
			Platform.runLater { update(it.items) }
		}
	}

	override fun execute(action: NotifierAction): Unit = when (action) {
		NotifierAction.ENABLE -> {
			config.enabled = true
			controller.saveConfig()
		}
		NotifierAction.DISABLE -> {
			config.enabled = false
			controller.saveConfig()
		}
		NotifierAction.TOGGLE -> execute(if (config.enabled) NotifierAction.DISABLE else NotifierAction.ENABLE)
		else -> LOGGER.debug("Executing action ${action}")
	}

	private fun update(statusItems: Collection<StatusItem>) {
		comboboxNewServiceConfig.items.setAll(unusedServiceConfigs) // TODO
		val items = statusItems
				.filter { it.priority >= config.minPriority }
				.filter { it.status >= config.minStatus }
				.filter { config.services.isEmpty() || config.services.contains(it.serviceId) }
		tabController?.update(items)
	}

	private fun addService() {
		comboboxNewServiceConfig.selectionModel.selectedItem?.also {
			tableServiceConfigs.items.add(it)
			config.services.add(it.uuid)
			controller.saveConfig()
		}
		comboboxNewServiceConfig.items.setAll(unusedServiceConfigs)
		comboboxNewServiceConfig.selectionModel.select(null)
	}

	private fun removeLabel(serviceConfig: ServiceConfig) {
		Alert(Alert.AlertType.CONFIRMATION).apply {
			title = "Delete confirmation"
			headerText = "Delete confirmation"
			contentText = "Do you really want to delete label ${serviceConfig.name}?"
			buttonTypes.setAll(ButtonType.YES, ButtonType.NO)
		}.showAndWait().ifPresent {
			it.takeIf { it == ButtonType.YES }?.also {
				tableServiceConfigs.items.remove(serviceConfig)
				config.services.remove(serviceConfig.uuid)
				controller.saveConfig()
				comboboxNewServiceConfig.items.setAll(unusedServiceConfigs)
			}
		}
	}
}