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

import com.poterion.monitor.api.ui.NavigationItem
import com.poterion.monitor.data.StatusItem
import com.poterion.monitor.data.services.ServiceConfig
import com.poterion.monitor.data.services.ServiceSubConfig
import javafx.beans.property.BooleanProperty
import javafx.beans.property.SimpleBooleanProperty
import retrofit2.Retrofit

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
abstract class Service<out Config : ServiceConfig<out ServiceSubConfig>>(config: Config) :
		AbstractModule<Config>(config) {

	override val navigationRoot: NavigationItem
		get() = NavigationItem(
				titleProperty = config.nameProperty,
				icon = definition.icon,
				sub = listOf())

	var http: HttpServiceModule? = null
		get() {
			if (field == null) field = HttpServiceModule(controller.applicationConfiguration, config)
			return field
		}
		private set

	var refresh: Boolean
		get() = refreshProperty.get()
		set(value) = refreshProperty.set(value)

	val refreshProperty: BooleanProperty = SimpleBooleanProperty(false)

	protected val retrofit: Retrofit?
		get() = http?.retrofit

	/**
	 * Check implementation.
	 *
	 * @param updater Status updater callback
	 */
	abstract fun check(updater: (Collection<StatusItem>) -> Unit)
}