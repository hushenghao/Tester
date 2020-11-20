package com.dede.tester.ext

import android.app.ActivityManager
import android.app.AppOpsManager
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.fragment.app.Fragment
import kotlin.math.roundToInt


fun Context.dip(dp: Float): Int {
    return (this.resources.displayMetrics.density * dp).roundToInt()
}

fun Fragment.dip(dp: Float): Int {
    return (this.resources.displayMetrics.density * dp).roundToInt()
}

fun Fragment.toast(@StringRes id: Int) {
    Toast.makeText(requireContext(), id, Toast.LENGTH_SHORT).show()
}

object Util {

    fun checkPermission(context: Context, vararg permissions: String): Boolean {
        return permissions.map { ContextCompat.checkSelfPermission(context, it) }
            .none { it != PackageManager.PERMISSION_GRANTED }
    }

    fun isLocationEnable(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return LocationManagerCompat.isLocationEnabled(locationManager)
    }

    /**
     * Return whether service is running.
     *
     * @param clazz The name of class.
     * @return `true`: yes<br></br>`false`: no
     */
    fun isServiceRunning(context: Context, clazz: Class<*>): Boolean {
        return try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val info: List<ActivityManager.RunningServiceInfo>? = am.getRunningServices(0x7FFFFFFF)
            if (info == null || info.size == 0) return false
            val name = clazz.name
            for (aInfo in info) {
                if (name == aInfo.service.className) return true
            }
            false
        } catch (ignore: java.lang.Exception) {
            false
        }
    }

    fun checkMockLocation(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            var allowed: Int = AppOpsManager.MODE_IGNORED
            try {
                val myUid = Process.myUid()
                val opstrMockLocation = AppOpsManager.OPSTR_MOCK_LOCATION
                val packageName = context.packageName
                allowed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    appOpsManager.unsafeCheckOpNoThrow(opstrMockLocation, myUid, packageName)
                } else {
                    appOpsManager.checkOpNoThrow(opstrMockLocation, myUid, packageName)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return allowed == AppOpsManager.MODE_ALLOWED
        }

        try {
            val allowMockLocation = Settings.Secure.ALLOW_MOCK_LOCATION
            return Settings.Secure.getInt(context.contentResolver, allowMockLocation) == 1
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return false
    }
}