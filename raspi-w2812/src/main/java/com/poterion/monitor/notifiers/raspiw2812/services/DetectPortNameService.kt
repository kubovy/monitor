package com.poterion.monitor.notifiers.raspiw2812.services

import com.poterion.monitor.notifiers.raspiw2812.control.SerialPortCommunicator
import javafx.concurrent.Service
import javafx.concurrent.Task

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
class DetectPortNameService : Service<String?>() {
	override fun createTask(): Task<String?> = object : Task<String?>() {
		override fun call(): String? = SerialPortCommunicator.findPort()
	}
}