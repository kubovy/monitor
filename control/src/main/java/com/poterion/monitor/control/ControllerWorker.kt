package com.poterion.monitor.control

import java.util.concurrent.Callable

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
class ControllerWorker(private val check: () -> Unit) : Callable<Boolean> {
	private var running = true

	fun stop() {
		running = false
	}

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
