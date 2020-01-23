package com.poterion.monitor.notifiers.devopslight.data

/**
 * WS281x light patterns.
 *
 * @author Jan Kubovy [jan@kubovy.eu]
 */
enum class LightPattern(val code: Int,
						val delay: Int?,
						val width: Int?,
						val fading: Int?,
						val min: Int?,
						val max: Int?,
						val timeout: Int?) {
	OFF(0x00, null, null, null, null, null, null),
	FULL(0x01, 1000, null, 0, null, 255, 50),
	BLINK(0x02, 500, null, 0, 0, 255, 3),
	FADE_IN(0x03, 200, null, 0, 0, 255, 3),
	FADE_OUT(0x04, 200, null, 0, 0, 255, 3),
	FADE_INOUT(0x05, 100, null, 0, 0, 255, 3),
	FADE_TOGGLE(0x06, 100, null, 0, 0, 255, 3),
	ROTATION(0x07, 500, 10, 24, 0, 255, 3),
	WIPE(0x08, 500, null, 0, 0, 255, 1),
	LIGHTHOUSE(0x09, 750, 5, 32, 0, 255, 3),
	CHAISE(0x0A, 300, 10, 24, 0, 255, 3),
	THEATER(0x0B, 1000, 3, 0, 128, 255, 3);

	val title: String
		get() = name.replace("WS281x_LIGHT_", "").replace("_", " ").toLowerCase()
				.let { it[0].toUpperCase() + it.substring(1) }
}