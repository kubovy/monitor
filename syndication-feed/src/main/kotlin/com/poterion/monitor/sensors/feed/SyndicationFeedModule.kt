package com.poterion.monitor.sensors.feed

import com.poterion.monitor.api.controllers.ControllerInterface
import com.poterion.monitor.api.modules.ServiceModule
import com.poterion.monitor.data.ModuleConfig
import com.poterion.monitor.data.data.ApplicationConfiguration
import com.poterion.monitor.data.nextUUID
import com.poterion.monitor.sensors.feed.control.SyndicationFeedService
import com.poterion.monitor.sensors.feed.data.SyndicationFeedConfig
import com.poterion.utils.javafx.Icon
import kotlin.reflect.KClass

object SyndicationFeedModule : ServiceModule<SyndicationFeedConfig, SyndicationFeedService> {
	override val configClass: KClass<SyndicationFeedConfig> = SyndicationFeedConfig::class

	override val title: String
		get() = "Syndication Feed"

	override val icon: Icon = SyndicationFeedIcon.RSS

	override fun createController(controller: ControllerInterface, applicationConfiguration: ApplicationConfiguration):
			SyndicationFeedService =
		SyndicationFeedService(controller,
			SyndicationFeedConfig(uuid = applicationConfiguration.services.nextUUID(),
				name = title))

	override fun loadController(controller: ControllerInterface, config: ModuleConfig): SyndicationFeedService =
		SyndicationFeedService(controller,
			config as SyndicationFeedConfig)
}