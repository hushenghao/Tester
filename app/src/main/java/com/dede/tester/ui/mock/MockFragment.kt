package com.dede.tester.ui.mock

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Point
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.baidu.mapapi.map.*
import com.baidu.mapapi.model.LatLng
import com.dede.tester.R
import com.dede.tester.ext.Util
import com.dede.tester.ext.dip
import com.dede.tester.ext.toast
import com.dede.tester.mock.MockService
import com.dede.tester.ui.favorite.FavoriteHelper
import com.google.android.material.floatingactionbutton.FloatingActionButton


class MockFragment : Fragment() {

    companion object {
        const val EXTRA_LOCATION = "location"
        const val EXTRA_NAME = "name"
    }

    private val mockViewModel: MockViewModel by viewModels()
    private lateinit var mapView: MapView
    private lateinit var tvLocation: TextView
    private lateinit var fabMock: FloatingActionButton
    private lateinit var baiduMap: BaiduMap

    private var mockMode = false
    private var mockPoint: LatLng? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_mock, container, false)
        mapView = root.findViewById(R.id.bmapView)
        fabMock = root.findViewById(R.id.fab_mock)
        tvLocation = root.findViewById(R.id.tv_location)
        initView()
        requireContext().registerReceiver(stopReceiver, IntentFilter(MockService.ACTION_STOP))
        return root
    }

    private val stopReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            mockMode = false
            setMockViewStatus(false)
        }
    }

    private fun initView() {
        baiduMap = mapView.map
        baiduMap.mapType = BaiduMap.MAP_TYPE_NORMAL
        baiduMap.setMapStatus(MapStatusUpdateFactory.zoomTo(16f))
        mapView.post {
            mapView.zoomControlsPosition =
                Point(mapView.width - dip(60f), mapView.height - dip(130f))
        }

        this.mockMode = Util.isServiceRunning(requireContext(), MockService::class.java)
        setMockViewStatus(this.mockMode)

        val firstFavor = FavoriteHelper.loadFavorite()?.first()
        val arguments = this.arguments
        var name: String? = null
        if (arguments != null) {
            mockPoint = arguments.getParcelable<LatLng>(EXTRA_LOCATION)
            name = arguments.getString(EXTRA_NAME)
        }
        if (mockPoint == null && firstFavor != null) {
            name = firstFavor.poiName
            mockPoint = firstFavor.pt
        }
        if (mockPoint == null) {
            startLocation()
        } else {
            baiduMap.setMapStatus(MapStatusUpdateFactory.newLatLng(mockPoint))
            setMockPoint(mockPoint!!, name, true)
        }

        mockViewModel.location.observe(viewLifecycleOwner, Observer {
            if (!mockViewModel.isSuccess(it)) {
                tvLocation.text = getString(R.string.text_location_error)
                tvLocation.isClickable = false
            }
            val locData = MyLocationData.Builder()
                .accuracy(it.radius) // 此处设置开发者获取到的方向信息，顺时针0-360
                .direction(it.direction)
                .latitude(it.latitude)
                .longitude(it.longitude)
                .build()
            baiduMap.isMyLocationEnabled = true
            baiduMap.setMyLocationData(locData)
            val myLocationConfiguration =
                MyLocationConfiguration(MyLocationConfiguration.LocationMode.FOLLOWING, false, null)
            baiduMap.setMyLocationConfiguration(myLocationConfiguration)

            tvLocation.text = getString(R.string.text_location_success)
            tvLocation.isClickable = true
        })
        baiduMap.setOnMapClickListener(object : BaiduMap.OnMapClickListener {
            override fun onMapClick(p0: LatLng?) {
                setMockPoint(p0 ?: return, null, false)
            }

            override fun onMapPoiClick(p0: MapPoi?) {
                setMockPoint(p0?.position ?: return, p0.name, false)
            }
        })

        val drawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_location)
        drawable?.setBounds(0, 0, dip(23f), dip(23f))
        tvLocation.setCompoundDrawables(drawable, null, null, null)
        tvLocation.setOnClickListener {
            val locationEnable = Util.isLocationEnable(requireContext())
            if (locationEnable) {
                startLocation()
            } else {
                toast(R.string.tip_location_disable)
                startLocationSetting()
            }
        }

        fabMock.setOnClickListener {
            if (mockMode) {
                setMockStatus(false)
                return@setOnClickListener
            }
            val result = Util.checkMockLocation(requireContext())
            if (!result) {
                toast(R.string.tip_mock_closed)
                startMockLocation()
                return@setOnClickListener
            }
            if (mockPoint == null) {
                toast(R.string.tip_location_undfined)
                return@setOnClickListener
            }
            setMockStatus(true)
        }
    }

    private fun startLocationSetting() {
        kotlin.runCatching {
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }.onFailure {
            toast(R.string.tip_open_location_error)
        }
    }

    private fun startMockLocation() {
        kotlin.runCatching {
            val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
            intent.putExtra(":settings:fragment_args_key", "mock_location_app")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }.onFailure {
            toast(R.string.tip_open_devmode_error)
        }
    }

    private fun startLocation() {
        val arrayOf = arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        if (Util.checkPermission(requireContext(), *arrayOf)) {
            tvLocation.isClickable = false
            tvLocation.setText(R.string.text_location_ing)
            mockViewModel.startLocation(requireContext(), mockMode)
        } else {
            requestPermissions(arrayOf, 1)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode != 1) return
        startLocation()
    }

    private fun setMockViewStatus(running: Boolean) {
        if (running) {
            fabMock.setImageResource(R.drawable.ic_baseline_stop)
        } else {
            fabMock.setImageResource(R.drawable.ic_baseline_start)
        }
        MapViewHelper.setMapStyle(mapView, running)
    }

    private fun setMockStatus(startMock: Boolean) {
        val intent = Intent(requireContext(), MockService::class.java)
        if (!startMock) {
            mockMode = false
            requireContext().stopService(intent)
        } else {
            mockMode = true
            intent.putExtra(MockService.EXTRA_MOCK_LOCATION, mockPoint)
            requireContext().startService(intent)
//            ContextCompat.startForegroundService(requireContext(), intent)
            toast(R.string.tip_mock_location_tip)
        }
        setMockViewStatus(this.mockMode)
    }

    /**
     * 设置Mock坐标
     */
    private fun setMockPoint(point: LatLng, name: String?, isFavorite: Boolean) {
        this.mockPoint = point
        baiduMap.clear()
        val bitmap = BitmapDescriptorFactory
            .fromResource(R.drawable.b_poi)
        val option: OverlayOptions = MarkerOptions()
            .position(point)
            .scaleX(0.5f)
            .scaleY(0.5f)
            .icon(bitmap)
        baiduMap.addOverlay(option)

        if (isFavorite) {// 收藏的坐标
//            baiduMap.addOverlay(option)
//            val mTextOptions: OverlayOptions = TextOptions()
//                .text(name ?: "收藏") //文字内容
//                .bgColor(-0x55000100) //背景色
//                .fontSize(24) //字号
//                .fontColor(-0xff01) //文字颜色
//                .position(point)
//            baiduMap.addOverlay(mTextOptions)
            return
        }

        val offset = (-bitmap.bitmap.height * 0.5f).toInt()
        val button = Button(requireContext())
        button.setText(R.string.text_add_favorite)
        button.setOnClickListener {
            FavoriteHelper.addFavorite(point, name)
            setMockPoint(point, name, true)
        }
        val infoWindow = InfoWindow(button, point, offset)
        baiduMap.showInfoWindow(infoWindow)
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onDestroyView() {
        requireContext().unregisterReceiver(stopReceiver)
        baiduMap.isMyLocationEnabled = false
        mapView.onDestroy()
        super.onDestroyView()
    }

}