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
package com.poterion.monitor.api.controllers

import com.poterion.monitor.api.modules.Module
import com.poterion.monitor.api.ui.NavigationItem
import com.poterion.utils.kotlin.noop
import com.poterion.monitor.data.ModuleConfig
import javafx.beans.property.ObjectProperty
import javafx.scene.Node
import javafx.scene.Parent

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
interface ModuleInstanceInterface<out Config : ModuleConfig> {
	/** Module definition singleton */
	val definition: Module<Config, ModuleInstanceInterface<Config>>

	/** Module configuration model */
	val config: Config

	/** UI controller */
	val controller: ControllerInterface

	/** Navigation root of the sub menu in the tray */
	val navigationRoot: NavigationItem?
		get() = null

	/** Configuration rows for the module if any. */
	val configurationRows: List<Pair<Node, Node>>
		get() = emptyList()

	/** Configuration rows for the module if any. */
	val configurationRowsLast: List<Pair<Node, Node>>
		get() = emptyList()

	/** Additional configuration if not fitting the rows pattern. */
	val configurationAddition: List<Parent>
		get() = emptyList()

	/** Own tab for the module if any. */
	val configurationTab: Parent?
		get() = null

	val configurationTabIcon: ObjectProperty<Node?>

	val exitRequest: Boolean
		get() = true

	/** Initialization of a module controller. Call after controllers are created. */
	fun initialize() = noop()

	fun destroy() = noop()
}