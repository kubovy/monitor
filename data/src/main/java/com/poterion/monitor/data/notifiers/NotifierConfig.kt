package com.poterion.monitor.data.notifiers

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=JsonTypeInfo.As.PROPERTY, property="@class")
@JsonSubTypes(
        JsonSubTypes.Type(name = "TrayConfig", value = TrayConfig::class),
        JsonSubTypes.Type(name = "RaspiW2812Config", value = RaspiW2812Config::class))
interface NotifierConfig {
    var name: String
    var enabled: Boolean
}