package com.poterion.monitor.control

import com.poterion.monitor.control.services.Services
import java.util.concurrent.Callable

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
class ControllerWorker : Callable<Boolean> {
    private var running = true

    fun stop() {
        running = false
    }

    override fun call(): Boolean {
        while (running) try {
            Services.check()
            Thread.sleep(1_000L)
        } catch (e: InterruptedException) {
            running = false
        }
        return !running
    }
}
