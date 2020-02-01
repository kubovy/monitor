package com.poterion.monitor

import com.poterion.monitor.control.ApplicationController
import com.poterion.monitor.notifiers.deploymentcase.DeploymentCaseModule
import com.poterion.monitor.notifiers.devopslight.DevOpsLight
import com.poterion.monitor.notifiers.notifications.NotificationsModule
import com.poterion.monitor.notifiers.tabs.NotificationTabsModule
import com.poterion.monitor.notifiers.tray.SystemTrayModule
import com.poterion.monitor.sensors.alertmanager.AlertManagerModule
import com.poterion.monitor.sensors.gerritcodereview.GerritCodeReviewModule
import com.poterion.monitor.sensors.jenkins.JenkinsModule
import com.poterion.monitor.sensors.jira.JiraModule
import com.poterion.monitor.sensors.sonar.SonarModule
import com.poterion.monitor.sensors.storyboard.StoryboardModule
import com.poterion.monitor.ui.ConfigurationController
import javafx.application.Application
import javafx.application.Platform
import javafx.stage.Stage
import java.io.File

/*
 * @startuml
 * package Core {
 *   interface Data
 *   interface API
 *   abstract class UI
 *   class Control
 * }
 *
 * class "Service X" as ServiceX <<Service>>
 *
 * class "Notifier Y" as NotifierY <<Notifier>>
 * class "System Tray" as SystemTray <<Notifier>>
 *
 * Data <|-- API
 * API <|-left- Control
 * API <|-right- UI
 * API <|-- NotifierY
 * API <|-- SystemTray
 * API <|-- ServiceX
 *
 * UI <|-- SystemTray
 *
 * @enduml
 */
/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
class Main : Application() {
	companion object {
		private lateinit var configFile: String

		@JvmStatic
		fun main(args: Array<String>) {
			configFile = args.map { File(it) }.firstOrNull { it.exists() }?.absolutePath
					?: File(System.getProperty("user.home"))
							.resolve(".config")
							.resolve("poterion-monitor")
							.resolve("config.yaml")
							.absolutePath

			launch(Main::class.java, *args)
		}
	}

	override fun start(primaryStage: Stage) {
		Platform.setImplicitExit(false)

		val controller = ApplicationController(primaryStage, configFile,
				AlertManagerModule,
				GerritCodeReviewModule,
				JenkinsModule,
				JiraModule,
				SonarModule,
				StoryboardModule,
				DeploymentCaseModule,
				DevOpsLight,
				NotificationsModule,
				NotificationTabsModule,
				SystemTrayModule)
		controller.start()
		if (controller.applicationConfiguration.showOnStartup) {
			ConfigurationController.create(controller)
			if (controller.applicationConfiguration.startMinimized) primaryStage.isIconified = true
		}
	}
}


