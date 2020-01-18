package com.poterion.monitor.ui

import com.poterion.monitor.api.controllers.ControllerInterface
import com.poterion.monitor.api.controllers.ModuleInstanceInterface
import com.poterion.monitor.api.controllers.Notifier
import com.poterion.monitor.api.controllers.Service
import com.poterion.monitor.api.lib.toIcon
import com.poterion.monitor.api.lib.toImage
import com.poterion.monitor.api.lib.toImageView
import com.poterion.monitor.api.modules.NotifierModule
import com.poterion.monitor.api.modules.ServiceModule
import com.poterion.monitor.api.ui.CommonIcon
import com.poterion.monitor.api.utils.factory
import com.poterion.monitor.data.HttpConfig
import com.poterion.monitor.data.HttpProxy
import com.poterion.monitor.data.ModuleConfig
import com.poterion.monitor.data.Priority
import com.poterion.monitor.data.auth.BasicAuthConfig
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.geometry.Pos
import javafx.geometry.VPos
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.layout.GridPane
import javafx.scene.layout.HBox
import javafx.scene.layout.RowConstraints
import javafx.scene.layout.VBox
import java.util.*
import kotlin.Comparator

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

			val config = controller.applicationConfiguration
			controller.stage.apply {
				icons.add(CommonIcon.SETTINGS.toImage())
				title = "Configuration"
				//stage.isResizable = false
				minWidth = 800.0
				minHeight = 600.0

				scene = Scene(root, config.windowWidth, config.windowHeight).apply {
					widthProperty().addListener { _, _, value ->
						config.windowWidth = value.toDouble()
						controller.saveConfig()
					}
					heightProperty().addListener { _, _, value ->
						config.windowHeight = value.toDouble()
						controller.saveConfig()
					}
				}
				show()
			}
		}
	}

	@FXML private lateinit var tabPaneMain: TabPane
	@FXML private lateinit var tabCommon: Tab
	@FXML private lateinit var splitPane: SplitPane
	@FXML private lateinit var tree: TreeView<ModuleItem>
	@FXML private lateinit var vboxContent: VBox
	@FXML private lateinit var gridPane: GridPane

	private lateinit var controller: ControllerInterface

	@FXML
	fun initialize() {
		tree.apply {
			isShowRoot = false
			selectionModel.selectionMode = SelectionMode.SINGLE
			factory { item, empty ->
				//text = item?.takeUnless { empty }?.title?.get()
				graphic = item?.takeUnless { empty }?.graphic
						?: item?.takeUnless { empty }?.module?.definition?.icon?.toImageView()

				if (item != null && !empty) {
					item.module?.config?.name?.also { item.title.set(it) }
					textProperty().bind(item.title)
				} else {
					if (textProperty().isBound) textProperty().unbind()
				}

				contextMenu = when (item?.takeUnless { empty }?.title?.value) {
					"Services" -> controller
							.validModules { it.services }
							.mapNotNull { it as? ServiceModule<*, *> }
							.map { module ->
								MenuItem("Add ${module.title} service", module.icon.toImageView()).apply {
									setOnAction {
										controller.add(module)?.also { treeItem.children.addItem(it) }
										treeView.refresh()
									}
								}
							}
							.let { ContextMenu(*it.toTypedArray()) }
					"Notifiers" -> controller
							.validModules { it.notifiers }
							.mapNotNull { it as? NotifierModule<*, *> }
							.map { module ->
								MenuItem("Add ${module.title} notifier", module.icon.toImageView()).apply {
									setOnAction {
										controller.add(module)?.also { treeItem.children.addItem(it) }
										treeView.refresh()
									}
								}
							}
							.let { ContextMenu(*it.toTypedArray()) }
					is String -> ContextMenu(MenuItem("Delete").apply {
						setOnAction {
							treeItem.value.module?.destroy()
							treeItem.remove()
							treeView.refresh()
						}
					})
					else -> null
				}
			}

			selectionModel.selectedItemProperty().addListener { _, _, newValue -> select(newValue) }
		}
		select(null)
	}

	private fun ControllerInterface.validModules(getter: (ControllerInterface) -> Collection<ModuleInstanceInterface<*>>) =
			modules.filter { m -> !m.singleton || !getter(this).map { it.definition }.contains(m) }

	private fun load() {
		splitPane.setDividerPosition(0, controller.applicationConfiguration.commonSplit)
		splitPane.dividers.first().positionProperty().addListener { _, _, value ->
			controller.applicationConfiguration.commonSplit = value.toDouble()
			controller.saveConfig()
		}



		tree.root = TreeItem<ModuleItem>().apply {
			children.addAll(
					TreeItem(ModuleItem(SimpleStringProperty("Services"), UiIcon.SERVICES.toImageView())).apply {
						controller.services.forEach { children.addItem(it) }
						//?.sortedBy { it.config.name }
						//?.map { TreeItem(ModuleItem(module = it)) }

						//?.also { children.addAll(it) }
						isExpanded = true
					},
					TreeItem(ModuleItem(SimpleStringProperty("Notifiers"), UiIcon.NOTIFIERS.toImageView())).apply {
						controller.notifiers.forEach { children.addItem(it) }
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
				Comparator.comparing<TreeItem<ModuleItem>, String> { it.value.module?.config?.name ?: "" })

		module.configurationTab?.let {
			val tab = Tab(module.config.name, module.configurationTab)
			tab.userData = module
			tab.textProperty().bind(item.value.title)
			tabPaneMain.tabs.add(tab)
			FXCollections.sort<Tab>(tabPaneMain.tabs, Comparator.comparing<Tab, String> {
				when (it) {
					tabCommon -> " 0"
					else -> it.text ?: " Z"
				}
			})
		}

		controller.saveConfig()
	}

	private fun TreeItem<ModuleItem>.remove() {
		controller.services.removeIf { it.config == value?.module?.config }
		controller.notifiers.removeIf { it.config == value?.module?.config }
		controller.applicationConfiguration.services.remove(value.module?.config?.uuid)
		controller.applicationConfiguration.notifiers.remove(value.module?.config?.uuid)
		parent.children.remove(this)

		tabPaneMain.tabs.removeIf { it.userData == value.module }

		controller.saveConfig()
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
				(label as? Label)?.alignment = Pos.CENTER_RIGHT
				gridPane.rowConstraints.add(RowConstraints(30.0, Control.USE_COMPUTED_SIZE, Double.MAX_VALUE, javafx.scene.layout.Priority.ALWAYS, VPos.TOP, true))
			}
			treeItem.value?.module?.configurationAddition?.forEach { vboxContent.children.add(it) }
		}
	}

	private fun initializeModule(treeItem: TreeItem<ModuleItem>?): Int = gridPane.run {
		var row = 0
		addRow(row++,
				Label("Type").apply {
					maxWidth = Double.MAX_VALUE
					maxHeight = Double.MAX_VALUE
					alignment = Pos.CENTER_RIGHT
				},
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
		addRow(row++,
				Label("Name").apply {
					maxWidth = Double.MAX_VALUE
					maxHeight = Double.MAX_VALUE
					alignment = Pos.CENTER_RIGHT
				},
				TextField(treeItem?.value?.module?.config?.name ?: "").apply {
					textProperty().addListener { _, _, value ->
						treeItem?.value?.title?.set(value)
						FXCollections.sort<TreeItem<ModuleItem>>(treeItem?.parent?.children,
								Comparator.comparing<TreeItem<ModuleItem>, String> { it.value.title.value ?: "" })
						tree.refresh()
						treeItem?.value?.module?.config?.name = value
					}
					focusedProperty().addListener { _, _, hasFocus -> if (!hasFocus) controller.saveConfig() }
				})
		addRow(row++,
				Label("Enabled").apply {
					maxWidth = Double.MAX_VALUE
					maxHeight = Double.MAX_VALUE
					alignment = Pos.CENTER_RIGHT
				},
				CheckBox().apply {
					maxHeight = Double.MAX_VALUE
					isSelected = treeItem?.value?.module?.config?.enabled == true
					selectedProperty().addListener { _, _, value ->
						treeItem?.value?.module?.config?.enabled = value
						controller.saveConfig()
					}
				})

		row = treeItem?.value?.module?.let { it as? Service }?.let { initializeServiceModule(row, it) } ?: row
		row = treeItem?.value?.module?.let { it as? Notifier }?.let { initializeNotifierModule(row, it) } ?: row

		return row
	}

	private fun initializeServiceModule(row: Int, module: Service<*>): Int = gridPane.run {
		addRow(row,
				Label("Default priority").apply {
					maxWidth = Double.MAX_VALUE
					maxHeight = Double.MAX_VALUE
					alignment = Pos.CENTER_RIGHT
				},
				ComboBox<Priority>(FXCollections.observableArrayList(*Priority.values())).apply {
					factory { item, empty ->
						text = item?.takeUnless { empty }?.name
						graphic = item?.takeUnless { empty }?.toIcon()?.toImageView()
					}
					GridPane.setHgrow(this, javafx.scene.layout.Priority.ALWAYS)
					selectionModel.apply {
						select(module.config.priority)
						selectedItemProperty().addListener { _, _, value ->
							module.config.priority = value
							controller.saveConfig()
						}
					}
				})
		addRow(row + 1,
				Label("Check interval").apply {
					maxWidth = Double.MAX_VALUE
					maxHeight = Double.MAX_VALUE
					alignment = Pos.CENTER_RIGHT
				},
				HBox(TextField(module.config.checkInterval.toString()).apply {
					HBox.setHgrow(this, javafx.scene.layout.Priority.ALWAYS)
					textProperty().addListener { _, _, value -> value.toLongOrNull()?.also { module.config.checkInterval = it } }
					focusedProperty().addListener { _, _, hasFocus -> if (!hasFocus) controller.saveConfig() }
				}, Label("ms")).apply { alignment = Pos.CENTER })

		return initializeHttpService(row + 2, module.config)
	}

	private fun initializeHttpService(row: Int, config: HttpConfig): Int = gridPane.run {
		addRow(row,
				Label("URL").apply {
					maxWidth = Double.MAX_VALUE
					maxHeight = Double.MAX_VALUE
					alignment = Pos.CENTER_RIGHT
				},
				TextField(config.url).apply {
					textProperty().addListener { _, _, value -> config.url = value }
					focusedProperty().addListener { _, _, hasFocus -> if (!hasFocus) controller.saveConfig() }
				})

		val usernameField = TextField(config.auth?.username ?: "")
		val passwordField = PasswordField()

		addRow(row + 1,
				Label("Auth").apply {
					maxWidth = Double.MAX_VALUE
					maxHeight = Double.MAX_VALUE
					alignment = Pos.CENTER_RIGHT
				},
				HBox(
						usernameField.apply {
							HBox.setHgrow(this, javafx.scene.layout.Priority.ALWAYS)
							textProperty().addListener { _, _, value ->
								val usr = value.takeIf { it.isNotEmpty() }
								val pwd = passwordField.text.takeIf { it.isNotEmpty() }
								config.auth = if (usr == null && pwd == null)
									null else BasicAuthConfig(usr ?: "", pwd ?: "")
							}
							focusedProperty().addListener { _, _, hasFocus -> if (!hasFocus) controller.saveConfig() }
						},
						passwordField.apply {
							HBox.setHgrow(this, javafx.scene.layout.Priority.ALWAYS)
							text = config.auth?.password ?: ""
							textProperty().addListener { _, _, value ->
								val usr = usernameField.text.takeIf { it.isNotEmpty() }
								val pwd = value.takeIf { it.isNotEmpty() }
								config.auth = if (usr == null && pwd == null)
									null else BasicAuthConfig(usr ?: "", pwd ?: "")
							}
							focusedProperty().addListener { _, _, hasFocus -> if (!hasFocus) controller.saveConfig() }
						}).apply { alignment = Pos.CENTER })

		addRow(row + 2,
				Label("Trust certificate").apply {
					maxWidth = Double.MAX_VALUE
					maxHeight = Double.MAX_VALUE
					alignment = Pos.CENTER_RIGHT
				},
				CheckBox().apply {
					maxHeight = Double.MAX_VALUE
					isSelected = config.trustCertificate
					selectedProperty().addListener { _, _, value ->
						config.trustCertificate = value
						controller.saveConfig()
					}
				})

		val proxyAddressField = TextField(config.proxy?.address ?: "")
		val proxyPortField = TextField(config.proxy?.port?.toString() ?: "")

		addRow(row + 3,
				Label("Proxy").apply {
					maxWidth = Double.MAX_VALUE
					maxHeight = Double.MAX_VALUE
					alignment = Pos.CENTER_RIGHT
				},
				HBox(
						proxyAddressField.apply {
							HBox.setHgrow(this, javafx.scene.layout.Priority.ALWAYS)
							textProperty().addListener { _, _, value ->
								val address = value.takeIf { it.isNotEmpty() }
								val port = proxyPortField.text.takeIf { it.isNotEmpty() }?.toIntOrNull() ?: 80
								config.proxy = if (address == null)
									null else HttpProxy(address, port)
							}
							focusedProperty().addListener { _, _, hasFocus -> if (!hasFocus) controller.saveConfig() }
						},
						proxyPortField.apply {
							minWidth = 75.0
							promptText = "80"
							HBox.setHgrow(this, javafx.scene.layout.Priority.NEVER)
							textProperty().addListener { _, _, value ->
								val address = proxyAddressField.text.takeIf { it.isNotEmpty() }
								val port = value.takeIf { it.isNotEmpty() }?.toIntOrNull() ?: 80
								config.proxy = if (address == null)
									null else HttpProxy(address, port)
							}
							focusedProperty().addListener { _, _, hasFocus -> if (!hasFocus) controller.saveConfig() }
						}).apply { alignment = Pos.CENTER })

		val proxyUsernameField = TextField(config.proxy?.auth?.username ?: "")
		val proxyPasswordField = PasswordField()

		addRow(row + 4,
				Label("Proxy Auth").apply {
					maxWidth = Double.MAX_VALUE
					maxHeight = Double.MAX_VALUE
					alignment = Pos.CENTER_RIGHT
				},
				HBox(
						proxyUsernameField.apply {
							HBox.setHgrow(this, javafx.scene.layout.Priority.ALWAYS)
							textProperty().addListener { _, _, value ->
								val usr = value.takeIf { it.isNotEmpty() }
								val pwd = proxyPasswordField.text.takeIf { it.isNotEmpty() }
								config.proxy?.auth = if (usr == null && pwd == null)
									null else BasicAuthConfig(usr ?: "", pwd ?: "")
							}
							focusedProperty().addListener { _, _, hasFocus -> if (!hasFocus) controller.saveConfig() }
						},
						proxyPasswordField.apply {
							HBox.setHgrow(this, javafx.scene.layout.Priority.ALWAYS)
							text = config.proxy?.auth?.password ?: ""
							textProperty().addListener { _, _, value ->
								val usr = proxyUsernameField.text.takeIf { it.isNotEmpty() }
								val pwd = value.takeIf { it.isNotEmpty() }
								config.proxy?.auth = if (usr == null && pwd == null)
									null else BasicAuthConfig(usr ?: "", pwd ?: "")
							}
							focusedProperty().addListener { _, _, hasFocus -> if (!hasFocus) controller.saveConfig() }
						}).apply { alignment = Pos.CENTER })

		addRow(row + 5,
				Label("Connection timeout").apply {
					maxWidth = Double.MAX_VALUE
					maxHeight = Double.MAX_VALUE
					alignment = Pos.CENTER_RIGHT
				},
				HBox(TextField(config.connectTimeout?.toString() ?: "").apply {
					HBox.setHgrow(this, javafx.scene.layout.Priority.ALWAYS)
					textProperty().addListener { _, _, value -> value.toLongOrNull().also { config.connectTimeout = it } }
					focusedProperty().addListener { _, _, hasFocus -> if (!hasFocus) controller.saveConfig() }
				}, Label("ms")).apply { alignment = Pos.CENTER })
		addRow(row + 6,
				Label("Read timeout").apply {
					maxWidth = Double.MAX_VALUE
					maxHeight = Double.MAX_VALUE
					alignment = Pos.CENTER_RIGHT
				},
				HBox(TextField(config.readTimeout?.toString() ?: "").apply {
					HBox.setHgrow(this, javafx.scene.layout.Priority.ALWAYS)
					textProperty().addListener { _, _, value -> value.toLongOrNull().also { config.readTimeout = it } }
					focusedProperty().addListener { _, _, hasFocus -> if (!hasFocus) controller.saveConfig() }
				}, Label("ms")).apply { alignment = Pos.CENTER })
		addRow(row + 7,
				Label("Write timeout").apply {
					maxWidth = Double.MAX_VALUE
					maxHeight = Double.MAX_VALUE
					alignment = Pos.CENTER_RIGHT
				},
				HBox(TextField(config.writeTimeout?.toString() ?: "").apply {
					HBox.setHgrow(this, javafx.scene.layout.Priority.ALWAYS)
					textProperty().addListener { _, _, value -> value.toLongOrNull().also { config.writeTimeout = it } }
					focusedProperty().addListener { _, _, hasFocus -> if (!hasFocus) controller.saveConfig() }
				}, Label("ms")).apply { alignment = Pos.CENTER })
		return row + 6
	}

	private fun initializeNotifierModule(row: Int, module: Notifier<*>): Int = gridPane.run {
		addRow(row,
				Label("Minimum priority").apply {
					maxWidth = Double.MAX_VALUE
					maxHeight = Double.MAX_VALUE
					alignment = Pos.CENTER_RIGHT
				},
				ComboBox<Priority>(FXCollections.observableArrayList(*Priority.values())).apply {
					factory { item, empty ->
						text = item?.takeUnless { empty }?.name
						graphic = item?.takeUnless { empty }?.toIcon()?.toImageView()
					}
					GridPane.setHgrow(this, javafx.scene.layout.Priority.ALWAYS)
					selectionModel.apply {
						select(module.config.minPriority)
						selectedItemProperty().addListener { _, _, value ->
							module.config.minPriority = value
							controller.saveConfig()
						}
					}
				})
		return row + 1
	}
}
