package com.jhon.mihealthreader.root

import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.io.SuFileInputStream
import java.io.InputStream

class LibsuRootShell : PrivilegedShell {
    override fun run(vararg commands: String): ShellOutput {
        val result = Shell.cmd(buildShellCommand(commands)).exec()
        return ShellOutput(result.code, result.out, result.err)
    }

    override fun openFile(path: String): InputStream = SuFileInputStream.open(path)

    override fun isGranted(): Boolean =
        runCatching { outputIndicatesRoot(run("id")) }.getOrDefault(false)
}
