package com.umnicode.samp_launcher

import android.Manifest
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Window
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView

interface PermissionRequestCallback {
    fun Finished(isGranted: Boolean)
}

class PermissionRequest(val Permission: String, var Callbacks: MutableList<PermissionRequestCallback>)

class MainActivity : AppCompatActivity() {
    private lateinit var PermissionRequests: MutableList<PermissionRequest>
    private var PermissionRequestID: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        this.PermissionRequests = mutableListOf()

        try {
            supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
            supportActionBar?.hide()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        setContentView(R.layout.activity_main)
        val navView: BottomNavigationView = findViewById(R.id.nav_view)

        // Bug fix
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        // Passing each menu ID as a set of IDs because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home, R.id.navigation_settings
            )
        )

        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        // **التعديل الهجومي لمنع الكراش:** طلب صلاحيات التخزين فور فتح التطبيق
        RequestStoragePermission(object : PermissionRequestCallback {
            override fun Finished(isGranted: Boolean) {
                if (isGranted) {
                    Log.i("Permissions", "Storage permissions granted successfully.")
                } else {
                    Log.w("Permissions", "Storage permissions denied by user.")
                }
            }
        })
    }

    // When we move app to background, we can for example remove dirs or install game APK ( it's undefined behavior, but works)
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)

        if (hasFocus) {
            try {
                (this.applicationContext as LauncherApplication).Installer.ReCheckInstallResources(this)
            } catch (e: Exception) {
                Log.e("LauncherApplication", "Error in ReCheckInstallResources: " + e.message)
            }
        }
    }

    // Permission manager system
    private fun FindPermissionRequest(Permission: String): PermissionRequest? {
        // Simple search for permission
        for (LPermission in this.PermissionRequests) {
            if (LPermission.Permission == Permission) return LPermission
        }
        return null
    }

    fun RequestPermission(Permission: String, Callback: PermissionRequestCallback) {
        // Check does this permission was already granted
        if (ActivityCompat.checkSelfPermission(this, Permission) == PackageManager.PERMISSION_GRANTED) {
            Callback.Finished(true)
            return
        }

        val PermissionReq: PermissionRequest? = this.FindPermissionRequest(Permission)

        if (PermissionReq != null) { // If permission request exist - add current callback to list
            PermissionReq.Callbacks.add(Callback)
        } else {
            this.PermissionRequests.add(PermissionRequest(Permission, mutableListOf(Callback)))
            ActivityCompat.requestPermissions(this, arrayOf(Permission), this.PermissionRequestID)
            this.PermissionRequestID++
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        // requestCode of PermissionRequestSystem
        for (i in permissions.indices) {
            for (Index in PermissionRequests.indices) {
                if (PermissionRequests[Index].Permission == permissions[i]) {
                    // Notify all listeners
                    for (Callback in PermissionRequests[Index].Callbacks) {
                        Callback.Finished(grantResults[i] == PackageManager.PERMISSION_GRANTED)
                    }

                    // Remove request from array
                    this.PermissionRequests.removeAt(Index)
                    break
                }
            }
        }
    }

    // Top level permissions API:
    fun RequestStoragePermission(Callback: PermissionRequestCallback) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            this.RequestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, object : PermissionRequestCallback {
                override fun Finished(isGrantedW: Boolean) {
                    RequestPermission(Manifest.permission.READ_EXTERNAL_STORAGE, object : PermissionRequestCallback {
                        override fun Finished(isGrantedR: Boolean) {
                            Callback.Finished(isGrantedR && isGrantedW)
                        }
                    })
                }
            })
        } else {
            Callback.Finished(true)
        }
    }

    fun IsStorageReadPermissionsGranted(): Boolean {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }

    fun IsStorageWritePermissionsGranted(): Boolean {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }

    fun IsStoragePermissionsGranted(): Boolean {
        return this.IsStorageReadPermissionsGranted() && this.IsStorageWritePermissionsGranted()
    }
}
