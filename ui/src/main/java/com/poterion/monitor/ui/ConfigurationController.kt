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
package com.poterion.monitor.ui

import com.poterion.monitor.api.CommonIcon
import com.poterion.monitor.api.StatusCollector
import com.poterion.monitor.api.controllers.ControllerInterface
import com.poterion.monitor.api.controllers.ModuleInstanceInterface
import com.poterion.monitor.api.controllers.Notifier
import com.poterion.monitor.api.controllers.Service
import com.poterion.monitor.api.modules.NotifierModule
import com.poterion.monitor.api.modules.ServiceModule
import com.poterion.monitor.api.objectMapper
import com.poterion.monitor.api.utils.toIcon
import com.poterion.monitor.data.*
import com.poterion.monitor.data.Priority
import com.poterion.monitor.data.auth.AuthConfig
import com.poterion.monitor.data.auth.BasicAuthConfig
import com.poterion.monitor.data.auth.TokenAuthConfig
import com.poterion.monitor.data.data.SilencedStatusItem
import com.poterion.utils.javafx.*
import com.poterion.utils.kotlin.noop
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.geometry.HPos
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.geometry.VPos
import javafx.scene.Cursor
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.image.ImageView
import javafx.scene.input.KeyCode
import javafx.scene.layout.*
import java.net.URI
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.Comparator

/**
 * @author Jan Kubovy [jan@kubovy.eu]
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

	@FXML
	private lateinit var tabPaneMain: TabPane
	@FXML
	private lateinit var tabCommon: Tab
	@FXML
	private lateinit var splitPane: SplitPane
	@FXML
	private lateinit var tree: TreeView<ModuleItem>
	@FXML
	private lateinit var imageViewLogo: ImageView
	@FXML
	private lateinit var vboxContent: VBox
	@FXML
	private lateinit var gridPane: GridPane

	private lateinit var controller: ControllerInterface

	private val tabPaneMainComparator = Comparator.comparing<Tab, String> {
		when (it) {
			tabCommon -> " 0"
			else -> it.text ?: " Z"
		}
	}

	private val treeComparator = Comparator.comparing<TreeItem<ModuleItem>, String> {
		it.value.title.value ?: it.value.module?.config?.name ?: ""
	}

	private val tableSilencedStatusItems = TableView<SilencedStatusItem>().apply {
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

	private val tableColumnServiceName = TableColumn<SilencedStatusItem, StatusItem>("Service Name").apply {
		isSortable = false
		minWidth = 150.0
		prefWidth = Region.USE_COMPUTED_SIZE
		maxWidth = Double.MAX_VALUE
		cell("serviceId") { item, _, empty ->
			val service = item?.takeUnless { empty }?.item?.service(controller.applicationConfiguration)
			graphic = controller
					.takeUnless { empty }
					?.modules
					?.find { module -> module.configClass == service?.let { it::class } }
					?.icon
					?.toImageView()
			text = item?.takeUnless { empty }?.item?.serviceName(controller.applicationConfiguration)
		}
	}

	private val tableColumnTitle = TableColumn<SilencedStatusItem, StatusItem>("Title").apply {
		isSortable = false
		minWidth = 150.0
		prefWidth = Region.USE_COMPUTED_SIZE
		maxWidth = Double.MAX_VALUE
		cell("item") { _, value, empty ->
			text = value?.takeUnless { empty }?.title
		}
	}

	private val tableColumnSilencedAt = TableColumn<SilencedStatusItem, String>("Silenced").apply {
		isSortable = false
		isResizable = false
		minWidth = 190.0
		prefWidth = 190.0
		maxWidth = Region.USE_PREF_SIZE
		cell("silencedAt")
	}

	private val tableColumnLastChange = TableColumn<SilencedStatusItem, String>("Last Change").apply {
		isSortable = false
		isResizable = false
		minWidth = 190.0
		prefWidth = 190.0
		maxWidth = Region.USE_PREF_SIZE
		cell("lastChange")
	}

	private val tableColumnUntil = TableColumn<SilencedStatusItem, Boolean>("Until").apply {
		isSortable = false
		isResizable = false
		minWidth = 100.0
		prefWidth = Region.USE_COMPUTED_SIZE
		maxWidth = Region.USE_PREF_SIZE
		cell("untilChanged") { _, value, empty ->
			text = value.takeUnless { empty }?.let { if (it) "Until changed" else "Forever" }
		}
	}

	private val tableColumnAction = TableColumn<SilencedStatusItem, Status>("").apply {
		isSortable = false
		isResizable = false
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
		tree.setOnKeyPressed { event ->
			when (event.code) {
				KeyCode.D -> if (event.isControlDown) tree.selectionModel.selectedItem?.duplicate() else noop()
				KeyCode.DELETE -> tree.selectionModel.selectedItem?.delete()
				else -> noop()
			}
		}
		tree.apply {
			isShowRoot = false
			selectionModel.selectionMode = SelectionMode.SINGLE
			cell { _, item, empty ->
				//text = item?.takeUnless { empty }?.title?.get()
				graphic = item?.takeUnless { empty }?.let { it.icon ?: it.module?.definition?.icon }?.toImageView()

				if (item != null && !empty) {
					item.module?.config?.name?.also { item.title.set(it) }
					textProperty().bind(item.title)
				} else {
					if (textProperty().isBound) textProperty().unbind()
					text = null
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
					is String -> ContextMenu(
							MenuItem("Duplicade [Ctrl+D]", CommonIcon.DUPLICATE.toImageView()).apply {
								setOnAction { treeItem?.duplicate() }
							},
							MenuItem("Delete [Delete]", CommonIcon.TRASH.toImageView()).apply {
								setOnAction { treeItem?.delete() }
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
					TreeItem(ModuleItem(SimpleStringProperty("Application"), CommonIcon.SETTINGS)),
					TreeItem(ModuleItem(SimpleStringProperty("Services"), UiIcon.SERVICES)).apply {
						controller.services.forEach { children.addItem(it) }
						isExpanded = true
					},
					TreeItem(ModuleItem(SimpleStringProperty("Notifiers"), UiIcon.NOTIFIERS)).apply {
						controller.notifiers.forEach { children.addItem(it) }
						isExpanded = true
					},
					TreeItem(ModuleItem(SimpleStringProperty("About"), CommonIcon.APPLICATION)))
		}

		tableSilencedStatusItems.columns.addAll(tableColumnServiceName, tableColumnTitle, tableColumnSilencedAt,
				tableColumnLastChange, tableColumnUntil, tableColumnAction)
		StatusCollector.status.sample(10, TimeUnit.SECONDS, true).subscribe {
			tableSilencedStatusItems.items.setAll(controller.applicationConfiguration.silenced.values)
		}
	}

	private fun ObservableList<TreeItem<ModuleItem>>.addItem(module: ModuleInstanceInterface<*>) {
		val item = TreeItem(ModuleItem(module = module))
		add(item)
		FXCollections.sort<TreeItem<ModuleItem>>(this, treeComparator)

		module.configurationTab
				?.let { Tab(module.config.name, it) }
				?.also { tab ->
					tab.userData = module
					tab.graphicProperty().bind(module.configurationTabIcon)
					tab.textProperty().bind(item.value.title)
					tabPaneMain.tabs.add(tab)
				}
		controller.saveConfig()
		FXCollections.sort<Tab>(tabPaneMain.tabs, tabPaneMainComparator)
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
		gridPane.apply {
			padding = Insets(5.0)
			children.clear()
			rowConstraints.clear()
		}

		val rowCount = if (treeItem?.value?.module == null) when (treeItem?.value?.title?.get()) {
			"Application" -> initializeApplication()
			else -> initializeContainer(treeItem)
		} else initializeModule(treeItem)

		gridPane.rowConstraints.addAll((0 until rowCount).map {
			RowConstraints(30.0,
					Control.USE_COMPUTED_SIZE,
					Double.MAX_VALUE,
					javafx.scene.layout.Priority.ALWAYS,
					VPos.TOP,
					true)
		})
	}

	private fun initializeApplication(): Int = gridPane.run {
		imageViewLogo.image = null
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

	private fun initializeContainer(treeItem: TreeItem<ModuleItem>?): Int = gridPane.run {
		var row = 0
		imageViewLogo.image = treeItem?.value?.icon?.toImage(64, 64)
		when (treeItem?.value?.title?.value) {
			"Services" -> {
				addRow(row++,
						CheckBox().apply {
							//maxHeight = Double.MAX_VALUE
							alignment = Pos.CENTER_RIGHT
							isSelected = controller.applicationConfiguration.btDiscovery
							selectedProperty().addListener { _, _, value ->
								controller.applicationConfiguration.btDiscovery = value
								controller.saveConfig()
							}
							GridPane.setHalignment(this, HPos.RIGHT)
						},
						Label("Bluetooth discovery").apply { maxWidth = Double.MAX_VALUE })
				row = initializeProxy(row, { controller.applicationConfiguration.proxy },
						{ controller.applicationConfiguration.proxy = it })
				row = controller.applicationConfiguration.services.values.initializeModuleReferences(row, "Services:")
			}
			"Notifiers" -> {
				row = controller.applicationConfiguration.notifiers.values.initializeModuleReferences(row, "Notifiers:")
			}
			"About" -> {
				row = initializeAbout(row)
			}
			else -> noop()
		}
		return row
	}

	private fun Collection<ModuleConfig>.initializeModuleReferences(rowCount: Int, title: String): Int = gridPane.run {
		var row = rowCount
		addRow(row++, Label(title).apply {
			maxWidth = Double.MAX_VALUE
			maxHeight = Double.MAX_VALUE
			alignment = Pos.CENTER_RIGHT
		}, Pane())
		this@initializeModuleReferences.sortedBy { it.name }.forEach { module ->
			addRow(row++,
					CheckBox().apply {
						//maxHeight = Double.MAX_VALUE
						alignment = Pos.CENTER_RIGHT
						isSelected = module.enabled
						selectedProperty().addListener { _, _, value ->
							module.enabled = value
							controller.saveConfig()
						}
						GridPane.setHalignment(this, HPos.RIGHT)
					},
					HBox(
							(controller.modules
									.find { it.configClass == module::class }
									?.icon
									?: UiIcon.SERVICES)
									.toImageView(16, 16)
									.apply { maxHeight = Double.MAX_VALUE },
							Label(module.name).apply {
								maxWidth = Double.MAX_VALUE
							}).apply {
						spacing = 5.0
						maxWidth = Double.MAX_VALUE
						maxHeight = Double.MAX_VALUE
					})
		}
		return row
	}

	private fun initializeModule(treeItem: TreeItem<ModuleItem>?): Int = gridPane.run {
		imageViewLogo.image = treeItem?.value?.let { it.icon ?: it.module?.definition?.icon }?.toImage(64, 64)
		var row = 0
		addRow(row++,
				Label("Name").apply {
					maxWidth = Double.MAX_VALUE
					maxHeight = Double.MAX_VALUE
					alignment = Pos.CENTER_RIGHT
				},
				TextField(treeItem?.value?.module?.config?.name ?: "").apply {
					promptText = "No name"
					textProperty().addListener { _, _, value ->
						treeItem?.value?.title?.set(value)
						treeItem?.value?.module?.config?.name = value
					}
					focusedProperty().addListener { _, _, hasFocus ->
						if (!hasFocus) {
							controller.saveConfig()
							FXCollections.sort<TreeItem<ModuleItem>>(treeItem?.parent?.children, treeComparator)
							FXCollections.sort<Tab>(tabPaneMain.tabs, tabPaneMainComparator)
							//tree.refresh()
						}
					}
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
						val module = treeItem?.value?.module
						if (value) when (module) {
							is Service<*> -> module.refresh = true
							is Notifier<*> -> module.selectedServices.forEach { it.refresh = true }
						}
						controller.saveConfig()
					}
				})

		row = treeItem?.value?.module?.let { it as? Service }?.initializeServiceModule(row) ?: row
		row = treeItem?.value?.module?.let { it as? Notifier }?.initializeNotifierModule(row) ?: row

		treeItem?.value?.module?.configurationRows?.forEach { (label, content) ->
			gridPane.addRow(row++, label, content)
			(label as? Label)?.alignment = Pos.CENTER_RIGHT
		}

		treeItem?.value?.module?.configurationRowsLast?.forEach { (label, content) ->
			gridPane.addRow(row++, label, content)
			(label as? Label)?.alignment = Pos.CENTER_RIGHT
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
				HBox(
						TextField(config.checkInterval?.toString() ?: "").apply {
							maxWidth = 100.0
							promptText = "Manual update only fi left empty"
							HBox.setHgrow(this, javafx.scene.layout.Priority.ALWAYS)
							textProperty().addListener { _, _, value -> config.checkInterval = value.toLongOrNull() }
							focusedProperty().addListener { _, _, hasFocus -> if (!hasFocus) controller.saveConfig() }
						},
						Label("ms").apply {
							maxHeight = Double.MAX_VALUE
							padding = Insets(5.0)
						},
						Button("", CommonIcon.REFRESH.toImageView()).apply { setOnAction { refresh = true } }
				).apply {
					alignment = Pos.CENTER_LEFT
				})

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
					promptText = "https://..."
					textProperty().addListener { _, _, value -> config.url = value }
					focusedProperty().addListener { _, _, hasFocus -> if (!hasFocus) controller.saveConfig() }
				})

		row = initializeAuthentication(row, { config.auth }, { config.auth = it })

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

		addRow(row++,
				Label("Connection timeout").apply {
					maxWidth = Double.MAX_VALUE
					maxHeight = Double.MAX_VALUE
					alignment = Pos.CENTER_RIGHT
				},
				HBox(
						TextField(config.connectTimeout?.toString() ?: "").apply {
							maxWidth = 100.0
							promptText = "10000"
							HBox.setHgrow(this, javafx.scene.layout.Priority.ALWAYS)
							textProperty().addListener { _, _, value ->
								value.toLongOrNull().also { config.connectTimeout = it }
							}
							focusedProperty().addListener { _, _, hasFocus -> if (!hasFocus) controller.saveConfig() }
						},
						Label("ms").apply {
							maxHeight = Double.MAX_VALUE
							padding = Insets(5.0)
						}).apply { alignment = Pos.CENTER_LEFT })

		addRow(row++,
				Label("Read timeout").apply {
					maxWidth = Double.MAX_VALUE
					maxHeight = Double.MAX_VALUE
					alignment = Pos.CENTER_RIGHT
				},
				HBox(
						TextField(config.readTimeout?.toString() ?: "").apply {
							maxWidth = 100.0
							promptText = "10000"
							HBox.setHgrow(this, javafx.scene.layout.Priority.ALWAYS)
							textProperty().addListener { _, _, value ->
								value.toLongOrNull().also { config.readTimeout = it }
							}
							focusedProperty().addListener { _, _, hasFocus -> if (!hasFocus) controller.saveConfig() }
						},
						Label("ms").apply {
							maxHeight = Double.MAX_VALUE
							padding = Insets(5.0)
						}).apply { alignment = Pos.CENTER_LEFT })

		addRow(row++,
				Label("Write timeout").apply {
					maxWidth = Double.MAX_VALUE
					maxHeight = Double.MAX_VALUE
					alignment = Pos.CENTER_RIGHT
				},
				HBox(
						TextField(config.writeTimeout?.toString() ?: "").apply {
							maxWidth = 100.0
							promptText = "10000"
							HBox.setHgrow(this, javafx.scene.layout.Priority.ALWAYS)
							textProperty().addListener { _, _, value ->
								value.toLongOrNull().also { config.writeTimeout = it }
							}
							focusedProperty().addListener { _, _, hasFocus -> if (!hasFocus) controller.saveConfig() }
						},
						Label("ms").apply {
							maxHeight = Double.MAX_VALUE
							padding = Insets(5.0)
						}).apply { alignment = Pos.CENTER_LEFT })

		return row
	}

	private fun initializeProxy(rowIndex: Int,
								getter: () -> HttpProxy?,
								setter: (HttpProxy?) -> Unit): Int = gridPane.run {
		var row = rowIndex

		val textFieldProxyAddress = TextField(getter()?.address ?: "")
				.apply { promptText = "Proxy URL or IP address" }
		val textFieldProxyPort = TextField(getter()?.port?.toString() ?: "")
				.apply { promptText = "80" }
		val textFieldNoProxy = TextField(getter()?.noProxy ?: "")
				.apply {
					promptText = ".example.com,.another.net"
				}

		val radioBasicAuth = RadioButton()
		val textFieldUsername = TextField()
		val textFieldPassword = PasswordField()
		val radioTokenAuth = RadioButton()
		val textFieldToken = TextField()

		addRow(row++,
				Label("Proxy").apply {
					maxWidth = Double.MAX_VALUE
					maxHeight = Double.MAX_VALUE
					alignment = Pos.CENTER_RIGHT
				},
				HBox(*listOfNotNull(
						textFieldProxyAddress.apply {
							HBox.setHgrow(this, javafx.scene.layout.Priority.ALWAYS)
							textProperty().addListener { _, _, value ->
								val address = value.takeIf { it.isNotEmpty() }
								val port = textFieldProxyPort.text.takeIf { it.isNotEmpty() }?.toIntOrNull() ?: 80
								val noProxy = textFieldNoProxy.text?.takeIf { it.isNotBlank() }
								setter(if (address == null) null else HttpProxy(address, port, noProxy))
							}
							focusedProperty().addListener { _, _, hasFocus -> if (!hasFocus) controller.saveConfig() }
						},
						textFieldProxyPort.apply {
							minWidth = 75.0
							maxWidth = 75.0
							HBox.setHgrow(this, javafx.scene.layout.Priority.NEVER)
							textProperty().addListener { _, _, value ->
								val address = textFieldProxyAddress.text.takeIf { it.isNotEmpty() }
								val port = value.takeIf { it.isNotEmpty() }?.toIntOrNull() ?: 80
								val noProxy = textFieldNoProxy.text?.takeIf { it.isNotBlank() }
								setter(if (address == null) null else HttpProxy(address, port, noProxy))
							}
							focusedProperty().addListener { _, _, hasFocus -> if (!hasFocus) controller.saveConfig() }
						}).toTypedArray()
				).apply {
					maxWidth = 500.0
					alignment = Pos.CENTER
				})

		addRow(row++,
				Label("No Proxy").apply {
					maxWidth = Double.MAX_VALUE
					maxHeight = Double.MAX_VALUE
					alignment = Pos.CENTER_RIGHT
				},
				textFieldNoProxy.apply {
					maxWidth = 500.0
					HBox.setHgrow(this, javafx.scene.layout.Priority.ALWAYS)
					textProperty().addListener { _, _, value ->
						val address = textFieldProxyAddress.text.takeIf { it.isNotEmpty() }
						val port = textFieldProxyPort.text.takeIf { it.isNotEmpty() }?.toIntOrNull() ?: 80
						val noProxy = value?.takeIf { it.isNotBlank() }
						setter(if (address == null) null else HttpProxy(address, port, noProxy))
					}
					focusedProperty().addListener { _, _, hasFocus -> if (!hasFocus) controller.saveConfig() }
				})

		addRow(row++, Pane(), Label("A comma-separated list of domain extensions proxy should not be used for."))

		return initializeAuthentication(row, { getter()?.auth }, { getter()?.auth = it },
				radioBasicAuth, textFieldUsername, textFieldPassword, radioTokenAuth, textFieldToken)
	}

	private fun initializeAuthentication(rowIndex: Int,
										 getter: () -> AuthConfig?,
										 setter: (AuthConfig?) -> Unit,
										 radBasicAuth: RadioButton? = null,
										 txtUsername: TextField? = null,
										 txtPassword: PasswordField? = null,
										 radTokenAuth: RadioButton? = null,
										 txtToken: TextField? = null): Int = gridPane.run {
		var row = rowIndex

		val toggleGroupAuth = ToggleGroup()
		val radioBasicAuth = (radBasicAuth ?: RadioButton()).apply { toggleGroup = toggleGroupAuth }
		val textFieldUsername = (txtUsername ?: TextField()).apply {
			text = getter()?.let { it as? BasicAuthConfig }?.username ?: ""
			promptText = "No username"
		}
		val textFieldPassword = (txtPassword ?: PasswordField())
				.apply { promptText = "No password" }
		val radioTokenAuth = (radTokenAuth ?: RadioButton()).apply { toggleGroup = toggleGroupAuth }
		val textFieldToken = (txtToken ?: TextField()).apply {
			text = getter()?.let { it as? TokenAuthConfig }?.token ?: ""
			promptText = "No token"
		}

		addRow(row++,
				Label("Basic Auth").apply {
					maxWidth = Double.MAX_VALUE
					maxHeight = Double.MAX_VALUE
					alignment = Pos.CENTER_RIGHT
				},
				HBox(
						radioBasicAuth.apply {
							maxHeight = Double.MAX_VALUE
							isSelected = getter() is BasicAuthConfig
							selectedProperty().addListener { _, _, value ->
								if (value && !isDisable) {
									val usr = textFieldUsername.text.takeIf { it.isNotBlank() }
									val pwd = textFieldPassword.text.takeIf { it.isNotBlank() }
									setter(if (usr == null && pwd == null)
										null else BasicAuthConfig(username = usr ?: "", password = pwd ?: ""))
									controller.saveConfig()
								}
							}
						},
						textFieldUsername.apply {
							HBox.setHgrow(this, javafx.scene.layout.Priority.ALWAYS)
							textProperty().addListener { _, _, value ->
								if (radioBasicAuth.isSelected && !isDisable) {
									val usr = value.takeIf { it.isNotBlank() }
									val pwd = textFieldPassword.text.takeIf { it.isNotBlank() }
									setter(if (usr == null && pwd == null)
										null else BasicAuthConfig(username = usr ?: "", password = pwd ?: ""))
								}
							}
							focusedProperty().addListener { _, _, hasFocus -> if (!hasFocus) controller.saveConfig() }
						},
						textFieldPassword.apply {
							HBox.setHgrow(this, javafx.scene.layout.Priority.ALWAYS)
							text = getter()?.let { it as? BasicAuthConfig }?.password ?: ""
							textProperty().addListener { _, _, value ->
								if (radioBasicAuth.isSelected && !isDisable) {
									val usr = textFieldUsername.text.takeIf { it.isNotBlank() }
									val pwd = value.takeIf { it.isNotBlank() }
									setter(if (usr == null && pwd == null)
										null else BasicAuthConfig(username = usr ?: "", password = pwd ?: ""))
								}
							}
							focusedProperty().addListener { _, _, hasFocus -> if (!hasFocus) controller.saveConfig() }
						}).apply {
					maxWidth = 500.0
					alignment = Pos.CENTER
				})

		addRow(row++,
				Label("Bearer Token").apply {
					maxWidth = Double.MAX_VALUE
					maxHeight = Double.MAX_VALUE
					alignment = Pos.CENTER_RIGHT
				},
				HBox(
						radioTokenAuth.apply {
							maxHeight = Double.MAX_VALUE
							isSelected = getter() is TokenAuthConfig
							selectedProperty().addListener { _, _, value ->
								if (value && !isDisable) {
									setter(textFieldToken.text
											.takeIf { it.isNotEmpty() }
											?.let { TokenAuthConfig(token = it) })
									controller.saveConfig()
								}
							}
						},
						textFieldToken.apply {
							HBox.setHgrow(this, javafx.scene.layout.Priority.ALWAYS)
							textProperty().addListener { _, _, value ->
								if (radioTokenAuth.isSelected && !isDisable) setter(value
										.takeIf { it.isNotBlank() }
										?.let { TokenAuthConfig(token = it) })
							}
							focusedProperty().addListener { _, _, hasFocus -> if (!hasFocus) controller.saveConfig() }
						}).apply {
					maxWidth = 500.0
					alignment = Pos.CENTER
				})
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
					selectionModel.select(config.minStatus)
					selectionModel.selectedItemProperty().addListener { _, _, value ->
						config.minStatus = value
						controller.saveConfig()
					}
				})

		return row
	}

	private fun initializeAbout(rowCount: Int): Int = gridPane.run {
		var row = rowCount

		addRow(row++,
				Label("Author:").apply {
					maxWidth = Double.MAX_VALUE
					maxHeight = Double.MAX_VALUE
					alignment = Pos.CENTER_RIGHT
				},
				Label("Jan Kubovy (jan@kubovy.eu)").apply { maxHeight = Double.MAX_VALUE })

		addRow(row++,
				Label("Icons:").apply {
					maxWidth = Double.MAX_VALUE
					maxHeight = Double.MAX_VALUE
					alignment = Pos.CENTER_RIGHT
				},
				Label("Icons by Icon8 (https://icons8.com)").apply {
					maxHeight = Double.MAX_VALUE
					style = "-fx-text-fill: #009"
					setOnMouseEntered { scene.cursor = Cursor.HAND; }
					setOnMouseExited { scene.cursor = Cursor.DEFAULT; }
					setOnMouseClicked { URI("https://icons8.com").openInExternalApplication() }
				})

		return row
	}

	private fun TreeItem<ModuleItem>.duplicate() {
		val module = value?.module?.definition
		if (module != null) {
			val config = objectMapper
					.readValue(objectMapper.writeValueAsString(value.module?.config), ModuleConfig::class.java)
					.apply {
						uuid = UUID.randomUUID().toString()
						name = "${name} (Copy)"
						enabled = false
					}
			val ctrl = module.loadController(controller, config)
			controller.add(ctrl)?.also { parent.children.addItem(it) }
			tree.refresh()
		}
	}

	private fun TreeItem<ModuleItem>.delete() {
		confirmDialog(
				title = "Delete confirmation",
				content = "Do you really want to delete ${this.value.title.get()}?") {
			this.value.module?.destroy()
			this.remove()
			tree.refresh()
		}
	}

	private fun removeSilencedStatusItem(statusItem: SilencedStatusItem) {
		confirmDialog(
				title = "Unsilence confirmation",
				content = "Do you really want to unsilence ${statusItem.item.title}?") {
			tableSilencedStatusItems.items.remove(statusItem)
			controller.applicationConfiguration.silenced.remove(statusItem.item.id)
			controller.saveConfig()
		}
	}
}
