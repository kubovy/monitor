package com.poterion.monitor.notifiers.raspiw2812.data

import com.poterion.monitor.ui.Icon
import javafx.scene.paint.Color
import kotlin.math.roundToInt

data class LightConfig(var pattern: String,
					   var color1: Color,
					   var color2: Color,
					   var wait: Long,
					   var width: Int,
					   var fading: Int,
					   var min: Int,
					   var max: Int) {

	override fun toString(): String =
			"${pattern} ${color1.toLightString()} ${color2.toLightString()} ${wait} ${width} ${fading} ${min} ${max}"

	private fun Color.toLightString(): String =
			"${(red * 255).roundToInt()},${(green * 255).roundToInt()},${(blue * 255).roundToInt()}"
}