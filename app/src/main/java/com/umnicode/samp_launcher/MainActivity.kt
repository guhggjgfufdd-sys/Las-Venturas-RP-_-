package com.umnicode.samp_launcher

import android.Manifest
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.view.Window
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    // واجهة للتوافق المباشر مع جافا
    fun interface PermissionCallback {
        fun onResult(isGranted: Boolean)
    }

    private var permissionCallback: PermissionCallback? = null

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
    }

    fun IsStoragePermissionsGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            val read = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
            val write = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            read == PackageManager.PERMISSION_GRANTED && write == PackageManager.PERMISSION_GRANTED
        }
    }

    fun RequestStoragePermission(callback: PermissionCallback) {
        this.permissionCallback = callback

        if (IsStoragePermissionsGranted()) {
            callback.onResult(true)
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            } catch (e: Exception) {
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                startActivity(intent)
            }
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ),
                1001
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001) {
            val isGranted = grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            permissionCallback?.onResult(isGranted)
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)

        if (hasFocus) {
            if (permissionCallback != null && IsStoragePermissionsGranted()) {
                permissionCallback?.onResult(true)
                permissionCallback = null
            }

            try {
                val app = applicationContext as? LauncherApplication
                app?.Installer?.ReCheckInstallResources(this)
            } catch (e: Throwable) {
                Log.e("LauncherApplication", "Error in ReCheckInstallResources: ${e.message}")
            }
        }
    }
}
