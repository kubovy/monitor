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
package com.poterion.monitor.notifiers.deploymentcase.ui

import com.poterion.utils.kotlin.noop
import com.poterion.utils.javafx.toImageView
import com.poterion.monitor.notifiers.deploymentcase.*
import com.poterion.monitor.notifiers.deploymentcase.control.*
import com.poterion.monitor.notifiers.deploymentcase.data.*
import javafx.collections.FXCollections
import javafx.collections.ListChangeListener
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.geometry.Pos
import javafx.scene.Parent
import javafx.scene.control.*
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.util.Callback
import javafx.util.StringConverter

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
class ConfigWindowTabStateMachine {
	companion object {
		internal fun getRoot(saveConfig: () -> Unit): Pair<ConfigWindowTabStateMachine, Parent> =
				FXMLLoader(ConfigWindowTabStateMachine::class.java.getResource("config-window-tab-state-machine.fxml"))
						.let { it.load<Parent>() to it.getController<ConfigWindowTabStateMachine>() }
						.let { (root, ctrl) ->
							ctrl.saveConfig = saveConfig
							ctrl to root
						}

	}

	@FXML private lateinit var treeStateMachine: TreeView<StateMachineItem>

	private lateinit var saveConfig: () -> Unit

	private var clipboardStateMachineItem: TreeItem<StateMachineItem>? = null

	@FXML
	fun initialize() {
		treeStateMachine.initEditable()
		treeStateMachine.selectionModel.selectedItemProperty().addListener { _, _, _ -> treeStateMachine.refresh() }
		treeStateMachine.isShowRoot = false
		treeStateMachine.root = TreeItem<StateMachineItem>(Placeholder())
		val stateMachineChangeListener = ListChangeListener<State> { treeStateMachine.setStateMachine(it.list) }
		SharedUiData.stateMachineProperty.addListener { _, oldStates, newStates ->
			oldStates?.removeListener(stateMachineChangeListener)
			newStates?.addListener(stateMachineChangeListener)
			treeStateMachine.setStateMachine(newStates)
		}
	}

	@FXML
	fun onStateMachineTreeKeyPressed(keyEvent: KeyEvent) {
		val selected = treeStateMachine.selectionModel.selectedItem
		when (keyEvent.code) {
			KeyCode.UP -> if (keyEvent.isControlDown || keyEvent.isMetaDown) {
				val parent = selected.parent
				val index = parent.children.indexOf(selected)
				if (index > 0) {
					parent.children[index] = parent.children[index - 1]
					parent.children[index - 1] = selected
					treeStateMachine.selectionModel.select(selected)
					treeStateMachine.refresh()
					SharedUiData.stateMachine.setAll(treeStateMachine.toStateMachine(SharedUiData.devices, SharedUiData.variables))
					saveConfig()
				}
			}
			KeyCode.DOWN -> if (keyEvent.isControlDown || keyEvent.isMetaDown) {
				val parent = selected.parent
				val index = parent.children.indexOf(selected)
				if (index < parent.children.size - 1) {
					parent.children[index] = parent.children[index + 1]
					parent.children[index + 1] = selected
					treeStateMachine.selectionModel.select(selected)
					treeStateMachine.refresh()
					SharedUiData.stateMachine.setAll(treeStateMachine.toStateMachine(SharedUiData.devices, SharedUiData.variables))
					saveConfig()
				}
			}
			KeyCode.C -> if (keyEvent.isControlDown) {
				clipboardStateMachineItem = selected
			}
			KeyCode.D,
			KeyCode.V -> {
				if (keyEvent.isControlDown) {
					val item = if (keyEvent.code == KeyCode.D) selected else clipboardStateMachineItem
					when (item?.value) {
						is Evaluation -> {
							val conditions = item.children[0].children.mapNotNull { (it.value as? Condition)?.copy() }
							val actions = item.children[1].children.mapNotNull { (it.value as? Action)?.copy() }
							val evaluation = TreeItem<StateMachineItem>(Evaluation()).apply {
								children.addAll(
										TreeItem<StateMachineItem>(Placeholder("Conditions", DeploymentCaseIcon.CONDITIONS)
												.apply { isExpanded = true })
												.apply { children.addAll(conditions.map { TreeItem<StateMachineItem>(it) }) },
										TreeItem<StateMachineItem>(Placeholder("Actions", DeploymentCaseIcon.ACTIONS)
												.apply { isExpanded = true })
												.apply { children.addAll(actions.map { TreeItem<StateMachineItem>(it) }) })
							}
							var parent: TreeItem<StateMachineItem>? = selected
							while (parent != null && parent.value !is State) parent = parent.parent
							parent?.children?.add(evaluation)
							evaluation.expandAll()
						}
						is Condition -> {
							var parent: TreeItem<StateMachineItem>? = selected
							while (parent != null && parent.value !is Evaluation) parent = parent.parent
							(item.value as? Condition)?.copy()
									?.let { TreeItem<StateMachineItem>(it) }
									?.also { parent?.children?.get(0)?.children?.add(it) }
							parent?.expandAll()
						}
						is Action -> {
							var parent: TreeItem<StateMachineItem>? = selected
							while (parent != null && parent.value !is Evaluation) parent = parent.parent
							(item.value as? Action)?.copy()
									?.let { TreeItem<StateMachineItem>(it) }
									?.also { parent?.children?.get(1)?.children?.add(it) }
							parent?.expandAll()
						}
					}
					SharedUiData.stateMachine.setAll(treeStateMachine.toStateMachine(SharedUiData.devices, SharedUiData.variables))
					saveConfig()
				}
			}
			KeyCode.HELP, // MacOS mapping of INSERT key
			KeyCode.INSERT -> {
				when (selected?.value) {
					null -> {
						val state = TreeItem<StateMachineItem>(State("${treeStateMachine.root.children.size}"))
						treeStateMachine.root.children.add(state)
						treeStateMachine.root.isExpanded = true
						state.expandAll()
					}
					is State -> {
						val evaluation = TreeItem<StateMachineItem>(Evaluation()).apply {
							children.addAll(
									TreeItem<StateMachineItem>(Placeholder("Conditions", DeploymentCaseIcon.CONDITIONS)),
									TreeItem<StateMachineItem>(Placeholder("Actions", DeploymentCaseIcon.ACTIONS)))
						}
						selected.children.add(evaluation)
						selected.isExpanded = true
						evaluation.expandAll()
					}
					is Evaluation -> {
						val evaluation = TreeItem<StateMachineItem>(Evaluation()).apply {
							children.addAll(
									TreeItem<StateMachineItem>(Placeholder("Conditions", DeploymentCaseIcon.CONDITIONS)),
									TreeItem<StateMachineItem>(Placeholder("Actions", DeploymentCaseIcon.ACTIONS)))
						}
						selected.parent.children.add(evaluation)
						evaluation.expandAll()
					}
					is Placeholder -> {
						if (selected.value?.getTitle(SharedUiData.devices, SharedUiData.variables) == "Conditions") {
							selected.children.add(TreeItem<StateMachineItem>(Condition()))
						} else {
							selected.children.add(TreeItem<StateMachineItem>(Action()))
						}
						selected.isExpanded = true
					}
					is Condition -> {
						selected.parent.children.add(TreeItem<StateMachineItem>(Condition()))
					}
					is Action -> {
						selected.parent.children.add(TreeItem<StateMachineItem>(Action()))
						selected.parent.isExpanded = true
					}
				}
				selected?.isExpanded = true
				SharedUiData.stateMachine.setAll(treeStateMachine.toStateMachine(SharedUiData.devices, SharedUiData.variables))
				saveConfig()
			}
			KeyCode.DELETE -> {
				treeStateMachine.selectionModel.selectedItem
						?.takeUnless { it.value is Placeholder }
						?.takeIf { item -> item.value.let { it !is State || it.findReferences().isEmpty() } }
						?.takeIf { item -> SharedUiData.pipelineStatus.keys.find { it == (item.value as? State)?.name } == null }
						?.takeIf { _ ->
							Alert(Alert.AlertType.CONFIRMATION).apply {
								title = "Delete confirmation"
								headerText = "Do you want to delete selected node?"
							}.showAndWait().filter { it === ButtonType.OK }.isPresent
						}
						?.also { it.parent.children.remove(it) }
						?.also {
							SharedUiData.stateMachine.setAll(treeStateMachine.toStateMachine(SharedUiData.devices, SharedUiData.variables))
							saveConfig()
						}
			}
			else -> noop()
		}
	}

	private fun TreeView<StateMachineItem>.initEditable() {
		cellFactory = Callback<TreeView<StateMachineItem>, TreeCell<StateMachineItem>> { _ ->
			object : TreeCell<StateMachineItem>() {
				private var textField: TextField? = null
				private var vBox: VBox? = null
				private var comboBox1: ComboBox<Device>? = null
				private var comboBox2: ComboBox<Variable>? = null
				private var checkbox: CheckBox? = null

				override fun startEdit() {
					super.startEdit()

					item?.also { item ->
						when (item) {
							is State -> if (item.findReferences().isEmpty()) {
								text = null
								graphic = createTextField(item)
							}
							is Condition,
							is Action -> {
								text = null
								graphic = createDoubleComboBox()
							}
						}
					}
				}

				override fun cancelEdit() {
					super.cancelEdit()
					commitEdit(save())
					text = item?.getTitle(SharedUiData.devices, SharedUiData.variables)
					graphic = item?.icon?.toImageView()
				}

				override fun updateItem(item: StateMachineItem?, empty: Boolean) {
					super.updateItem(item, empty)

					when {
						empty -> {
							text = null
							graphic = null
						}
						isEditing -> {
							text = null
							when (item) {
								is State -> {
									textField?.text = item.name
									graphic = textField
								}
								is Condition -> {
									comboBox1?.selectionModel?.select(item.device?.toDevice(SharedUiData.devices))
									comboBox2?.selectionModel?.select(item.value?.toVariable(SharedUiData.variables))
									graphic = vBox
								}
								is Action -> {
									comboBox1?.selectionModel?.select(item.device?.toDevice(SharedUiData.devices))
									comboBox2?.selectionModel?.select(item.value?.toVariable(SharedUiData.variables))
									checkbox?.isSelected = item.includingEnteringState
									graphic = vBox
								}
							}
						}
						else -> {
							text = item?.getTitle(SharedUiData.devices, SharedUiData.variables)
							graphic = item?.icon?.toImageView()
						}
					}
					style = when {
						treeView.selectionModel.selectedItem?.value == item -> null
						treeView.selectionModel.selectedIndex == index -> null
						item is State && SharedUiData.pipelineStatus.keys.find { it == item.name } != null -> "-fx-background-color: #FFCCFF"
						item is State && item.findReferences().isEmpty() -> "-fx-background-color: #CCCCCC"
						item is State && item.findReferences().isNotEmpty() -> "-fx-background-color: #CCCCFF"
						item is Condition -> "-fx-background-color: #CCFFCC"
						item is Action && treeItem?.parent?.parent?.children?.getOrNull(0)?.children?.isEmpty() == true -> "-fx-background-color: #FFEECC"
						item is Action && item.includingEnteringState -> "-fx-background-color: #FFEECC"
						item is Action && !item.includingEnteringState -> "-fx-background-color: #CCFFFF"
						else -> null
					}
				}

				private fun createTextField(state: State): TextField? {
					textField = TextField(state.name)
					textField?.minWidth = width - graphicTextGap * 2
					textField?.focusedProperty()?.addListener { _, _, newValue ->
						if (!newValue) commitEdit(save())
					}
					textField?.setOnAction { commitEdit(save()) }
					textField?.selectedText
					return textField
				}

				private fun createDoubleComboBox(): VBox? {
					item?.also { item ->
						when (item) {
							is Condition -> {
								comboBox1 = ComboBox(FXCollections.observableArrayList(SharedUiData
										.devices
										.filter {
											// it.type == VariableType.BOOLEAN
											it.kind == DeviceKind.MCP23017 && it.key.toInt() < 32

													|| it.kind == DeviceKind.BLUETOOTH
													&& it.key == BluetoothKey.CONNECTED.key

													|| it.kind == DeviceKind.VIRTUAL
													&& VirtualKey.values().find { k -> it.key == k.key }?.condition == true
										}))
										.apply { selectionModel.select(item.device?.toDevice(SharedUiData.devices)) }
								comboBox2 = ComboBox(SharedUiData.variables.filtered { it.type == VariableType.BOOLEAN })
										.apply { selectionModel.select(item.value?.toVariable(SharedUiData.variables)) }
								checkbox = null
							}
							is Action -> {
								comboBox2 = ComboBox()
								comboBox1 = ComboBox(FXCollections.observableArrayList(SharedUiData
										.devices
										.filter {
											(it.kind != DeviceKind.MCP23017 || it.key.toInt() >= 32)

													&& (it.kind != DeviceKind.VIRTUAL
													|| VirtualKey.values().find { k -> it.key == k.key }?.condition == false)
										}))
										.apply {
											selectionModel.selectedItemProperty().addListener { _, oldValue, newValue ->
												if (oldValue == null || oldValue.type != newValue?.type) {
													comboBox2?.items?.clear()
													comboBox2?.items?.addAll(SharedUiData
															.variables
															.filter { it.type == newValue.type })
													comboBox2?.selectionModel?.select(item.value
															?.toVariable(SharedUiData.variables))
												}
											}
										}
										.apply { selectionModel.select(item.device?.toDevice(SharedUiData.devices)) }
								checkbox = CheckBox("Including entering state").apply {
									isSelected = item.includingEnteringState
								}
							}
						}
					}

					comboBox1?.converter = object : StringConverter<Device>() {
						override fun toString(device: Device?) = device?.getDisplayName()
						override fun fromString(string: String?) = string?.toDevice(SharedUiData.devices)
					}
					comboBox2?.converter = object : StringConverter<Variable>() {
						override fun toString(device: Variable?) = device?.getDisplayNameValue()
						override fun fromString(string: String?) = string?.toVariableFromValue(SharedUiData.variables)
					}

					comboBox1?.focusedProperty()?.addListener { _, _, newValue -> if (!newValue) save() }
					comboBox2?.focusedProperty()?.addListener { _, _, newValue -> if (!newValue) save() }
					checkbox?.selectedProperty()?.addListener { _, _, _ -> save() }

					val submitButton = Button("Submit").apply {
						setOnAction {
							commitEdit(save())
						}
					}

					val hBox1 = HBox(1.0).apply {
						minWidth = width - graphicTextGap * 2
						alignment = Pos.CENTER_LEFT

						children.addAll(comboBox1, Label("="), comboBox2)
					}

					val hBox2 = HBox(1.0).apply {
						minWidth = width - graphicTextGap * 2
						alignment = Pos.CENTER_LEFT

						if (item is Action && checkbox != null) children.add(checkbox)
						children.add(submitButton)
					}

					vBox = VBox(1.0).apply {
						minWidth = width - graphicTextGap * 2
						alignment = Pos.CENTER_LEFT
						children.addAll(hBox1, hBox2)
					}

					return vBox
				}

				private fun save() = item?.also { item ->
					when (item) {
						is State -> {
							item.name = textField?.text ?: ""
						}
						is Condition -> {
							item.device = comboBox1?.selectionModel?.selectedItem?.toData()
							item.value = comboBox2?.selectionModel?.selectedItem?.name
						}
						is Action -> {
							item.device = comboBox1?.selectionModel?.selectedItem?.toData()
							item.value = comboBox2?.selectionModel?.selectedItem?.name
							item.includingEnteringState = checkbox?.isSelected ?: false
						}
					}
				}
			}
		}

		setOnEditCommit { event ->
			editingItem?.value = event?.newValue
			SharedUiData.stateMachine.setAll(treeStateMachine.toStateMachine(SharedUiData.devices, SharedUiData.variables))
			saveConfig()
		}
	}

	private fun State.findReferences(): List<StateMachineItem> = findInStateMachine {
		it is Action
				&& it.device?.toDevice(SharedUiData.devices)?.kind == DeviceKind.VIRTUAL
				&& it.device?.toDevice(SharedUiData.devices)?.key == VirtualKey.GOTO.key
				&& (it.value?.toVariable(SharedUiData.variables)?.value == this.name
				|| it.value?.toVariable(SharedUiData.variables)?.value == "${treeStateMachine.selectionModel.selectedIndex}")
				|| SharedUiData.pipelineStatus.values.contains(this.name)
	}
}