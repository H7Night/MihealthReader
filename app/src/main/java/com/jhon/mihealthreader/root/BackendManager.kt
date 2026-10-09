package com.jhon.mihealthreader.root

import android.content.Context
import com.jhon.mihealthreader.local.BackendPreferences
import com.jhon.mihealthreader.local.BackendType

class BackendManager(private val context: Context) {

    fun current(): PrivilegedShell = when (BackendPreferences(context).get()) {
        BackendType.ROOT -> LibsuRootShell()
        BackendType.SHIZUKU -> ShizukuShell(context.applicationContext)
    }
}
