package com.dede.tester.ui.mock

import android.content.Context
import com.baidu.mapapi.map.MapCustomStyleOptions
import com.baidu.mapapi.map.MapView
import java.io.File

object MapViewHelper {

    private const val CUSTOM_CONFIG_DIR = "customConfigdir"
    private const val CUSTOM_CONFIG_DARK = "f7ba57ce2cd4c3de59507d7780daf8d4.sty"
    private const val CUSTOM_CONFIG_DARK_ID = "23fbb37796f2225aa48bcb2cac895f59"
    private const val CUSTOM_CONFIG_LIGHT = "d41d8cd98f00b204e9800998ecf8427e.sty"
    private const val CUSTOM_CONFIG_LIGHT_ID = "718b957e50c563a91a3d39e18da7eefa"

    fun initMapResource(context: Context) {
        val assets = context.resources.assets
        val list = assets.list(CUSTOM_CONFIG_DIR)
        val customDir = File(context.filesDir, CUSTOM_CONFIG_DIR)
        customDir.mkdirs()
        list?.forEach {
            val stream = assets.open(CUSTOM_CONFIG_DIR + File.separator + it)
            val file = File(customDir, it)
            file.writeBytes(stream.readBytes())
            stream.close()
        }
    }

    fun setMapStyle(mapView: MapView, darkMode: Boolean) {
        val customDir = File(mapView.context.filesDir, CUSTOM_CONFIG_DIR)
        mapView.setMapCustomStyleEnable(true)// 自定义样式
        val customStyleOptions = MapCustomStyleOptions()
        if (darkMode) {
            val localPath = File(customDir, CUSTOM_CONFIG_DARK).absolutePath
            customStyleOptions.localCustomStylePath(localPath) //本地离线样式文件路径，如果在线方式加载失败，会默认加载本地样式文件。
            customStyleOptions.customStyleId(CUSTOM_CONFIG_DARK_ID) //在线样式文件对应的id。
        } else {
            val localPath = File(customDir, CUSTOM_CONFIG_LIGHT).absolutePath
            customStyleOptions.localCustomStylePath(localPath) //本地离线样式文件路径，如果在线方式加载失败，会默认加载本地样式文件。
            customStyleOptions.customStyleId(CUSTOM_CONFIG_LIGHT_ID) //在线样式文件对应的id。
        }
        mapView.setMapCustomStyle(customStyleOptions, null)
    }
}