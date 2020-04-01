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
package com.poterion.monitor.control

import com.poterion.monitor.api.StatusCollector
import com.poterion.monitor.api.controllers.Service
import com.poterion.monitor.api.modules.ServiceModule
import com.poterion.monitor.data.data.ApplicationConfiguration
import com.poterion.monitor.data.services.ServiceConfig
import com.poterion.monitor.data.services.ServiceSubConfig
import com.poterion.utils.kotlin.parallelStreamIntermediate
import org.slf4j.LoggerFactory
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import kotlin.math.max

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
class ControllerWorker private constructor(
		private val config: ApplicationConfiguration,
		private val services: Collection<Service<ServiceConfig<out ServiceSubConfig>>>) :

		Callable<Boolean> {

	companion object {
		private val LOGGER = LoggerFactory.getLogger(ControllerWorker::class.java)
		private var instance: ControllerWorker? = null
		private val executor = Executors.newSingleThreadExecutor()

		fun start(config: ApplicationConfiguration, services: Collection<Service<ServiceConfig<out ServiceSubConfig>>>) {
			if (instance == null || instance?.running == false) {
				instance = instance ?: ControllerWorker(config, services)
				instance?.running = true
				executor.submit(instance!!)
			}
		}

		fun stop() {
			executor.shutdown()
			instance?.running = false
			executor.shutdownNow()
		}
	}

	private var running = true
	private val serviceLastChecked = mutableMapOf<String, Long>()

	override fun call(): Boolean {
		Thread.currentThread().name = "Controller Worker"
		while (running) try {
			val parallelism = max(3, Runtime.getRuntime().availableProcessors() / 2)
			if (!config.paused) services.filter { it.shouldRun }
					.parallelStreamIntermediate(parallelism) { service ->
						if (!config.paused && service.config.enabled) try {
							service.check {
								StatusCollector.update(it,
										(service.definition as? ServiceModule)?.staticNotificationSet != false)
							}
						} catch (t: Throwable) {
							LOGGER.error(t.message, t)
						} finally {
							serviceLastChecked[service.config.uuid] = System.currentTimeMillis()
						}
					}
			Thread.sleep(1_000L)
		} catch (e: InterruptedException) {
			running = false
		}
		return !running
	}
}
