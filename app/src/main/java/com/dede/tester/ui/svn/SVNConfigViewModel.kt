package com.dede.tester.ui.svn

import android.content.Context
import android.content.SharedPreferences
import android.os.Parcel
import android.os.Parcelable
import android.text.TextUtils
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.preference.PreferenceManager
import com.dede.tester.ui.svn.SVNConfigFragment.Companion.DEFAULT_URL

class SVNConfigViewModel : ViewModel(), SharedPreferences.OnSharedPreferenceChangeListener {

    class SVNConfig(
        var svnUrl: String = DEFAULT_URL,
        var user: String? = null,
        var password: String? = null
    ) : Parcelable {
        constructor(parcel: Parcel) : this(
            parcel.readString() ?: DEFAULT_URL,
            parcel.readString(),
            parcel.readString()
        )

        fun check(): Boolean {
            return !TextUtils.isEmpty(svnUrl) &&
                    !TextUtils.isEmpty(user) &&
                    !TextUtils.isEmpty(password)
        }

        fun isEmpty(): Boolean {
            return TextUtils.isEmpty(svnUrl) &&
                    TextUtils.isEmpty(user) &&
                    TextUtils.isEmpty(password)
        }

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeString(svnUrl)
            parcel.writeString(user)
            parcel.writeString(password)
        }

        override fun describeContents(): Int {
            return 0
        }

        companion object CREATOR : Parcelable.Creator<SVNConfig> {
            override fun createFromParcel(parcel: Parcel): SVNConfig {
                return SVNConfig(parcel)
            }

            override fun newArray(size: Int): Array<SVNConfig?> {
                return arrayOfNulls(size)
            }
        }
    }

    private val _svnConfig = SVNConfig()

    val svnConfig = MutableLiveData<SVNConfig>()

    fun loadConfig(context: Context) {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        _svnConfig.svnUrl =
            sharedPreferences.getString(SVNConfigFragment.KEY_SVN_URL, DEFAULT_URL) ?: DEFAULT_URL
        _svnConfig.user = sharedPreferences.getString(SVNConfigFragment.KEY_SVN_USER, null)
        _svnConfig.password = sharedPreferences.getString(SVNConfigFragment.KEY_SVN_PASSWORD, null)
        svnConfig.value = _svnConfig
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        sharedPreferences ?: return
        when (key) {
            SVNConfigFragment.KEY_SVN_URL -> {
                _svnConfig.svnUrl = sharedPreferences.getString(key, DEFAULT_URL) ?: DEFAULT_URL
            }
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
