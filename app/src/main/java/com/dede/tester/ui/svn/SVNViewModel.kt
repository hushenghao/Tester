package com.dede.tester.ui.svn

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dede.tester.ext.DownloadExt
import com.dede.tester.ext.getDownloadFile
import com.dede.tester.ext.installApk
import kotlinx.coroutines.*
import org.tmatesoft.svn.core.*
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager
import org.tmatesoft.svn.core.io.SVNRepositoryFactory
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList

class SVNViewModel : ViewModel() {

    val list = MutableLiveData<List<SVNDirEntry>>()
    val error = MutableLiveData<Exception>()
    val downloadStatus = MutableLiveData<Boolean>()

    var sortType: Int = 0

    private var parentSVNDirEntry: SVNDirEntry? = null

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

            // 没有到根目录
            val hasParent = repositoryRoot != null && svnUrl != repositoryRoot &&
                    !repositoryRoot.toString().startsWith(svnUrl.toString())
            if (hasParent) {
                // 手动构建第一个返回上层的空位
                val parentSVNDirEntry =
                    createParentSvnDirEntry(svnUrl.removePathTail(), repositoryRoot!!)
                arrayList.add(0, parentSVNDirEntry)
                this@SVNViewModel.parentSVNDirEntry = parentSVNDirEntry
            }
            if (!isActive) {
                return@launch
            }
            this@SVNViewModel.list.value = arrayList
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

    fun download(
        context: Context,
        authenticationManager: ISVNAuthenticationManager,
        svnDirEntry: SVNDirEntry
    ) {
        DownloadExt.initContext(context)
        viewModelScope.launch {
            downloadStatus.value = true
            val name = svnDirEntry.name
            Toast.makeText(context, "开始下载 $name", Toast.LENGTH_SHORT).show()
            val file = withContext(Dispatchers.IO) {
                val repository = SVNRepositoryFactory.create(svnDirEntry.url)
                repository.authenticationManager = authenticationManager

                val file = context.getDownloadFile(name)
                if (file.exists()) {
                    file.delete()
                }
                Log.i("HomeViewModel", "download: " + file.absolutePath)
                try {
                    val output = ProgressFileOutputStream(svnDirEntry.size, file)
                    async(Dispatchers.Main) {
                        while (!output.isClosed) {
                            DownloadExt.showProgressNotify(svnDirEntry.name, output.percent)
                            delay(1000L)
                        }
                        DownloadExt.showDownloadFinish(svnDirEntry.name, file)
                    }
                    val l = repository.getFile("", -1, null, output)
                    output.close()
                    Log.i("HomeViewModel", "download: $l")
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
                Toast.makeText(context, "下载完成 $name", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "下载失败 $name", Toast.LENGTH_SHORT).show()
            }

            installApk(context, file)
            downloadStatus.value = false
        }
    }

    fun isParentSvnDirEntry(svnDirEntry: SVNDirEntry): Boolean {
        return svnDirEntry === parentSVNDirEntry
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