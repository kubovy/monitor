package com.poterion.monitor.gerrit.code.review

import com.poterion.monitor.api.controllers.ControllerInterface
import com.poterion.monitor.api.modules.ServiceModule
import com.poterion.monitor.api.ui.Icon
import com.poterion.monitor.data.ApplicationConfiguration
import com.poterion.monitor.data.nextUUID
import com.poterion.monitor.gerrit.code.review.control.GerritCodeReviewService
import com.poterion.monitor.gerrit.code.review.data.GerritCodeReviewConfig
import com.poterion.monitor.gerrit.code.review.ui.GerritCodeReviewIcon
import kotlin.reflect.KClass

object GerritCodeReviewModule : ServiceModule<GerritCodeReviewConfig, GerritCodeReviewService> {
	override val configClass: KClass<GerritCodeReviewConfig> = GerritCodeReviewConfig::class

	override val title: String
		get() = "Gerrit Code Review"

	override val icon: Icon = GerritCodeReviewIcon.GERRIT

	override val staticNotificationSet: Boolean
		get() = false

	override fun createController(controller: ControllerInterface, applicationConfiguration: ApplicationConfiguration):
			GerritCodeReviewService = GerritCodeReviewConfig(uuid = applicationConfiguration.services.nextUUID(), name = title)
			.also { applicationConfiguration.services[it.uuid] = it }
			.let { GerritCodeReviewService(controller, it) }

	override fun loadControllers(controller: ControllerInterface, applicationConfiguration: ApplicationConfiguration):
			Collection<GerritCodeReviewService> = applicationConfiguration
			.services
			.values
			.filterIsInstance<GerritCodeReviewConfig>()
			.map { GerritCodeReviewService(controller, it) }
}