package com.poterion.monitor.api.modules

import com.poterion.monitor.api.controllers.ControllerInterface
import com.poterion.monitor.data.Config
import kotlin.reflect.KClass

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
interface Module<out Conf : Any, out Ctrl : Any> {
	val configClass: KClass<out Conf>
	fun createControllers(controller: ControllerInterface, config: Config): Collection<Ctrl>
}