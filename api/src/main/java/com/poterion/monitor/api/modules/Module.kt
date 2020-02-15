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
package com.poterion.monitor.api.modules

import com.poterion.monitor.api.controllers.ControllerInterface
import com.poterion.monitor.api.controllers.ModuleInstanceInterface
import com.poterion.monitor.data.ModuleConfig
import com.poterion.monitor.data.data.ApplicationConfiguration
import com.poterion.utils.javafx.Icon
import kotlin.reflect.KClass

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
interface Module<out Configuration : ModuleConfig, out Controller : ModuleInstanceInterface<Configuration>> {
	/** Configuration class */
	val configClass: KClass<out Configuration>

	/** Title */
	val title: String

	/** Module icon shown in the context menu of the tray. */
	val icon: Icon

	/** Exactly one module needs to be configured */
	val singleton: Boolean
		get() = false

	/**
	 * Create a new UI controller.
	 *
	 * @param controller Controller
	 * @param applicationConfiguration Configuration
	 */
	fun createController(controller: ControllerInterface, applicationConfiguration: ApplicationConfiguration): Controller?

	fun loadController(controller: ControllerInterface, config: ModuleConfig): Controller

	/**
	 * Load all UI controllers from configuration.
	 *
	 * @param controller Controller
	 * @param applicationConfiguration Configuration
	 */
	fun loadControllers(controller: ControllerInterface, applicationConfiguration: ApplicationConfiguration): Collection<Controller>
}