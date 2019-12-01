package com.poterion.monitor.notifiers.devops.light

import com.poterion.monitor.notifiers.devops.light.data.LightConfig

fun List<LightConfig>.deepCopy(): List<LightConfig> = map { it.deepCopy() }

fun LightConfig.deepCopy(): LightConfig = copy(
		color1 = color1.copy(),
		color2 = color2.copy(),
		color3 = color3.copy(),
		color4 = color4.copy(),
		color5 = color5.copy(),
		color6 = color6.copy())
