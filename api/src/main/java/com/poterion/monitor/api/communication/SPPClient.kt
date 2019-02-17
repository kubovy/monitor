package com.poterion.monitor.api.communication

import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.util.ArrayList
import java.util.Vector

import javax.bluetooth.*
import javax.microedition.io.Connector
import javax.microedition.io.StreamConnection
import javax.bluetooth.RemoteDevice



/**
 * A simple SPP client that connects with an SPP server
 */
class SPPClient : DiscoveryListener {

	companion object {
//		private val lock = Any() //object used for waiting
		private var vecDevices: Vector<RemoteDevice> = Vector() //vector containing the devices discovered
		private var connectionURL: String? = null

		@Throws(IOException::class)
		@JvmStatic
		fun main(args: Array<String>) {
			val client = SPPClient()
//			try {
//				synchronized(lock) {
//					lock.wait()
//				}
//			} catch (e: InterruptedException) {
//				e.printStackTrace()
//			}

			client.startDiscovery()
			Thread.sleep(10_000L)

			//print all devices in vecDevices
			val deviceCount = vecDevices.size

			if (deviceCount <= 0) {
				println("No Devices Found .")
				System.exit(0)
			} else {
				//print bluetooth device addresses and names in the format [ No. address (name) ]
				println("Bluetooth Devices: ")
				for (i in 0 until deviceCount) {
					val remoteDevice = vecDevices.elementAt(i) as RemoteDevice
					println((i + 1).toString() + ". " + remoteDevice.bluetoothAddress + " (" + remoteDevice.getFriendlyName(true) + ")")
				}
			}

			print("Choose Device index: ")
			val bReader = BufferedReader(InputStreamReader(System.`in`))
			val chosenIndex = bReader.readLine()
			val index = Integer.parseInt(chosenIndex.trim { it <= ' ' })

			// check for spp service
			val remoteDevice = vecDevices.elementAt(index - 1) as RemoteDevice

			val uuidSet = arrayOfNulls<UUID>(1)
			uuidSet[0] = UUID("1101", true)
			println("\nSearching for service...")
			client.agent?.searchServices(null, uuidSet, remoteDevice, client)

//			try {
//				synchronized(lock) {
//					lock.wait()
//				}
//			} catch (e: InterruptedException) {
//				e.printStackTrace()
//			}
			Thread.sleep(30_000L)
			println("\nSearching for service completed.")

			if (connectionURL == null) {
				println("Device does not support SPP Service.")
				System.exit(0)
			}
			//connect to the server and send a line of text
			val streamConnection = Connector.open(connectionURL) as StreamConnection
			// send string
			val outStream = streamConnection.openOutputStream()
			val pWriter = PrintWriter(OutputStreamWriter(outStream))
			pWriter.write("Test String from SPP Client\r\n")
			pWriter.flush()
			// read response
			val inStream = streamConnection.openInputStream()
			val bReader2 = BufferedReader(InputStreamReader(inStream))
			val lineRead = bReader2.readLine()
			println(lineRead)
		}//main
	}

	private var onDeviceDiscovery: ActionListener? = null
	private var onConnectionSuccessful: ActionListener? = null
	private var onConnectionFailed: ActionListener? = null

	private var agent: DiscoveryAgent? = null

	internal var partnerName: String? = null

	internal var reader: BufferedReader? = null
	internal var writer: PrintWriter? = null

	val deviceInfos: ArrayList<RemoteDeviceInfo>
		get() {
			val res = ArrayList<RemoteDeviceInfo>()
			for (rd in vecDevices) {
				try {
					val rdi = RemoteDeviceInfo(rd.getFriendlyName(true), rd.bluetoothAddress)
					res.add(rdi)
				} catch (e: IOException) {
					e.printStackTrace()
				}

			}
			return res
		}

	internal var isOK = false

	init {
		try {
			//display local device address and name
			val localDevice = LocalDevice.getLocalDevice()
			println("Address: " + localDevice.bluetoothAddress)
			println("Name: " + localDevice.friendlyName)

			agent = localDevice.discoveryAgent

		} catch (e: BluetoothStateException) {
			e.printStackTrace()
		}

	}

	fun setOnDeviceDiscovery(onDeviceDiscovery: ActionListener) {
		this.onDeviceDiscovery = onDeviceDiscovery
	}

	fun setOnConnectionFailed(onConnectionFailed: ActionListener) {
		this.onConnectionFailed = onConnectionFailed
	}

	fun setOnConnectionSuccessful(onConnectionSuccessful: ActionListener) {
		this.onConnectionSuccessful = onConnectionSuccessful
	}

	fun startDiscovery() {
		vecDevices = Vector()
		println("Starting device inquiry...")
		try {
			agent?.startInquiry(DiscoveryAgent.LIAC, this)
		} catch (e: BluetoothStateException) {
			e.printStackTrace()
		}

	}

	fun connect(index: Int) {
		try {
			// check for spp service
			val remoteDevice = vecDevices.elementAt(index)
			partnerName = remoteDevice.getFriendlyName(true)
			val uuidSet = arrayOfNulls<UUID>(1)
			uuidSet[0] = UUID("1101", true)
			println("\nSearching for service...")
			agent?.searchServices(null, uuidSet, remoteDevice, this)
		} catch (e: BluetoothStateException) {
			e.printStackTrace()
		} catch (e: IOException) {
			e.printStackTrace()
		}

	}

	// methods of DiscoveryListener
	override fun deviceDiscovered(btDevice: RemoteDevice, cod: DeviceClass) {
		// add the device to the vector
		println("[NEW] ${btDevice} (${cod})")
		if (!vecDevices.contains(btDevice)) {
			vecDevices.addElement(btDevice)
		}
	}

	//implement this method since services are not being discovered
	override fun servicesDiscovered(transID: Int, servRecord: Array<ServiceRecord>?) {
		println("[SRV] ${transID} (${servRecord?.joinToString(", ")})")
		if (servRecord != null && servRecord.isNotEmpty()) {
			connectionURL = servRecord[0].getConnectionURL(0, false)
		}
		isOK = true
		try {
			val streamConnection = Connector.open(connectionURL!!) as StreamConnection
			// send string
			val outStream = streamConnection.openOutputStream()
			writer = PrintWriter(OutputStreamWriter(outStream))
			// read response
			val inStream = streamConnection.openInputStream()
			reader = BufferedReader(InputStreamReader(inStream))
			if (onConnectionSuccessful != null) onConnectionSuccessful!!.actionPerformed(ActionEvent(this, ActionEvent.RESERVED_ID_MAX + 1, ""))
		} catch (e: IOException) {
			e.printStackTrace()
		}

	}

	//implement this method since services are not being discovered
	override fun serviceSearchCompleted(transID: Int, respCode: Int) {
		if (!isOK) {
			if (onConnectionFailed != null) onConnectionFailed!!.actionPerformed(ActionEvent(this, ActionEvent.RESERVED_ID_MAX + 1, ""))
		}
	}

	override fun inquiryCompleted(discType: Int) {
		if (onDeviceDiscovery != null) onDeviceDiscovery!!.actionPerformed(ActionEvent(this, ActionEvent.RESERVED_ID_MAX + 1, ""))
		println("Device Inquiry Completed. ")
	}
}