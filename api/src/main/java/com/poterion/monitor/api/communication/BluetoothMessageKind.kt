package com.poterion.monitor.api.communication

/**
 * Bluetooth message kind.
 *
 * @param code Code of the message type.
 * @author Jan Kubovy <jan@kubovy.eu>
 */
enum class BluetoothMessageKind(override var code: Int): MessageKind {
	/** Cyclic redundancy check message */
	CRC(0x00),
	/** ID of device message */
	IDD(0x01),
	/** Plain message */
	PLAIN(0xFE),
	/** Unknown message */
	UNKNOWN(0xFF);

	override val byteCode = code.toByte()
}