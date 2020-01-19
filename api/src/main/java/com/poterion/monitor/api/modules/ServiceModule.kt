package com.poterion.monitor.api.modules

import com.poterion.monitor.api.controllers.Service
import com.poterion.monitor.data.services.ServiceConfig

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
interface ServiceModule<out Conf : ServiceConfig, out Ctrl : Service<Conf>> : Module<Conf, Ctrl> {
	/**
	 * Weather the notification set is static.
	 *
	 * This means that the same notification set will be sent with different states. If false a changing set may be
	 * sent. This is true for example for alert manager which only send alerts and not an de-escalation notification.
	 */
	val staticNotificationSet: Boolean
		get() = true
}