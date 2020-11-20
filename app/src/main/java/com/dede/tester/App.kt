package com.dede.tester

import android.app.Application
import com.baidu.mapapi.CoordType
import com.baidu.mapapi.SDKInitializer
import com.dede.tester.ui.mock.MapViewHelper

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        SDKInitializer.initialize(this);
        SDKInitializer.setCoordType(CoordType.BD09LL);// 使用火星坐标系，Android GPS默认坐标系
        MapViewHelper.initMapResource(this)
    }
}