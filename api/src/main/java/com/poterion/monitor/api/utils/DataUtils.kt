/******************************************************************************
 * Copyright (C) 2020 Jan Kubovy (jan@kubovy.eu)                              *
 *                                                                            *
 * This program is free software: you can redistribute it and/or modify       *
 * it under the terms of the GNU General Public License as published by       *
 * the Free Software Foundation, either version 3 of the License, or          *
 * (at your option) any later version.                                        *
 *                                                                            *
 * This program is distributed in the hope that it will be useful,            *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of             *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the              *
 * GNU General Public License for more details.                               *
 *                                                                            *
 * You should have received a copy of the GNU General Public License          *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.      *
 ******************************************************************************/
package com.poterion.monitor.api.utils

import com.poterion.monitor.api.data.RGBColor
import javafx.scene.paint.Color
import kotlin.math.roundToInt

fun String.toRGBColor(): RGBColor? = "^#?([0-9A-Fa-f]{2})([0-9A-Fa-f]{2})([0-9A-Fa-f]{2})".toRegex()
		.matchEntire(this)
		?.groupValues
		?.mapNotNull { it.toIntOrNull(16) }
		?.takeIf { it.size == 3 }
		?.let { RGBColor(it[0], it[1], it[2]) }

fun String.toColor(): Color? = toRGBColor()?.toColor()

fun Color.toRGBColor(): RGBColor = RGBColor(red.relativeToByte(), green.relativeToByte(), blue.relativeToByte())

fun Color.toHex(prefix: String = "#") = toRGBColor().toHex(prefix)

fun RGBColor.toColor(): Color = Color.rgb(red, green, blue)

fun RGBColor.toHex(prefix: String = "#") = listOf(red, green, blue).joinToString("", prefix) { "%02X".format(it) }

private fun Double.relativeToByte(): Int = (this * 255.0).roundToInt()