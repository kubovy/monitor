package com.poterion.monitor.ui

import com.poterion.monitor.api.controllers.ControllerInterface
import com.poterion.monitor.api.controllers.ModuleInstanceInterface
import com.poterion.monitor.api.controllers.Notifier
import com.poterion.monitor.api.controllers.Service
import com.poterion.monitor.api.lib.toImage
import com.poterion.monitor.api.lib.toImageView
import com.poterion.monitor.api.modules.NotifierModule
import com.poterion.monitor.api.modules.ServiceModule
import com.poterion.monitor.api.ui.CommonIcon
import com.poterion.monitor.data.HttpConfig
import com.poterion.monitor.data.ModuleConfig
import com.poterion.monitor.data.Priority
import com.poterion.monitor.data.auth.BasicAuthConfig
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.geometry.HPos
import javafx.geometry.Pos
import javafx.geometry.VPos
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.image.ImageView
import javafx.scene.layout.GridPane
import javafx.scene.layout.HBox
import javafx.scene.layout.RowConstraints
import javafx.scene.layout.VBox
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
				icons.add(CommonIcon.SETTINGS.toImage())
				title = "Configuration"
				//stage.isResizable = false
				minWidth = 1200.0
				minHeight = 1000.0
				scene = Scene(root, 1200.0, 1000.0)
				show()
			}
		}
	}

	@FXML private lateinit var tabPaneMain: TabPane
	@FXML private lateinit var tabCommon: Tab
	@FXML private lateinit var tree: TreeView<ModuleItem>
	@FXML private lateinit var vboxContent: VBox
	@FXML private lateinit var gridPane: GridPane

	private var controller: ControllerInterface? = null

	@FXML
	fun initialize() {
		tree.apply {
			isShowRoot = false
			selectionModel.selectionMode = SelectionMode.SINGLE
			setCellFactory { _ ->
				object : TreeCell<ModuleItem>() {
					override fun updateItem(item: ModuleItem?, empty: Boolean) {
						super.updateItem(item, empty)
						item?.module?.config?.name?.also { item.title.set(it) }
						item?.also { textProperty().bind(it.title) }
						when (item?.title?.value) {
							"Services" -> controller
									?.modules
									?.filterNot { it.singleton }
									?.mapNotNull { it as? ServiceModule<*, *> }
									?.map { module ->
										MenuItem("Add ${module.title} service", module.icon.toImageView()).apply {
											setOnAction {
												controller?.add(module)?.also { treeItem.children.addItem(it) }
											}
										}
									}
									?.also { contextMenu = ContextMenu(*it.toTypedArray()) }
							"Notifiers" -> controller
									?.modules
									?.filterNot { it.singleton }
									?.mapNotNull { it as? NotifierModule<*, *> }
									?.map { module ->
										MenuItem("Add ${module.title} notifier", module.icon.toImageView()).apply {
											setOnAction {
												controller?.add(module)?.also { treeItem.children.addItem(it) }
											}
										}
									}
									?.also { contextMenu = ContextMenu(*it.toTypedArray()) }
							else -> {
								if (item?.module?.definition?.singleton == false) {
									contextMenu = ContextMenu(MenuItem("Delete").apply {
										setOnAction { treeItem.remove() }
									})
								}
							}
						}

						graphic = item?.graphic ?: item?.module?.definition?.icon?.toImageView()
					}
				}
			}
			selectionModel.selectedItemProperty().addListener { _, _, newValue ->
				select(newValue)
			}
		}
		select(null)
	}

	private fun load() {
		tree.root = TreeItem<ModuleItem>().apply {
			children.addAll(
					TreeItem(ModuleItem(SimpleStringProperty("Services"), UiIcon.SERVICES.toImageView())).apply {
						controller?.services?.forEach { children.addItem(it) }
						//?.sortedBy { it.config.name }
						//?.map { TreeItem(ModuleItem(module = it)) }

						//?.also { children.addAll(it) }
						isExpanded = true
					},
					TreeItem(ModuleItem(SimpleStringProperty("Notifiers"), UiIcon.NOTIFIERS.toImageView())).apply {
						controller?.notifiers?.forEach { children.addItem(it) }
						//?.sortedBy { it.config.name }?.map { TreeItem(ModuleItem(module = it)) }
						//?.also { children.addAll(it) }
						isExpanded = true
					})
		}
	}

	private fun ObservableList<TreeItem<ModuleItem>>.addItem(module: ModuleInstanceInterface<*>) {
		val item = TreeItem(ModuleItem(module = module))
		add(item)
		FXCollections.sort<TreeItem<ModuleItem>>(this,
				Comparator.comparing<TreeItem<ModuleItem>, String> { it.value.title.value ?: "" })

		module.configurationTab?.let {
			val tab = Tab(module.config.name, module.configurationTab)
			tab.userData = module
			tab.textProperty().bind(item.value.title)
			tabPaneMain.tabs.add(tab)
			FXCollections.sort<Tab>(tabPaneMain.tabs,
					Comparator.comparing<Tab, String> { if (it == tabCommon) "" else (it.text ?: "") })
		}

		controller?.saveConfig()
	}

	private fun TreeItem<ModuleItem>.remove() {
		controller?.applicationConfiguration?.services?.removeIf { it == value.module?.config }
		controller?.applicationConfiguration?.notifiers?.removeIf { it == value.module?.config }
		parent.children.remove(this)

		tabPaneMain.tabs.removeIf { it.userData == value.module }

		controller?.saveConfig()
	}

	private fun select(treeItem: TreeItem<ModuleItem>?) {
		vboxContent.children.clear()
		vboxContent.children.add(gridPane)
		gridPane.children.clear()
		gridPane.rowConstraints.clear()

		if (treeItem?.value?.module != null) {
			var rows = initializeModule(treeItem)
			gridPane.rowConstraints.addAll((0 until rows).map {
				RowConstraints(30.0, Control.USE_COMPUTED_SIZE, Double.MAX_VALUE, javafx.scene.layout.Priority.ALWAYS, VPos.TOP, true)
			})

			treeItem.value?.module?.configurationRows?.forEach { (label, content) ->
				gridPane.addRow(rows++, label, content)
				gridPane.rowConstraints.add(RowConstraints(30.0, Control.USE_COMPUTED_SIZE, Double.MAX_VALUE, javafx.scene.layout.Priority.ALWAYS, VPos.TOP, true))
			}
			treeItem.value?.module?.configurationAddition?.forEach { vboxContent.children.add(it) }
		}
	}

	private fun initializeModule(treeItem: TreeItem<ModuleItem>?): Int = gridPane.run {
		var row = 0
		addRow(row++, Label("Type").apply { GridPane.setHalignment(this, HPos.RIGHT) },
				ComboBox<String>().apply {
					GridPane.setHgrow(this, javafx.scene.layout.Priority.ALWAYS)
					val loader = ServiceLoader.load(ModuleConfig::class.java)
					items.add(null)
					items.addAll(loader.map { it::class.simpleName })
					isDisable = true
					selectionModel.apply {
						treeItem?.value?.module?.config?.type?.also(::select)
					}
				})
		addRow(row++, Label("Name").apply { GridPane.setHalignment(this, HPos.RIGHT) },
				TextField(treeItem?.value?.module?.config?.name ?: "").apply {
					textProperty().addListener { _, _, value ->
						treeItem?.value?.title?.set(value)
						FXCollections.sort<TreeItem<ModuleItem>>(treeItem?.parent?.children,
								Comparator.comparing<TreeItem<ModuleItem>, String> { it.value.title.value ?: "" })
						tree.refresh()
						treeItem?.value?.module?.config?.name = value
					}
					focusedProperty().addListener { _, _, hasFocus -> if (!hasFocus) controller?.saveConfig() }
				})
		addRow(row++, Label("Enabled").apply { GridPane.setHalignment(this, HPos.RIGHT) },
				CheckBox().apply {
					isSelected = treeItem?.value?.module?.config?.enabled == true
					selectedProperty().addListener { _, _, value ->
						treeItem?.value?.module?.config?.enabled = value
						controller?.saveConfig()
					}
				})

		row = treeItem?.value?.module?.let { it as? Service }?.let { initializeServiceModule(row, it) } ?: row
		row = treeItem?.value?.module?.let { it as? Notifier }?.let { initializeNotifierModule(row, it) } ?: row

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
					textProperty().addListener { _, _, value -> value.toLongOrNull()?.also { module.config.checkInterval = it } }
					focusedProperty().addListener { _, _, hasFocus -> if (!hasFocus) controller?.saveConfig() }
				}, Label("ms")).apply { alignment = Pos.CENTER })

		return initializeHttpService(row + 2, module.config)
	}

	private fun initializeHttpService(row: Int, config: HttpConfig): Int = gridPane.run {
		addRow(row, Label("URL").apply { GridPane.setHalignment(this, HPos.RIGHT) },
				TextField(config.url).apply {
					textProperty().addListener { _, _, value -> config.url = value }
					focusedProperty().addListener { _, _, hasFocus -> if (!hasFocus) controller?.saveConfig() }
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
							}
							focusedProperty().addListener { _, _, hasFocus -> if (!hasFocus) controller?.saveConfig() }
						},
						PasswordField().apply {
							HBox.setHgrow(this, javafx.scene.layout.Priority.ALWAYS)
							text = config.auth?.password ?: ""
							textProperty().addListener { _, _, value ->
								val usr = value.takeIf { it.isNotEmpty() }
								val pwd = config.auth?.password?.takeIf { it.isNotEmpty() }
								config.auth = if (usr == null && pwd == null)
									null else (config.auth ?: BasicAuthConfig(usr ?: "", pwd ?: ""))
							}
							focusedProperty().addListener { _, _, hasFocus -> if (!hasFocus) controller?.saveConfig() }
						}).apply { alignment = Pos.CENTER })
		addRow(row + 3, Label("Connection timeout").apply { GridPane.setHalignment(this, HPos.RIGHT) },
				HBox(TextField(config.connectTimeout?.toString() ?: "").apply {
					HBox.setHgrow(this, javafx.scene.layout.Priority.ALWAYS)
					textProperty().addListener { _, _, value -> value.toLongOrNull().also { config.connectTimeout = it } }
					focusedProperty().addListener { _, _, hasFocus -> if (!hasFocus) controller?.saveConfig() }
				}, Label("ms")).apply { alignment = Pos.CENTER })
		addRow(row + 4, Label("Read timeout").apply { GridPane.setHalignment(this, HPos.RIGHT) },
				HBox(TextField(config.readTimeout?.toString() ?: "").apply {
					HBox.setHgrow(this, javafx.scene.layout.Priority.ALWAYS)
					textProperty().addListener { _, _, value -> value.toLongOrNull().also { config.readTimeout = it } }
					focusedProperty().addListener { _, _, hasFocus -> if (!hasFocus) controller?.saveConfig() }
				}, Label("ms")).apply { alignment = Pos.CENTER })
		addRow(row + 5, Label("Write timeout").apply { GridPane.setHalignment(this, HPos.RIGHT) },
				HBox(TextField(config.writeTimeout?.toString() ?: "").apply {
					HBox.setHgrow(this, javafx.scene.layout.Priority.ALWAYS)
					textProperty().addListener { _, _, value -> value.toLongOrNull().also { config.writeTimeout = it } }
					focusedProperty().addListener { _, _, hasFocus -> if (!hasFocus) controller?.saveConfig() }
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
