package com.dede.tester.ext

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.text.TextUtils
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import com.dede.tester.BuildConfig
import java.io.File
import java.lang.ref.WeakReference
import kotlin.math.roundToInt


object DownloadExt {

    private const val CHANNEL_ID = "download_channel"
    private var notificationId = 1

    private var appContext: WeakReference<Context>? = null

    fun initContext(context: Context) {
        if (appContext?.get() != null) return
        appContext = WeakReference(context.applicationContext)
    }

    fun showProgressNotify(contextTitle: String, percent: Float) {
        val context = appContext?.get() ?: return
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = notificationManager.getNotificationChannel(CHANNEL_ID)
            if (notificationChannel == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "文件下载",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    setSound(null, null)
                }
                notificationManager.createNotificationChannel(channel)
            }
        }

        val progress = (100 * percent).roundToInt()
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setProgress(100, progress, false)
            .setContentTitle(contextTitle)
            .setContentText("$progress %")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setSound(null)
            .setAutoCancel(true)
            .setOnlyAlertOnce(false)
            .build()
        notificationManager.notify(notificationId, notification)
    }

    fun showDownloadFinish(contextTitle: String, file: File) {
        val context = appContext?.get() ?: return
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (!file.isApk(context)) {
            notificationManager.cancel(notificationId)
        }
        val activity = PendingIntent.getActivity(context, 0, context.createInstallIntent(file), 0)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(contextTitle)
            .setContentText("点击安装")
            .setContentIntent(activity)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setSound(null)
            .setAutoCancel(true)
            .setOnlyAlertOnce(false)
            .build()
        notificationManager.notify(notificationId, notification)
        notificationId++// 自增id，以供下次下载
    }

}

fun Context.getDownloadFile(name: String): File {
    return File(getDownloadDir(), name)
}

fun Context.getDownloadDir(): File {
    val cacheDir = this.externalCacheDir ?: this.cacheDir
    return File(cacheDir, "download").apply {
        if (!exists()) this.mkdirs()
    }
}

fun File.isApk(context: Context): Boolean {
    try {
        val packageManager = context.packageManager
        val info = packageManager.getPackageArchiveInfo(this.absolutePath, 0)
        return !TextUtils.isEmpty(info?.packageName)
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return false
}

fun Context.createInstallIntent(file: File): Intent {
    val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".fileProvider", file)
    } else {
        Uri.fromFile(file)
    }
    val intent = Intent(Intent.ACTION_VIEW)
    intent.setDataAndType(uri, "application/vnd.android.package-archive")
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    Log.i("DownloadExt", "createInstallIntent: is ${Build.BRAND}")
    if (Build.BRAND.equals("vivo", true) || Build.BRAND.equals("oppo", true)) {
        intent.putExtra("installDir", true)
    }
    return intent
}

fun installApk(context: Context, file: File) {
    if (!file.isApk(context)) {
        return
    }
    val intent = context.createInstallIntent(file)
    context.startActivity(intent)
}
