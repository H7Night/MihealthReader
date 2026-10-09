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
import com.jhon.mihealthreader.local.LocalStore
import com.jhon.mihealthreader.local.MiHealthDatabase
import com.jhon.mihealthreader.parse.StepSample
import com.jhon.mihealthreader.root.PrivilegedShell
import com.jhon.mihealthreader.root.ShellOutput
import androidx.room.Room
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
import org.robolectric.RuntimeEnvironment
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ExtractionViewModelTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val main = StandardTestDispatcher()

    private lateinit var db: MiHealthDatabase
    private lateinit var store: LocalStore

    @Before
    fun setUp() {
        Dispatchers.setMain(main)
        db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), MiHealthDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        store = LocalStore(db)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        db.close()
    }

    private fun granted() = object : PrivilegedShell {
        override fun run(vararg commands: String) =
            ShellOutput(0, listOf("uid=0(root) gid=0(root)"), emptyList())

        override fun openFile(path: String): InputStream = ByteArrayInputStream(ByteArray(0))

        override fun isGranted(): Boolean = true
    }

    private fun viewModel(
        root: PrivilegedShell,
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
        localStore = store,
        io = main,
    )

    @Test
    fun `errors when root denied`() = runTest(main) {
        val vm = viewModel(
            object : PrivilegedShell {
                override fun run(vararg commands: String) = ShellOutput(1, emptyList(), listOf("denied"))
                override fun openFile(path: String): InputStream = ByteArrayInputStream(ByteArray(0))
                override fun isGranted(): Boolean = false
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
        assertEquals(0, state.newRecords)
        assertTrue(File(state.snapshot, "manifest.json").exists())
    }

    @Test
    fun `summary reflects preloaded local store`() = runTest(main) {
        store.save(
            com.jhon.mihealthreader.parse.ParsedData(
                sleep = emptyList(),
                samples = listOf(StepSample("dev", 0, 0, 10, 5, 1)),
                activity = emptyList(),
                skipped = 0,
                outputFile = File("out.json"),
            ),
            "com.mi.health",
            1000,
        )
        val vm = viewModel(
            granted(),
            { emptyList() },
            { _, _ -> error("extractor should not run") },
            { _, _ -> error("dumper should not run") },
            ManifestWriter(),
        )
        advanceUntilIdle()
        assertEquals(1, vm.summary.value.stepSamples)
        assertEquals(1000L, vm.summary.value.lastExtractedAt)
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
