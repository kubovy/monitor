package com.poterion.monitor.data.notifiers

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
interface NotifierConfig {
    var type: String
    var name: String
    var enabled: Boolean
}