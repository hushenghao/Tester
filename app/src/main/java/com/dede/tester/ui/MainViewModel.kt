package com.dede.tester.ui

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import com.dede.tester.ext.getDownloadDir
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Created by hsh on 2020/4/3 10:51 AM
 */
class MainViewModel : ViewModel() {

    val subTitle = MutableLiveData<String>()

    val sortType = MutableLiveData<Int>()

    fun updateSort(context: Context, sortType: Int) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
            .putInt("custom_key_sort_type", sortType).apply()
        this.sortType.value = sortType
    }

    fun initSort(context: Context) {
        val index =
            PreferenceManager.getDefaultSharedPreferences(context).getInt("custom_key_sort_type", 0)
        sortType.value = index
    }

    fun clearDownload(context: Context) {
        val downloadDir = context.getDownloadDir()

        suspend fun finish(context: Context) = withContext(Dispatchers.Main) {
            if (!isActive) {
                return@withContext
            }
            Toast.makeText(context, "删除完成", Toast.LENGTH_SHORT).show()
        }

        viewModelScope.launch(Dispatchers.IO) {
            val listFiles = downloadDir.listFiles()
            if (listFiles == null || listFiles.isEmpty()) {
                finish(context)
                return@launch
            }
            for (file in listFiles) {
                file.delete()
            }
            finish(context)
        }
    }

}