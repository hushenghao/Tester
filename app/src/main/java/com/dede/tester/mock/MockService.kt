package com.dede.tester.mock

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager
import android.net.Uri
import android.os.*
import androidx.core.app.NotificationCompat
import com.baidu.mapapi.model.LatLng
import com.dede.tester.R
import com.dede.tester.ui.MainActivity

/**
 * Mock Location Service
 */
class MockService : Service() {

    companion object {
        const val EXTRA_MOCK_LOCATION = "mock_location"
        const val EXTRA_START_MOCK = "start_mock"
        const val CHANNEL_ID = "MOCK_SERVICE"
        const val NOTIFICATION_ID = 1
        const val ACTION_STOP = "com.dede.tester.mock.MockService"
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notificationChannel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notify_group_name),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationChannel.setSound(null, null)
            notificationChannel.setShowBadge(false)
            notificationManager.createNotificationChannel(notificationChannel)
        }
    }

    private fun createNotification(): Notification {
        val broadcast =
            PendingIntent.getBroadcast(this, 0, Intent(ACTION_STOP), 0)
        val activity = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java)
                .setData(Uri.parse("http://dede.tester/main/mock"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            0
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentText(getString(R.string.notify_content))
            .setContentTitle(getString(R.string.notify_title))
            .setSmallIcon(R.drawable.ic_baseline_start)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(activity)
            .addAction(-1, getString(R.string.action_stop), broadcast)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(false)
            .build()
    }

    private val mockControlCallback = Handler.Callback {
        when (it.what) {
            MockController.MSG_MOCK_STARTED -> {
            }
            MockController.MSG_MOCK_STOPED -> {
            }
            MockController.MSG_MOCK_EXITED -> {
            }
            MockController.MSG_PENDING_ERROR -> {
                val e = it.obj as Throwable
                e.printStackTrace()
            }
        }
        true
    }

    private lateinit var mockController: MockController
    private val callbackHandler = Handler(Looper.getMainLooper(), mockControlCallback)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        registerReceiver(stopReceiver, IntentFilter(ACTION_STOP))
        mockController = MockController(
            this, callbackHandler,
            LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val command = super.onStartCommand(intent, flags, startId)
        val point = intent?.getParcelableExtra<LatLng>(EXTRA_MOCK_LOCATION) ?: return command
        mockController.startMock(point)
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)
//        val notificationManager =
//            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//        notificationManager.notify(NOTIFICATION_ID, notification)
        return command
    }

    private val stopReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            stopForeground(true)
            mockController.stopMock()
        }
    }

    override fun onDestroy() {
        mockController.exitMock()
        unregisterReceiver(stopReceiver)
        stopForeground(true)
        super.onDestroy()
    }
}