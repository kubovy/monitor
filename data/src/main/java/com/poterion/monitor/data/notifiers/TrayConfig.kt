package com.poterion.monitor.data.notifiers

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
data class TrayConfig(override var name: String = "",
                      override var enabled: Boolean = true) : NotifierConfig