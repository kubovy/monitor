package com.poterion.monitor.api.modules

import com.poterion.monitor.api.controllers.ControllerInterface
import com.poterion.monitor.data.Config
import com.poterion.monitor.data.services.ServiceConfig
import kotlin.reflect.KClass

interface Module<out Conf : Any, out Controller : Any> {
	val configClass: KClass<out Conf>
	fun createControllers(controller: ControllerInterface, config: Config): Collection<Controller>
}