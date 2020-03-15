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
package com.poterion.monitor.sensors.gerritcodereview

import com.poterion.monitor.api.controllers.ControllerInterface
import com.poterion.monitor.api.modules.ServiceModule
import com.poterion.monitor.data.ModuleConfig
import com.poterion.monitor.data.data.ApplicationConfiguration
import com.poterion.monitor.data.nextUUID
import com.poterion.monitor.sensors.gerritcodereview.control.GerritCodeReviewService
import com.poterion.monitor.sensors.gerritcodereview.data.GerritCodeReviewConfig
import com.poterion.utils.javafx.Icon
import kotlin.reflect.KClass

object GerritCodeReviewModule : ServiceModule<GerritCodeReviewConfig, GerritCodeReviewService> {
	override val configClass: KClass<GerritCodeReviewConfig> = GerritCodeReviewConfig::class

	override val title: String
		get() = "Gerrit Code Review"

	override val icon: Icon = GerritCodeReviewIcon.GERRIT

	override fun createController(controller: ControllerInterface, applicationConfiguration: ApplicationConfiguration):
			GerritCodeReviewService = GerritCodeReviewService(controller,
			GerritCodeReviewConfig(uuid = applicationConfiguration.serviceMap.nextUUID(), name = title))

	override fun loadController(controller: ControllerInterface, config: ModuleConfig): GerritCodeReviewService =
			GerritCodeReviewService(controller, config as GerritCodeReviewConfig)
}