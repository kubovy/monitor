package com.poterion.monitor.control

import javafx.application.Platform
import java.util.concurrent.Callable
import java.util.concurrent.Executors

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
class ControllerWorker(private val check: () -> Unit) : Callable<Boolean> {
	companion object {
		private var instance: ControllerWorker? = null
		private val executor = Executors.newSingleThreadExecutor()

		fun start(check: () -> Unit) {
			instance?.running = true
			instance = ControllerWorker(check)
			executor.submit(instance)
		}

		fun stop() {
			executor.shutdown()
			instance?.running = false
			executor.shutdownNow()
		}
	}

	private var running = true

	override fun call(): Boolean {
		while (running) try {
			check.invoke()
			Thread.sleep(1_000L)
		} catch (e: InterruptedException) {
			running = false
		}
		return !running
	}
}
