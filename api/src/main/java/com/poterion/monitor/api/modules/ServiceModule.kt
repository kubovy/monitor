package com.poterion.monitor.api.modules

import com.poterion.monitor.api.controllers.Service
import com.poterion.monitor.data.services.ServiceConfig

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
interface ServiceModule<out Conf : ServiceConfig, out Ctrl : Service<Conf>> : Module<Conf, Ctrl>