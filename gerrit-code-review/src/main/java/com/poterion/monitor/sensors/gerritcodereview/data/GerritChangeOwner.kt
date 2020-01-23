package com.poterion.monitor.sensors.gerritcodereview.data

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
data class GerritChangeOwner(@JsonProperty("_account_id") var accountId: Int? = null,
							 var name: String? = null,
							 var email: String? = null,
							 var username: String? = null)