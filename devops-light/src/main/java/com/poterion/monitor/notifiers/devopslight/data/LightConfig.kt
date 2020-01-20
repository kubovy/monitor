package com.poterion.monitor.notifiers.devopslight.data

import com.poterion.monitor.api.data.RGBColor

/**
 * WS281x light configuration.
 *
 * @author Jan Kubovy <jan@kubovy.eu>
 */
data class LightConfig(var pattern: LightPattern = LightPattern.OFF,
					   var color1: RGBColor = RGBColor(),
					   var color2: RGBColor = RGBColor(),
					   var color3: RGBColor = RGBColor(),
					   var color4: RGBColor = RGBColor(),
					   var color5: RGBColor = RGBColor(),
					   var color6: RGBColor = RGBColor(),
					   var color7: RGBColor = RGBColor(),
					   var delay: Int = LightPattern.OFF.delay ?: 1000,
					   var width: Int = LightPattern.OFF.width ?: 0,
					   var fading: Int = LightPattern.OFF.fading ?: 0,
					   var min: Int = LightPattern.OFF.min ?: 0,
					   var max: Int = LightPattern.OFF.max ?: 255,
					   var timeout: Int = LightPattern.OFF.timeout ?: 50)