package com.jhon.mihealthreader.ui

import com.jhon.mihealthreader.data.ExportRepository
import com.jhon.mihealthreader.discover.TargetApp
import com.jhon.mihealthreader.discover.TargetFinder
import com.jhon.mihealthreader.dump.DbDumpResult
import com.jhon.mihealthreader.dump.Dumper
import com.jhon.mihealthreader.dump.ManifestWriter
import com.jhon.mihealthreader.dump.TableDump
import com.jhon.mihealthreader.extract.Extractor
import com.jhon.mihealthreader.extract.Snapshot
import com.jhon.mihealthreader.root.RootShell
import com.jhon.mihealthreader.root.ShellOutput
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ExtractionViewModelTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val main = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(main)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun granted() = object : RootShell {
        override fun run(vararg commands: String) =
            ShellOutput(0, listOf("uid=0(root) gid=0(root)"), emptyList())
    }

    private fun viewModel(
        root: RootShell,
        finder: TargetFinder,
        extractor: Extractor,
        dumper: Dumper,
        writer: ManifestWriter,
    ) = ExtractionViewModel(
        rootShell = root,
        targetFinder = finder,
        extractor = extractor,
        dumper = dumper,
        manifestWriter = writer,
        repository = ExportRepository(tmp.root),
        androidVersion = 34,
        appVersion = "0.1.0",
        exportsDir = tmp.root,
        io = main,
    )

    @Test
    fun `errors when root denied`() = runTest(main) {
        val vm = viewModel(
            object : RootShell {
                override fun run(vararg commands: String) = ShellOutput(1, emptyList(), listOf("denied"))
            },
            { emptyList() },
            { _, _ -> error("extractor should not run") },
            { _, _ -> error("dumper should not run") },
            ManifestWriter(),
        )
        vm.start()
        advanceUntilIdle()
        assertTrue(vm.state.value is ExtractionState.Error)
    }

    @Test
    fun `errors when no target found`() = runTest(main) {
        val vm = viewModel(
            granted(),
            { emptyList() },
            { _, _ -> error("extractor should not run") },
            { _, _ -> error("dumper should not run") },
            ManifestWriter(),
        )
        vm.start()
        advanceUntilIdle()
        assertTrue(vm.state.value is ExtractionState.Error)
    }

    @Test
    fun `success writes manifest and reports table count`() = runTest(main) {
        val root = File(tmp.root, "T1").apply { mkdirs() }
        val raw = File(root, "raw/com.xiaomi.wearable/databases").apply { mkdirs() }
        val dbFile = File(raw, "mi.db").apply { writeText("x") }
        val extractor = Extractor { _, _ -> Snapshot(root, raw, "com.xiaomi.wearable", listOf(dbFile)) }
        val dumper = Dumper { db, _ ->
            DbDumpResult(db.name, true, listOf(TableDump("t", 2, File("json/${db.name}/t.json"))))
        }
        val vm = viewModel(
            granted(),
            { listOf(TargetApp("com.xiaomi.wearable", "/data/data/com.xiaomi.wearable/databases", listOf("mi.db"))) },
            extractor,
            dumper,
            ManifestWriter(),
        )
        vm.start()
        advanceUntilIdle()

        val state = vm.state.value as ExtractionState.Success
        assertEquals(1, state.tableCount)
        assertTrue(File(state.snapshot, "manifest.json").exists())
    }

    @Test
    fun `ignores duplicate start while running`() = runTest(main) {
        var calls = 0
        val finder = TargetFinder { calls++; emptyList() }
        val vm = viewModel(
            granted(),
            finder,
            { _, _ -> error("extractor should not run") },
            { _, _ -> error("dumper should not run") },
            ManifestWriter(),
        )
        vm.start()
        vm.start()
        advanceUntilIdle()
        assertEquals(1, calls)
    }
}
