package com.poterion.monitor.api.communication

import javafx.beans.property.SimpleStringProperty

class RemoteDeviceInfo @JvmOverloads constructor(name: String = "", address: String = "") {

	private val _deviceName = SimpleStringProperty(name)
	private val _deviceAddress = SimpleStringProperty(address)

	var deviceName: String
		get() = _deviceName.get()
		set(value) {
			_deviceName.set(value)
		}

	var deviceAddress: String
		get() = _deviceAddress.get()
		set(value) {
			_deviceAddress.set(value)
		}
}