package com.poterion.monitor.data.notifiers

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
data class RaspiW2812Config(override var name: String = "",
                            override var enabled: Boolean = true,
                            var deviceName: String = "",
                            var items: Collection<RaspiW2812ItemConfig> = emptyList()) : NotifierConfig