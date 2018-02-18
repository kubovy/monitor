package com.poterion.monitor.data.auth

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "@class")
@JsonSubTypes(
		JsonSubTypes.Type(name = "BasicAuthConfig", value = BasicAuthConfig::class))
interface AuthConfig
