package com.umnicode.samp_launcher;

import android.content.Context;
import android.app.Application;
import android.util.Log;

import com.umnicode.samp_launcher.core.SAMP.SAMPInstaller;

public class LauncherApplication extends Application {
    private Context _Context;
    public UserConfig userConfig;
    public SAMPInstaller Installer;

    @Override
    public void onCreate() {
        super.onCreate();
        
        try {
            this._Context = this.getApplicationContext();
            
            // محاولة جلب الإعدادات مع حماية من الـ Crash إذا كان الـ String غير موجود
            String configName = "";
            try {
                configName = this._Context.getString(R.string.user_config_name);
            } catch (Exception e) {
                configName = "default_config"; // قيمة احتياطية لمنع الانغلاق
            }
            
            this.userConfig = new UserConfig(this._Context, configName);
            this.Installer = new SAMPInstaller(this._Context);
            
        } catch (Exception e) {
            Log.e("SAMP_CRASH", "Error in LauncherApplication onCreate: ", e);
        }
    }
}
