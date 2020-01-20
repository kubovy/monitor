package com.poterion.monitor.ui

import com.poterion.monitor.api.controllers.ControllerInterface
import com.poterion.monitor.api.lib.toImageView
import com.poterion.monitor.api.CommonIcon
import com.poterion.monitor.api.utils.cell
import com.poterion.monitor.api.utils.factory
import com.poterion.monitor.data.Status
import com.poterion.monitor.data.services.ServiceConfig
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.input.KeyCode
import javafx.scene.layout.HBox
import javafx.scene.layout.Region
import javafx.scene.layout.VBox

internal class ServiceSettingsPlugin(private val controller: ControllerInterface,
									 private val serviceIds: MutableSet<String>) {

	private val unusedServiceConfigs: Collection<ServiceConfig>
		get() = controller
				.applicationConfiguration
				.services
				.filterKeys { !serviceIds.contains(it) }
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

	internal val rowService = Label("Service").apply {
		maxWidth = Double.MAX_VALUE
		maxHeight = Double.MAX_VALUE
		alignment = Pos.CENTER_RIGHT
	} to HBox(comboboxNewServiceConfig, Button("Add").apply { setOnAction { addService() } })

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

	internal val vboxServiceConfigs = VBox(tableServiceConfigs)
			.apply { VBox.setVgrow(this, javafx.scene.layout.Priority.ALWAYS) }

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
		tableServiceConfigs.items.addAll(controller.applicationConfiguration.services.filterKeys { serviceIds.contains(it) }.values)
	}

	private fun addService() {
		comboboxNewServiceConfig.selectionModel.selectedItem?.also {
			tableServiceConfigs.items.add(it)
			serviceIds.add(it.uuid)
			controller.saveConfig()
		}
		comboboxNewServiceConfig.items.setAll(unusedServiceConfigs)
		comboboxNewServiceConfig.selectionModel.clearSelection()
	}

	private fun removeLabel(serviceConfig: ServiceConfig) {
		Alert(Alert.AlertType.CONFIRMATION).apply {
			title = "Delete confirmation"
			headerText = "Delete confirmation"
			contentText = "Do you really want to delete service ${serviceConfig.name}?"
			buttonTypes.setAll(ButtonType.YES, ButtonType.NO)
		}.showAndWait().ifPresent {
			it.takeIf { it == ButtonType.YES }?.also {
				tableServiceConfigs.items.remove(serviceConfig)
				serviceIds.remove(serviceConfig.uuid)
				controller.saveConfig()
				comboboxNewServiceConfig.items.setAll(unusedServiceConfigs)
			}
		}
	}
}