package com.poterion.monitor.api.communication

/**
 * Message kind interface.
 *
 * @author Jan Kubovy <jan@kubovy.eu>
 */
interface MessageKind {
	/** Code of the message type. */
	val code: Int

	/** Byte code of the message kind. */
	val byteCode: Byte

	/** Confirmation wait delay per chunk in ms */
	val delay: Long?
}