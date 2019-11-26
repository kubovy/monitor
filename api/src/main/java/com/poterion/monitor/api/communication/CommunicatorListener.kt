package com.poterion.monitor.api.communication

/**
 * Bluetooth listener interface.
 *
 * @author Jan Kubovy <jan@kubovy.eu>
 */
interface CommunicatorListener {
	/**
	 * On connecting callback
	 *
	 * @param channel Channel triggering the event.
	 */
	fun onConnecting(channel: Channel)

	/**
	 * On connection established callback
	 *
	 * @param channel Channel triggering the event.
	 */
	fun onConnect(channel: Channel)

	/**
	 * On connection lost callback
	 *
	 * @param channel Channel triggering the event.
	 */
	fun onDisconnect(channel: Channel)

	/**
	 * On message callback
	 *
	 * @param channel Channel triggering the event.
	 * @param message Received message
	 */
	fun onMessageReceived(channel: Channel, message: IntArray)

	/**
	 * On message sent callback.
	 *
	 * @param channel Channel triggering the event.
	 * @param message Sent raw message
	 * @param remaining Remaining message count in the queue.
	 */
	fun onMessageSent(channel: Channel, message: IntArray, remaining: Int)
}