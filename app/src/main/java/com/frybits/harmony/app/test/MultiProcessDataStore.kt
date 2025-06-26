package com.frybits.harmony.app.test

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.MultiProcessDataStoreFactory
import androidx.datastore.core.Serializer
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import com.frybits.harmony.app.datastore.TestKvData
import java.io.File
import java.io.InputStream
import java.io.OutputStream

object TestKvDataSerializer : Serializer<TestKvData> {
    override val defaultValue: TestKvData = TestKvData.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): TestKvData =
        TestKvData.parseFrom(input)

    override suspend fun writeTo(t: TestKvData, output: OutputStream) =
        t.writeTo(output)
}

object MultiProcessDeviceStore {
    private const val FILE_NAME = "device_info.ds"

    @Volatile
    private var INSTANCE: DataStore<TestKvData>? = null

    fun get(context: Context): DataStore<TestKvData> {
        val appCtx = context.applicationContext
        return INSTANCE ?: synchronized(this) {
            INSTANCE ?: buildStore(appCtx).also { INSTANCE = it }
        }
    }

    private fun buildStore(appCtx: Context): DataStore<TestKvData> =
        MultiProcessDataStoreFactory.create(
            serializer = TestKvDataSerializer,
            corruptionHandler = ReplaceFileCorruptionHandler { TestKvData.getDefaultInstance() },
            produceFile = { File(appCtx.filesDir, FILE_NAME) }
        )
}
