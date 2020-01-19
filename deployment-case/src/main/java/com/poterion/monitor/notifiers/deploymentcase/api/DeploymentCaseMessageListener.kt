package com.poterion.monitor.notifiers.deploymentcase.api

import com.poterion.monitor.notifiers.deploymentcase.data.Device

/**
 * Deployment case message listener interface.
 *
 * @author Jan Kubovy <jan@kubovy.eu>
 */
interface DeploymentCaseMessageListener {
	/**
	 * On progress change callback.
	 *
	 * @param progress Current progress
	 * @param count Total work to be done
	 * @param disable Whether controls should be disabled until done
	 */
	fun onProgress(progress: Int, count: Int, disable: Boolean) {
	}

	/**
	 * On action callback.
	 *
	 * @param device Device
	 * @param value Value
	 */
	fun onAction(device: Device, value: String) {
	}

	/**
	 * On verification callback.
	 *
	 * @param verified Whether state machine was verified or not.
	 */
	fun onVerification(verified: Boolean) {
	}
}