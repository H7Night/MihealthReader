package com.jhon.mihealthreader.root

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import android.os.ParcelFileDescriptor
import com.jhon.mihealthreader.IUserService
import rikka.shizuku.Shizuku
import java.io.InputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

internal fun shizukuGranted(ping: Boolean, permissionGranted: Boolean, id: ShellOutput): Boolean =
    ping && permissionGranted && outputIndicatesRoot(id)

class ShizukuShell(context: Context) : PrivilegedShell {

    @Volatile
    private var service: IUserService? = null
    private val latch = CountDownLatch(1)

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            service = IUserService.Stub.asInterface(binder)
            latch.countDown()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
        }
    }

    init {
        val args = Shizuku.UserServiceArgs(
            ComponentName(context.packageName, ShizukuReaderService::class.java.name),
        ).daemon(false).processNameSuffix("reader").debuggable(false).version(1)
        Shizuku.bindUserService(args, connection)
    }

    private fun await(): IUserService {
        if (service == null) {
            runCatching { latch.await(15, TimeUnit.SECONDS) }
        }
        return service ?: error("Shizuku user service 未连接")
    }

    override fun run(vararg commands: String): ShellOutput {
        val lines = await().run(buildShellCommand(commands))
        val exit = lines.firstOrNull()?.removePrefix("exit=")?.toIntOrNull() ?: -1
        val errIndex = lines.indexOf("--err--")
        val stdout = if (errIndex > 1) lines.subList(1, errIndex).toList() else emptyList()
        val stderr = if (errIndex >= 0) lines.subList(errIndex + 1, lines.size).toList() else emptyList()
        return ShellOutput(exit, stdout, stderr)
    }

    override fun openFile(path: String): InputStream =
        ParcelFileDescriptor.AutoCloseInputStream(await().openFile(path))

    override fun isGranted(): Boolean {
        val ping = runCatching { Shizuku.pingBinder() }.getOrDefault(false)
        if (!ping) return false
        val granted = runCatching {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        }.getOrDefault(false)
        if (!granted) return false
        val id = runCatching { run("id") }.getOrElse { return false }
        return shizukuGranted(ping = true, permissionGranted = true, id = id)
    }
}
