package com.poterion.monitor.notifiers.devops.light

import com.poterion.monitor.notifiers.devops.light.data.LightColor
import com.poterion.monitor.notifiers.devops.light.data.LightConfig
import javafx.scene.paint.Color
import kotlin.math.roundToInt

fun Color.toLightColor(): LightColor = LightColor(red.relativeToByte(), green.relativeToByte(), blue.relativeToByte())

fun LightColor.toColor(): Color = Color(red.toDouble() / 255.0, green.toDouble() / 255.0, blue.toDouble() / 255.0, 1.0)

fun List<LightConfig>.deepCopy(): List<LightConfig> = map { it.deepCopy() }

fun LightConfig.deepCopy(): LightConfig = copy(
		color1 = color1.copy(),
		color2 = color2.copy(),
		color3 = color3.copy(),
		color4 = color4.copy(),
		color5 = color5.copy(),
		color6 = color6.copy())

private fun Double.relativeToByte(): Int = (this * 255.0).roundToInt()