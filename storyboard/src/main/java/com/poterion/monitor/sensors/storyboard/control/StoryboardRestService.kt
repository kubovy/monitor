package com.poterion.monitor.sensors.storyboard.control

import com.poterion.monitor.sensors.storyboard.data.Project
import com.poterion.monitor.sensors.storyboard.data.Story
import com.poterion.monitor.sensors.storyboard.data.Task
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
interface StoryboardRestService {
	companion object {
		val STORIES_DEFAULT_STATES = listOf("todo", "active", "inprogress", "invalid", "review") // active, merged, invalid
		val TASKS_DEFAULT_STATES = listOf("todo", "inprogress", "invalid", "review", "merged") //
	}

	@GET("/api/v1/projects")
	fun projects(@Query("name") name: String): Call<Collection<Project>>


	@GET("/api/v1/stories")
	fun stories(@Query("project_id") projectId: Int,
				@Query("status") statuses: Collection<String> = STORIES_DEFAULT_STATES): Call<Collection<Story>>

	@GET("/api/v1/stories/{storyId}/tasks")
	fun tasks(@Path("storyId") storyId: Int,
			  @Query("status") statuses: Collection<String> = TASKS_DEFAULT_STATES): Call<Collection<Task>>
}