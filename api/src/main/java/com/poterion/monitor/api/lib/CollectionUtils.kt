package com.poterion.monitor.api.lib

inline fun <T> Iterable<T>.intermediate(action: (T) -> Unit): Iterable<T> = apply {
	for (element in this) action(element)
}

inline fun <T> Iterable<T>.intermediateIndexed(action: (index: Int, T) -> Unit): Iterable<T> = apply {
	var index = 0
	for (element in this) action(index++, element)
}

inline fun <T> Iterable<T>.append(action: () -> Iterable<T>): Iterable<T> = this + action()

inline fun <K, V> Map<out K, V>.intermediate(action: (Map.Entry<K, V>) -> Unit): Map<out K, V> = apply {
	for (element in this) action(element)
}

