package com.poterion.monitor.notifiers.devops.light.data

/**
 * WS281x light configuration.
 *
 * @author Jan Kubovy <jan@kubovy.eu>
 */
data class LightConfig(var pattern: LightPattern = LightPattern.OFF,
					   var color1: LightColor = LightColor(),
					   var color2: LightColor = LightColor(),
					   var color3: LightColor = LightColor(),
					   var color4: LightColor = LightColor(),
					   var color5: LightColor = LightColor(),
					   var color6: LightColor = LightColor(),
					   var color7: LightColor = LightColor(),
					   var delay: Int = LightPattern.OFF.delay ?: 1000,
					   var width: Int = LightPattern.OFF.width ?: 0,
					   var fading: Int = LightPattern.OFF.fading ?: 0,
					   var min: Int = LightPattern.OFF.min ?: 0,
					   var max: Int = LightPattern.OFF.max ?: 255,
					   var timeout: Int = LightPattern.OFF.timeout ?: 50)