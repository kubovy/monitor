package com.poterion.monitor.api

import com.poterion.monitor.data.notifiers.NotifierAction

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
interface NotifierController {
    fun execute(action: NotifierAction, vararg params: String)
}