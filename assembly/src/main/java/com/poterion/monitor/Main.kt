package com.poterion.monitor

import com.poterion.monitor.control.ApplicationController
import com.poterion.monitor.notifiers.deploymentcase.DeploymentCaseModule
import com.poterion.monitor.notifiers.devops.light.DevOpsLight
import com.poterion.monitor.notifiers.tray.SystemTrayModule
import com.poterion.monitor.sensors.alertmanager.AlertManagerModule
import com.poterion.monitor.sensors.jenkins.JenkinsModule
import com.poterion.monitor.sensors.sonar.SonarModule
import com.poterion.monitor.ui.ConfigurationController
import javafx.application.Application
import javafx.application.Platform
import javafx.stage.Stage
import org.slf4j.Logger
import org.slf4j.LoggerFactory

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
 * class "Dev/Ops Light" as DevOpsLight <<Notifier>>
 * class "Raspi W2812" as RaspiW2812 <<Notifier>>
 * class "System Tray" as SystemTray <<Notifier>>
 *
 * Data <|-- API
 * API <|-left- Control
 * API <|-right- UI
 * API <|-- DevOpsLight
 * API <|-- RaspiW2812
 * API <|-- SystemTray
 * API <|-- Jenkins
 * API <|-- Sonar
 *
 * UI <|-- SystemTray
 *
 * Control <|-- Assembly
 * DevOpsLight <|-- Assembly
 * Jenkins <|-- Assembly
 * RaspiW2812 <|-- Assembly
 * Sonar <|-- Assembly
 * SystemTray <|-- Assembly
 *
 * @enduml
 */
/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
class Main : Application() {
	companion object {
		private val LOGGER: Logger = LoggerFactory.getLogger(Application::class.java)
		private const val CONFIG_FILE: String = "config.yaml"

		@JvmStatic
		fun main(args: Array<String>) {
			launch(Main::class.java, *args)
		}
	}

	override fun start(primaryStage: Stage) {
		Platform.setImplicitExit(false)

		val controller = ApplicationController(primaryStage, CONFIG_FILE).apply {
			registerModule(AlertManagerModule)
			registerModule(JenkinsModule)
			registerModule(SonarModule)
			registerModule(DeploymentCaseModule)
			registerModule(DevOpsLight)
			registerModule(SystemTrayModule)
		}
		controller.start()
		if (controller.applicationConfiguration.showOnStartup) ConfigurationController.create(controller)
	}
}


