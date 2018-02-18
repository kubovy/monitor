package com.poterion.monitor.api.ui

data class Item(val title: String? = null,
                val enabled: Boolean = true,
                val checked: Boolean? = null,
                val action: (() -> Unit)? = null,
                val sub: MutableList<Item>? = null)