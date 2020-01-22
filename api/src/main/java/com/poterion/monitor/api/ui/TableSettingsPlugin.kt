package com.poterion.monitor.api.ui

import com.poterion.monitor.api.CommonIcon
import com.poterion.monitor.api.controllers.ControllerInterface
import com.poterion.monitor.api.utils.cell
import com.poterion.monitor.api.utils.factory
import com.poterion.monitor.api.utils.noop
import com.poterion.monitor.api.utils.toImageView
import com.poterion.monitor.data.ModuleConfig
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.*
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
							 private val items: MutableCollection<S>,
							 private var displayName: S.() -> String,
							 private val columnDefinitions: List<ColumnDefinition<S, *>>,
							 private val comparator: Comparator<S>,
							 buttonText: String = "Add",
							 private val actions: List<(S) -> Button?> = emptyList()) {

	data class ColumnDefinition<S, T>(val name: String,
									  var getter: S.() -> T?,
									  var setter: S.(T) -> Unit = {},
									  val mutator: S.(T) -> S = {
										  setter(it)
										  this
									  },
									  val initialValue: T,
									  val isEditable: Boolean = false,
									  val title: T.() -> String = { toString() },
									  val options: (() -> List<T>)? = null,
									  val icon: T.() -> Icon? = { null })

	private var newItem: S = createItem()
	private val changeListener: MutableCollection<() -> Unit> = mutableListOf()

	private fun <T> ColumnDefinition<S, T>.createControl() = if (options == null) {
		TextField(initialValue.title()).also { textField ->
			changeListener.add { textField.text = initialValue.title() }
			textField.textProperty().addListener { _, _, value ->
				newItem = newItem.mutator((value as? T) ?: initialValue)
			}
			textField.maxWidth = Double.MAX_VALUE
			HBox.setHgrow(textField, Priority.SOMETIMES)
		}
	} else {
		ComboBox<T>().also { combobox ->
			combobox.items.setAll(options.invoke())
			changeListener.add {
				combobox.items.clear()
				combobox.items.addAll(options.invoke())
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
						Label(columnDefinitions[it].name).apply {
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
		VBox.setVgrow(this, javafx.scene.layout.Priority.ALWAYS)
		setOnKeyReleased { event ->
			when (event.code) {
				KeyCode.DELETE -> selectionModel.selectedItem?.also { removeItem(it) }
				else -> noop()
			}
		}
	}

	val vbox = VBox(table)
			.apply { VBox.setVgrow(this, javafx.scene.layout.Priority.ALWAYS) }

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
							textField.textProperty().addListener { _, _, v ->
								tableRow.item = item.mutator((v as? T) ?: initialValue)
							}
							textField.focusedProperty().addListener { _, _, focused ->
								if (!focused) save()
							}
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
								combobox.items.setAll(options.invoke())
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
		table.items.addAll(items)
		table.items.sortWith(comparator)
	}

	private fun addItem() {
		if (!items.contains(newItem)) {
			items.add(newItem)
			table.items.add(newItem)
			newItem = createItem()
			save()
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
				table.items.remove(item)
				save()
			}
		}
	}

	private fun save() {
		controller.saveConfig()
		table.items.sortWith(comparator)
		table.refresh()
		changeListener.forEach { it() }
	}
}