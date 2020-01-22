package com.poterion.monitor.api.utils

import javafx.scene.control.ComboBox
import javafx.scene.control.ListCell
import javafx.scene.control.ListView
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