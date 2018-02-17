package com.poterion.monitor.data

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
enum class Status {
    NONE,
    UNKNOWN,
    OK,
    INFO,
    NOTIFICATION,
    CONNECTION_ERROR,
    SERVICE_ERROR,
    WARNING,
    ERROR,
    FATAL
}