package com.dede.tester.ui.mock

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.baidu.location.BDAbstractLocationListener
import com.baidu.location.BDLocation
import com.baidu.location.LocationClient
import com.baidu.location.LocationClientOption

class MockViewModel : ViewModel() {

    val location: MutableLiveData<BDLocation> = MutableLiveData<BDLocation>()

    private var locationClient: LocationClient? = null

    fun isSuccess(location: BDLocation): Boolean {
        val locType = location.locType
        return (locType == BDLocation.TypeGpsLocation || locType == BDLocation.TypeNetWorkLocation)
    }

    private val locationListener = object : BDAbstractLocationListener() {
        override fun onReceiveLocation(location: BDLocation) {
            this@MockViewModel.location.postValue(location)
            stopLocationInternal()
        }
    }

    fun startLocation(context: Context, isMock: Boolean) {
        startLocationInternal(context, isMock)
    }

    private fun startLocationInternal(context: Context, useSensors: Boolean) {
        val locationClient = this.locationClient
        if (locationClient != null) {
            locationClient.registerLocationListener(locationListener)
            locationClient.restart()
            return
        }
        val option = LocationClientOption()
        option.isOpenGps = true // 打开gps
        option.setCoorType("bd09ll") // 设置坐标类型
        if (useSensors) {
            option.locationMode = LocationClientOption.LocationMode.Device_Sensors// 仅限设备
        } else {
            option.locationMode = LocationClientOption.LocationMode.Hight_Accuracy
        }
        option.setIsNeedLocationDescribe(true)
        option.scanSpan = 1000
        option.isNeedNewVersionRgc = true// 获取最新地址
        option.setEnableSimulateGps(true)// 允许GPS模拟结果
        val client = LocationClient(context.applicationContext, option)
        client.registerLocationListener(locationListener)
        client.start()
        this.locationClient = client
    }

    private fun stopLocationInternal() {
        val locationClient = this.locationClient
        if (locationClient != null) {
            locationClient.unRegisterLocationListener(locationListener)
            locationClient.stop()
        }
    }

    override fun onCleared() {
        stopLocationInternal()
        super.onCleared()
    }

}