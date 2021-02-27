/******************************************************************************
 * Copyright (c) 2020 Jan Kubovy <jan@kubovy.eu>                              *
 *                                                                            *
 * This program is free software: you can redistribute it and/or modify it    *
 * under the terms of the GNU General Public License as published by the Free *
 * Software Foundation, version 3.                                            *
 *                                                                            *
 * This program is distributed in the hope that it will be useful, but        *
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY *
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License    *
 * for more details.                                                          *
 *                                                                            *
 * You should have received a copy of the GNU General Public License along    *
 * with this program. If not, see <https://www.gnu.org/licenses/>.            *
 ******************************************************************************/
package com.poterion.monitor.api.workers

import com.poterion.monitor.api.controllers.HttpServiceModule
import com.poterion.monitor.api.services.PoterionRestService
import com.poterion.monitor.data.HttpConfig
import com.poterion.monitor.data.data.ApplicationConfiguration
import io.reactivex.subjects.BehaviorSubject
import javafx.util.Duration
import org.slf4j.LoggerFactory
import java.util.concurrent.Callable
import java.util.concurrent.Executors

/**
 * New version update checker.
 *
 * @author Jan Kubovy [jan@kubovy.eu]
 */
class UpdateChecker private constructor(applicationConfiguration: ApplicationConfiguration) : Callable<Boolean> {

	companion object {
		private val LOGGER = LoggerFactory.getLogger(UpdateChecker::class.java)
		val lastVersion: BehaviorSubject<String> = BehaviorSubject.create()
		private var instance: UpdateChecker? = null
		private val executor = Executors.newSingleThreadExecutor()

		fun start(applicationConfiguration: ApplicationConfiguration) {
			if (instance == null || instance?.running == false) {
				instance = (instance ?: UpdateChecker(applicationConfiguration)).also {
					it.running = true
					executor.submit(it)
				}
			}
		}

		fun stop() {
			executor.shutdown()
			instance?.running = false
			executor.shutdownNow()
		}
	}

	private var running = true
	private val http = HttpServiceModule(applicationConfiguration, HttpConfig(url = "https://artifacts.poterion.com"))
	private val service
		get() = http.retrofit?.create(PoterionRestService::class.java)

	override fun call(): Boolean {
		Thread.currentThread().name = "Update Checker"
		Thread.sleep(Duration.millis(1.0).toMillis().toLong())
		while (running) try {
			var sleep = Duration.hours(1.0)
			try {
				service?.latest()?.execute()
						?.takeIf { it.isSuccessful }
						?.body()
						?.trim()
						?.also { lastVersion.onNext(it) }
			} catch (e: Exception) {
				LOGGER.error(e.message, e)
				sleep = Duration.minutes(5.0)
			}
			Thread.sleep(sleep.toMillis().toLong())
		} catch (e: InterruptedException) {
			running = false
		}
		return !running
	}

}
