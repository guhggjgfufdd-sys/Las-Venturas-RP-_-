package com.umnicode.samp_launcher

import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.view.Window
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
            supportActionBar?.hide()
        } catch (e: Throwable) {
            Log.e("MainActivity", "Error hiding action bar: ${e.message}")
        }

        try {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } catch (e: Throwable) {
            Log.e("MainActivity", "Error setting orientation: ${e.message}")
        }

        try {
            setContentView(R.layout.activity_main)

            val navView: BottomNavigationView? = findViewById(R.id.nav_view)
            val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as? NavHostFragment

            if (navHostFragment != null && navView != null) {
                val navController = navHostFragment.navController
                val appBarConfiguration = AppBarConfiguration(
                    setOf(R.id.navigation_home, R.id.navigation_settings)
                )
                setupActionBarWithNavController(navController, appBarConfiguration)
                navView.setupWithNavController(navController)
            }
        } catch (e: Throwable) {
            Log.e("MainActivity", "Error initializing navigation: ${e.message}")
        }

        // طلب إذن الملفات الشامل لأندرويد الحديث
        checkStoragePermission()
    }

    private fun checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.data = Uri.parse("package:$packageName")
                    startActivity(intent)
                } catch (e: Exception) {
                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    startActivity(intent)
                }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)

        if (hasFocus) {
            try {
                val app = applicationContext as? LauncherApplication
                app?.Installer?.ReCheckInstallResources(this)
            } catch (e: Throwable) {
                Log.e("LauncherApplication", "Error in ReCheckInstallResources: ${e.message}")
            }
        }
    }
}
