package com.poterion.monitor.notifiers.deploymentcase.api

import com.poterion.monitor.api.communication.MessageKind

/**
 * Deployment case message kind.
 *
 * @param code Code of the message type.
 * @author Jan Kubovy <jan@kubovy.eu>
 */
enum class DeploymentCaseMessageKind(final override var code: Int, override val delay: Long? = null) : MessageKind {
	/** Configuration checksum */
	CONFIGURATION(0x80),
	/** Request state machine */
	PULL_STATE_MACHINE(0x81),
	/** State machine transmission */
	PUSH_STATE_MACHINE(0x82, 2_500L),
	/** Request current state */
	GET_STATE(0x83),
	/** Transmission of current partial or whole current state */
	SET_STATE(0x84);

	override val byteCode = code.toByte()
}