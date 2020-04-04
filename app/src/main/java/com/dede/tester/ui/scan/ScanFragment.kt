package com.dede.tester.ui.scan

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Point
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.SurfaceHolder
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import com.dede.tester.R
import com.uuzuche.lib_zxing.activity.CaptureFragment
import com.uuzuche.lib_zxing.activity.CodeUtils
import com.uuzuche.lib_zxing.activity.ZXingLibrary
import com.uuzuche.lib_zxing.camera.CameraManager
import kotlinx.android.synthetic.main.fragment_scan.*


/**
 * 扫一扫页面
 */
class ScanFragment : CaptureFragment(), CodeUtils.AnalyzeCallback {

    override fun onCreate(savedInstanceState: Bundle?) {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), 10)
        }

        ZXingLibrary.initDisplayOpinion(requireContext())
        super.onCreate(savedInstanceState)

        // 自定义布局
        CodeUtils.setFragmentArgs(this, R.layout.fragment_scan)
        analyzeCallback = this
    }

    private val callback = object : SurfaceHolder.Callback {
        override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
        }

        override fun surfaceDestroyed(holder: SurfaceHolder?) {
        }

        override fun surfaceCreated(holder: SurfaceHolder?) {
            scanSurfaceView()
        }
    }

    override fun onResume() {
        super.onResume()
        preview_view.holder.addCallback(callback)
    }

    override fun onPause() {
        super.onPause()
        preview_view.holder.removeCallback(callback)
    }

    private fun scanSurfaceView() {
        val manager = CameraManager.get()
        val configManagerField = CameraManager::class.java.getDeclaredField("configManager")
        configManagerField.isAccessible = true
        val configManager = configManagerField.get(manager)
        val clazz = Class.forName("com.uuzuche.lib_zxing.camera.CameraConfigurationManager")
        val screenResolutionField = clazz.getDeclaredField("screenResolution")
        val cameraResolutionField = clazz.getDeclaredField("cameraResolution")
        screenResolutionField.isAccessible = true
        cameraResolutionField.isAccessible = true
        val screenResolution = (screenResolutionField.get(configManager) as? Point) ?: return
        val cameraResolution = (cameraResolutionField.get(configManager) as? Point) ?: return

        Log.i("ScanFragment", "cameraResolution: $cameraResolution")
        Log.i("ScanFragment", "screenResolution: $screenResolution")
        val surfaceView = preview_view ?: return
        val cameraRatio = cameraResolution.x * 1f / cameraResolution.y
        val screenRatio = screenResolution.x * 1f / screenResolution.y
        if (cameraRatio == screenRatio) return// 比例相同，不用缩放

        val height: Int
        val width: Int
        if (cameraRatio > screenRatio) {
            height = screenResolution.y
            width = (screenResolution.y / cameraRatio + .5f).toInt()
        } else {
            width = screenResolution.x
            height = (screenResolution.x * cameraRatio + .5f).toInt()
        }
        surfaceView.layoutParams = surfaceView.layoutParams.apply {
            this.width = width
            this.height = height
        }
    }

    override fun onAnalyzeSuccess(mBitmap: Bitmap?, result: String?) {
        Log.i("ScanFragment", "Scan Relut: $result")
        if (TextUtils.isEmpty(result)) {
            return
        }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(result))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(Intent.createChooser(intent, "选择浏览器打开"))

        val navController = findNavController()
        navController.navigateUp()// 扫描页弹栈
    }

    override fun onAnalyzeFailed() {
        Toast.makeText(requireContext(), "扫描失败", Toast.LENGTH_SHORT).show()
    }
}
