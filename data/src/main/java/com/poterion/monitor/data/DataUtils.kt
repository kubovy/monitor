package com.poterion.monitor.data

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
fun StatusItem.key() = "${this.serviceName}-${this.title}"