package com.dede.tester.ui.svn

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dede.tester.BuildConfig
import kotlinx.coroutines.*
import org.tmatesoft.svn.core.*
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager
import org.tmatesoft.svn.core.io.SVNRepositoryFactory
import java.io.File
import java.io.IOException
import java.lang.ref.WeakReference
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.roundToInt

class SVNViewModel : ViewModel() {

    val list = MutableLiveData<List<SVNDirEntry>>()
    val error = MutableLiveData<Exception>()

    var sortType: Int = 0

    fun loadDirEntry(authenticationManager: ISVNAuthenticationManager, svnUrl: SVNURL) {
        viewModelScope.launch {
            var repositoryRoot: SVNURL? = null
            val collection = withContext(Dispatchers.IO) {
                val repository = SVNRepositoryFactory.create(svnUrl)
                repository.authenticationManager = authenticationManager
                val list = try {
                    repositoryRoot = repository.getRepositoryRoot(true)
                    repository.getDir("", -1, null as SVNProperties?, null as Collection<*>?)
                } catch (e: SVNException) {
                    e.printStackTrace()
                    error.postValue(e)
                    emptyList<SVNDirEntry>()
                }
                list
            }

            val arrayList = ArrayList(collection.map { it as SVNDirEntry })
            sort(arrayList)// 排序
            if (repositoryRoot != null &&
                svnUrl != repositoryRoot &&
                !repositoryRoot.toString().startsWith(svnUrl.toString())
            ) {// 没有到根目录
                // 手动构建第一个返回上层的空位
                val parentSvnDirEntry =
                    createParentSvnDirEntry(svnUrl.removePathTail(), repositoryRoot!!)
                arrayList.add(0, parentSvnDirEntry)
            }
            if (!isActive) {
                return@launch
            }
            list.value = arrayList
        }
    }

    private fun sort(arrayList: ArrayList<SVNDirEntry>) {
        when (sortType) {
            0 -> {
                arrayList.sortBy { it.name }
            }
            1 -> {
                arrayList.sortByDescending { it.name }
            }
            2 -> {
                arrayList.sortBy { it.date }
            }
            3 -> {
                arrayList.sortByDescending { it.date }
            }
        }
    }

    val downloadStatus = MutableLiveData<Boolean>()

    private var appContext: WeakReference<Context>? = null
    private val channelId = "download_channel"
    private var notificationId = 1

    private fun showProgress(svnDirEntry: SVNDirEntry, percent: Float) {
        val context = appContext?.get() ?: return
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = notificationManager.getNotificationChannel(channelId)
            if (notificationChannel == null) {
                val channel = NotificationChannel(
                    channelId,
                    "文件下载",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    setSound(null, null)
                }
                notificationManager.createNotificationChannel(channel)
            }
        }

        val progress = (100 * percent).roundToInt()
        val notification = NotificationCompat.Builder(context, channelId)
            .setProgress(100, progress, false)
            .setContentTitle(svnDirEntry.name)
            .setContentText("${progress} %")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setSound(null)
            .setAutoCancel(true)
            .setOnlyAlertOnce(false)
            .build()
        notificationManager.notify(notificationId, notification)
    }

    private fun showFinish(svnDirEntry: SVNDirEntry, file: File) {
        val context = appContext?.get() ?: return
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (!isApk(context, file)) {
            notificationManager.cancel(notificationId)
        }
        val activity = PendingIntent.getActivity(context, 0, getInstallIntent(context, file), 0)
        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle(svnDirEntry.name)
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

    fun download(
        context: Context,
        authenticationManager: ISVNAuthenticationManager,
        svnDirEntry: SVNDirEntry
    ) {
        appContext = WeakReference(context.applicationContext)
        viewModelScope.launch {
            downloadStatus.value = true
            val name = svnDirEntry.name
            Toast.makeText(context, "开始下载 ${name}", Toast.LENGTH_SHORT).show()
            val file = withContext(Dispatchers.IO) {
                val repository = SVNRepositoryFactory.create(svnDirEntry.url)
                repository.authenticationManager = authenticationManager

                val file = getDownloadFile(context, svnDirEntry)
                if (file.exists()) {
                    file.delete()
                }
                Log.i("HomeViewModel", "download: " + file.absolutePath)
                try {
                    val output = ProgressFileOutputStream(svnDirEntry.size, file)
                    async(Dispatchers.Main) {
                        while (!output.isClosed) {
                            showProgress(svnDirEntry, output.percent)
                            delay(1000L)
                        }
                        showFinish(svnDirEntry, file)
                    }
                    val l = repository.getFile("", -1, null, output)
                    output.close()
                    Log.i("HomeViewModel", "download: " + l)
                } catch (e: IOException) {
                    e.printStackTrace()
                } catch (e: SVNException) {
                    e.printStackTrace()
                }
                file
            }

            if (!isActive) {
                return@launch
            }

            if (file.exists() && file.isFile) {
                Toast.makeText(context, "下载完成 ${name}", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "下载失败 ${name}", Toast.LENGTH_SHORT).show()
            }

            install(context, file)
            downloadStatus.value = false
        }
    }

    fun getDownloadFile(context: Context, svnDirEntry: SVNDirEntry): File {
        return File(context.externalCacheDir, svnDirEntry.name)
    }

    private fun isApk(context: Context, file: File): Boolean {
        try {
            val packageManager = context.packageManager
            val info = packageManager.getPackageArchiveInfo(file.absolutePath, 0)
            return !TextUtils.isEmpty(info?.packageName)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    private fun getInstallIntent(context: Context, file: File): Intent {
        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".fileProvider", file)
        } else {
            Uri.fromFile(file)
        }
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(uri, "application/vnd.android.package-archive")
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        return intent
    }

    fun install(context: Context, file: File) {
        if (!isApk(context, file)) {
            return
        }
        val intent = getInstallIntent(context, file)
        context.startActivity(intent)
    }

    override fun onCleared() {
        viewModelScope.coroutineContext.cancel()
    }

    fun isParentSvnDirEntry(svnDirEntry: SVNDirEntry): Boolean {
        return svnDirEntry.name == "返回上一层"
    }

    private fun createParentSvnDirEntry(svnUrl: SVNURL, repositoryRoot: SVNURL): SVNDirEntry {
        return SVNDirEntry(
            svnUrl,
            repositoryRoot,
            "返回上一层",
            SVNNodeKind.DIR,
            0,
            false,
            0,
            Date(),
            null
        )
    }
}