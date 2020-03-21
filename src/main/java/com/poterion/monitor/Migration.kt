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
package com.poterion.monitor

import com.poterion.monitor.api.Shared
import com.poterion.monitor.api.toVersionNumber
import java.io.File

fun migrate() {
	if (Shared.configFile.exists()) {
		var content = Shared.configFile.bufferedReader().use { it.readText() }
		val configVersion = ".*\\nversion: \"?([0-9.])\"?.*".toRegex(RegexOption.DOT_MATCHES_ALL)
				.matchEntire(content)
				?.groupValues
				?.takeIf { it.size == 2 }
				?.get(1)
				?: "0"
		val configVersionNumber = configVersion.toVersionNumber()
		val appVersion = Shared.properties.getProperty("version", "0")
		val appVersionNumber = appVersion.toVersionNumber()

		if (configVersionNumber < appVersionNumber) {
			val backupFile = File(Shared.configFile.absolutePath + "-v" + configVersion)
			if (!backupFile.exists() || backupFile.delete()) Shared.configFile.copyTo(backupFile)

			// Migration 1
			if (configVersionNumber <= 0) {
				val regex = ".*(\\n {4}services:(\\n {4}- \"([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\"))+).*"
						.toRegex(RegexOption.DOT_MATCHES_ALL)

				var matches = regex.matchEntire(content)
				while (matches != null) {
					var replacement = matches.groupValues[1]
					replacement.lines().forEach { _ ->
						replacement = replacement.replace(
								" {4}- \"([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})\"".toRegex(),
								"    - uuid: \"\$1\"")
					}
					content = content.replace(matches.groupValues[1], replacement)
					matches = regex.matchEntire(content)
				}
			}

			Shared.configFile.bufferedWriter().use { it.write(content) }
		}
	}
}