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
package com.poterion.monitor.sensors.alertmanager

import com.poterion.monitor.api.controllers.ControllerInterface
import com.poterion.monitor.api.modules.ServiceModule
import com.poterion.monitor.data.ModuleConfig
import com.poterion.monitor.data.data.ApplicationConfiguration
import com.poterion.monitor.data.nextUUID
import com.poterion.monitor.sensors.alertmanager.control.AlertManagerService
import com.poterion.monitor.sensors.alertmanager.data.AlertManagerConfig
import com.poterion.monitor.sensors.alertmanager.data.AlertManagerLabelConfig
import com.poterion.utils.javafx.Icon
import kotlin.reflect.KClass

object AlertManagerModule : ServiceModule<AlertManagerLabelConfig, AlertManagerConfig, AlertManagerService> {
	override val configClass: KClass<AlertManagerConfig> = AlertManagerConfig::class

	override val title: String
		get() = "Alert Manager"

	override val icon: Icon = AlertManagerIcon.ALERT_MANAGER

	override fun createController(controller: ControllerInterface, applicationConfiguration: ApplicationConfiguration):
			AlertManagerService = AlertManagerService(controller,
			AlertManagerConfig(uuid = applicationConfiguration.serviceMap.nextUUID(), name = title))

	override fun loadController(controller: ControllerInterface, config: ModuleConfig): AlertManagerService =
			AlertManagerService(controller, config as AlertManagerConfig)
}