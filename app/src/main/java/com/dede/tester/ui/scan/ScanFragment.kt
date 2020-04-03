package com.dede.tester.ui.scan

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import com.dede.tester.R
import com.uuzuche.lib_zxing.activity.CaptureFragment
import com.uuzuche.lib_zxing.activity.CodeUtils
import com.uuzuche.lib_zxing.activity.ZXingLibrary


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

        super.onCreate(savedInstanceState)

        ZXingLibrary.initDisplayOpinion(requireContext())
        // 自定义布局
        CodeUtils.setFragmentArgs(this, R.layout.fragment_scan)
        analyzeCallback = this
    }

    override fun onAnalyzeSuccess(mBitmap: Bitmap?, result: String?) {
        Log.i("ScanFragment", "onAnalyzeSuccess: " + result)

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(result ?: return))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(Intent.createChooser(intent, "选择浏览器打开"))

        val navController = findNavController()
        navController.navigateUp()// 扫描页弹栈
    }

    override fun onAnalyzeFailed() {
        Toast.makeText(requireContext(), "扫描失败", Toast.LENGTH_SHORT).show()
    }
}
