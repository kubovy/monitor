package com.poterion.monitor

import com.poterion.monitor.control.Controller
import com.poterion.monitor.sensors.jenkins.JenkinsModule
import com.poterion.monitor.sensors.sonar.SonarModule
import com.poterion.monitor.notifiers.raspiw2812.RaspiW2812Module
import com.poterion.monitor.notifiers.tray.SystemTrayModule
import javafx.application.Application
import javafx.application.Platform
import javafx.stage.Stage

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
class Main : Application() {
	companion object {
		@JvmStatic
		fun main(args: Array<String>) {
			launch(Main::class.java, *args)
		}
	}

	override fun start(primaryStage: Stage) {
		Platform.setImplicitExit(false)

		val controller = Controller(primaryStage).apply {
			registerModule(JenkinsModule)
			registerModule(SonarModule)
			registerModule(RaspiW2812Module)
			registerModule(SystemTrayModule)
		}
		controller.start()
	}
}


