package com.dede.tester

import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.dede.tester.ui.MainViewModel
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*

class MainActivity : AppCompatActivity() {

    private val appBarConfiguration by lazy {
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration(setOf(R.id.nav_svn, R.id.nav_svn_config, R.id.nav_scan), drawer_layout)
    }

    private val navController by lazy { findNavController(R.id.nav_host_fragment) }
    private val mainViewModel by viewModels<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        setupActionBarWithNavController(navController, appBarConfiguration)
        nav_view.setupWithNavController(navController)
        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id == R.id.nav_scan) {
                fab.visibility = View.GONE
            } else {
                fab.visibility = View.VISIBLE
                if (destination.id != R.id.nav_svn) {
                    toolbar.subtitle = null
                }
            }
            invalidateOptionsMenu()
        }
        mainViewModel.subTitle.observe(this, Observer {
            if (navController.currentDestination?.id == R.id.nav_svn) {
                toolbar.subtitle = it
            } else {
                toolbar.subtitle = null
            }
        })

        fab.setOnClickListener {
            navController.navigate(
                R.id.nav_scan,
                null,
                NavOptions.Builder()
                    .setEnterAnim(R.anim.fragment_open_enter)
                    .setPopEnterAnim(R.anim.fragment_open_enter)
                    .setExitAnim(R.anim.fragment_close_exit)
                    .setPopExitAnim(R.anim.fragment_close_exit)
                    .build()
            )
        }

        navController.handleDeepLink(intent)
        installShortcut()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        navController.handleDeepLink(intent)
    }

    private fun installShortcut() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) {
            return
        }
        val shortcutManager = getSystemService(ShortcutManager::class.java) ?: return
        val intent = Intent(this, MainActivity::class.java)
            .setAction(Intent.ACTION_VIEW)
            .setData(Uri.parse("http://dede.tester/main/scan"))
        val shortcut = ShortcutInfo.Builder(this, "scan")
            .setShortLabel(getString(R.string.scan))
            .setLongLabel(getString(R.string.scan))
            .setIcon(Icon.createWithResource(this, R.drawable.ic_scan))
            .setIntent(intent)
            .build()

        shortcutManager.dynamicShortcuts = listOf(shortcut)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        if (navController.currentDestination?.id == R.id.nav_svn) {
            menuInflater.inflate(R.menu.main, menu)
            return true
        }
        return false
    }

    private fun getNavFragment(): NavHostFragment? {
        return supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
    }

    private var s = 0L

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(Gravity.LEFT)) {
            drawer_layout.closeDrawer(Gravity.LEFT)
            return
        }

        if (getNavFragment()?.childFragmentManager?.backStackEntryCount ?: 0 >= 1) {
            super.onBackPressed()
            return
        }

        val c = System.currentTimeMillis()
        if (s + 1000 > c) {
            super.onBackPressed()
        } else {
            Snackbar.make(coordinator, "再按一次退出", Snackbar.LENGTH_SHORT).show()
            s = c
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_svn_to_svn_config) {
            navController.navigate(item.itemId)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

}
