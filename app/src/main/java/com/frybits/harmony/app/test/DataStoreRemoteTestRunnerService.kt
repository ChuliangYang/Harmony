package com.frybits.harmony.app.test

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.Message
import android.os.Messenger
import android.os.SystemClock
import android.util.Log
import androidx.collection.SparseArrayCompat
import androidx.collection.set
import androidx.core.os.bundleOf
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.frybits.harmony.app.ACK_EVENT
import com.frybits.harmony.app.ITERATIONS_KEY
import com.frybits.harmony.app.LOG_EVENT
import com.frybits.harmony.app.LOG_KEY
import com.frybits.harmony.app.REMOTE_MESSENGER_KEY
import com.frybits.harmony.app.RESULTS_EVENT
import com.frybits.harmony.app.RESULTS_KEY
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val LOG_TAG = "Remote"

@AndroidEntryPoint
class DataStoreRemoteTestRunnerService : Service() {

    @Inject
    lateinit var dataStore: DataStore<Preferences>

    override fun onBind(intent: Intent): IBinder? = null

    private val serviceScope = MainScope()
    private var currJob: Job? = null
    private val testKeyMap = hashMapOf<String, Int>()
    private val intToStringMap = hashMapOf<Int, String>()
    private val timeCaptureMap = SparseArrayCompat<Long>()

    private lateinit var remoteMessenger: Messenger
    private var iterations = 0

    private var isStarted = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (isStarted) {
            throw IllegalStateException("Service should not be started more than once!")
        }
        intent?.extras?.let { bundle ->
            var nextToRead = 0
            currJob = serviceScope.launch {
                dataStore.data.collect { prefs ->
                    val prefsMap = prefs.asMap()
                    val size = prefsMap.size
                    val now = SystemClock.elapsedRealtimeNanos()
                    if (size > nextToRead) {
                        for (readKey in nextToRead until size) {
                            val newKey = intToStringMap[readKey] ?: run {
                                remoteMessenger.send(Message.obtain().apply {
                                    what = LOG_EVENT
                                    data = bundleOf(
                                        LOG_KEY to LogEvent(
                                            priority = Log.ERROR,
                                            tag = LOG_TAG,
                                            message = "intToStringMap is missing key = $readKey"
                                        )
                                    )
                                })
                                return@collect
                            }
                            val value = prefs.get(stringPreferencesKey(newKey)) ?: run {
                                remoteMessenger.send(Message.obtain().apply {
                                    what = LOG_EVENT
                                    data = bundleOf(
                                        LOG_KEY to LogEvent(
                                            priority = Log.ERROR,
                                            tag = LOG_TAG,
                                            message = "Key=$newKey is not set"
                                        )
                                    )
                                })
                                return@collect
                            }
                            val activityTestTime = value.toLongOrNull() ?: run {
                                remoteMessenger.send(Message.obtain().apply {
                                    what = LOG_EVENT
                                    data = bundleOf(
                                        LOG_KEY to LogEvent(
                                            priority = Log.ERROR,
                                            tag = LOG_TAG,
                                            message = "Value is not set for key $newKey!"
                                        )
                                    )
                                })
                                return@collect
                            }
                            val diff = now - activityTestTime
                            val newKeyInt = testKeyMap[newKey] ?: run {
                                remoteMessenger.send(Message.obtain().apply {
                                    what = LOG_EVENT
                                    data = bundleOf(
                                        LOG_KEY to LogEvent(
                                            priority = Log.ERROR,
                                            tag = LOG_TAG,
                                            message = "testKeyMap is missing $newKey!"
                                        )
                                    )
                                })
                                return@collect
                            }
                            timeCaptureMap[newKeyInt] = diff
                        }
                        nextToRead = size
                    }
                }
            }

            iterations = bundle.getInt(ITERATIONS_KEY)
            require(iterations > 0) { "Must have at least 1 iteration" }
            repeat(iterations) {
                testKeyMap[it.toString()] = it
                intToStringMap[it] = it.toString()
            }
            remoteMessenger =
                requireNotNull(bundle.getParcelable(REMOTE_MESSENGER_KEY)) { "No messenger provided" }
        } ?: throw IllegalStateException("Service should not be restarted!")
        remoteMessenger.send(Message.obtain().apply { what = ACK_EVENT })
        isStarted = true
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        currJob?.cancel()
        remoteMessenger.send(Message.obtain().apply {
            what = RESULTS_EVENT
            data = bundleOf(RESULTS_KEY to LongArray(iterations) { timeCaptureMap[it] ?: -1L })
        })
        super.onDestroy()
    }
}