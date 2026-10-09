package com.jhon.mihealthreader.discover

fun interface PackageScanner {
    fun installedPackages(): List<String>
}
