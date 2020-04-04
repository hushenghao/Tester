package com.dede.tester.ui.svn

import android.os.Bundle
import android.text.InputType
import android.text.TextUtils
import android.text.method.PasswordTransformationMethod
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.preference.EditTextPreference
import androidx.preference.EditTextPreference.OnBindEditTextListener
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.dede.tester.R
import com.dede.tester.ext.findCoordinator
import com.google.android.material.snackbar.Snackbar


class SVNConfigFragment : PreferenceFragmentCompat() {

    companion object {
        const val KEY_SVN_USER = "svn_user"
        const val KEY_SVN_PASSWORD = "svn_password"
        const val KEY_SVN_URL = "svn_url"
        private const val KEY_SVN_LOGIN = "svn_login"

        const val EXTRA_AUTO_LOGIN = "auto_login"
        const val EXTRA_SVN_CONFIG = "svn_config"

        const val DEFAULT_URL = "http://svn.guchele.cn/svn/sherry/trunk/app/android/releases/"
    }

    private val viewModel: SVNConfigViewModel by viewModels()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preference_svn_config)
        findPreference<EditTextPreference>(KEY_SVN_PASSWORD)?.setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_TEXT_VARIATION_PASSWORD
            editText.transformationMethod = PasswordTransformationMethod.getInstance()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fun navigateSvn(config: SVNConfigViewModel.SVNConfig?): Boolean {
            if (config?.check() != true) {
                return false
            }
            findNavController().navigate(R.id.action_svn_config_to_svn, Bundle().apply {
                putParcelable(EXTRA_SVN_CONFIG, config)
            })
            return true
        }

        findPreference<Preference>(KEY_SVN_LOGIN)?.setOnPreferenceClickListener {
            val config = viewModel.svnConfig.value
            if (!navigateSvn(config)) {
                Snackbar.make(findCoordinator(), "是不是哪里不对？", Snackbar.LENGTH_SHORT).show()
            }
            return@setOnPreferenceClickListener true
        }
        val autoLogin = arguments?.getBoolean(EXTRA_AUTO_LOGIN, true) ?: true
        viewModel.svnConfig.observe(viewLifecycleOwner, Observer {
            findPreference<EditTextPreference>(KEY_SVN_URL)?.summary = it.svnUrl
            findPreference<EditTextPreference>(KEY_SVN_USER)?.summary = it.user ?: "请输入"
            findPreference<EditTextPreference>(KEY_SVN_PASSWORD)?.summary =
                getShowPassword(it.password) ?: "请输入"
            if (!autoLogin) {
                return@Observer
            }
            navigateSvn(it)
        })
        // 加载配置
        viewModel.loadConfig(requireContext())
    }

    private fun getShowPassword(password: String?): String? {
        if (TextUtils.isEmpty(password)) return null

        return (0..password!!.length)
            .map { "" }
            .reduce { acc, _ -> "$acc*" }
    }

    private val preference by lazy { PreferenceManager.getDefaultSharedPreferences(context) }

    override fun onStart() {
        super.onStart()
        preference.registerOnSharedPreferenceChangeListener(viewModel)
    }

    override fun onStop() {
        super.onStop()
        preference.unregisterOnSharedPreferenceChangeListener(viewModel)
    }

}
