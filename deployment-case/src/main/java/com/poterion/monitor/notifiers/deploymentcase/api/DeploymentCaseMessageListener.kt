package com.poterion.monitor.notifiers.deploymentcase.api

import com.poterion.monitor.notifiers.deploymentcase.data.Action

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
	 * @param action Action
	 */
	fun onAction(action: Action) {
	}
}