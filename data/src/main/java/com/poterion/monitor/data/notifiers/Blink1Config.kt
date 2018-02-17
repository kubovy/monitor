package com.poterion.monitor.data.notifiers

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
data class Blink1Config(override var name: String = "",
                        override var enabled: Boolean = true,
                        var url: String = "") : NotifierConfig