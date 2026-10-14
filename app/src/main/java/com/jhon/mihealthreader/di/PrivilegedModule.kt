package com.jhon.mihealthreader.di

import android.content.Context
import com.jhon.mihealthreader.discover.PackageScanner
import com.jhon.mihealthreader.discover.TargetFinder
import com.jhon.mihealthreader.discover.TargetLocator
import com.jhon.mihealthreader.dump.DbDumper
import com.jhon.mihealthreader.dump.Dumper
import com.jhon.mihealthreader.dump.ManifestWriter
import com.jhon.mihealthreader.extract.DbExtractor
import com.jhon.mihealthreader.extract.Extractor
import com.jhon.mihealthreader.extract.SourceReader
import com.jhon.mihealthreader.parse.Normalizer
import com.jhon.mihealthreader.root.BackendManager
import com.jhon.mihealthreader.root.PrivilegedShell
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PrivilegedModule {

    @Provides
    @Singleton
    fun privilegedShell(@ApplicationContext context: Context): PrivilegedShell =
        BackendManager(context).current()

    @Provides
    fun packageScanner(@ApplicationContext context: Context): PackageScanner =
        PackageScanner { context.packageManager.getInstalledPackages(0).map { it.packageName } }

    @Provides
    fun targetFinder(shell: PrivilegedShell, scanner: PackageScanner): TargetFinder =
        TargetLocator(shell, scanner)

    @Provides
    fun extractor(shell: PrivilegedShell): Extractor =
        DbExtractor(SourceReader { path -> shell.openFile(path) })

    @Provides
    fun dumper(): Dumper = DbDumper()

    @Provides
    fun manifestWriter(): ManifestWriter = ManifestWriter()

    @Provides
    fun normalizer(): Normalizer = Normalizer()
}
