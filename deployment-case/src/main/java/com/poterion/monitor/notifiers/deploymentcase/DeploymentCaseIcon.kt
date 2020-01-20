package com.poterion.monitor.notifiers.deploymentcase

import com.poterion.monitor.api.ui.Icon
import java.io.ByteArrayInputStream
import java.io.InputStream

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
enum class DeploymentCaseIcon : Icon {
	NUCLEAR_FOOTBALL,
	BLUETOOTH,
	CONNECTED,
	DISCONNECTED,
	VERIFIED,
	MISMATCH,
	UNVERIFIED,

	STATE,
	EVALUATION,
	CONDITIONS,
	CONDITION,
	ACTIONS,
	ACTION
}