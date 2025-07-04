package com.frybits.harmony.app.test

import com.frybits.harmony.app.database.TestDataEntity
import com.frybits.harmony.app.database.TestEntity
import com.frybits.harmony.app.database.TestEntityWithDataRelation
import com.frybits.harmony.app.database.TestSuiteDataRelation
import com.frybits.harmony.app.database.TestSuiteEntity
import java.util.UUID

/*
 *  Copyright 2021 Pablo Baxter
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 * Created by Pablo Baxter (Github: pablobaxter)
 * https://github.com/pablobaxter/Harmony
 */

data class TestSuite(val id: UUID, val timestamp: Long)

private fun TestSuite.toEntity(): TestSuiteEntity {
    return TestSuiteEntity(
        id = id,
        timestamp = timestamp
    )
}

private fun TestSuiteEntity.toModel(): TestSuite {
    return TestSuite(id, timestamp)
}

object TestType {
    const val WRITE = "write"
    const val READ = "read"
    const val IPC = "ipc"
}

object TestSource {
    const val SHARED_PREFS = "sharedPrefs"
    const val HARMONY = "harmony"
    const val MMKV = "mmkv"
    const val TRAY = "tray"
    const val DATASTORE = "datastore"
}

sealed class TestData(
    val parentTestEntityId: UUID,
    val testDataType: String,
    val results: LongArray
)

private fun TestData.toEntity(): TestDataEntity {
    return TestDataEntity(
        parentTestEntityId = parentTestEntityId,
        testDataType = testDataType,
        results = results
    )
}

private fun TestDataEntity.toModel(): TestData {
    return when (testDataType) {
        TestType.WRITE -> WriteTestData(parentTestEntityId, results)
        TestType.READ -> ReadTestData(parentTestEntityId, results)
        TestType.IPC -> IpcTestData(parentTestEntityId, results)
        else -> throw IllegalStateException("Unknown test type")
    }
}

class WriteTestData(parentTestEntityId: UUID, results: LongArray) : TestData(parentTestEntityId,
    TestType.WRITE, results)
class ReadTestData(parentTestEntityId: UUID, results: LongArray) : TestData(parentTestEntityId,
    TestType.READ, results)
class IpcTestData(parentTestEntityId: UUID, results: LongArray) : TestData(parentTestEntityId,
    TestType.IPC, results)

data class TestRun(
    val testEntityId: UUID,
    val parentTestSuiteId: UUID,
    val numIterations: Int,
    val isAsync: Boolean,
    val isEncrypted: Boolean,
    val source: String
)

private fun TestRun.toEntity(): TestEntity {
    return TestEntity(
        testEntityId = testEntityId,
        parentTestSuiteId = parentTestSuiteId,
        numIterations = numIterations,
        isAsync = isAsync,
        isEncrypted = isEncrypted,
        source = source
    )
}

private fun TestEntity.toModel(): TestRun {
    return TestRun(
        testEntityId = testEntityId,
        parentTestSuiteId = parentTestSuiteId,
        numIterations = numIterations,
        isAsync = isAsync,
        isEncrypted = isEncrypted,
        source = source
    )
}

data class TestSuiteData(
    val testSuite: TestSuite,
    val testEntityWithDataList: List<TestEntityWithData>
)

fun TestSuiteData.toRelation(): TestSuiteDataRelation {
    return TestSuiteDataRelation(
        testSuite = testSuite.toEntity(),
        testEntityWithDataList = testEntityWithDataList.map { it.toRelation() }
    )
}

fun TestSuiteDataRelation.toModel(): TestSuiteData {
    return TestSuiteData(
        testSuite = testSuite.toModel(),
        testEntityWithDataList = testEntityWithDataList.map { it.toModel() }
    )
}

data class TestEntityWithData(
    val entity: TestRun,
    val testDataList: List<TestData>
)

private fun TestEntityWithData.toRelation(): TestEntityWithDataRelation {
    return TestEntityWithDataRelation(
        entity = entity.toEntity(),
        testDataList = testDataList.map { it.toEntity() }
    )
}

private fun TestEntityWithDataRelation.toModel(): TestEntityWithData {
    return TestEntityWithData(
        entity = entity.toModel(),
        testDataList = testDataList.map { it.toModel() }
    )
}
