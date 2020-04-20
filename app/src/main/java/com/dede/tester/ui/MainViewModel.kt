package com.dede.tester.ui

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.preference.PreferenceManager

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

}