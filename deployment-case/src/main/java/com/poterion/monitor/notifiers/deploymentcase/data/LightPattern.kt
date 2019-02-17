package com.poterion.monitor.notifiers.deploymentcase.data

enum class LightPattern(val description: String) {
	LIGHT("Light"),
	BLINK("Blink"),
	FADE_IN("Fade in"),
	FADE_OUT("Fade out"),
	FADE_TOGGLE("Fade toggle")
}