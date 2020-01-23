package com.poterion.monitor.api.modules

import com.poterion.monitor.api.controllers.Notifier
import com.poterion.monitor.data.notifiers.NotifierConfig

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
interface NotifierModule<out Conf : NotifierConfig, out Ctrl : Notifier<Conf>> : Module<Conf, Ctrl>