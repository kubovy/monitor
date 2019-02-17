/*
 * This is the source code of PC-status.
 * It is licensed under GNU AGPL v3 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Andrea Bravaccino.
 */
package com.poterion.monitor.api.communication

//import pcstatus.dataPackage.SingletonStaticGeneralStats

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.*
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import javax.bluetooth.RemoteDevice
import javax.bluetooth.UUID
import javax.microedition.io.Connector
import javax.microedition.io.StreamConnection
import javax.microedition.io.StreamConnectionNotifier

/**
 * Class that implements an SPP Server which accepts single line of
 * message from an SPP client and sends a single line of response to the client.
 *
 * @author Andrea Bravaccino
 */

internal class BluetoothSPP {
	private var outputStream: OutputStream? = null
	private var inputStream: InputStream? = null
	private var streamConnNotifier: StreamConnection? = null

	private var connectionIsAvaible = false
	private var lastAliveTimestamp = System.currentTimeMillis()

	private val bluetoothServerExecutor = Executors.newSingleThreadExecutor()
	private val incomingMessageThreadExecutor = Executors.newSingleThreadExecutor()
	private val outgoingMessageThreadExecutor = Executors.newSingleThreadExecutor()

	private val startBluetoothServer = Thread(Runnable {
		Thread.currentThread().name = "startServerBluetooth"
		bluetoothServer()
	}, "startServerBluetooth")


	private val receiveMessageThread = Thread(Runnable {
		Thread.currentThread().name = "receiveMessageThread"
		inputStream/*?.let { BufferedReader(InputStreamReader(it)) }?*/?.use { input ->
			while (!Thread.interrupted()) try {
				val kind = input.read()
				when (kind) {
					0x00 -> {
						val crc = input.read()
						LOGGER.info("CRC: ${"0x%02X".format(crc and 0xFF)}")
					}
					0xFE -> {
						var ch = input.read()
						var message = ""
						while (ch != '\n'.toInt()) {
							message += ch.toChar()
							ch = input.read()
						}
						LOGGER.info(">> ${message}")
					}
				}
//				if (reader.ready()) try {
//					val ch = reader.readLine()
//					LOGGER.info(">> ${ch}")
//					//LOGGER.info("0x%02X ".format(ch))
			} catch (e: IOException) {
				LOGGER.error(e.message, e)
				Thread.currentThread().interrupt()
				throw e
			}
		}

	}, "receiveMessageThread")

	private val sendMessageThread = Thread(Runnable {
		Thread.currentThread().name = "sendMessageThread"
		outputStream/*?.let { OutputStreamWriter(it) }*/?.use { output ->
			output.write("HELO".toByteArray())
			output.flush()
			lastAliveTimestamp = System.currentTimeMillis()
			while (!Thread.interrupted()) try {
				if (writeBuffer.isNotEmpty()) {
					//writer.write(0xFE)
					val message = listOf(0xFE.toByte(), *writeBuffer.poll().map { it.toByte() }.toTypedArray())
					output.write(message.toByteArray())
					val crc = message.map { it.toInt() }.takeIf { it.isNotEmpty() }?.reduce { acc, i -> acc + i } ?: 0
					LOGGER.info("Message: ${message} (CRC: ${"0x%02X".format(crc and 0xFF)})")
					output.flush()
					lastAliveTimestamp = System.currentTimeMillis()
				} else if (System.currentTimeMillis() - lastAliveTimestamp > 1000) {
//					writer.flush()
					lastAliveTimestamp = System.currentTimeMillis()
				}
			} catch (e: IOException) {
				LOGGER.error(e.message, e)
				//restart()
				throw e
			}
		}
	}, "sendMessageThread")

	companion object {
		private val LOGGER: Logger = LoggerFactory.getLogger(BluetoothSPP::class.java)
		private val writeBuffer = ConcurrentLinkedQueue<String>()

		@JvmStatic
		fun main(args: Array<String>) {
			val spp = BluetoothSPP()
			while (true) {
				spp.start()
				val scanner = Scanner(System.`in`)
				var input: String? = null

				while (input?.toLowerCase() != "exit") {
					System.out.print(">> ")
					while (!scanner.hasNextLine()) Thread.sleep(100)
					input = scanner.nextLine()
					//System.out.println("ACK: \"${input}\"")
					writeBuffer.add(input)
				}
				spp.stop()
				LOGGER.info("BYE")
			}
		}
	}

	fun start() {
		if (!startBluetoothServer.isAlive) {
			bluetoothServerExecutor.execute(startBluetoothServer)
		}
	}

	fun restart() {
		closeConnection()
		if (!startBluetoothServer.isInterrupted) startBluetoothServer.interrupt()
		if (!sendMessageThread.isInterrupted) sendMessageThread.interrupt()
		if (!receiveMessageThread.isInterrupted) sendMessageThread.interrupt()
		start()
	}

	fun stop() {
		incomingMessageThreadExecutor.shutdownNow()
		outgoingMessageThreadExecutor.shutdownNow()
		bluetoothServerExecutor.shutdownNow()
	}

	/**
	 * start server creating an UUID and a connection String,
	 * then waiting for a device to connect
	 */
	private fun bluetoothServer() {
		if (sendMessageThread.isAlive) sendMessageThread.interrupt()
		if (receiveMessageThread.isAlive) receiveMessageThread.interrupt()

		while (streamConnNotifier == null) try {
			//Create a UUID for SPP
			val uuid = UUID("1101", true) // 1101
			//Create the servicve url
			val connectionString = "btspp://3481F41A4B29:6;authenticate=false;encrypt=false;master=false"

			//open server url
			streamConnNotifier = Connector.open(connectionString) as StreamConnection //StreamConnectionNotifier //StreamConnection

			//Wait for client connection
			println("\nServer Started. Waiting for clients to connect...")

			//val connection = streamConnNotifier!!.acceptAndOpen()

			//println("Remote device address: " + RemoteDevice.getRemoteDevice(connection).bluetoothAddress)
			//println("Remote device name: " + RemoteDevice.getRemoteDevice(connection).getFriendlyName(true))

			//the stream is opened both in and out
			outputStream = streamConnNotifier?.openOutputStream()
			inputStream = streamConnNotifier?.openInputStream()
			connectionIsAvaible = true

			if (!sendMessageThread.isInterrupted) sendMessageThread.interrupt()
			outgoingMessageThreadExecutor.execute(sendMessageThread)

			if (!receiveMessageThread.isInterrupted) receiveMessageThread.interrupt()
			incomingMessageThreadExecutor.execute(receiveMessageThread)
		} catch (e: IOException) {
			LOGGER.error(e.message, e)
			//e.printStackTrace()
			//in case of problems, the connection is stopped
			closeConnection()
		}

	}

	/**
	 * close all stream and interrupt all thread, then set false and modify the bluetooth
	 * label to alert the entire program that the bluetooth is no longer available
	 */
	fun closeConnection() {
		try {
			if (sendMessageThread.isAlive) sendMessageThread.interrupt()
			if (receiveMessageThread.isAlive) receiveMessageThread.interrupt()
			if (startBluetoothServer.isAlive) startBluetoothServer.interrupt()
			outputStream?.close()
			inputStream?.close()
			streamConnNotifier?.close()
			streamConnNotifier = null
			connectionIsAvaible = false
		} catch (e: IOException) {
			LOGGER.error(e.message, e)
		} finally {
			//set false and change bluetooth label to alert the entire program
			connectionIsAvaible = false
		}
	}
}