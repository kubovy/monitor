package com.poterion.monitor

import com.poterion.monitor.control.Controller
import com.poterion.monitor.notifiers.deploymentcase.DeploymentCaseModule
import com.poterion.monitor.notifiers.raspi.ws281x.RaspiWS281xModule
import com.poterion.monitor.notifiers.tray.SystemTrayModule
import com.poterion.monitor.sensors.jenkins.JenkinsModule
import com.poterion.monitor.sensors.sonar.SonarModule
import com.poterion.monitor.ui.ConfigurationController
import javafx.application.Application
import javafx.application.Platform
import javafx.stage.Stage

/*
 * @startuml
 * package Core {
 *   interface Data
 *   interface API
 *   abstract class UI
 *   class Control
 * }
 *
 * class Jenkins <<Service>>
 * class Sonar <<Service>>
 *
 * class "System Tray" as SystemTray <<Notifier>>
 * class "Raspi W2812" as RaspiW2812 <<Notifier>>
 *
 * Data <|-- API
 * API <|-left- Control
 * API <|-- RaspiW2812
 * API <|-- SystemTray
 * API <|-right- UI
 * API <|-- Jenkins
 * API <|-- Sonar
 *
 * UI <|-- SystemTray
 *
 * Control <|-- Assembly
 * Jenkins <|-- Assembly
 * Sonar <|-- Assembly
 * SystemTray <|-- Assembly
 * RaspiW2812 <|-- Assembly
 *
 * @enduml
 */
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
			registerModule(DeploymentCaseModule)
			registerModule(RaspiWS281xModule)
			registerModule(SystemTrayModule)
		}
		controller.start()
		if (controller.config.showOnStartup) ConfigurationController.create(controller)
	}
}


