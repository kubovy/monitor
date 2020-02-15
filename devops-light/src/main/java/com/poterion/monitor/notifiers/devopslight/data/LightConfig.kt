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
package com.poterion.monitor.notifiers.devopslight.data

import com.poterion.monitor.api.data.RGBColor

/**
 * WS281x light configuration.
 *
 * @author Jan Kubovy [jan@kubovy.eu]
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