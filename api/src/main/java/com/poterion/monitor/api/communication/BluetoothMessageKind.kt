package com.poterion.monitor.api.communication

enum class BluetoothMessageKind(var code: Int) {
	CRC(0x00),
	IDD(0x01),
	PULL_STATE_MACHINE(0x80),
	PUSH_STATE_MACHINE(0x81),
	VALUE_CHANGED(0x82),
	SET_VALUE(0x83),
	GET_STATE(0x84),
	CURRENT_STATE(0x85),
	PLAIN(0xFE),
	UNKNOWN(0xFF);

	val byteCode = code.toByte()
}