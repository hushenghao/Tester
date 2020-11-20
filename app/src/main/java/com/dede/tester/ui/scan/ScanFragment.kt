package com.dede.tester.ui.scan

import android.Manifest
import android.content.ComponentName
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Point
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.SurfaceHolder
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsIntent
import androidx.browser.customtabs.CustomTabsServiceConnection
import androidx.browser.trusted.TrustedWebActivityIntentBuilder
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import com.dede.tester.R
import com.google.androidbrowserhelper.trusted.TwaLauncher
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

    private val connection = object : CustomTabsServiceConnection() {
        override fun onCustomTabsServiceConnected(name: ComponentName, client: CustomTabsClient) {
            // 预加载，参数暂时是保留参数，随便传
            client.warmup(0)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        (requireActivity() as? AppCompatActivity)?.supportActionBar?.hide()
    }

    private var surfaceCreated = false

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        super.surfaceDestroyed(holder)
        surfaceCreated = false
    }
//
//    override fun handleDecode(result: Result?, barcode: Bitmap?) {
//        // 关闭声音
//        val playBeepField = CaptureFragment::class.java.getDeclaredField("playBeep")
//        playBeepField.isAccessible = true
//        playBeepField.set(this, false)
//        super.handleDecode(result, barcode)
//    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        super.surfaceCreated(holder)
        if (!surfaceCreated) {
            scanSurfaceView()
            surfaceCreated = true
        }
    }

    override fun onStart() {
        super.onStart()
        // 预加载
        CustomTabsClient.bindCustomTabsService(
            requireContext(),
            requireContext().packageName,
            connection
        )
    }

    override fun onResume() {
        super.onResume()
        if (surfaceCreated) {
            scanSurfaceView()
        }
    }

    override fun onDestroyView() {
        requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        (requireActivity() as? AppCompatActivity)?.supportActionBar?.show()
        super.onDestroyView()
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
        if (/*!URLUtil.isValidUrl(result) || */!Patterns.WEB_URL.matcher(result!!).matches()) {
            return
        }

        val uri = Uri.parse(result)
        val twaLauncher = TwaLauncher(requireContext())
        val builder = TrustedWebActivityIntentBuilder(uri)
            .setToolbarColor(ContextCompat.getColor(requireContext(), R.color.colorPrimary))
        try {
            val field =
                TrustedWebActivityIntentBuilder::class.java.getDeclaredField("mIntentBuilder")
            field.isAccessible = true
            val customTabsBuilder = field.get(builder) as CustomTabsIntent.Builder
            customTabsBuilder.setShowTitle(true)// 显示标题栏
                .addDefaultShareMenuItem()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        twaLauncher.launch(builder, null, null, null, null)

        val navController = findNavController()
        navController.navigateUp()// 扫描页弹栈
    }

    override fun onAnalyzeFailed() {
        Toast.makeText(requireContext(), "扫描失败", Toast.LENGTH_SHORT).show()
    }
}
