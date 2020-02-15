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
package com.poterion.monitor.notifiers.deploymentcase.api

import com.poterion.utils.kotlin.noop
import com.poterion.monitor.notifiers.deploymentcase.data.Device

/**
 * Deployment case message listener interface.
 *
 * @author Jan Kubovy [jan@kubovy.eu]
 */
interface DeploymentCaseMessageListener {
	/**
	 * On progress change callback.
	 *
	 * @param progress Current progress
	 * @param count Total work to be done
	 * @param disable Whether controls should be disabled until done
	 */
	fun onProgress(progress: Int, count: Int, disable: Boolean) = noop()

	/**
	 * On action callback.
	 *
	 * @param device Device
	 * @param value Value
	 */
	fun onAction(device: Device, value: String) = noop()

	/**
	 * On verification callback.
	 *
	 * @param verified Whether state machine was verified or not.
	 */
	fun onVerification(verified: Boolean) = noop()
}