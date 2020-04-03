package com.dede.tester

import android.os.Bundle
import android.view.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
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
                toolbar.visibility = View.GONE
                window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            } else {
                toolbar.visibility = View.VISIBLE
                window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
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
            navController.navigate(R.id.nav_scan)
        }
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
            Snackbar.make(drawer_layout, "再按一次退出", Snackbar.LENGTH_SHORT).show()
            s = c
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.nav_svn_config_unlogin) {
            navController.navigate(R.id.nav_svn_config_unlogin)// svn配置
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

}
