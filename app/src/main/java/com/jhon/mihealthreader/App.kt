package com.jhon.mihealthreader

import android.app.Application
import com.topjohnwu.superuser.Shell
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Shell.setDefaultBuilder(
            Shell.Builder.create().setFlags(Shell.FLAG_MOUNT_MASTER),
        )
    }
}
