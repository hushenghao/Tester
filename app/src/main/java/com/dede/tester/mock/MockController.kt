package com.dede.tester.mock

import android.content.Context
import android.location.Criteria
import android.location.Location
import android.location.LocationManager
import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import android.os.SystemClock
import android.util.Log
import com.baidu.mapapi.model.LatLng
import java.util.*

class MockController : HandlerThread, Handler.Callback {

    companion object {
        const val MSG_PENDING_ERROR = -1
        const val MSG_MOCK_STARTED = 1
        const val MSG_MOCK_STOPED = 4
        const val MSG_MOCK_EXITED = 2

        private const val INTERNAL_MSG_START_MOCK = 2
        private const val INTERNAL_MSG_STOP_MOCK = 3
        private const val INTERNAL_MSG_EXIT_MOCK = 4
    }

    private val context: Context
    private val locationManager: LocationManager
    private val allProvider: Array<String>
    private val callbackHandler: Handler

    private val mockHandler: Handler

    private var started = false
    private var initialized = false

    private val random = Random()

    constructor(
        context: Context,
        callbackHandler: Handler,
        vararg providers: String
    ) : super("Mock Thread") {
        this.context = context
        this.callbackHandler = callbackHandler
        this.allProvider = providers as Array<String>
        locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        start()
        mockHandler = Handler(this.looper, this)
    }

    override fun handleMessage(msg: Message): Boolean {
        when (msg.what) {
            INTERNAL_MSG_START_MOCK -> {// 模拟定位
                if (!initialized) {
                    for (provider in allProvider) {
                        addMockProvider(provider)
                    }
                    initialized = true
                }
                val point = msg.obj as LatLng
                for (provider in allProvider) {
                    setMockLocation(provider, point)
                }
                val obtain = Message.obtain(mockHandler, INTERNAL_MSG_START_MOCK, point)
                mockHandler.sendMessageDelayed(obtain, 1000)
                if (!started) {
                    Message.obtain(this.callbackHandler, MSG_MOCK_STARTED, point).sendToTarget()
                    started = true
                }
            }
            INTERNAL_MSG_STOP_MOCK -> {// 关闭模拟定位
                for (provider in allProvider) {
                    clearMockProvider(provider)
                }
                started = false
                Message.obtain(callbackHandler, MSG_MOCK_STOPED).sendToTarget()
            }
            INTERNAL_MSG_EXIT_MOCK -> {
                for (provider in allProvider) {
                    clearMockProvider(provider)
                }
                started = false
                initialized = false
                mockHandler.looper.quitSafely()
                Message.obtain(callbackHandler, MSG_MOCK_EXITED).sendToTarget()
            }
        }
        return true
    }

    fun startMock(point: LatLng) {
        Log.i("MockController", "${point.latitude}, ${point.longitude}")
        val gps84 = Convert.bd09_To_gps84(point.latitude, point.longitude)
        Log.i("MockController", "${gps84[0]}, ${gps84[1]}")
        val convert = LatLng(gps84[0], gps84[1])
        this.mockHandler.removeMessages(INTERNAL_MSG_START_MOCK)
        Message.obtain(this.mockHandler, INTERNAL_MSG_START_MOCK, convert).sendToTarget()
    }

    fun stopMock() {
        this.mockHandler.removeMessages(INTERNAL_MSG_START_MOCK)
        Message.obtain(this.mockHandler, INTERNAL_MSG_STOP_MOCK).sendToTarget()
    }

    fun exitMock() {
        this.mockHandler.removeMessages(INTERNAL_MSG_START_MOCK)
        Message.obtain(this.mockHandler, INTERNAL_MSG_EXIT_MOCK).sendToTarget()
    }

    private fun pendingError(e: Throwable) {
        Message.obtain(this.callbackHandler, MSG_PENDING_ERROR, e).sendToTarget()
    }

    private fun addMockProvider(provider: String) {
        clearMockProvider(provider)
        kotlin.runCatching {
            locationManager.addTestProvider(
                provider, false, false, false,
                true, true, true, true, Criteria.POWER_LOW, Criteria.ACCURACY_LOW
            )
            locationManager.setTestProviderEnabled(provider, true)
        }.onFailure {
            pendingError(it)
            it.printStackTrace()
        }
    }

    private fun setMockLocation(provider: String, point: LatLng) {
        val location = Location(provider)
        location.time = System.currentTimeMillis()
        location.elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
        location.speed = random.nextFloat()
        location.altitude = random.nextDouble()
        location.longitude = point.longitude + randomOffset()
        location.latitude = point.latitude + randomOffset()
        location.accuracy = random.nextFloat()
        kotlin.runCatching {
            locationManager.setTestProviderLocation(provider, location)
        }.onFailure {
            pendingError(it)
            it.printStackTrace()
        }
    }

    private fun randomOffset(): Double {
        return (random.nextInt(100) - 50) * 1e-6
    }

    private fun clearMockProvider(provider: String) {
        kotlin.runCatching {
            locationManager.setTestProviderEnabled(provider, false)
        }
        kotlin.runCatching {
            locationManager.removeTestProvider(provider)
        }
    }

}