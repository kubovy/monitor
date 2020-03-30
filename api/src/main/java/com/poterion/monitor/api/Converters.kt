/******************************************************************************
 * Copyright (c) 2020 Jan Kubovy <jan@kubovy.eu>                              *
 *                                                                            *
 * This program is free software: you can redistribute it and/or modify it    *
 * under the terms of the GNU General Public License as published by the Free *
 * Software Foundation, version 3.                                            *
 *                                                                            *
 * This program is distributed in the hope that it will be useful, but        *
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY *
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License    *
 * for more details.                                                          *
 *                                                                            *
 * You should have received a copy of the GNU General Public License along    *
 * with this program. If not, see <https://www.gnu.org/licenses/>.            *
 ******************************************************************************/
package com.poterion.monitor.api

import com.poterion.communication.serial.payload.RgbColor
import javafx.scene.paint.Color
import kotlin.math.roundToInt

/**
 * Converts [RgbColor] to [Color].
 * @author Jan Kubovy [jan@kubovy.eu]
 */
fun RgbColor.toColor() = Color.rgb(red, green, blue)

/**
 * Converts a [Color] to [RgbColor].
 * @author Jan Kubovy [jan@kubovy.eu]
 */
fun Color.toRGBColor(): RgbColor = listOf(red, green, blue)
		.map { (it * 255.0).roundToInt() }
		.let { (red, green, blue) -> RgbColor(red, green, blue) }

