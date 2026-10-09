package com.jhon.mihealthreader.root

import com.topjohnwu.superuser.Shell

class LibsuRootShell : RootShell {
    override fun run(vararg commands: String): ShellOutput {
        val result = Shell.cmd(*commands).exec()
        return ShellOutput(result.code, result.out, result.err)
    }
}
