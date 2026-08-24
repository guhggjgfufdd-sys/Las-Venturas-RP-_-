package com.umnicode.samp_launcher;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.umnicode.samp_launcher.core.SAMP.SAMPInstaller;

public class LauncherApplication extends Application {
    private static final String TAG = "LauncherApplication";
    private Context _Context;
    public UserConfig userConfig;
    public SAMPInstaller Installer;

    @Override
    public void onCreate() {
        super.onCreate();

        this._Context = this.getApplicationContext();

        // تهيئة UserConfig مع حماية لمنع إغلاق التطبيق في حال عدم وجود الصلاحيات
        try {
            String configName = getString(R.string.user_config_name);
            this.userConfig = new UserConfig(this._Context, configName);
        } catch (Exception e) {
            Log.e(TAG, "Error initializing UserConfig: " + e.getMessage());
            this.userConfig = new UserConfig(this._Context, "user_config");
        }

        // تهيئة SAMPInstaller بحماية لتفادي الكراش عند بدء التشغيل
        try {
            this.Installer = new SAMPInstaller(this._Context);
        } catch (Exception e) {
            Log.e(TAG, "Error initializing SAMPInstaller: " + e.getMessage());
        }
    }
}
