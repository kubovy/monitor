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
package com.poterion.monitor.api

import java.io.File

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
object Shared {
	/** Configuration directory. */
	var configDirectory: File = File(System.getProperty("user.home"))
			.resolve(".config")
			.resolve("poterion-monitor")

	/** Configuration file. */
	var configFile: File = File("config.yaml")
		get() = if (field.isAbsolute) field else configDirectory.resolve(field).absoluteFile

	/** Cache file. */
	var cacheFile: File = File("cache.yaml")
		get() = if (field.isAbsolute) field else configDirectory.resolve(field).absoluteFile
}