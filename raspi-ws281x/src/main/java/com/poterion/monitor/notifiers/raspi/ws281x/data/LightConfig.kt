package com.poterion.monitor.notifiers.raspi.ws281x.data

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
data class LightConfig(var pattern: String = "light",
					   var color1: LightColor = LightColor(),
					   var color2: LightColor = LightColor(),
					   var color3: LightColor = LightColor(),
					   var color4: LightColor = LightColor(),
					   var color5: LightColor = LightColor(),
					   var color6: LightColor = LightColor(),
					   var wait: Long = 50L,
					   var width: Int = 3,
					   var fading: Int = 0,
					   var min: Int = 0,
					   var max: Int = 100)