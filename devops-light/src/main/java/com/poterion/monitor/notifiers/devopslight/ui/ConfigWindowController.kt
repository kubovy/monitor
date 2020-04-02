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
package com.poterion.monitor.notifiers.devopslight.ui

import com.poterion.communication.serial.communicator.BluetoothCommunicator
import com.poterion.communication.serial.communicator.Channel
import com.poterion.communication.serial.communicator.USBCommunicator
import com.poterion.communication.serial.listeners.RgbLightCommunicatorListener
import com.poterion.communication.serial.payload.RgbColor
import com.poterion.communication.serial.payload.RgbLightConfiguration
import com.poterion.communication.serial.payload.RgbLightPattern
import com.poterion.monitor.api.CommonIcon
import com.poterion.monitor.api.toColor
import com.poterion.monitor.api.toRGBColor
import com.poterion.monitor.api.utils.title
import com.poterion.monitor.api.utils.toIcon
import com.poterion.monitor.data.Status
import com.poterion.monitor.data.services.ServiceConfig
import com.poterion.monitor.data.services.ServiceSubConfig
import com.poterion.monitor.notifiers.devopslight.DevOpsLightIcon
import com.poterion.monitor.notifiers.devopslight.control.DevOpsLightNotifier
import com.poterion.monitor.notifiers.devopslight.data.DevOpsLightConfig
import com.poterion.monitor.notifiers.devopslight.data.DevOpsLightItemConfig
import com.poterion.monitor.notifiers.devopslight.data.StateConfig
import com.poterion.monitor.notifiers.devopslight.deepCopy
import com.poterion.utils.javafx.*
import com.poterion.utils.kotlin.ensureSuffix
import com.poterion.utils.kotlin.noop
import javafx.application.Platform
import javafx.beans.InvalidationListener
import javafx.beans.property.IntegerProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.FXCollections
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Parent
import javafx.scene.control.*
import javafx.scene.control.Alert.AlertType
import javafx.scene.image.ImageView
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.layout.Background
import javafx.scene.layout.BackgroundFill
import javafx.scene.layout.CornerRadii
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import javafx.util.StringConverter
import kotlin.math.roundToInt


/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
class ConfigWindowController : RgbLightCommunicatorListener {
	companion object {
		internal fun getRoot(config: DevOpsLightConfig, controller: DevOpsLightNotifier): Pair<Parent, ConfigWindowController> =
				FXMLLoader(ConfigWindowController::class.java.getResource("config-window.fxml"))
						.let { it.load<Parent>() to it.getController<ConfigWindowController>() }
						.let { (root, ctrl) ->
							ctrl.config = config
							ctrl.notifier = controller
							ctrl.load()
							root to ctrl
						}
	}

	@FXML private lateinit var splitPane: SplitPane
	@FXML private lateinit var treeConfigs: TreeView<StateConfig>
	@FXML private lateinit var comboServiceConfig: ComboBox<ServiceConfig<out ServiceSubConfig>>
	@FXML private lateinit var comboServiceSubConfig: ComboBox<String>
	@FXML private lateinit var buttonAddConfig: Button
	@FXML private lateinit var buttonDeleteConfig: Button

	@FXML private lateinit var textServiceName: TextField
	@FXML private lateinit var comboboxStatus: ComboBox<Status>
	@FXML private lateinit var comboBoxPattern: ComboBox<RgbLightPattern>
	@FXML private lateinit var choiceRainbow: ChoiceBox<String>
	@FXML private lateinit var comboBoxColor1: ColorPicker
	@FXML private lateinit var comboBoxColor2: ColorPicker
	@FXML private lateinit var comboBoxColor3: ColorPicker
	@FXML private lateinit var comboBoxColor4: ColorPicker
	@FXML private lateinit var comboBoxColor5: ColorPicker
	@FXML private lateinit var comboBoxColor6: ColorPicker
	@FXML private lateinit var comboBoxColor7: ColorPicker
	@FXML private lateinit var textDelay: TextField
	@FXML private lateinit var textWidth: TextField
	@FXML private lateinit var labelFade: Label
	@FXML private lateinit var textFade: TextField
	@FXML private lateinit var sliderMin: Slider
	@FXML private lateinit var sliderMax: Slider
	@FXML private lateinit var textTimeout: TextField
	@FXML private lateinit var labelMinValue: Label
	@FXML private lateinit var labelMaxValue: Label
	@FXML private lateinit var buttonTestLight: Button
	@FXML private lateinit var buttonSaveLight: Button
	@FXML private lateinit var buttonClearLight: Button

	@FXML private lateinit var tableLightConfigs: TableView<RgbLightConfiguration>
	@FXML private lateinit var columnLightPattern: TableColumn<RgbLightConfiguration, RgbLightPattern>
	@FXML private lateinit var columnLightColor1: TableColumn<RgbLightConfiguration, RgbColor>
	@FXML private lateinit var columnLightColor2: TableColumn<RgbLightConfiguration, RgbColor>
	@FXML private lateinit var columnLightColor3: TableColumn<RgbLightConfiguration, RgbColor>
	@FXML private lateinit var columnLightColor4: TableColumn<RgbLightConfiguration, RgbColor>
	@FXML private lateinit var columnLightColor5: TableColumn<RgbLightConfiguration, RgbColor>
	@FXML private lateinit var columnLightColor6: TableColumn<RgbLightConfiguration, RgbColor>
	@FXML private lateinit var columnLightColor7: TableColumn<RgbLightConfiguration, RgbColor>
	@FXML private lateinit var columnLightDelay: TableColumn<RgbLightConfiguration, Int>
	@FXML private lateinit var columnLightWidth: TableColumn<RgbLightConfiguration, Int>
	@FXML private lateinit var columnLightFading: TableColumn<RgbLightConfiguration, Int>
	@FXML private lateinit var columnLightMinimum: TableColumn<RgbLightConfiguration, Int>
	@FXML private lateinit var columnLightMaximum: TableColumn<RgbLightConfiguration, Int>
	@FXML private lateinit var columnLightTimeout: TableColumn<RgbLightConfiguration, Int>

	@FXML private lateinit var buttonTestLightSequence: Button
	@FXML private lateinit var buttonTurnOffLight: Button
	@FXML private lateinit var buttonMoveUpLight: Button
	@FXML private lateinit var buttonMoveDownLight: Button
	@FXML private lateinit var buttonDeleteLight: Button
	@FXML private lateinit var btnConnect: Button

	@FXML private lateinit var iconBluetooth: ImageView
	@FXML private lateinit var iconUSB: ImageView

	private lateinit var config: DevOpsLightConfig
	private lateinit var notifier: DevOpsLightNotifier
	private var currentLightConfiguration: List<RgbLightConfiguration> = emptyList()
	private val clipboard = mutableListOf<RgbLightConfiguration>()

	private val patterns = RgbLightPattern.values().toList()

	private val selectedConfig = SimpleObjectProperty<DevOpsLightItemConfig?>(null)
	private val selectedLightConfigs = SimpleObjectProperty<ObservableList<RgbLightConfiguration>?>(null)

	private val configComparator: Comparator<TreeItem<StateConfig>> = Comparator { i1, i2 ->
		notifier.titleComparator.compare(i1.value.title, i2.value.title)
	}

	private val ServiceConfig<*>.icon: Icon?
		get() = notifier.controller.modules.find { it.configClass == this::class }?.icon

	@FXML
	fun initialize() {
		val unselectedLightConfig = treeConfigs
				.selectionModel
				.selectedItemProperty()
				.mapped { it?.value?.lightConfigs }
				.isNull

		comboboxStatus.items = Status.values().toObservableList()
		comboboxStatus.isDisable = true
		comboboxStatus.factory { item, empty ->
			graphic = item.takeUnless { empty }?.toIcon()?.toImageView()
			text = item.takeUnless { empty }?.name
		}

		comboBoxPattern.apply {
			items.addAll(patterns)
			selectionModel.select(0)
			converter = object : StringConverter<RgbLightPattern>() {
				override fun toString(obj: RgbLightPattern?): String = obj?.title ?: ""
				override fun fromString(string: String?): RgbLightPattern = patterns
						.firstOrNull { it.title == string }
						?: patterns.first()
			}
			factory { item, empty ->
				text = item?.takeUnless { empty }?.title
			}
			disableProperty().bind(unselectedLightConfig)
		}

		val customColorChangeListener = ListChangeListener<Color> { change ->
			setCustomColors(change.list)
			config.customColors.setAll(change.list.map { it.toRGBColor() })
			notifier.controller.saveConfig()
		}

		choiceRainbow.items.addAll("Colors", "Rainbow Row", "Rainbow Row Circle", "Rainbow", "Rainbow Circle")
		choiceRainbow.disableProperty().bind(unselectedLightConfig
				.or(comboBoxPattern.selectionModel.selectedIndexProperty().isEqualTo(0)))

		comboBoxColor1.customColors.addListener(customColorChangeListener)
		comboBoxColor1.disableProperty().bind(unselectedLightConfig
				.or(comboBoxPattern.selectionModel.selectedIndexProperty().isEqualTo(0))
				.or(choiceRainbow.selectionModel.selectedIndexProperty().greaterThan(0)))

		comboBoxColor2.customColors.addListener(customColorChangeListener)
		comboBoxColor2.disableProperty().bind(unselectedLightConfig
				.or(comboBoxPattern.selectionModel.selectedIndexProperty().isEqualTo(0))
				.or(choiceRainbow.selectionModel.selectedIndexProperty().greaterThan(0)))

		comboBoxColor3.customColors.addListener(customColorChangeListener)
		comboBoxColor3.disableProperty().bind(unselectedLightConfig
				.or(comboBoxPattern.selectionModel.selectedIndexProperty().isEqualTo(0))
				.or(choiceRainbow.selectionModel.selectedIndexProperty().greaterThan(0)))

		comboBoxColor4.customColors.addListener(customColorChangeListener)
		comboBoxColor4.disableProperty().bind(unselectedLightConfig
				.or(comboBoxPattern.selectionModel.selectedIndexProperty().isEqualTo(0))
				.or(choiceRainbow.selectionModel.selectedIndexProperty().greaterThan(0)))

		comboBoxColor5.customColors.addListener(customColorChangeListener)
		comboBoxColor5.disableProperty().bind(unselectedLightConfig
				.or(comboBoxPattern.selectionModel.selectedIndexProperty().isEqualTo(0)))

		comboBoxColor6.customColors.addListener(customColorChangeListener)
		comboBoxColor6.disableProperty().bind(unselectedLightConfig
				.or(comboBoxPattern.selectionModel.selectedIndexProperty().isEqualTo(0)))

		comboBoxColor7.customColors.addListener(customColorChangeListener)
		comboBoxColor7.disableProperty().bind(unselectedLightConfig
				.or(comboBoxPattern.selectionModel.selectedIndexProperty().isEqualTo(0))
				.or(choiceRainbow.selectionModel.selectedIndexProperty().greaterThan(0)))

		textDelay.disableProperty().bind(unselectedLightConfig
				.or(comboBoxPattern.selectionModel.selectedItemProperty().matches { it?.delay == null }))

		textWidth.disableProperty().bind(unselectedLightConfig
				.or(comboBoxPattern.selectionModel.selectedItemProperty().matches { it?.width == null }))

		textFade.disableProperty().bind(unselectedLightConfig
				.or(comboBoxPattern.selectionModel.selectedItemProperty().matches { it?.fading == null }
						.and(choiceRainbow.selectionModel.selectedIndexProperty().isEqualTo(0))))

		sliderMin.valueProperty().addListener { _, _, value ->
			labelMinValue.text = "${value.toInt()} (${(value.toDouble() * 100.0 / 255.0).roundToInt()}%)"
		}
		sliderMin.disableProperty().bind(unselectedLightConfig
				.or(comboBoxPattern.selectionModel.selectedItemProperty().matches { it?.min == null }))

		sliderMax.valueProperty().addListener { _, _, value ->
			labelMaxValue.text = "${value.toInt()} (${(value.toDouble() * 100.0 / 255.0).roundToInt()}%)"
		}
		sliderMax.disableProperty().bind(unselectedLightConfig
				.or(comboBoxPattern.selectionModel.selectedItemProperty().matches { it?.max == null }))

		textTimeout.disableProperty().bind(unselectedLightConfig
				.or(comboBoxPattern.selectionModel.selectedItemProperty().matches { it?.timeout == null }))

		treeConfigs.apply {
			isShowRoot = false
			selectionModel.selectionMode = SelectionMode.SINGLE
			cell { _, item, empty ->
				text = item?.takeUnless { empty }
						?.let { i -> i.title to (i.lightConfigs.size.takeIf { i.status != null }) }
						?.let { (title, n) -> (if (title.isEmpty()) "Default" else title) to n }
						?.let { (title, n) -> title to (n?.let { (if (n == 0) " (empty)" else " (${n})") } ?: "") }
						?.let { (title, suffix) -> "${title}${suffix}" }
				graphic = item?.takeUnless { empty }
						?.let {
							it.status?.toIcon()
									?: notifier.controller.applicationConfiguration.serviceMap[it.serviceId]?.icon
						}
						?.toImageView()

				style = item
						?.takeUnless { empty }
						?.let { i -> i.lightConfigs.takeIf { i.status != null } }
						?.takeIf { it.isEmpty() }
						?.let { "-fx-text-fill: #999; -fx-font-style: italic;" }
				contextMenu = item?.takeUnless { empty }?.createContextMenu()
			}
			selectionModel.selectedItemProperty().addListener { _, _, item ->
				var selected = item
				while (selected?.value?.status != null) selected = selected.parent
				val itemConfig = config.items
						.find { it.id == selected?.value?.serviceId && it.subId == selected?.value?.subConfigId }
				selectedConfig.set(itemConfig)
				selectedLightConfigs.set(when (item?.value?.status) {
					Status.NONE -> itemConfig?.statusNone
					//Status.OFF -> null
					Status.UNKNOWN -> itemConfig?.statusUnknown
					Status.OK -> itemConfig?.statusOk
					Status.INFO -> itemConfig?.statusInfo
					Status.NOTIFICATION -> itemConfig?.statusNotification
					Status.WARNING -> itemConfig?.statusWarning
					Status.ERROR -> itemConfig?.statusError
					Status.CONNECTION_ERROR -> itemConfig?.statusConnectionError
					Status.SERVICE_ERROR -> itemConfig?.statusServiceError
					Status.FATAL -> itemConfig?.statusFatal
					else -> null
				})
				comboboxStatus.value = item?.value?.status
				textServiceName.text = (item?.value?.takeIf { it.status == null } ?: item?.parent?.value)?.title
				tableLightConfigs.selectionModel.clearSelection()
				tableLightConfigs.items = selectedLightConfigs.get() ?: FXCollections.emptyObservableList()
			}
		}

		tableLightConfigs.apply {
			selectionModel.selectionMode = SelectionMode.SINGLE
			selectionModel.selectedItemProperty().addListener { _, _, value -> selectLightConfig(value) }
		}

		columnLightPattern.cell("pattern") { item, value, empty ->
			text = value?.takeUnless { empty }?.title
			contextMenu = item?.takeUnless { empty }?.createContextMenu()
		}

		columnLightColor1.init("color1")
		columnLightColor2.init("color2")
		columnLightColor3.init("color3")
		columnLightColor4.init("color4")
		columnLightColor5.init("color5")
		columnLightColor6.init("color6")
		columnLightColor7.init("color7")

		columnLightDelay.cell("delay") { item, value, empty ->
			text = value?.takeUnless { empty }?.takeIf { item?.pattern?.delay != null }?.let { "${it} ms" }
			alignment = Pos.CENTER_RIGHT
			contextMenu = item?.takeUnless { empty }?.createContextMenu()
		}
		columnLightWidth.cell("width") { item, value, empty ->
			text = value?.takeUnless { empty }?.takeIf { item?.pattern?.width != null }?.toString()
			alignment = Pos.CENTER_RIGHT
			contextMenu = item?.takeUnless { empty }?.createContextMenu()
		}
		columnLightFading.cell("fading") { item, value, empty ->
			text = value?.takeUnless { empty }?.takeIf { item?.pattern?.fading != null }?.toString()
			alignment = Pos.CENTER_RIGHT
			contextMenu = item?.takeUnless { empty }?.createContextMenu()
		}
		columnLightMinimum.cell("minimum") { item, value, empty ->
			text = value?.takeUnless { empty }?.takeIf { item?.pattern?.min != null }
					?.times(100.0)?.div(255.0)?.roundToInt()?.toString()?.ensureSuffix("%")
			alignment = Pos.CENTER_RIGHT
			contextMenu = item?.takeUnless { empty }?.createContextMenu()
		}
		columnLightMaximum.cell("maximum") { item, value, empty ->
			text = value?.takeUnless { empty }?.takeIf { item?.pattern?.max != null }
					?.times(100.0)?.div(255.0)?.roundToInt()?.toString()?.ensureSuffix("%")
			alignment = Pos.CENTER_RIGHT
			contextMenu = item?.takeUnless { empty }?.createContextMenu()
		}
		columnLightTimeout.cell("timeout") { item, value, empty ->
			text = value?.takeUnless { empty }?.takeIf { item?.pattern?.timeout != null }?.toString()?.ensureSuffix("x")
			alignment = Pos.CENTER_RIGHT
			contextMenu = item?.takeUnless { empty }?.createContextMenu()
		}

		val tableLightConfigsSize: IntegerProperty = SimpleIntegerProperty(tableLightConfigs.items.size)
		tableLightConfigs.items.addListener(InvalidationListener { tableLightConfigsSize.set(tableLightConfigs.items.size) })

		buttonTestLight.disableProperty().bind(unselectedLightConfig)
		buttonSaveLight.disableProperty().bind(unselectedLightConfig)
		buttonClearLight.disableProperty().bind(unselectedLightConfig)
		buttonTestLightSequence.disableProperty().bind(unselectedLightConfig)
		buttonMoveUpLight.disableProperty().bind(tableLightConfigs.selectionModel.selectedItemProperty().isNull
				.or(tableLightConfigs.selectionModel.selectedIndexProperty().isEqualTo(0)))
		buttonMoveDownLight.disableProperty().bind(tableLightConfigs.selectionModel.selectedItemProperty().isNull
				.or(tableLightConfigs.selectionModel.selectedIndexProperty().isEqualTo(tableLightConfigsSize.subtract(1))))
		buttonClearLight.disableProperty().bind(tableLightConfigs.selectionModel.selectedItemProperty().isNull)
		buttonDeleteLight.disableProperty().bind(tableLightConfigs.selectionModel.selectedItemProperty().isNull)

		buttonDeleteConfig.disableProperty().bind(treeConfigs.selectionModel.selectedItemProperty()
				.matches { it?.value?.serviceId == null })

		treeConfigs.selectionModel.clearSelection()
		selectLightConfig(null)
	}

	private fun load() {
		splitPane.setDividerPosition(0, config.split)
		splitPane.dividers.first().positionProperty().addListener { _, _, value ->
			config.split = value.toDouble()
			notifier.controller.saveConfig()
		}

		config.items.forEach { config -> config.id = config.id?.takeIf { it.isNotEmpty() } } // Fix old configs
		if (config.items.none { it.id == null }) config.items.add(DevOpsLightItemConfig())

		treeConfigs.root = TreeItem(StateConfig("Configurations")).apply {
			children.addAll(config.items.map { it.toTreeItem() })
			children.sortWith(configComparator)
		}

		treeConfigs.selectionModel.select(config
				.selectedItemId
				.split(";")
				.takeIf { it.size >= 3 }
				?.map { it.takeIf { it.isNotBlank() } }
				?.let { (id, subId, status) ->
					val parent = treeConfigs.root.find { it?.serviceId == id && it?.subConfigId == subId }
					if (status == null) parent else parent?.find { it?.status?.name == status }
				}
				?: treeConfigs.root.children.firstOrNull())
		treeConfigs.selectionModel.selectedItemProperty().addListener { _, _, selected ->
			config.selectedItemId = (if (selected?.value?.status != null) selected.parent else selected)
					?.value
					?.let { it.serviceId to it.subConfigId }
					?.let { (serviceId, subId) -> Triple(serviceId, subId, selected?.value?.status?.name) }
					?.let { (serviceId, subId, status) -> "${serviceId ?: ""};${subId ?: ""};${status ?: ""}" }
					?: ";;"
			notifier.controller.saveConfig()
		}
		config.items.addListener(ListChangeListener { change ->
			while (change.next()) when {
				change.wasAdded() -> change.addedSubList.forEach { lightConfig ->
					treeConfigs.root.children.add(lightConfig.toTreeItem())
					treeConfigs.root.children.sortWith(configComparator)

					comboServiceConfig.apply {
						selectionModel.clearSelection()
						value = null
					}

					notifier.controller.saveConfig()
				}
				change.wasRemoved() -> change.removed
						.map { c ->
							treeConfigs.root.children
									.find { it.value?.serviceId == c.id && it.value?.subConfigId == c.subId }
						}
						.forEach { treeItem ->
							treeItem?.value?.key?.also { config.expanded.remove(it) }
							treeItem?.parent?.children?.remove(treeItem)

							notifier.controller.saveConfig()
						}
			}
		})

		comboServiceConfig.apply {
			factory { item, empty ->
				val serviceConfig = item.takeUnless { empty }
				if (serviceConfig != null) {
					textProperty().bind(serviceConfig.nameProperty)
					graphic = serviceConfig.icon?.toImageView()
				} else {
					if (textProperty().isBound) textProperty().unbind()
					text = null
					graphic = null
				}
			}
			items = notifier.controller
					.applicationConfiguration
					.services
					.sorted(compareBy { it.name })

			selectionModel.selectedItemProperty().addListener { _, _, service ->
				comboServiceSubConfig.items.clear()
				comboServiceSubConfig.items.add(null)
				service?.subConfig?.mapNotNull { it?.configTitle }?.sorted()
						?.also { comboServiceSubConfig.items.addAll(it) }
				comboServiceSubConfig.selectionModel.clearSelection()
			}

			selectionModel.clearSelection()
			value = null
		}

		comboServiceSubConfig.factory { item, empty -> text = (item ?: "").takeUnless { empty } }

		setCustomColors(config.customColors.map { it.toColor() })

		// Status
		iconBluetooth.image =
				if (notifier.bluetoothCommunicator.isConnected) DevOpsLightIcon.BLUETOOTH_CONNECTED.toImage()
				else DevOpsLightIcon.BLUETOOTH_DISCONNECTED.toImage()
		iconUSB.image =
				if (notifier.usbCommunicator.isConnected) DevOpsLightIcon.USB_CONNECTED.toImage()
				else DevOpsLightIcon.USB_DISCONNECTED.toImage()

		notifier.bluetoothCommunicator.register(this)
		notifier.usbCommunicator.register(this)

		notifier.controller.applicationConfiguration.services.forEach { service ->
			val listener = serviceSubConfigListenerMap
					.getOrPut(service.uuid) { createServiceSubConfigListener(service) }
			service.subConfig.addListener(listener)
		}
		notifier.controller.applicationConfiguration.services.addListener(ListChangeListener { change ->
			while (change.next()) when {
				change.wasAdded() -> change.addedSubList.forEach { service ->
					val listener = serviceSubConfigListenerMap
							.getOrPut(service.uuid) { createServiceSubConfigListener(service) }
					service.subConfig.addListener(listener)
				}
				change.wasRemoved() -> change.removed.forEach { service ->
					service.subConfig.removeListener(serviceSubConfigListenerMap.remove(service.uuid))
					treeConfigs.root.children.removeIf { it?.value?.serviceId == service.uuid }
				}
			}
		})
	}

	private val serviceSubConfigListenerMap = mutableMapOf<String, ListChangeListener<ServiceSubConfig>>()

	private fun createServiceSubConfigListener(service: ServiceConfig<out ServiceSubConfig>) = object : ListChangeListener<ServiceSubConfig> {
		private val serviceId = service.uuid

		override fun onChanged(change: ListChangeListener.Change<out ServiceSubConfig>) {
			while (change.next()) if (change.wasRemoved()) change.removed.forEach { removed ->
				treeConfigs.root.children.removeIf {
					it?.value?.serviceId == serviceId
							&& it.value?.subConfigId == removed?.configTitle
				}
			}
		}
	}

	@FXML
	fun onKeyPressed(keyEvent: KeyEvent) = when (keyEvent.code) {
		KeyCode.F3 -> onTestLight()
		KeyCode.F4 -> onTestLightSequence()
		KeyCode.F5 -> onReconnect()
		KeyCode.F12 -> onTurnOffLight()
		KeyCode.S -> if (keyEvent.isControlDown) onSaveLight() else null
		KeyCode.ESCAPE -> onClearLight()
		else -> null
	}

	@FXML
	fun onAddConfig() = (comboServiceConfig.value to comboServiceSubConfig.value)
			.let { (config, sub) -> config.let { it to sub } }
			.also { (service, subConfig) -> config.items.add(DevOpsLightItemConfig(service.uuid, subConfig)) }

	@FXML
	fun onDeleteSelectedConfig() {
		var selected = treeConfigs.selectionModel.selectedItem
		while (selected != null && selected.value.status != null) selected = selected.parent
		val stateConfig = selected?.value

		if (stateConfig != null && selected.value.serviceId != null) Alert(AlertType.CONFIRMATION).apply {
			title = "Delete confirmation"
			headerText = "Delete ${stateConfig.title}"
			contentText = "Do you really want to delete the this whole configuration?"
			buttonTypes.setAll(ButtonType.YES, ButtonType.NO)
		}.showAndWait().ifPresent { btnType ->
			btnType.takeIf { it == ButtonType.YES }
					?.takeIf { stateConfig.serviceId != null }
					?.also {
						config.items.removeIf { it.id == stateConfig.serviceId && it.subId == stateConfig.subConfigId }
					}
		}
	}

	@FXML
	fun onKeyPressedInTree(keyEvent: KeyEvent) {
		if (treeConfigs.selectionModel.selectedItem != null) when (keyEvent.code) {
			KeyCode.DELETE -> onDeleteSelectedConfig()
			KeyCode.C -> if (keyEvent.isControlDown) {
				clipboard.clear()
				clipboard.addAll(tableLightConfigs.items.deepCopy())
			}
			KeyCode.V -> if (keyEvent.isControlDown) {
				selectedLightConfigs.get()?.addAll(clipboard.deepCopy())
				notifier.controller.saveConfig()
				treeConfigs.refresh()
			}
			else -> noop()
		}
	}

	@FXML
	fun onTestLight() {
		createLightConfig()?.also { notifier.changeLights(listOf(it)) }
	}

	@FXML
	fun onTestLightSequence() {
		tableLightConfigs.items.takeIf { it.isNotEmpty() }?.also { notifier.changeLights(it) }
	}

	@FXML
	fun onTurnOffLight() {
		notifier.changeLights(listOf(RgbLightConfiguration()))
	}

	@FXML
	fun onMoveUpLight() {
		val selectedLight = tableLightConfigs.selectionModel.selectedItem?.copy()
		tableLightConfigs.selectionModel.selectedIndex
				.takeIf { it > 0 && it < tableLightConfigs.items.size }
				?.also {
					tableLightConfigs.items.removeAt(it)
					tableLightConfigs.items.add(it - 1, selectedLight)
					tableLightConfigs.selectionModel.select(it - 1)
					notifier.controller.saveConfig()
				}
	}

	@FXML
	fun onMoveDownLight() {
		val selectedLight = tableLightConfigs.selectionModel.selectedItem?.copy()
		tableLightConfigs.selectionModel.selectedIndex
				.takeIf { it >= 0 && it < tableLightConfigs.items.size - 1 }
				?.also {
					tableLightConfigs.items.removeAt(it)
					tableLightConfigs.items.add(it + 1, selectedLight)
					tableLightConfigs.selectionModel.select(it + 1)
					notifier.controller.saveConfig()
				}
	}

	@FXML
	fun onSaveLight() {
		val selectedIndex = tableLightConfigs.selectionModel.selectedIndex
		val configuredLight = createLightConfig()
		if (configuredLight != null) {
			if (selectedIndex < 0) {
				tableLightConfigs.items.add(configuredLight)
			} else if (selectedIndex >= 0) {
				tableLightConfigs.items[tableLightConfigs.selectionModel.selectedIndex] = configuredLight
			}
			tableLightConfigs.refresh()
			notifier.controller.saveConfig()
			tableLightConfigs.selectionModel.clearSelection()
			treeConfigs.refresh()
		}
	}

	@FXML
	fun onClearLight() = tableLightConfigs.selectionModel.clearSelection()

	@FXML
	fun onDeleteLight() {
		tableLightConfigs.selectionModel.selectedIndex.takeIf { it >= 0 }?.also {
			tableLightConfigs.items.removeAt(it)
			notifier.controller.saveConfig()
			treeConfigs.refresh()
		}
	}

	@FXML
	fun onReconnect() {
		if (notifier.bluetoothCommunicator.isConnected || notifier.usbCommunicator.isConnected) {
			if (notifier.bluetoothCommunicator.isConnected) notifier.bluetoothCommunicator.disconnect()
			if (notifier.usbCommunicator.isConnected) notifier.usbCommunicator.disconnect()
		} else if (notifier.bluetoothCommunicator.isConnecting || notifier.usbCommunicator.isConnecting) {
			if (notifier.bluetoothCommunicator.isConnecting) notifier.bluetoothCommunicator.disconnect()
			if (notifier.usbCommunicator.isConnecting) notifier.usbCommunicator.disconnect()
		} else {
			notifier.bluetoothCommunicator.connect(BluetoothCommunicator.Descriptor(config.deviceAddress, 6))
			notifier.usbCommunicator.connect(USBCommunicator.Descriptor(config.usbPort))
		}
	}

	@FXML
	fun onKeyPressedInTable(keyEvent: KeyEvent) = when (keyEvent.code) {
		KeyCode.DELETE -> onDeleteLight()
		KeyCode.UP -> if (keyEvent.isAltDown) onMoveUpLight() else null
		KeyCode.DOWN -> if (keyEvent.isAltDown) onMoveDownLight() else null
		KeyCode.C -> if (keyEvent.isControlDown) {
			clipboard.clear()
			tableLightConfigs.selectionModel.selectedItem?.copy()?.also { clipboard.add(it) }
			null
		} else null
		KeyCode.V -> if (keyEvent.isControlDown) {
			tableLightConfigs.items.addAll(tableLightConfigs.selectionModel.selectedIndex.takeIf { it >= 0 } ?: 0,
					clipboard.deepCopy())
			notifier.controller.saveConfig()
			treeConfigs.refresh()
			null
		} else null
		else -> null
	}

	override fun onConnecting(channel: Channel) = Platform.runLater {
		btnConnect.text = "Cancel [F5]"
	}

	override fun onConnect(channel: Channel) = Platform.runLater {
		btnConnect.text = "Disconnect [F5]"
		when (channel) {
			Channel.BLUETOOTH -> iconBluetooth.image = DevOpsLightIcon.BLUETOOTH_CONNECTED.toImage()
			Channel.USB -> iconUSB.image = DevOpsLightIcon.USB_CONNECTED.toImage()
		}
	}

	override fun onDisconnect(channel: Channel) = Platform.runLater {
		btnConnect.text = "Connect [F5]"
		when (channel) {
			Channel.BLUETOOTH -> iconBluetooth.image = DevOpsLightIcon.BLUETOOTH_DISCONNECTED.toImage()
			Channel.USB -> iconUSB.image = DevOpsLightIcon.USB_DISCONNECTED.toImage()
		}
	}

	override fun onRgbLightCountChanged(channel: Channel, count: Int) = noop()

	override fun onRgbLightConfiguration(channel: Channel, num: Int, count: Int, index: Int,
										 configuration: RgbLightConfiguration) = noop()

	internal fun changeLights(lightConfiguration: List<RgbLightConfiguration>?) {
		currentLightConfiguration = lightConfiguration ?: emptyList()
	}

	private fun selectLightConfig(lightConfig: RgbLightConfiguration?) {
		buttonSaveLight.text = if (lightConfig == null) "Add [Ctrl+S]" else "Save [Ctrl+S]"
		(patterns.firstOrNull { it == lightConfig?.pattern } ?: patterns.first()).also { pattern ->
			comboBoxPattern.selectionModel.select(pattern)
			comboBoxColor1.value = lightConfig?.color1?.toColor() ?: Color.BLACK
			comboBoxColor2.value = lightConfig?.color2?.toColor() ?: Color.BLACK
			comboBoxColor3.value = lightConfig?.color3?.toColor() ?: Color.BLACK
			comboBoxColor4.value = lightConfig?.color4?.toColor() ?: Color.BLACK
			comboBoxColor5.value = lightConfig?.color5?.toColor() ?: Color.BLACK
			comboBoxColor6.value = lightConfig?.color6?.toColor() ?: Color.BLACK
			comboBoxColor7.value = lightConfig?.color7?.toColor() ?: Color.BLACK
			textDelay.text = "${lightConfig?.delay ?: 50}"
			textWidth.text = "${lightConfig?.width ?: 3}"
			textFade.text = "${lightConfig?.fading ?: 0}"
			sliderMin.value = lightConfig?.minimum?.toDouble() ?: 0.0
			sliderMax.value = lightConfig?.maximum?.toDouble() ?: 255.0
			textTimeout.text = "${lightConfig?.timeout ?: 10}"
			choiceRainbow.selectionModel.select(lightConfig?.rainbow ?: 0)
		}
	}

	private fun createLightConfig(): RgbLightConfiguration? {
		val pattern = comboBoxPattern.selectionModel.selectedItem
		val color1 = comboBoxColor1.value?.toRGBColor()
		val color2 = comboBoxColor2.value?.toRGBColor()
		val color3 = comboBoxColor3.value?.toRGBColor()
		val color4 = comboBoxColor4.value?.toRGBColor()
		val color5 = comboBoxColor5.value?.toRGBColor()
		val color6 = comboBoxColor6.value?.toRGBColor()
		val color7 = comboBoxColor7.value?.toRGBColor()
		val delay = textDelay.text?.toIntOrNull() ?: 1000
		val width = textWidth.text?.toIntOrNull() ?: 3
		val fade = textFade.text?.toIntOrNull() ?: 0
		val min = sliderMin.value.toInt()
		val max = sliderMax.value.toInt()
		val timeout = textTimeout.text.toIntOrNull() ?: 10
		val rainbow = choiceRainbow.selectionModel.selectedIndex

		return if (pattern != null
				&& color1 != null
				&& color2 != null
				&& color3 != null
				&& color4 != null
				&& color5 != null
				&& color6 != null
				&& color7 != null)
			RgbLightConfiguration(pattern, color1, color2, color3, color4, color5, color6, color7, delay, width, fade,
					min, max, timeout, rainbow)
		else null
	}

	private fun setCustomColors(colors: List<Color>) {
		val customColors = colors
				.takeUnless { it.isEmpty() }
				?: listOf(Color.RED, Color.LIME, Color.BLUE, Color.MAGENTA, Color.YELLOW, Color.CYAN)
		// Color.AZURE, Color.AQUA, Color.PINK, Color.OLIVE, Color.NAVY, Color.MAROON
		if (!comboBoxColor1.customColors.isSame(customColors)) comboBoxColor1.customColors.setAll(customColors)
		if (!comboBoxColor2.customColors.isSame(customColors)) comboBoxColor2.customColors.setAll(customColors)
		if (!comboBoxColor3.customColors.isSame(customColors)) comboBoxColor3.customColors.setAll(customColors)
		if (!comboBoxColor4.customColors.isSame(customColors)) comboBoxColor4.customColors.setAll(customColors)
		if (!comboBoxColor5.customColors.isSame(customColors)) comboBoxColor5.customColors.setAll(customColors)
		if (!comboBoxColor6.customColors.isSame(customColors)) comboBoxColor6.customColors.setAll(customColors)
		if (!comboBoxColor7.customColors.isSame(customColors)) comboBoxColor7.customColors.setAll(customColors)
	}

	private fun ServiceConfig<*>?.title(subId: String?): String = this?.name
			?.let { name -> "${name}${subId?.let { " (${it})" } ?: ""}" } ?: "Default"

	private fun DevOpsLightItemConfig.title(service: ServiceConfig<*>?): String = service.title(subId)

	private fun List<Color>.isSame(other: List<Color>): Boolean = this.size == other.size
			&& this.mapIndexed { index, color -> color to other[index] }
			.map { (c, o) -> c.red == o.red && c.green == o.green && c.blue == o.blue }
			.takeUnless { it.isEmpty() }
			?.reduce { acc, b -> acc && b }
			?: true

	private fun TableColumn<RgbLightConfiguration, RgbColor>.init(propertyName: String) = cell(propertyName) { item, value, empty ->
		graphic = Pane().takeUnless { empty }?.apply {
			background = Background(BackgroundFill(value?.toColor() ?: Color.TRANSPARENT,
					CornerRadii.EMPTY,
					Insets.EMPTY))
		}
		contextMenu = item?.takeUnless { empty }?.createContextMenu()
	}

	private val StateConfig.key: String
		get() = "${serviceId ?: "default"}-${subConfigId ?: "all"}"


	private fun DevOpsLightItemConfig.toTreeItem() = TreeItem(StateConfig(
			title = title(notifier.controller.applicationConfiguration.serviceMap[id] as ServiceConfig<*>?),
			status = null,
			serviceId = id,
			subConfigId = subId)).apply {
		children.addAll(
				TreeItem(StateConfig("None", Status.NONE, statusNone)),
				//TreeItem(StateConfig("Off", Status.OFF, statusOff)),
				TreeItem(StateConfig("Unknown", Status.UNKNOWN, statusUnknown)),
				TreeItem(StateConfig("OK", Status.OK, statusOk)),
				TreeItem(StateConfig("Info", Status.INFO, statusInfo)),
				TreeItem(StateConfig("Notification", Status.NOTIFICATION, statusNotification)),
				TreeItem(StateConfig("Warning", Status.WARNING, statusWarning)),
				TreeItem(StateConfig("Error", Status.ERROR, statusError)),
				TreeItem(StateConfig("Connection Error", Status.CONNECTION_ERROR, statusConnectionError)),
				TreeItem(StateConfig("Service Error", Status.SERVICE_ERROR, statusServiceError)),
				TreeItem(StateConfig("Fatal", Status.FATAL, statusFatal)))
		monitorExpansion()
	}

	private fun TreeItem<StateConfig>.monitorExpansion() = monitorExpansion(
			{ item -> config.expanded.contains(item?.key) },
			{ item, expanded ->
				if (expanded) {
					if (!config.expanded.contains(item?.key)) config.expanded.add(item?.key)
				} else {
					config.expanded.remove(item?.key)
				}
				notifier.controller.saveConfig()
			})

	private fun StateConfig.createContextMenu() = (when (status) {
		null -> arrayOf(
				MenuItem("Add light for None status", CommonIcon.STATUS_NONE.toImageView()).apply {
					setOnAction {
						selectedConfig.get()?.statusNone?.add(RgbLightConfiguration())
						notifier.controller.saveConfig()
						treeConfigs.refresh()
					}
				},
				//MenuItem("Add light for Off status", CommonIcon.STATUS_OFF.toImageView()).apply {
				//	setOnAction {
				//		selectedConfig?.get()?.status?.add(RgbLightConfiguration())
				//		notifier.controller.saveConfig()
				//		treeConfigs.refresh()
				//	}
				//},
				MenuItem("Add light for Unknown status", CommonIcon.STATUS_UNKNOWN.toImageView()).apply {
					setOnAction {
						selectedConfig.get()?.statusUnknown?.add(RgbLightConfiguration())
						notifier.controller.saveConfig()
						treeConfigs.refresh()
					}
				},
				MenuItem("Add light for OK status", CommonIcon.STATUS_OK.toImageView()).apply {
					setOnAction {
						selectedConfig.get()?.statusOk?.add(RgbLightConfiguration())
						notifier.controller.saveConfig()
						treeConfigs.refresh()
					}
				},
				MenuItem("Add light for Info status", CommonIcon.STATUS_INFO.toImageView()).apply {
					setOnAction {
						selectedConfig.get()?.statusInfo?.add(RgbLightConfiguration())
						notifier.controller.saveConfig()
						treeConfigs.refresh()
					}
				},
				MenuItem("Add light for Notification status", CommonIcon.STATUS_NOTIFICATION.toImageView()).apply {
					setOnAction {
						selectedConfig.get()?.statusNotification?.add(RgbLightConfiguration())
						notifier.controller.saveConfig()
						treeConfigs.refresh()
					}
				},
				MenuItem("Add light for Warning status", CommonIcon.STATUS_WARNING.toImageView()).apply {
					setOnAction {
						selectedConfig.get()?.statusWarning?.add(RgbLightConfiguration())
						notifier.controller.saveConfig()
						treeConfigs.refresh()
					}
				},
				MenuItem("Add light for Error status", CommonIcon.STATUS_ERROR.toImageView()).apply {
					setOnAction {
						selectedConfig.get()?.statusError?.add(RgbLightConfiguration())
						notifier.controller.saveConfig()
						treeConfigs.refresh()
					}
				},
				MenuItem("Add light for Connection Error status", CommonIcon.BROKEN_LINK.toImageView()).apply {
					setOnAction {
						selectedConfig.get()?.statusConnectionError?.add(RgbLightConfiguration())
						notifier.controller.saveConfig()
						treeConfigs.refresh()
					}
				},
				MenuItem("Add light for Service Error status", CommonIcon.UNAVAILABLE.toImageView()).apply {
					setOnAction {
						selectedConfig.get()?.statusServiceError?.add(RgbLightConfiguration())
						notifier.controller.saveConfig()
						treeConfigs.refresh()
					}
				},
				MenuItem("Add light for Fatal status", CommonIcon.STATUS_FATAL.toImageView()).apply {
					setOnAction {
						selectedConfig.get()?.statusFatal?.add(RgbLightConfiguration())
						notifier.controller.saveConfig()
						treeConfigs.refresh()
					}
				}) +
				when (title) {
					"Default" -> emptyArray()
					else -> arrayOf(
							SeparatorMenuItem(),
							MenuItem("Delete [DEL]", CommonIcon.TRASH.toImageView()).apply {
								setOnAction { onDeleteSelectedConfig() }
							})
				}
		else -> arrayOf(
				MenuItem("Test sequence [F4]", DevOpsLightIcon.TEST.toImageView()).apply {
					setOnAction { onTestLightSequence() }
				},
				SeparatorMenuItem(),
				MenuItem("Copy [Ctrl+C]", CommonIcon.DUPLICATE.toImageView()).apply {
					setOnAction {
						clipboard.clear()
						clipboard.addAll(lightConfigs.deepCopy())
					}
				},
				MenuItem("Paste [Ctrl+V]", CommonIcon.PASTE.toImageView()).apply {
					setOnAction {
						lightConfigs.addAll(clipboard.deepCopy())
						notifier.controller.saveConfig()
						treeConfigs.refresh()
					}
				})
	}).let { ContextMenu(*it) }

	private fun RgbLightConfiguration.createContextMenu() = ContextMenu(
			MenuItem("Move up [Ctrl+Up]", DevOpsLightIcon.MOVE_UP.toImageView()).apply {
				setOnAction { onMoveUpLight() }
			},
			MenuItem("Move down [Ctrl+Down]", DevOpsLightIcon.MOVE_DOWN.toImageView()).apply {
				setOnAction { onMoveDownLight() }
			},
			SeparatorMenuItem(),
			MenuItem("Test [F3]", DevOpsLightIcon.TEST.toImageView()).apply {
				setOnAction { createLightConfig()?.also { notifier.changeLights(listOf(this@createContextMenu)) } }
			},
			SeparatorMenuItem(),
			MenuItem("Copy [Ctrl+C]", CommonIcon.DUPLICATE.toImageView()).apply {
				setOnAction {
					clipboard.clear()
					clipboard.addAll(tableLightConfigs.selectionModel.selectedItems ?: emptyList())
				}
			},
			MenuItem("Paste [Ctrl+V]", CommonIcon.PASTE.toImageView()).apply {
				setOnAction {
					tableLightConfigs.items.addAll(
							tableLightConfigs.selectionModel.selectedIndex.takeIf { it >= 0 } ?: 0,
							clipboard.deepCopy())
					notifier.controller.saveConfig()
					treeConfigs.refresh()
				}
			},
			MenuItem("Delete [DEL]", CommonIcon.TRASH.toImageView()).apply {
				setOnAction {
					tableLightConfigs.selectionModel.selectedIndices.forEach { tableLightConfigs.items.removeAt(it) }
					notifier.controller.saveConfig()
					treeConfigs.refresh()
				}
			})
}
