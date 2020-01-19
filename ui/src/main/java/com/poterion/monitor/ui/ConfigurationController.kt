package com.poterion.monitor.ui

import com.poterion.monitor.api.StatusCollector
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
import com.poterion.monitor.api.utils.cell
import com.poterion.monitor.api.utils.factory
import com.poterion.monitor.data.*
import com.poterion.monitor.data.Priority
import com.poterion.monitor.data.auth.BasicAuthConfig
import com.poterion.monitor.data.notifiers.NotifierConfig
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
import javafx.scene.input.KeyCode
import javafx.scene.layout.*
import java.util.*
import java.util.concurrent.TimeUnit
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

	private val tableSilencedStatusItems = TableView<StatusItem>().apply {
		minWidth = Region.USE_COMPUTED_SIZE
		minHeight = Region.USE_COMPUTED_SIZE
		prefWidth = Region.USE_COMPUTED_SIZE
		prefHeight = Region.USE_COMPUTED_SIZE
		maxWidth = Double.MAX_VALUE
		maxHeight = Double.MAX_VALUE
		VBox.setVgrow(this, javafx.scene.layout.Priority.ALWAYS)
		setOnKeyReleased { event ->
			when (event.code) {
				KeyCode.DELETE -> selectionModel.selectedItem?.also { removeSilencedStatusItem(it) }
				else -> {
					// Nothing to do
				}
			}
		}
	}

	private val tableColumnServiceName = TableColumn<StatusItem, String>("Service Name").apply {
		isSortable = false
		minWidth = 150.0
		prefWidth = Region.USE_COMPUTED_SIZE
		maxWidth = Double.MAX_VALUE
		cell("serviceId") { item, _, empty ->
			val service = item?.takeUnless { empty }?.service(controller.applicationConfiguration)
			graphic = controller
					.takeUnless { empty }
					?.modules
					?.find { module -> module.configClass == service?.let { it::class } }
					?.icon
					?.toImageView()
			text = item?.takeUnless { empty }?.serviceName(controller.applicationConfiguration)
		}
	}

	private val tableColumnTitle = TableColumn<StatusItem, String>("Title").apply {
		isSortable = false
		minWidth = 150.0
		prefWidth = Region.USE_COMPUTED_SIZE
		maxWidth = Double.MAX_VALUE
		cell("title")
	}

	private val tableColumnAction = TableColumn<StatusItem, Status>("").apply {
		isSortable = false
		minWidth = 48.0
		prefWidth = Region.USE_COMPUTED_SIZE
		maxWidth = Region.USE_PREF_SIZE
		cell { item, _, empty ->
			graphic = Button("", CommonIcon.TRASH.toImageView()).takeUnless { empty }?.apply {
				setOnAction { item?.also { removeSilencedStatusItem(it) } }
			}
		}
	}

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
		tabCommon.graphic = CommonIcon.SETTINGS.toImageView()
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
					TreeItem(ModuleItem(SimpleStringProperty("Application"), CommonIcon.SETTINGS.toImageView())),
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

		tableSilencedStatusItems.columns.addAll(tableColumnServiceName, tableColumnTitle, tableColumnAction)
		StatusCollector.status.sample(10, TimeUnit.SECONDS).subscribe {
			tableSilencedStatusItems.items.setAll(controller.applicationConfiguration.silenced.values)
		}
	}

	private fun ObservableList<TreeItem<ModuleItem>>.addItem(module: ModuleInstanceInterface<*>) {
		val item = TreeItem(ModuleItem(module = module))
		add(item)
		FXCollections.sort<TreeItem<ModuleItem>>(this,
				Comparator.comparing<TreeItem<ModuleItem>, String> { it.value.module?.config?.name ?: "" })

		module.configurationTab
				?.let { Tab(module.config.name, it) }
				?.also { tab ->
					tab.userData = module
					//tab.graphic = module.configurationTabIcon.value
					//module.configurationTabIcon.addListener { _, _, icon ->
					//	tab.graphic = icon
					//}
					tab.graphicProperty().bind(module.configurationTabIcon)
					tab.textProperty().bind(item.value.title)
					tabPaneMain.tabs.add(tab)
				}

		FXCollections.sort<Tab>(tabPaneMain.tabs, Comparator.comparing<Tab, String> {
			when (it) {
				tabCommon -> " 0"
				else -> it.text ?: " Z"
			}
		})

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

		val rowCount = if (treeItem?.value?.module == null) when (treeItem?.value?.title?.get()) {
			"Application" -> initializeApplication()
			else -> 0
		} else initializeModule(treeItem)

//			treeItem.value?.module?.configurationRows?.forEach { (label, content) ->
//				gridPane.addRow(rows++, label, content)
//				(label as? Label)?.alignment = Pos.CENTER_RIGHT
//				gridPane.rowConstraints.add(RowConstraints(30.0, Control.USE_COMPUTED_SIZE, Double.MAX_VALUE, javafx.scene.layout.Priority.ALWAYS, VPos.TOP, true))
//			}
//			treeItem.value?.module?.configurationAddition?.forEach { vboxContent.children.add(it) }

		gridPane.rowConstraints.addAll((0 until rowCount).map {
			RowConstraints(30.0, Control.USE_COMPUTED_SIZE, Double.MAX_VALUE, javafx.scene.layout.Priority.ALWAYS, VPos.TOP, true)
		})
	}

	private fun initializeApplication(): Int = gridPane.run {
		var row = 0
		addRow(row++,
				Label("Show on startup").apply {
					maxWidth = Double.MAX_VALUE
					maxHeight = Double.MAX_VALUE
					alignment = Pos.CENTER_RIGHT
				},
				CheckBox().apply {
					maxHeight = Double.MAX_VALUE
					isSelected = controller.applicationConfiguration.showOnStartup
					selectedProperty().addListener { _, _, value ->
						controller.applicationConfiguration.showOnStartup = value
						controller.saveConfig()
					}
				})
		addRow(row++,
				Label("Start minimized").apply {
					maxWidth = Double.MAX_VALUE
					maxHeight = Double.MAX_VALUE
					alignment = Pos.CENTER_RIGHT
				},
				CheckBox().apply {
					maxHeight = Double.MAX_VALUE
					isSelected = controller.applicationConfiguration.startMinimized
					selectedProperty().addListener { _, _, value ->
						controller.applicationConfiguration.startMinimized = value
						controller.saveConfig()
					}
				})
		addRow(row++,
				Label("Bluetooth discovery").apply {
					maxWidth = Double.MAX_VALUE
					maxHeight = Double.MAX_VALUE
					alignment = Pos.CENTER_RIGHT
				},
				CheckBox().apply {
					maxHeight = Double.MAX_VALUE
					isSelected = controller.applicationConfiguration.btDiscovery
					selectedProperty().addListener { _, _, value ->
						controller.applicationConfiguration.btDiscovery = value
						controller.saveConfig()
					}
				})
		addRow(row++,
				Label("Silenced status items:").apply {
					maxWidth = Double.MAX_VALUE
					maxHeight = Double.MAX_VALUE
					alignment = Pos.CENTER_RIGHT
				},
				Pane())

		vboxContent.children.add(VBox(tableSilencedStatusItems)
				.apply { VBox.setVgrow(this, javafx.scene.layout.Priority.ALWAYS) })

		return row
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

		row = treeItem?.value?.module?.let { it as? Service }?.initializeServiceModule(row) ?: row
		row = treeItem?.value?.module?.let { it as? Notifier }?.initializeNotifierModule(row) ?: row

		treeItem?.value?.module?.configurationRows?.forEach { (label, content) ->
			gridPane.addRow(row++, label, content)
			(label as? Label)?.alignment = Pos.CENTER_RIGHT
		}

		treeItem?.value?.module?.let { it as? Notifier }?.also { notifier ->
			notifier.config.let { it as? NotifierConfig }?.also { config ->
				val plugin = ServiceSettingsPlugin(notifier.controller, config.services)
				row = plugin.initializeNotifierServiceFilter(row)
				vboxContent.children.add(plugin.vboxServiceConfigs)
			}
		}

		treeItem?.value?.module?.configurationAddition?.forEach { vboxContent.children.add(it) }

		return row
	}

	private fun Service<*>.initializeServiceModule(rowIndex: Int): Int = gridPane.run {
		var row = rowIndex
		addRow(row++,
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
						select(config.priority)
						selectedItemProperty().addListener { _, _, value ->
							config.priority = value
							controller.saveConfig()
						}
					}
				})
		addRow(row++,
				Label("Check interval").apply {
					maxWidth = Double.MAX_VALUE
					maxHeight = Double.MAX_VALUE
					alignment = Pos.CENTER_RIGHT
				},
				HBox(TextField(config.checkInterval.toString()).apply {
					HBox.setHgrow(this, javafx.scene.layout.Priority.ALWAYS)
					textProperty().addListener { _, _, value -> value.toLongOrNull()?.also { config.checkInterval = it } }
					focusedProperty().addListener { _, _, hasFocus -> if (!hasFocus) controller.saveConfig() }
				}, Label("ms")).apply { alignment = Pos.CENTER })

		return initializeHttpService(row)
	}

	private fun Service<*>.initializeHttpService(rowIndex: Int): Int = gridPane.run {
		var row = rowIndex
		addRow(row++,
				Label("URL").apply {
					maxWidth = Double.MAX_VALUE
					maxHeight = Double.MAX_VALUE
					alignment = Pos.CENTER_RIGHT
				},
				TextField(config.url).apply {
					textProperty().addListener { _, _, value -> config.url = value }
					focusedProperty().addListener { _, _, hasFocus -> if (!hasFocus) controller.saveConfig() }
				})

		val usernameField = TextField(config.auth?.let { it as? BasicAuthConfig }?.username ?: "")
		val passwordField = PasswordField()

		addRow(row++,
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
									null else BasicAuthConfig(username = usr ?: "", password = pwd ?: "")
							}
							focusedProperty().addListener { _, _, hasFocus -> if (!hasFocus) controller.saveConfig() }
						},
						passwordField.apply {
							HBox.setHgrow(this, javafx.scene.layout.Priority.ALWAYS)
							text = config.auth?.let { it as? BasicAuthConfig }?.password ?: ""
							textProperty().addListener { _, _, value ->
								val usr = usernameField.text.takeIf { it.isNotEmpty() }
								val pwd = value.takeIf { it.isNotEmpty() }
								config.auth = if (usr == null && pwd == null)
									null else BasicAuthConfig(username = usr ?: "", password = pwd ?: "")
							}
							focusedProperty().addListener { _, _, hasFocus -> if (!hasFocus) controller.saveConfig() }
						}).apply { alignment = Pos.CENTER })

		addRow(row++,
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

		addRow(row++,
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

		val proxyUsernameField = TextField(config.proxy?.auth?.let { it as? BasicAuthConfig }?.username ?: "")
		val proxyPasswordField = PasswordField()

		addRow(row++,
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
									null else BasicAuthConfig(username = usr ?: "", password = pwd ?: "")
							}
							focusedProperty().addListener { _, _, hasFocus -> if (!hasFocus) controller.saveConfig() }
						},
						proxyPasswordField.apply {
							HBox.setHgrow(this, javafx.scene.layout.Priority.ALWAYS)
							text = config.proxy?.auth?.let { it as? BasicAuthConfig }?.password ?: ""
							textProperty().addListener { _, _, value ->
								val usr = proxyUsernameField.text.takeIf { it.isNotEmpty() }
								val pwd = value.takeIf { it.isNotEmpty() }
								config.proxy?.auth = if (usr == null && pwd == null)
									null else BasicAuthConfig(username = usr ?: "", password = pwd ?: "")
							}
							focusedProperty().addListener { _, _, hasFocus -> if (!hasFocus) controller.saveConfig() }
						}).apply { alignment = Pos.CENTER })

		addRow(row++,
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

		addRow(row++,
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

		addRow(row++,
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

		return row
	}

	private fun Notifier<*>.initializeNotifierModule(rowIndex: Int): Int = gridPane.run {
		var row = rowIndex
		addRow(row++,
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
						select(config.minPriority)
						selectedItemProperty().addListener { _, _, value ->
							config.minPriority = value
							controller.saveConfig()
						}
					}
				})

		addRow(row++,
				Label("Minimum status").apply {
					maxWidth = Double.MAX_VALUE
					maxHeight = Double.MAX_VALUE
					alignment = Pos.CENTER_RIGHT
				},
				ComboBox<Status>(FXCollections.observableList(Status.values().toList())).apply {
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
				})

		return row
	}

	private fun ServiceSettingsPlugin.initializeNotifierServiceFilter(rowIndex: Int):
			Int = gridPane.run {
		var row = rowIndex
		rowService.also { (label, hbox) -> addRow(row++, label, hbox) }
		return row
	}

	private fun removeSilencedStatusItem(statusItem: StatusItem) {
		Alert(Alert.AlertType.CONFIRMATION).apply {
			title = "Delete confirmation"
			headerText = "Delete confirmation"
			contentText = "Do you really want to unsilence ${statusItem.title}?"
			buttonTypes.setAll(ButtonType.YES, ButtonType.NO)
		}.showAndWait().ifPresent {
			it.takeIf { it == ButtonType.YES }?.also {
				tableSilencedStatusItems.items.remove(statusItem)
				controller.applicationConfiguration.silenced.remove(statusItem.id)
				controller.saveConfig()
			}
		}
	}
}
