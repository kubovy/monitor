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
import com.poterion.monitor.notifiers.tabs.NotificationTabsIcon
import com.poterion.monitor.notifiers.tabs.NotificationTabsModule
import com.poterion.monitor.notifiers.tabs.data.NotificationTabsConfig
import com.poterion.monitor.notifiers.tabs.ui.TabController
import com.poterion.utils.javafx.toImageView
import com.poterion.utils.kotlin.noop
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
		config.enabledProperty.addListener { _, _, enabled ->
			if (!enabled) {
				configurationTabIcon.set(NotificationTabsIcon.TABS.toImageView())
				tabController?.clear()
			}
		}
		StatusCollector.status.sample(10, TimeUnit.SECONDS, true).subscribe {
			if (config.enabled) Platform.runLater { update() }
		}
	}

	override fun onServicesChanged() {
		super.onServicesChanged()
		if (!config.services.any { it.uuid == config.selectedServiceId }) {
			config.selectedServiceId = null
			update()
		}
	}

	override fun update() {
		configurationTabIcon.set(StatusCollector.maxStatus(controller.applicationConfiguration.silencedMap.keys,
				config.minPriority, config.minStatus, config.services).toIcon().toImageView())
		tabController?.update(StatusCollector.filter(emptyList(), config.minPriority,
				config.minStatus, config.services, includingChildren = true))
	}

	override fun shutdown() = noop()
}