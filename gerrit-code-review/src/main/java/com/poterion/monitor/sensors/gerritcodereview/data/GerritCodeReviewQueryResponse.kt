/******************************************************************************
 * Copyright (C) 2020 Jan Kubovy (jan@kubovy.eu)                              *
 *                                                                            *
 * This program is free software: you can redistribute it and/or modify       *
 * it under the terms of the GNU General Public License as published by       *
 * the Free Software Foundation, either version 3 of the License, or          *
 * (at your option) any later version.                                        *
 *                                                                            *
 * This program is distributed in the hope that it will be useful,            *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of             *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the              *
 * GNU General Public License for more details.                               *
 *                                                                            *
 * You should have received a copy of the GNU General Public License          *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.      *
 ******************************************************************************/
package com.poterion.monitor.sensors.gerritcodereview.data

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
data class GerritCodeReviewQueryResponse(var id: String? = null,
										 var project: String? = null,
										 var branch: String? = null,
										 var topic: String? = null,
										 var hashtags: Set<String>? = null,
										 @JsonProperty("change_id") var changeId: String? = null,
										 var subject: String? = null,
										 var status: String? = null,
										 var created: String? = null,
										 var updated: String? = null,
										 @JsonProperty("submit_type") var submitType: String? = null,
										 var mergeable: Boolean? = null,
										 var submittable: Boolean? = null,
										 var insertions: Int? = null,
										 var deletions: Int? = null,
										 var _number: Int? = null,
										 var owner: GerritChangeOwner? = null,
										 var labels: Map<String, Map<String, GerritChangeOwner>> = emptyMap())