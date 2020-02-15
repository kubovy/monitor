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
package com.poterion.monitor.notifiers.tabs.control

import com.poterion.monitor.api.StatusCollector
import com.poterion.monitor.api.controllers.ControllerInterface
import com.poterion.monitor.api.controllers.ModuleInstanceInterface
import com.poterion.monitor.api.controllers.Notifier
import com.poterion.monitor.api.modules.Module
import com.poterion.monitor.api.utils.toIcon
import com.poterion.monitor.data.notifiers.NotifierAction
import com.poterion.monitor.notifiers.tabs.NotificationTabsModule
import com.poterion.monitor.notifiers.tabs.data.NotificationTabsConfig
import com.poterion.monitor.notifiers.tabs.ui.TabController
import com.poterion.utils.javafx.toImageView
import javafx.application.Platform
import javafx.scene.Parent
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
class NotificationTabsNotifier(override val controller: ControllerInterface, config: NotificationTabsConfig) : Notifier<NotificationTabsConfig>(config) {
	companion object {
		val LOGGER: Logger = LoggerFactory.getLogger(NotificationTabsNotifier::class.java)
	}

	override val definition: Module<NotificationTabsConfig, ModuleInstanceInterface<NotificationTabsConfig>> = NotificationTabsModule

	private var tabController: TabController? = null

	override var configurationTab: Parent? = null
		get() {
			if (field == null || tabController == null) {
				val (parent, ctrl) = TabController.getRoot(this)
				field = parent
				tabController = ctrl
			}
			return field
		}
		private set

	override fun initialize() {
		super.initialize()
		StatusCollector.status.sample(10, TimeUnit.SECONDS, true).subscribe {
			Platform.runLater {
				configurationTabIcon.set(it.maxStatus(controller.applicationConfiguration.silenced.keys,
						config.minPriority, config.minStatus, config.services).toIcon().toImageView())
				tabController?.update(it.filter(emptyList(), config.minPriority,
						config.minStatus, config.services, includingChildren = true))
			}
		}
	}

	override fun onServicesChanged() {
		super.onServicesChanged()
		if (!config.services.contains(config.selectedServiceId)) {
			config.selectedServiceId = null
			selectedServices.forEach { it.refresh = true }
		}
	}

	override fun execute(action: NotifierAction): Unit = when (action) {
		NotifierAction.ENABLE -> {
			config.enabled = true
			controller.saveConfig()
		}
		NotifierAction.DISABLE -> {
			config.enabled = false
			controller.saveConfig()
		}
		NotifierAction.TOGGLE -> execute(if (config.enabled) NotifierAction.DISABLE else NotifierAction.ENABLE)
		else -> LOGGER.debug("Executing action ${action}")
	}
}