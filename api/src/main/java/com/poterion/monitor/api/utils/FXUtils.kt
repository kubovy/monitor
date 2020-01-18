package com.poterion.monitor.api.utils

import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.ObservableValue
import javafx.scene.control.*
import javafx.scene.control.cell.PropertyValueFactory
import javafx.scene.control.cell.TreeItemPropertyValueFactory
import javafx.scene.input.MouseEvent
import javafx.util.Callback

fun <T> ComboBox<T>.factory(factory: ListCell<T>.(T?, Boolean) -> Unit) {
	cellFactory = Callback<ListView<T>, ListCell<T>> {
		object : ListCell<T>() {
			override fun updateItem(item: T, empty: Boolean) {
				super.updateItem(item, empty)
				factory(item, empty)
			}
		}
	}
	buttonCell = cellFactory.call(null)
}

fun <T> TreeView<T>.factory(factory: TreeCell<T>.(T?, Boolean) -> Unit) {
	cellFactory = Callback<TreeView<T>, TreeCell<T>> {
		object : TreeCell<T>() {
			override fun updateItem(item: T, empty: Boolean) {
				super.updateItem(item, empty)
				factory(item, empty)
			}
		}
	}
}

fun <S, T> TableColumn<S, T>.cellFactoryInternal(factory: (TableCell<S, T>.(S?, T?, Boolean) -> Unit)? = null) {
	if (factory != null) cellFactory = Callback<TableColumn<S, T>, TableCell<S, T>> {
		object : TableCell<S, T>() {
			override fun updateItem(item: T?, empty: Boolean) {
				super.updateItem(item, empty)
				factory(tableRow.item as? S, item, empty)
			}
		}
	}
}

fun <S, T> TableColumn<S, T>.cell(getter: (S?) -> T) {
	cellValueFactory = Callback<TableColumn.CellDataFeatures<S, T>, ObservableValue<T>> {
		SimpleObjectProperty(getter(it.value))
	}
}

fun <S, T> TableColumn<S, T>.cell(property: String? = null,
								  factory: (TableCell<S, T>.(S?, T?, Boolean) -> Unit)? = null) {
	cellValueFactory = PropertyValueFactory(property)
	cellFactoryInternal(factory)
}

fun <T> TreeTableView<T>.find(predicate: (T?) -> Boolean) = root.find(predicate)

fun <T> TreeItem<T>.find(predicate: (T?) -> Boolean): TreeItem<T>? {
	if (predicate(value)) return this

	for (child in children) {
		if (predicate(child.value)) return child
	}

	for (child in children) {
		val found = child.find(predicate)
		if (found != null) return found
	}
	return null
}

fun <T> TreeTableView<T>.row(factory: TreeTableRow<T>.(T?) -> Unit) = setRowFactory {
	TreeTableRow<T>().apply { factory(item) }
}

fun <T> TreeTableView<T>.setOnItemClick(handler: TreeTableRow<T>.(T?, MouseEvent) -> Unit) = row {
	setOnMouseClicked { event -> handler(item, event) }
}

fun <S, T> TreeTableColumn<S, T>.cellFactoryInternal(factory: (TreeTableCell<S, T>.(S?, T?, Boolean) -> Unit)? = null) {
	if (factory != null) cellFactory = Callback<TreeTableColumn<S, T>, TreeTableCell<S, T>> {
		object : TreeTableCell<S, T>() {
			override fun updateItem(item: T?, empty: Boolean) {
				super.updateItem(item, empty)
				factory(treeTableRow.item, item, empty)
			}
		}
	}
}

fun <S, T> TreeTableColumn<S, T>.cell(getter: (S?) -> T) {
	cellValueFactory = Callback<TreeTableColumn.CellDataFeatures<S, T>, ObservableValue<T>> {
		SimpleObjectProperty(getter(it.value.value))
	}
}

fun <S, T> TreeTableColumn<S, T>.cell(property: String? = null,
									  factory: (TreeTableCell<S, T>.(S?, T?, Boolean) -> Unit)? = null) {
	cellValueFactory = TreeItemPropertyValueFactory(property)
	cellFactoryInternal(factory)
}

