package com.poterion.monitor.sensors.gerritcodereview

import com.poterion.monitor.api.controllers.ControllerInterface
import com.poterion.monitor.api.modules.ServiceModule
import com.poterion.monitor.data.ModuleConfig
import com.poterion.monitor.data.data.ApplicationConfiguration
import com.poterion.monitor.data.nextUUID
import com.poterion.monitor.sensors.gerritcodereview.control.GerritCodeReviewService
import com.poterion.monitor.sensors.gerritcodereview.data.GerritCodeReviewConfig
import com.poterion.utils.javafx.Icon
import kotlin.reflect.KClass

object GerritCodeReviewModule : ServiceModule<GerritCodeReviewConfig, GerritCodeReviewService> {
	override val configClass: KClass<GerritCodeReviewConfig> = GerritCodeReviewConfig::class

	override val title: String
		get() = "Gerrit Code Review"

	override val icon: Icon = GerritCodeReviewIcon.GERRIT

	override fun createController(controller: ControllerInterface, applicationConfiguration: ApplicationConfiguration):
			GerritCodeReviewService = GerritCodeReviewService(controller,
			GerritCodeReviewConfig(uuid = applicationConfiguration.services.nextUUID(), name = title))

	override fun loadController(controller: ControllerInterface, config: ModuleConfig): GerritCodeReviewService =
			GerritCodeReviewService(controller, config as GerritCodeReviewConfig)
}