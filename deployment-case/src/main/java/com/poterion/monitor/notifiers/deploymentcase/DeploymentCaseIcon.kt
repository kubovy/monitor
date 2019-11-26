package com.poterion.monitor.notifiers.deploymentcase

import com.poterion.monitor.api.ui.Icon
import javafx.scene.image.Image
import java.io.ByteArrayInputStream
import java.io.InputStream

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
enum class DeploymentCaseIcon(private val file: String) : Icon {
	NUCLEAR_FOOTBALL("/icons/nuclear-football.png"),
	BLUETOOTH("/icons/bluetooth.png"),
	CONNECTED("/icons/connected.png"),
	DISCONNECTED("/icons/disconnected.png"),
	VERIFIED("/icons/verified.png"),
	MISMATCH("/icons/mismatch.png"),
	UNVERIFIED("/icons/unverified.png"),

	STATE("/icons/state.png"),
	EVALUATION("/icons/evaluation.png"),
	CONDITIONS("/icons/conditions.png"),
	CONDITION("/icons/condition.png"),
	ACTIONS("/icons/actions.png"),
	ACTION("/icons/action.png");


	override val inputStream: InputStream
		get() = DeploymentCaseIcon::class.java.getResourceAsStream(file)
				.use { it.readBytes() }
				.let { ByteArrayInputStream(it) }
}