package com.poterion.monitor.control

import com.poterion.monitor.api.StatusCollector
import com.poterion.monitor.api.controllers.Service
import com.poterion.monitor.api.modules.ServiceModule
import com.poterion.monitor.data.services.ServiceConfig
import org.slf4j.LoggerFactory
import java.util.concurrent.Callable
import java.util.concurrent.Executors

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
class ControllerWorker private constructor(private val services: Collection<Service<ServiceConfig>>) : Callable<Boolean> {
	companion object {
		private val LOGGER = LoggerFactory.getLogger(ControllerWorker::class.java)
		private var instance: ControllerWorker? = null
		private val executor = Executors.newSingleThreadExecutor()

		fun start(services: Collection<Service<ServiceConfig>>) {
			if (instance == null || instance?.running == false) {
				instance = instance ?: ControllerWorker(services)
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
			val now = System.currentTimeMillis()
			services.filter { it.refresh || (now - (serviceLastChecked[it.config.name] ?: 0L)) > it.config.checkInterval }
					.forEach { service ->
						service.refresh = false
						serviceLastChecked[service.config.name] = System.currentTimeMillis()
						try {
							service.check {
								StatusCollector.update(it,
										(service.definition as? ServiceModule)?.staticNotificationSet != false)
							}
						} catch (t: Throwable) {
							LOGGER.error(t.message, t)
						}
					}
			Thread.sleep(1_000L)
		} catch (e: InterruptedException) {
			running = false
		}
		return !running
	}
}
