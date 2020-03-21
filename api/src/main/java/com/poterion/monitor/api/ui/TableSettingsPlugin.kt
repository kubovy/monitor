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
package com.poterion.monitor.api.ui

import com.poterion.monitor.api.CommonIcon
import com.poterion.monitor.api.controllers.ControllerInterface
import com.poterion.monitor.data.ModuleConfig
import com.poterion.utils.javafx.Icon
import com.poterion.utils.javafx.cell
import com.poterion.utils.javafx.factory
import com.poterion.utils.javafx.toImageView
import com.poterion.utils.kotlin.noop
import javafx.beans.property.StringProperty
import javafx.beans.value.WritableValue
import javafx.collections.ObservableList
import javafx.collections.transformation.SortedList
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.Alert
import javafx.scene.control.Button
import javafx.scene.control.ButtonType
import javafx.scene.control.ComboBox
import javafx.scene.control.Control
import javafx.scene.control.Label
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
import javafx.scene.control.TextField
import javafx.scene.input.KeyCode
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.Region
import javafx.scene.layout.VBox

class TableSettingsPlugin<S>(private val tableName: String,
							 newLabel: String? = null,
							 private val controller: ControllerInterface,
							 private val config: ModuleConfig,
							 private var createItem: () -> S,
							 private val items: ObservableList<S>,
							 private var displayName: S.() -> String,
							 private val columnDefinitions: List<ColumnDefinition<S, *>>,
							 comparator: Comparator<S>,
							 buttonText: String = "Add",
							 private val actions: List<(S) -> Button?> = emptyList(),
							 private val onAdd: (S) -> Unit = {},
							 private val onRemove: (S) -> Unit = {},
							 private val onSave: () -> Unit = {},
							 private val fieldSizes: Array<Double> = emptyArray()) {

	data class ColumnDefinition<S, T>(val name: String,
									  val shortName: String? = null,
									  val property: S.() -> WritableValue<T>? = { null },
									  var getter: S.() -> T? = { property()?.value },
									  var setter: S.(T) -> Unit = { property()?.value = it },
									  val mutator: S.(T) -> S = {
										  setter(it)
										  this
									  },
									  val initialValue: T,
									  val isEditable: Boolean = false,
									  val title: T.() -> String = { toString() },
									  val options: ObservableList<T>? = null,
									  val icon: T.() -> Icon? = { null })

	private var newItem: S = createItem()
	private val changeListener: MutableCollection<() -> Unit> = mutableListOf()
	private var textFieldIndex = 0

	private fun <T> ColumnDefinition<S, T>.createControl() = if (options == null) {
		TextField(initialValue.title()).also { textField ->
			changeListener.add { textField.text = initialValue.title() }
			textField.textProperty().addListener { _, _, value ->
				newItem = newItem.mutator((value as? T) ?: initialValue)
			}
			textField.maxWidth = fieldSizes.getOrNull(textFieldIndex++) ?: Double.MAX_VALUE
			HBox.setHgrow(textField, Priority.SOMETIMES)
		}
	} else {
		ComboBox<T>().also { combobox ->
			combobox.items = options
			changeListener.add {
				combobox.items = options
				combobox.selectionModel.select(initialValue)
			}
			combobox.factory { item, empty ->
				graphic = item?.takeUnless { empty }?.let(icon)?.toImageView()
				text = item?.takeUnless { empty }?.let(title)
			}
			combobox.selectionModel.select(initialValue)
			combobox.selectionModel.selectedItemProperty().addListener { _, _, value ->
				newItem = newItem.mutator((value) ?: initialValue)
			}
		}
	}


	private val controlElements: List<Control> = columnDefinitions.map { it.createControl() }

	val rowNewItem = Label(newLabel ?: columnDefinitions.first().name).apply {
		maxWidth = Double.MAX_VALUE
		maxHeight = Double.MAX_VALUE
		alignment = Pos.CENTER_RIGHT
		padding = Insets(5.0)
	} to (listOf(controlElements.first()) +
			(1 until columnDefinitions.size).flatMap {
				listOf(
						Label(columnDefinitions[it].shortName ?: columnDefinitions[it].name).apply {
							maxWidth = Double.MAX_VALUE
							maxHeight = Double.MAX_VALUE
							padding = Insets(5.0)
						},
						controlElements[it])
			} +
			listOf(Button(buttonText).apply { setOnAction { addItem() } }))
			.let { HBox(*it.toTypedArray()) }
			.apply { spacing = 5.0 }

	private val table = TableView<S>().apply {
		minWidth = Region.USE_COMPUTED_SIZE
		minHeight = Region.USE_COMPUTED_SIZE
		prefWidth = Region.USE_COMPUTED_SIZE
		prefHeight = Region.USE_COMPUTED_SIZE
		maxWidth = Double.MAX_VALUE
		maxHeight = Double.MAX_VALUE
		VBox.setVgrow(this, Priority.ALWAYS)
		setOnKeyReleased { event ->
			when (event.code) {
				KeyCode.DELETE -> selectionModel.selectedItem?.also { removeItem(it) }
				else -> noop()
			}
		}
	}

	val vbox = VBox(table).apply { VBox.setVgrow(this, Priority.ALWAYS) }

	private fun <T> ColumnDefinition<S, T>.createColumn() = TableColumn<S, ColumnDefinition<S, T>>(name).apply {
		isSortable = false
		minWidth = 150.0
		prefWidth = config.tableColumnWidths["${tableName}-${name}"]?.toDouble() ?: Region.USE_COMPUTED_SIZE
		maxWidth = Double.MAX_VALUE
		widthProperty().addListener { _, _, value ->
			config.tableColumnWidths["${tableName}-${name}"] = value.toInt()
			controller.saveConfig()
		}
		cell { item, _, empty ->
			if (!this@createColumn.isEditable) {
				text = item
						.takeUnless { empty }
						?.let(getter)
						?.let(title)
				graphic = item
						.takeUnless { empty }
						?.let(getter)
						?.let(icon)
						?.toImageView()
			} else if (options == null) {
				text = null
				graphic = item
						?.takeUnless { empty }
						?.let(getter)
						?.let(title)
						?.let { TextField(it) }
						?.also { textField ->
							textField.maxWidth = Double.MAX_VALUE
							textField.maxHeight = Double.MAX_VALUE
							val property = item.property()
							if (property is StringProperty) {
								textField.textProperty().bindBidirectional(property)
							} else {
								textField.textProperty().addListener { _, _, v ->
									tableRow.item = item.mutator((v as? T) ?: initialValue)
								}
							}
							textField.focusedProperty().addListener { _, _, focused -> if (!focused) save() }
						}
			} else {
				text = null
				graphic = item
						?.takeUnless { empty }
						?.let(getter)
						?.let { value ->
							ComboBox<T>().also { combobox ->
								combobox.maxWidth = Double.MAX_VALUE
								combobox.maxHeight = Double.MAX_VALUE
								combobox.items = options
								combobox.factory { item, empty ->
									graphic = item?.takeUnless { empty }?.let(icon)?.toImageView()
									text = item?.takeUnless { empty }?.let(title)
								}
								combobox.selectionModel.select(value)
								combobox.selectionModel.selectedItemProperty().addListener { _, _, v ->
									tableRow.item = item.mutator(v)
									save()
								}
							}
						}
			}
		}
	}

	private val tableColumns = columnDefinitions.map { it.createColumn() }

	private val tableColumnAction = TableColumn<S, ColumnDefinition<S, *>>("").apply {
		isSortable = false
		minWidth = (actions.size + 1) * 48.0
		prefWidth = Region.USE_COMPUTED_SIZE
		maxWidth = Region.USE_PREF_SIZE
		cell { item, _, empty ->
			graphic = item?.takeUnless { empty }
					?.let { bean ->
						actions.mapNotNull { it(bean).apply { maxHeight = Double.MAX_VALUE } } +
								listOf(Button("", CommonIcon.TRASH.toImageView()).apply {
									maxHeight = Double.MAX_VALUE
									setOnAction { item.also { removeItem(it) } }
								})
					}
					?.let { HBox(*it.toTypedArray()) }
		}
	}

	init {
		table.columns.addAll(tableColumns)
		table.columns.add(tableColumnAction)
		table.items = SortedList(items, comparator)
	}

	private fun addItem() {
		if (!items.contains(newItem)) {
			items.add(newItem)
			newItem = createItem()
			save { onAdd(newItem) }
		}
	}

	private fun removeItem(item: S) {
		Alert(Alert.AlertType.CONFIRMATION).apply {
			title = "Delete confirmation"
			headerText = "Delete confirmation"
			contentText = "Do you really want to delete ${item?.displayName()}?"
			buttonTypes.setAll(ButtonType.YES, ButtonType.NO)
		}.showAndWait().ifPresent {
			it.takeIf { it == ButtonType.YES }?.also { _ ->
				items.remove(item)
				save { onRemove(item) }
			}
		}
	}

	private fun save(additionalCallback: () -> Unit = {}) {
		additionalCallback()
		controller.saveConfig()
		onSave()
		changeListener.forEach { it() }
	}
}