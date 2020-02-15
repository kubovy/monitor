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
package com.poterion.monitor.sensors.jira.data

enum class JiraIssueFieldName {
	ISSUETYPE,
	TIMESPENT,
	PROJECT,
	FIXVERSIONS,
	AGGREGATETIMESPENT,
	RESOLUTION,
	RESOLUTIONDATE,
	WORKRATIO,
	LASTVIEWED,
	WATCHES,
	CREATED,
	PRIORITY,
	LABELS,
	TIMEESTIMATE,
	AGGREGATETIMEORIGINALESTIMATE,
	VERSIONS,
	ISSUELINKS,
	ASSIGNEE,
	UPDATED,
	STATUS,
	COMPONENTS,
	TIMEORIGINALESTIMATE,
	DESCRIPTION,
	AGGREGATETIMEESTIMATE,
	SUMMARY,
	CREATOR,
	SUBTASKS,
	REPORTER,
	AGGREGATEPROGRESS,
	ENVIRONMENT,
	DUEDATE,
	PROGRESS,
	VOTES;

	override fun toString(): String = name.toLowerCase()
}