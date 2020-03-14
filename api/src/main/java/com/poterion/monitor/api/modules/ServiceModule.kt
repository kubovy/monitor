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
import com.poterion.monitor.api.controllers.Service
import com.poterion.monitor.data.data.ApplicationConfiguration
import com.poterion.monitor.data.services.ServiceConfig

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
interface ServiceModule<out Conf : ServiceConfig, out Ctrl : Service<Conf>> : Module<Conf, Ctrl> {
	/**
	 * Weather the notification set is static.
	 *
	 * This means that the same notification set will be sent with different states. If false a changing set may be
	 * sent. This is true for example for alert manager which only send alerts and not an de-escalation notification.
	 */
	val staticNotificationSet: Boolean
		get() = true

	override fun loadControllers(controller: ControllerInterface, applicationConfiguration: ApplicationConfiguration):
			Collection<Ctrl> = applicationConfiguration
			.services
			.filterIsInstance(configClass.java)
			.map { loadController(controller, it) }
}