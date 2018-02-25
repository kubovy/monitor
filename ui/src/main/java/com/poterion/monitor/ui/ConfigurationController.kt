package com.poterion.monitor.ui

import com.poterion.monitor.api.controllers.ControllerInterface
import com.poterion.monitor.api.controllers.ModuleInterface
import com.poterion.monitor.api.controllers.Notifier
import com.poterion.monitor.api.controllers.Service
import com.poterion.monitor.api.ui.CommonIcon
import com.poterion.monitor.data.HttpConfig
import com.poterion.monitor.data.ModuleConfig
import com.poterion.monitor.data.Priority
import com.poterion.monitor.data.auth.BasicAuthConfig
import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.geometry.HPos
import javafx.geometry.Pos
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.GridPane
import javafx.scene.layout.HBox
import javafx.scene.layout.RowConstraints
import java.util.*

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
class ConfigurationController {

	companion object {
		fun create(controller: ControllerInterface) {
			val loader = FXMLLoader(ConfigurationController::class.java.getResource("configuration.fxml"))
			val root = loader.load<Parent>()
			loader.getController<ConfigurationController>().apply {
				this.controller = controller
				this.load()
			}
			controller.stage.apply {
				CommonIcon.SETTINGS.inputStream.use { icons.add(Image(it)) }
				title = "Configuration"
				//stage.isResizable = false
				minWidth = 850.0
				minHeight = 650.0
				scene = Scene(root, 850.0, 650.0)
				show()
			}
		}
	}

	@FXML private lateinit var tabPaneMain: TabPane
	@FXML private lateinit var tabCommon: Tab
	@FXML private lateinit var tree: TreeView<ModuleItem>
	@FXML private lateinit var gridPane: GridPane

	private var controller: ControllerInterface? = null

	@FXML
	fun initialize() {
		tree.apply {
			isShowRoot = false
			selectionModel.selectionMode = SelectionMode.SINGLE
			setCellFactory {
				object : TreeCell<ModuleItem>() {
					override fun updateItem(item: ModuleItem?, empty: Boolean) {
						super.updateItem(item, empty)
						text = item?.title ?: item?.module?.config?.name
						graphic = item?.graphic ?: item?.module?.icon?.inputStream?.use { ImageView(Image(it, 16.0, 16.0, false, false)) }
					}
				}
			}
			selectionModel.selectedItemProperty().addListener { _, _, newValue ->
				select(newValue?.value?.module)
			}
		}
		select(null)
	}

	private fun load() {
		controller?.let { it.services + it.notifiers }
				?.mapNotNull { module -> module.configurationPane?.let { module.config.name to it } }
				?.sortedBy { (name, _) -> name }
				?.map { (name, configuration) -> Tab(name, configuration) }
				?.forEach { tab -> tabPaneMain.tabs.add(tab) }

		tree.root = TreeItem<ModuleItem>().apply {
			children.addAll(
					TreeItem(ModuleItem("Services", ImageView(UiIcon.SERVICES.image(16, 16)))).apply {
						controller?.services?.sortedBy { it.config.name }?.map { TreeItem(ModuleItem(module = it)) }
								?.also { children.addAll(it) }
						isExpanded = true
					},
					TreeItem(ModuleItem("Notifiers", ImageView(UiIcon.NOTIFIERS.image(16, 16)))).apply {
						controller?.notifiers?.sortedBy { it.config.name }?.map { TreeItem(ModuleItem(module = it)) }
								?.also { children.addAll(it) }
						isExpanded = true
					})
		}
	}

	private fun select(module: ModuleInterface<*>?) = gridPane.apply {
		children.clear()
		if (module != null) rowConstraints.apply {
			val rows = initializeModule(module)
			clear()
			addAll((0 until rows).map { RowConstraints(10.0, 30.0, Double.MAX_VALUE) })
		}
	}

	private fun initializeModule(module: ModuleInterface<*>): Int = gridPane.run {
		var row = 0
		addRow(row++, Label("Type").apply { GridPane.setHalignment(this, HPos.RIGHT) },
				ComboBox<String>().apply {
					GridPane.setHgrow(this, javafx.scene.layout.Priority.ALWAYS)
					val loader = ServiceLoader.load(ModuleConfig::class.java)
					items.add(null)
					items.addAll(loader.map { it::class.simpleName })
					isDisable = true
					selectionModel.apply {
						select(module.config.type)
						//selectedItemProperty().addListener { _, _, _ -> }
					}
				})
		addRow(row++, Label("Name").apply { GridPane.setHalignment(this, HPos.RIGHT) },
				TextField(module.config.name).apply {
					textProperty().addListener { _, _, value ->
						module.config.name = value
						controller?.saveConfig()
					}
				})
		addRow(row++, Label("Enabled").apply { GridPane.setHalignment(this, HPos.RIGHT) },
				CheckBox().apply {
					isSelected = module.config.enabled
					selectedProperty().addListener { _, _, value ->
						module.config.enabled = value
						controller?.saveConfig()
					}
				})

		if (module is Service) row += initializeServiceModule(row, module)
		if (module is Notifier) row += initializeNotifierModule(row, module)
		return row
	}

	private fun initializeServiceModule(row: Int, module: Service<*>): Int = gridPane.run {
		addRow(row, Label("Default priority").apply { GridPane.setHalignment(this, HPos.RIGHT) },
				ComboBox<Priority>(FXCollections.observableArrayList(*Priority.values())).apply {
					GridPane.setHgrow(this, javafx.scene.layout.Priority.ALWAYS)
					selectionModel.apply {
						select(module.config.priority)
						selectedItemProperty().addListener { _, _, value ->
							module.config.priority = value
							controller?.saveConfig()
						}
					}
				})
		addRow(row + 1, Label("Check interval").apply { GridPane.setHalignment(this, HPos.RIGHT) },
				HBox(TextField(module.config.checkInterval.toString()).apply {
					HBox.setHgrow(this, javafx.scene.layout.Priority.ALWAYS)
					textProperty().addListener { _, _, value ->
						value.toLongOrNull()?.also {
							module.config.checkInterval = it
							controller?.saveConfig()
						}
					}
				}, Label("ms")).apply { alignment = Pos.CENTER })

		return initializeHttpService(row + 2, module.config)
	}

	private fun initializeHttpService(row: Int, config: HttpConfig): Int = gridPane.run {
		addRow(row, Label("URL").apply { GridPane.setHalignment(this, HPos.RIGHT) },
				TextField(config.url).apply {
					textProperty().addListener { _, _, value ->
						config.url = value
						controller?.saveConfig()
					}
				})
		addRow(row + 1, Label("Trust certificate").apply { GridPane.setHalignment(this, HPos.RIGHT) },
				CheckBox().apply {
					isSelected = config.trustCertificate
					selectedProperty().addListener { _, _, value ->
						config.trustCertificate = value
						controller?.saveConfig()
					}
				})
		addRow(row + 2, Label("Auth").apply { GridPane.setHalignment(this, HPos.RIGHT) },
				HBox(
						TextField(config.auth?.username ?: "").apply {
							HBox.setHgrow(this, javafx.scene.layout.Priority.ALWAYS)
							textProperty().addListener { _, _, value ->
								val usr = config.auth?.username?.takeIf { it.isNotEmpty() }
								val pwd = value.takeIf { it.isNotEmpty() }
								config.auth = if (usr == null && pwd == null)
									null else (config.auth ?: BasicAuthConfig(usr ?: "", pwd ?: ""))
								controller?.saveConfig()
							}
						},
						PasswordField().apply {
							HBox.setHgrow(this, javafx.scene.layout.Priority.ALWAYS)
							text = config.auth?.password ?: ""
							textProperty().addListener { _, _, value ->
								val usr = value.takeIf { it.isNotEmpty() }
								val pwd = config.auth?.password?.takeIf { it.isNotEmpty() }
								config.auth = if (usr == null && pwd == null)
									null else (config.auth ?: BasicAuthConfig(usr ?: "", pwd ?: ""))
								controller?.saveConfig()
							}
						}).apply { alignment = Pos.CENTER })
		addRow(row + 3, Label("Connection timeout").apply { GridPane.setHalignment(this, HPos.RIGHT) },
				HBox(TextField(config.readTimeout?.toString() ?: "").apply {
					HBox.setHgrow(this, javafx.scene.layout.Priority.ALWAYS)
					textProperty().addListener { _, _, value ->
						value.toLongOrNull()?.also {
							config.connectTimeout = it
							controller?.saveConfig()
						}
					}
				}, Label("ms")).apply { alignment = Pos.CENTER })
		addRow(row + 4, Label("Read timeout").apply { GridPane.setHalignment(this, HPos.RIGHT) },
				HBox(TextField().apply {
					HBox.setHgrow(this, javafx.scene.layout.Priority.ALWAYS)
					textProperty().addListener { _, _, value ->
						value.toLongOrNull()?.also {
							config.readTimeout = it
							controller?.saveConfig()
						}
					}
				}, Label("ms")).apply { alignment = Pos.CENTER })
		addRow(row + 5, Label("Write timeout").apply { GridPane.setHalignment(this, HPos.RIGHT) },
				HBox(TextField().apply {
					HBox.setHgrow(this, javafx.scene.layout.Priority.ALWAYS)
					textProperty().addListener { _, _, value ->
						value.toLongOrNull()?.also {
							config.writeTimeout = it
							controller?.saveConfig()
						}
					}
				}, Label("ms")).apply { alignment = Pos.CENTER })
		return row + 6
	}

	private fun initializeNotifierModule(row: Int, module: Notifier<*>): Int = gridPane.run {
		addRow(row, Label("Minimum priority").apply { GridPane.setHalignment(this, HPos.RIGHT) },
				ComboBox<Priority>(FXCollections.observableArrayList(*Priority.values())).apply {
					GridPane.setHgrow(this, javafx.scene.layout.Priority.ALWAYS)
					selectionModel.apply {
						select(module.config.minPriority)
						selectedItemProperty().addListener { _, _, value ->
							module.config.minPriority = value
							controller?.saveConfig()
						}
					}
				})
		return row + 1
	}
}
