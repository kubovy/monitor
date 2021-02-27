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

import com.poterion.utils.javafx.Icon
import com.poterion.utils.javafx.toObservableList
import javafx.beans.property.*
import javafx.collections.ObservableList

/**
 * Navigation item.
 *
 * @param title Title
 * @param titleProperty Title property (if specified and not `null` then `title` is ignored)
 * @param icon Icon, `null` means no icon
 * @param iconProperty Icon property (if specified and not `null` then `icon` is ignored)
 * @param enabled Enabled
 * @param checked Checked, `null` means not check-able
 * @param checkedProperty Checked property (if specified and not `null` then `checked` is ignored)
 * @param recreate TODO
 * @param action Action
 * @param sub Sub menu, `null` means no submenu. If item has not submenu but may have this should be [emptyList]
 * @author Jan Kubovy [jan@kubovy.eu]
 */
class NavigationItem(val uuid: String? = null,
					 title: String? = null,
					 titleProperty: StringProperty? = null,
					 icon: Icon? = null,
					 iconProperty: ObjectProperty<Icon?>? = null,
					 enabled: Boolean = true,
					 checked: Boolean? = null,
					 checkedProperty: ObjectProperty<Boolean>? = null,
					 val recreate: (() -> List<NavigationItem>)? = null,
					 val action: (() -> Unit)? = null,
					 sub: List<NavigationItem>? = null) {
	var title: String?
		get() = titleProperty.get()
		set(value) = titleProperty.set(value)

	val titleProperty: StringProperty = titleProperty ?: SimpleStringProperty(title)

	var icon: Icon?
		get() = iconProperty.get()
		set(value) = iconProperty.set(value)

	val iconProperty: ObjectProperty<Icon?> = iconProperty ?: SimpleObjectProperty(icon)

	var enabled: Boolean
		get() = enabledProperty.get()
		set(value) = enabledProperty.set(value)

	val enabledProperty: BooleanProperty = SimpleBooleanProperty(enabled)

	val isCheckable: Boolean = checked != null || checkedProperty != null

	var checked: Boolean?
		get() = checkedProperty?.get()
		set(value) {
			checkedProperty?.set(value)
		}

	val checkedProperty: ObjectProperty<Boolean>? = checkedProperty ?: checked?.let { SimpleObjectProperty(it) }

	val sub: ObservableList<NavigationItem>? = sub?.toObservableList()
}