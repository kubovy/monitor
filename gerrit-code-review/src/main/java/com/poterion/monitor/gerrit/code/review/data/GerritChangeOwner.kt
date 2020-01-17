package com.poterion.monitor.gerrit.code.review.data

import com.fasterxml.jackson.annotation.JsonProperty

data class GerritChangeOwner(@JsonProperty("_account_id") var accountId: Int? = null,
							 var name: String? = null,
							 var email: String? = null,
							 var username: String? = null)