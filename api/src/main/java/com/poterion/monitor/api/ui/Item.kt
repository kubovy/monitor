package com.poterion.monitor.api.ui

import com.poterion.monitor.data.Config

data class Item(val title: String? = null,
				val enabled: Boolean = true,
				val checked: Boolean? = null,
				val update: ((Config) -> Any?)? = null,
				val action: (() -> Unit)? = null,
				val sub: MutableList<Item>? = null)