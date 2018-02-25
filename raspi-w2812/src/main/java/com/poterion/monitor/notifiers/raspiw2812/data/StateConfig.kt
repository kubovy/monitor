package com.poterion.monitor.notifiers.raspiw2812.data

import com.poterion.monitor.api.ui.Icon

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
data class StateConfig(var title: String,
					   val icon: Icon? = null,
					   var lightConfigs: List<LightConfig>? = null)