package com.jhon.mihealthreader.root

import android.content.pm.PackageManager
import rikka.shizuku.Shizuku

object ShizukuPermission {

    fun isAvailable(): Boolean = runCatching { Shizuku.pingBinder() }.getOrDefault(false)

    fun isGranted(): Boolean = runCatching {
        Shizuku.pingBinder() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    }.getOrDefault(false)

    fun request(requestCode: Int, onResult: (Boolean) -> Unit) {
        lateinit var listener: Shizuku.OnRequestPermissionResultListener
        listener = Shizuku.OnRequestPermissionResultListener { code, grantResult ->
            if (code == requestCode) {
                Shizuku.removeRequestPermissionResultListener(listener)
                onResult(grantResult == PackageManager.PERMISSION_GRANTED)
            }
        }
        Shizuku.addRequestPermissionResultListener(listener)
        Shizuku.requestPermission(requestCode)
    }
}
