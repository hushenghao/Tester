package com.dede.tester.ui.svn

import android.content.Context
import android.content.SharedPreferences
import android.text.TextUtils
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.preference.PreferenceManager

class SVNConfigViewModel : ViewModel(), SharedPreferences.OnSharedPreferenceChangeListener {

    class SVNConfig(var user: String? = null, var password: String? = null) {
        fun check(): Boolean {
            return !TextUtils.isEmpty(user) && !TextUtils.isEmpty(password)
        }
    }

    private val _svnConfig = SVNConfig()

    val svnConfig = MutableLiveData<SVNConfig>()

    fun loadConfig(context: Context) {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        _svnConfig.user = sharedPreferences.getString(SVNConfigFragment.KEY_SVN_USER, null)
        _svnConfig.password = sharedPreferences.getString(SVNConfigFragment.KEY_SVN_PASSWORD, null)
        svnConfig.value = _svnConfig
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        sharedPreferences ?: return
        when (key) {
            SVNConfigFragment.KEY_SVN_USER -> {
                _svnConfig.user = sharedPreferences.getString(key, null)
            }
            SVNConfigFragment.KEY_SVN_PASSWORD -> {
                _svnConfig.password = sharedPreferences.getString(key, null)
            }
        }
        svnConfig.value = _svnConfig
    }
}
