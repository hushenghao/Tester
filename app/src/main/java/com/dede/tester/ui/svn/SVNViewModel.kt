package com.dede.tester.ui.svn

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dede.tester.BuildConfig
import kotlinx.coroutines.*
import org.tmatesoft.svn.core.*
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager
import org.tmatesoft.svn.core.io.SVNRepositoryFactory
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList

class SVNViewModel : ViewModel() {

    val list = MutableLiveData<List<SVNDirEntry>>()
    val error = MutableLiveData<Exception>()

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
                    withContext(Dispatchers.Main) {
                        error.value = e
                    }
                    emptyList<SVNDirEntry>()
                }
                list
            }

            val arrayList = ArrayList(collection.map { it as SVNDirEntry })
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

    val downloadStatus = MutableLiveData<Boolean>()

    fun download(
        context: Context,
        authenticationManager: ISVNAuthenticationManager,
        svnDirEntry: SVNDirEntry
    ) {
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
                    val output = FileOutputStream(file)
                    val l = repository.getFile("", -1, null, output)
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

    fun install(context: Context, file: File) {
        if (!isApk(context, file)) {
            return
        }
        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".fileProvider", file)
        } else {
            Uri.fromFile(file)
        }
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(uri, "application/vnd.android.package-archive")
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
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