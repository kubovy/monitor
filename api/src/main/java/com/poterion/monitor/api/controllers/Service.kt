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

import com.poterion.monitor.api.maxStatus
import com.poterion.monitor.api.ui.NavigationItem
import com.poterion.monitor.api.utils.toIcon
import com.poterion.monitor.data.Status
import com.poterion.monitor.data.StatusItem
import com.poterion.monitor.data.services.ServiceConfig
import com.poterion.monitor.data.services.ServiceSubConfig
import com.poterion.utils.javafx.Icon
import com.poterion.utils.javafx.mapped
import javafx.beans.property.*
import javafx.collections.ListChangeListener
import org.slf4j.LoggerFactory
import retrofit2.Retrofit

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
abstract class Service<SC : ServiceSubConfig, out Config : ServiceConfig<out SC>>(config: Config) :
		AbstractModule<Config>(config) {

	companion object {
		private val LOGGER = LoggerFactory.getLogger(Service::class.java)
	}

	var statusProperty: ObjectProperty<Status> = SimpleObjectProperty(Status.NONE)
	private var interruptReason: String? = null
	private var lastChecked = 0L

	private var _navigationRoot: NavigationItem? = null
	override val navigationRoot: NavigationItem
		get() = _navigationRoot ?: NavigationItem(
				uuid = config.uuid,
				titleProperty = config.nameProperty,
				iconProperty = SimpleObjectProperty<Icon?>().also { property ->
					property.bind(statusProperty
							.mapped { if (it == null || it == Status.NONE) definition.icon else it.toIcon() })
				},
				sub = listOf(
						NavigationItem(
								title = "Enabled",
								checkedProperty = config.enabledProperty.asObject()),
						NavigationItem(
								title = "Refresh",
								action = { refresh = true }),
						NavigationItem()
				))
				.also { _navigationRoot = it }

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

	val lastErrorProperty = SimpleStringProperty(null)

	val shouldRun: Boolean
		get() = !controller.applicationConfiguration.paused
				&& config.enabled
				&& config.url.isNotBlank()
				&& (refresh
				|| (config.checkInterval?.let { (System.currentTimeMillis() - lastChecked) > it } ?: false))

	protected val retrofit: Retrofit?
		get() = http?.retrofit

	override fun initialize() {
		super.initialize()
		config.subConfig.forEach { addToNavigation(it) }
		config.subConfig.addListener(ListChangeListener { change ->
			when {
				change.wasRemoved() -> change.removed
						.forEach { subConfig -> navigationRoot.sub?.removeIf { it.uuid == subConfig.uuid } }
				change.wasAdded() -> change.addedSubList.forEach { addToNavigation(it) }
			}
		})
	}

	fun addToNavigation(subConfig: SC) {
		navigationRoot.sub?.add(NavigationItem(
				uuid = subConfig.uuid,
				title = subConfig.configTitle,
				action = { gotoSubConfig(subConfig) }))
	}

	abstract fun gotoSubConfig(subConfig: SC)

	/**
	 * Check implementation.
	 *
	 * @param updater Status updater callback
	 */
	fun check(updater: (Collection<StatusItem>) -> Unit) {
		interruptReason = null

		if (shouldRun) try {
			refresh = true
			val statusItems = doCheck()
			checkForInterruptions()
			statusProperty.set(statusItems.maxStatus(controller.applicationConfiguration.silencedMap.keys,
					config.priority, Status.NONE))
			updater(statusItems)
			lastChecked = System.currentTimeMillis()
		} catch (e: InterruptedException) {
			statusProperty.set(Status.NONE)
			LOGGER.info("${this.javaClass.simpleName} ${e.message}")
		} catch (e: Throwable) {
			statusProperty.set(Status.SERVICE_ERROR)
			LOGGER.error(e.message, e)
		} finally {
			refresh = false
		} else {
			statusProperty.set(Status.NONE)
			refresh = false
		}
	}

	fun interrupt(reason: String = "interrupted") {
		interruptReason = reason
	}

	protected abstract fun doCheck(): List<StatusItem>

	protected fun checkForInterruptions() {
		if (controller.applicationConfiguration.paused) throw InterruptedException("paused")
		if (!config.enabled) throw InterruptedException("disabled")
		if (interruptReason != null) throw InterruptedException(interruptReason)
		interruptReason = null
	}
}