package com.dede.tester.ui.svn

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.dede.tester.R

class SVNConfigFragment : PreferenceFragmentCompat() {

    companion object {
        const val KEY_SVN_USER = "svn_user"
        const val KEY_SVN_PASSWORD = "svn_password"
        private const val KEY_SVN_LOGIN = "svn_login"

        const val EXTRA_AUTO_LOGIN = "auto_login"
    }

    private val viewModel: SVNConfigViewModel by viewModels()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.preference_svn_config)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fun navigateSvn(config: SVNConfigViewModel.SVNConfig?) {
            if (config?.check() != true) {
                return
            }
            findNavController().popBackStack(R.id.nav_svn, true)// 弹出列表
            findNavController().popBackStack(R.id.nav_svn_config, true)// 弹出自己
            findNavController().navigate(R.id.nav_svn, Bundle().apply {
                putString(KEY_SVN_USER, config.user)
                putString(KEY_SVN_PASSWORD, config.password)
            })
        }

        findPreference<Preference>(KEY_SVN_LOGIN)?.setOnPreferenceClickListener {
            val config = viewModel.svnConfig.value
            navigateSvn(config)
            return@setOnPreferenceClickListener true
        }
        val autoLogin = arguments?.getBoolean(EXTRA_AUTO_LOGIN, true) ?: true
        viewModel.svnConfig.observe(viewLifecycleOwner, Observer {
            findPreference<EditTextPreference>(KEY_SVN_USER)?.summary = it.user ?: "请输入"
            findPreference<EditTextPreference>(KEY_SVN_PASSWORD)?.summary = it.password ?: "请输入"
            if (!autoLogin) {
                return@Observer
            }
            navigateSvn(it)
        })
        // 加载缓存
        viewModel.loadConfig(requireContext())
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