package com.poterion.monitor.control

import com.poterion.monitor.api.StatusCollector
import com.poterion.monitor.api.controllers.Service
import com.poterion.monitor.api.modules.ServiceModule
import com.poterion.monitor.data.services.ServiceConfig
import java.util.concurrent.Callable
import java.util.concurrent.Executors

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
class ControllerWorker private constructor(val services: Collection<Service<ServiceConfig>>) : Callable<Boolean> {
	companion object {
		private var instance: ControllerWorker? = null
		private val executor = Executors.newSingleThreadExecutor()

		fun start(services: Collection<Service<ServiceConfig>>) {
			if (instance == null || instance?.running == false) {
				instance = instance ?: ControllerWorker(services)
				instance?.running = true
				executor.submit(instance)
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
		while (running) try {
			val now = System.currentTimeMillis()
			services
					.filter { (now - (serviceLastChecked[it.config.name] ?: 0L)) > it.config.checkInterval }
					.forEach { service ->
						serviceLastChecked[service.config.name] = System.currentTimeMillis()
						service.check {
							StatusCollector.update(it,
									(service.definition as? ServiceModule)?.staticNotificationSet != false)
						}
					}
			Thread.sleep(1_000L)
		} catch (e: InterruptedException) {
			running = false
		}
		return !running
	}
}
