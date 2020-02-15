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

import javafx.scene.control.TableCell
import javafx.scene.control.TableColumn
import javafx.scene.control.TextField
import javafx.scene.control.TreeItem
import javafx.scene.control.cell.PropertyValueFactory
import javafx.util.Callback

fun TreeItem<*>.expandAll() {
	if (!isLeaf) {
		isExpanded = true
		for (child in children) child?.expandAll()
	}
}

fun <Entry> TableColumn<Entry, String>.initEditableText(propertyName: String,
														styler: (Entry?) -> String? = { null },
														isReadOnly: (Entry?) -> Boolean = { true },
														itemUpdater: (Entry, String) -> Unit,
														saveConfig: () -> Unit) {
	cellValueFactory = PropertyValueFactory<Entry, String>(propertyName)
	cellFactory = Callback<TableColumn<Entry, String>, TableCell<Entry, String>> {
		object : TableCell<Entry, String>() {
			private var textField: TextField? = null

			override fun startEdit() {
				super.startEdit()
				if (!isEmpty && !isReadOnly(tableRow?.item as? Entry)) {
					text = null
					graphic = createTextField()
					textField?.selectAll()
				}
			}

			override fun cancelEdit() {
				super.cancelEdit()
				commitEdit(save())
				text = item
				graphic = null
			}

			override fun updateItem(item: String?, empty: Boolean) {
				super.updateItem(item, empty)

				if (empty) {
					text = null
					graphic = null
				} else if (isEditing) {
					textField?.text = item
					text = null
					graphic = textField
				} else {
					text = item
					graphic = null
					style = styler(tableRow?.item as? Entry)
				}
			}

			private fun createTextField(): TextField? {
				textField = TextField(item)
				textField?.minWidth = width - graphicTextGap * 2
				textField?.focusedProperty()?.addListener { _, _, newValue -> if (!newValue) commitEdit(save()) }
				textField?.setOnAction { commitEdit(save()) }
				return textField
			}

			private fun save() = textField?.text ?: ""
		}
	}
	setOnEditCommit { event ->
		event?.tableView?.items?.also { items ->
			val item = items[event.tablePosition.row]
			if (item != null) itemUpdater(item, event.newValue)
			saveConfig()
		}
	}
}