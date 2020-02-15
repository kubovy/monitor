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
package com.poterion.monitor.notifiers.deploymentcase.control

import com.fasterxml.jackson.core.Version
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.poterion.monitor.data.notifiers.NotifierConfig
import com.poterion.monitor.data.notifiers.NotifierDeserializer
import com.poterion.monitor.data.services.ServiceConfig
import com.poterion.monitor.data.services.ServiceDeserializer
import com.poterion.monitor.notifiers.deploymentcase.data.*
import com.poterion.monitor.notifiers.deploymentcase.toVariable
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class StateMachineTest {

	data class TestData(var variables: List<Variable>? = null,
						var devices: List<Device>? = null,
						var stateMachine: List<State>? = null)

	private val mapper
		get() = ObjectMapper(YAMLFactory()).apply {
			registerModule(SimpleModule("PolymorphicServiceDeserializerModule", Version.unknownVersion()).apply {
				addDeserializer(ServiceConfig::class.java, ServiceDeserializer)
			})
			registerModule(SimpleModule("PolymorphicNotifierDeserializerModule", Version.unknownVersion()).apply {
				addDeserializer(NotifierConfig::class.java, NotifierDeserializer)
			})
			configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
		}

	private var testData: TestData? = null

	@Before
	fun setUp() {
		testData = mapper.readValue(
				StateMachineTest::class.java.getResourceAsStream("/sample-state-machine.yaml"),
				TestData::class.java)
	}

	@Test
	fun toByteArray() {
		val stateMachineBin = testData!!.stateMachine!!
				.toData(testData?.devices ?: emptyList(), testData?.variables ?: emptyList())
				.toByteArray()
		val stateMachineDump = dumpBinary(stateMachineBin).split("\n")
		OutputStreamWriter(FileOutputStream("StateMachineTest-toByteArray.bin")).use {
			it.write(dumpBinary(stateMachineBin))
		}
		InputStreamReader(StateMachineTest::class.java.getResourceAsStream("/sample-state-machine.bin"))
				.useLines { lines ->
					lines.forEachIndexed { i, line ->
						if (i < stateMachineDump.size && line == stateMachineDump[i]) {
							println(line)
						} else {
							println(line)
							fail("${line} != ${stateMachineDump[i]}")
						}
						assertEquals("Dump does not match!", line, stateMachineDump[i])
					}
				}
	}

	@Test
	fun toStateMachine() {
		val stateMachine = testData!!.stateMachine!!
				.toData(testData?.devices ?: emptyList(), testData?.variables ?: emptyList())
				.toByteArray()
				.toIntList()
				.toStateMachine(testData!!.stateMachine!!, testData!!.devices!!, testData!!.variables!!)

		assertStateMachine(testData!!.stateMachine!!, stateMachine)

		val stateMachineBin = stateMachine
				.toData(testData?.devices ?: emptyList(), testData?.variables ?: emptyList())
				.toByteArray()
		val stateMachineDump = dumpBinary(stateMachineBin).split("\n")
		OutputStreamWriter(FileOutputStream("StateMachineTest-toStateMachine.bin")).use {
			it.write(dumpBinary(stateMachineBin))
		}
		InputStreamReader(StateMachineTest::class.java.getResourceAsStream("/sample-state-machine.bin"))
				.useLines { lines ->
					lines.forEachIndexed { i, line ->
						if (i < stateMachineDump.size && line == stateMachineDump[i]) {
							println(line)
						} else {
							println(line)
							fail("${line} != ${stateMachineDump[i]}")
						}
						assertEquals("Dump does not match!", line, stateMachineDump[i])
					}
				}
	}

	private fun dumpBinary(byteArray: ByteArray): String {
		var chars = ""
		val sb = StringBuilder()
		//sb.append("ADDR: 0x00 0x01 0x02 0x03 0x04 0x05 0x06 0x07  ASCII\n\n")

		byteArray.forEachIndexed { i, b ->
			if (i % 8 == 0) sb.append("%04x: ".format(i))
			chars += if (b in 32..126) b.toChar() else '.'
			sb.append("0x%02X%s".format(b, if (b > 255) "!" else " "))
			if (i % 8 == 7) {
				sb.append(" ${chars} |\n")
				chars = ""
			}
		}

		(byteArray.size until (256 * 1024 / 8)).forEach { i ->
			if (i % 8 == 0) sb.append("%04x: ".format(i))
			sb.append("0xFF ")
			if (i % 8 == 7) sb.append(" ........ |\n")
		}

		return sb.toString()
	}

	private fun assertStateMachine(stateMachineA: List<State>, stateMachineB: List<State>) {
		assertEquals(stateMachineA.size, stateMachineB.size)
		(0 until stateMachineA.size).forEach { assertState(it, stateMachineA[it], stateMachineB[it]) }
	}

	private fun assertState(stateId: Int, stateA: State, stateB: State) {
		assertEquals("Evaluation count does not match in state ${stateId}!",
				stateA.evaluations.size, stateB.evaluations.size)
		(0 until stateA.evaluations.size)
				.forEach { assertEvaluation(stateId, it, stateA.evaluations[it], stateB.evaluations[it]) }
	}

	private fun assertEvaluation(stateId: Int, evaluationId: Int, evaluationA: Evaluation, evaluationB: Evaluation) {
		assertEquals("Condition count does not match in state: ${stateId}, evaluation: :${evaluationId}!",
				evaluationA.conditions.size, evaluationB.conditions.size)
		val conditionsA = evaluationA.conditions.sortedBy { it.device }
		val conditionsB = evaluationB.conditions.sortedBy { it.device }
		(0 until conditionsA.size)
				.forEach { assertCondition(stateId, evaluationId, it, conditionsA[it], conditionsB[it]) }

		assertEquals("Action count does not match in state: ${stateId}, evaluation: ${evaluationId}!",
				evaluationA.actions.size, evaluationB.actions.size)
		(0 until evaluationA.actions.size)
				.forEach { assertAction(stateId, evaluationId, it, evaluationA.actions[it], evaluationB.actions[it]) }
	}

	private fun assertCondition(stateId: Int, evaluationId: Int, conditionId: Int, conditionA: Condition, conditionB: Condition) {
		assertDevice(stateId, evaluationId, "condition", conditionId,
				conditionA.device?.toDevice(testData?.devices ?: emptyList()),
				conditionB.device?.toDevice(testData?.devices ?: emptyList()))
		assertVariable(stateId, evaluationId, "condition", conditionId,
				conditionA.value?.toVariable(testData?.variables ?: emptyList()),
				conditionB.value?.toVariable(testData?.variables ?: emptyList()))
	}

	private fun assertAction(stateId: Int, evaluationId: Int, actionId: Int, actionA: Action, actionB: Action) {
		assertDevice(stateId, evaluationId, "action", actionId,
				actionA.device?.toDevice(testData?.devices ?: emptyList()),
				actionB.device?.toDevice(testData?.devices ?: emptyList()))
		assertVariable(stateId, evaluationId, "action", actionId,
				actionA.value?.toVariable(testData?.variables ?: emptyList()),
				actionB.value?.toVariable(testData?.variables ?: emptyList()))
	}

	private fun assertDevice(stateId: Int, evaluationId: Int, type: String, id: Int, deviceA: Device?, deviceB: Device?) {
		//assertEquals("Device name does not match in state ${stateId}, evaluation: ${evaluationId}, ${type}: ${id}!",
		//		deviceA?.name, deviceB?.name)
		assertEquals("Device kind does not match in state ${stateId}, evaluation: ${evaluationId}, ${type}: ${id}!",
				deviceA?.kind, deviceB?.kind)
		assertEquals("Device key does not match in state ${stateId}, evaluation: ${evaluationId}, ${type}: ${id}!",
				deviceA?.key, deviceB?.key)
		assertEquals("Device type does not match in state ${stateId}, evaluation: ${evaluationId}, ${type}: ${id}!",
				deviceA?.type, deviceB?.type)
	}

	private fun assertVariable(stateId: Int, evaluationId: Int, type: String, id: Int, variableA: Variable?, variableB: Variable?) {
		//assertEquals("Variable name does not match in state ${stateId}, evaluation: ${evaluationId}, ${type}: ${id}!",
		//		variableA?.name, variableB?.name)
		assertEquals("Variable type does not match in state ${stateId}, evaluation: ${evaluationId}, ${type}: ${id}!",
				variableA?.type, variableB?.type)
		if (variableA?.type != VariableType.STATE) {
			assertEquals("Variable value does not match in state ${stateId}, evaluation: ${evaluationId}, ${type}: ${id}!",
					variableA?.value, variableB?.value)
		}
	}
}