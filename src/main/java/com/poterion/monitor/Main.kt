/******************************************************************************
 * Copyright (C) 2020 Jan Kubovy (jan@kubovy.eu)                              *
 *                                                                            *
 * This program is free software: you can redistribute it and/or modify       *
 * it under the terms of the GNU General Public License as published by       *
 * the Free Software Foundation, either version 3 of the License, or          *
 * (at your option) any later version.                                        *
 *                                                                            *
 * This program is distributed in the hope that it will be useful,            *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of             *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the              *
 * GNU General Public License for more details.                               *
 *                                                                            *
 * You should have received a copy of the GNU General Public License          *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.      *
 ******************************************************************************/
package com.poterion.monitor

import com.poterion.monitor.api.Shared
import com.poterion.monitor.control.ApplicationController
import com.poterion.monitor.notifiers.deploymentcase.DeploymentCaseModule
import com.poterion.monitor.notifiers.devopslight.DevOpsLight
import com.poterion.monitor.notifiers.notifications.NotificationsModule
import com.poterion.monitor.notifiers.tabs.NotificationTabsModule
import com.poterion.monitor.notifiers.tray.SystemTrayModule
import com.poterion.monitor.sensors.alertmanager.AlertManagerModule
import com.poterion.monitor.sensors.feed.SyndicationFeedModule
import com.poterion.monitor.sensors.gerritcodereview.GerritCodeReviewModule
import com.poterion.monitor.sensors.jenkins.JenkinsModule
import com.poterion.monitor.sensors.jira.JiraModule
import com.poterion.monitor.sensors.sonar.SonarModule
import com.poterion.monitor.sensors.storyboard.StoryboardModule
import com.poterion.monitor.ui.ConfigurationController
import javafx.application.Application
import javafx.application.Platform
import javafx.stage.Stage
import org.apache.commons.cli.*
import java.io.File
import kotlin.system.exitProcess


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
class Main: Application() {
	companion object {
		@JvmStatic
		fun main(args: Array<String>) {
			val settingsDirectory = Option.builder("d")
					.longOpt("directory")
					.argName("DIRECTORY")
					.desc("Settings directory where default config and cache is stored.")
					.hasArg()
					.build()
			val configurationFile = Option.builder("c")
					.longOpt("config")
					.argName("FILE")
					.desc("Configuration file. If relative, it will be store inside the DIRECTORY path.")
					.hasArg()
					.build()
			val cacheFile = Option.builder()
					.longOpt("cache")
					.argName("FILE")
					.desc("Cache file. If relative, it will be store inside the DIRECTORY path.")
					.hasArg()
					.build()
			val help = Option.builder("h")
					.longOpt("help")
					.desc("Show this screen.")
					.build()


			val options = Options()
			options.addOption(settingsDirectory)
			options.addOption(configurationFile)
			options.addOption(cacheFile)
			options.addOption(help)

			val parser: CommandLineParser = DefaultParser()
			try {
				val line = parser.parse(options, args)
				if (line.hasOption("help")) {
					val formatter = HelpFormatter()
					formatter.printHelp("poterion-monitor", options)
					exitProcess(0)
				}

				line.getOptionValue("directory")?.also { Shared.configDirectory = File(it) }
				line.getOptionValue("config")?.also { Shared.configFile = File(it) }
				line.getOptionValue("cache")?.also { Shared.cacheFile = File(it) }

				launch(Main::class.java, *args)
			} catch (e: ParseException) { // oops, something went wrong
				System.err.println("Parsing failed.  Reason: " + e.message)
				val formatter = HelpFormatter()
				formatter.printHelp("poterion-monitor", options)
				exitProcess(0)
			}
		}
	}

	override fun start(primaryStage: Stage) {
		Platform.setImplicitExit(false)

		val controller = ApplicationController(primaryStage,
				AlertManagerModule,
				GerritCodeReviewModule,
				JenkinsModule,
				JiraModule,
				SonarModule,
				StoryboardModule,
				SyndicationFeedModule,
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


