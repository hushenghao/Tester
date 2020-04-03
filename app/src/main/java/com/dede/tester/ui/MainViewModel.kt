package com.dede.tester.ui

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

/**
 * Created by hsh on 2020/4/3 10:51 AM
 */
class MainViewModel : ViewModel() {

    val subTitle = MutableLiveData<String>()
}