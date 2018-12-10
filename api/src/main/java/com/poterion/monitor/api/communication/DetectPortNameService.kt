package com.poterion.monitor.api.communication

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