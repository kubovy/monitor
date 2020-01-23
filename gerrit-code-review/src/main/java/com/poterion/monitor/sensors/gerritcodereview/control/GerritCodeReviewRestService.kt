package com.poterion.monitor.sensors.gerritcodereview.control

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * @author Jan Kubovy [jan@kubovy.eu]
 */
interface GerritCodeReviewRestService {
	/**
	 * Due to a wrongly serialized JSON we need to receive string and parse it ourselves.
	 */
	@GET("changes/")
	fun check(@Query("q") q: String): Call<String>
}