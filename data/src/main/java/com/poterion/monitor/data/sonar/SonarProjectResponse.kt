package com.poterion.monitor.data.sonar

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
data class SonarProjectResponse(var id: Int = -1,
                                var key: String = "",
                                var name: String = "",
                                var scope: String = "",
                                var qualifier: String = "",
                                var date: String = "",
                                var creationDate: String = "",
                                var lname: String = "",
                                var version: String = "",
                                var branch: String = "",
                                var msr: Collection<SonarProjectMsrResponse> = emptyList())