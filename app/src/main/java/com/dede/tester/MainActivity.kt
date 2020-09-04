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
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.get
import androidx.lifecycle.Observer
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.dede.tester.ui.MainViewModel
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlin.math.max
import kotlin.math.min

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
                    .setEnterAnim(R.anim.fragment_fade_enter)
                    .setPopEnterAnim(R.anim.fragment_fade_enter)
                    .setExitAnim(R.anim.fragment_fade_exit)
                    .setPopExitAnim(R.anim.fragment_fade_exit)
                    .build()
            )
        }

        mainViewModel.initSort(this)// 加载排序方式

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
            val sortMenu = menu.findItem(R.id.group_sort).subMenu
            mainViewModel.sortType.observe(this, Observer {
                val index = max(0, min(it, sortMenu.size() - 1))
                sortMenu[index].isChecked = true
            })
            return true
        }
        return false
    }

    private fun getNavFragment(): NavHostFragment? {
        return supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(Gravity.LEFT)) {
            drawer_layout.closeDrawer(Gravity.LEFT)
            return
        }

        super.onBackPressed()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val sortIndex = getSortIndex(item.itemId)
        if (sortIndex >= 0) {
            mainViewModel.updateSort(this, sortIndex)
            return true
        }
        return when (item.itemId) {
            R.id.action_svn_to_svn_config -> {
                navController.navigate(item.itemId)
                true
            }
            R.id.action_clear_dir -> {
                AlertDialog.Builder(this)
                    .setTitle("提示")
                    .setMessage("是否所有清空下载文件？")
                    .setNegativeButton("取消", null)
                    .setPositiveButton("确定") { _, _ ->
                        mainViewModel.clearDownload(this)
                    }
                    .create()
                    .show()
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }

    private fun getSortIndex(itemId: Int): Int {
        return when (itemId) {
            R.id.sort_name_asc -> 0
            R.id.sort_name_des -> 1
            R.id.sort_data_asc -> 2
            R.id.sort_data_des -> 3
            else -> -1
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

}
