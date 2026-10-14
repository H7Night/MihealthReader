# MihealthReader — Hilt + Health Connect + MD3 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Migrate MihealthReader to Hilt, add a Health Connect write layer for the already-parsed metrics, align the tech stack with Kenko, and tidy MD3 fallback colors — without touching the privileged read channel or the parsers.

**Architecture:** Replace the manual wiring in `MainActivity` with Hilt modules; add a `data/health/` package whose record-mapping lives in pure, JVM-testable functions and whose SDK calls are thin; trigger writes after extraction and from a Settings "sync" action; keep `normalized.json` export unchanged.

**Tech Stack:** Kotlin 2.2.21 · AGP 8.13.2 · Compose BOM 2025.11.00 · Material3 1.5 · Room 2.8.3 · Hilt 2.57.2 · KSP 2.2.21-2.0.4 · AndroidX Health Connect `connect-client` 1.1.0 · libsu 5.2.2 · Shizuku 13.1.5 · JUnit4 + Robolectric.

**Spec:** `docs/superpowers/specs/2026-10-13-mihealthreader-hilt-healthconnect-md3-design.md`

## Global Constraints

- Work on branch `feat/hilt-healthconnect-md3` (base `feat/spec-sync`). Never commit to `main`.
- `minSdk 26`, `targetSdk 36`, `compileSdk 36`, JVM target 17.
- Keep `PrivilegedShell` (`LibsuRootShell` / `ShizukuShell`), the `fun interface` seams, the AIDL `IUserService`, and all parsers/`LocalStore` semantics unchanged.
- Keep Room `exportSchema = true` and additive migrations only; never add a destructive fallback.
- Unit tests run on JVM (`./gradlew :app:testDebugUnitTest`); never run instrumented tests on a device holding real data.
- All user-facing strings stay Chinese (this app is Chinese-only); no hardcoded strings added to new Compose.

## Review Focus

- **HC unavailable / no permission**: writing must be a no-op that never crashes or blocks extraction (the app must work identically when Health Connect is absent).
- **Absent/duplicate records**: re-running sync must not duplicate (stable `clientRecordId`).
- **Large heart-rate history**: thousands of samples must not OOM — batch inserts.
- **Sleep stage fidelity**: 1&5→AWAKE, 2→DEEP, 3→LIGHT, 4→REM, matching `SleepParser.stateName`, never inverted.
- **Privileged read unchanged**: after the Hilt migration the selected backend and its failure fallback behave exactly as before.

---

## Task 1: Align the tech stack

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`
- Modify: `build.gradle.kts` (root)
- Modify: `settings.gradle.kts` (only if plugin resolution needs it)

**Interfaces:**
- Produces: bumped versions; `health-connect-client` and Hilt available on the classpath; `targetSdk/compileSdk = 36`.

- [ ] **Step 1: Bump `gradle/libs.versions.toml`**

Set versions:
```toml
agp = "8.13.2"
kotlin = "2.2.21"
ksp = "2.2.21-2.0.4"
coreKtx = "1.17.0"
lifecycle = "2.9.4"
activityCompose = "1.11.0"
composeBom = "2025.11.00"
navigationCompose = "2.9.6"
coroutines = "1.10.2"
room = "2.8.3"
hilt = "2.57.2"
healthConnect = "1.1.0"
# keep libsu = "5.2.2", shizuku = "13.1.5", junit, robolectric
```
Add libraries:
```toml
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-compiler", version.ref = "hilt" }
health-connect-client = { group = "androidx.health.connect", name = "connect-client", version.ref = "healthConnect" }
```
Add plugins:
```toml
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
```

- [ ] **Step 2: Apply plugins + deps in `app/build.gradle.kts`**

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}
```
`compileSdk = 36`, `targetSdk = 36`. Add deps:
```kotlin
implementation(libs.hilt.android)
ksp(libs.hilt.compiler)
implementation(libs.health.connect.client)
```

- [ ] **Step 3: Root `build.gradle.kts`**

Add `alias(libs.plugins.hilt) apply false` alongside the other `apply false` plugin declarations (match the file's existing style).

- [ ] **Step 4: Build**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL (dependencies resolve; no code changes yet).

- [ ] **Step 5: Commit**

```bash
git add gradle/libs.versions.toml app/build.gradle.kts build.gradle.kts settings.gradle.kts
git commit -m "chore: align tech stack and add hilt + health connect"
```

---

## Task 2: Migrate manual wiring to Hilt

**Files:**
- Modify: `app/src/main/java/com/jhon/mihealthreader/App.kt`
- Modify: `app/src/main/java/com/jhon/mihealthreader/MainActivity.kt`
- Modify: `app/src/main/java/com/jhon/mihealthreader/ui/ExtractionViewModel.kt`
- Modify: `app/src/main/java/com/jhon/mihealthreader/ui/AppNav.kt`
- Create: `app/src/main/java/com/jhon/mihealthreader/di/DatabaseModule.kt`
- Create: `app/src/main/java/com/jhon/mihealthreader/di/AppModule.kt`
- Create: `app/src/main/java/com/jhon/mihealthreader/di/PrivilegedModule.kt`
- Create: `app/src/main/java/com/jhon/mihealthreader/di/RepositoryModule.kt`

**Interfaces:**
- Consumes: Task 1's Hilt dependency.
- Produces: injectable `MiHealthDatabase`, its 6 DAOs, `PrivilegedShell`, `TargetFinder`, `Extractor`, `Dumper`, `ManifestWriter`, `Normalizer`, `LocalStore`, `ParsedRepository`; `ExtractionViewModel` becomes `@HiltViewModel`.

- [ ] **Step 1: `@HiltAndroidApp` Application**

`App.kt`:
```kotlin
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
```

- [ ] **Step 2: `di/AppModule.kt`**

```kotlin
package com.jhon.mihealthreader.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

@Qualifier @Retention(AnnotationRetention.BINARY) annotation class IoDispatcher

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides @IoDispatcher
    fun io(): CoroutineDispatcher = Dispatchers.IO

    @Provides @Singleton
    fun appScope(@IoDispatcher io: CoroutineDispatcher): CoroutineScope =
        CoroutineScope(SupervisorJob() + io)
}
```

- [ ] **Step 3: `di/DatabaseModule.kt`**

```kotlin
package com.jhon.mihealthreader.di

import android.content.Context
import com.jhon.mihealthreader.local.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides @Singleton
    fun db(@ApplicationContext context: Context): MiHealthDatabase = MiHealthDatabase.build(context)

    @Provides fun stepDao(db: MiHealthDatabase) = db.stepSampleDao()
    @Provides fun sleepDao(db: MiHealthDatabase) = db.sleepDao()
    @Provides fun syncDao(db: MiHealthDatabase) = db.syncStateDao()
    @Provides fun metricDao(db: MiHealthDatabase) = db.metricDao()
    @Provides fun workoutDao(db: MiHealthDatabase) = db.workoutDao()
    @Provides fun bodyDao(db: MiHealthDatabase) = db.bodyMeasurementDao()
}
```

- [ ] **Step 4: `di/PrivilegedModule.kt`**

```kotlin
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
    @Provides @Singleton
    fun shell(@ApplicationContext context: Context): PrivilegedShell =
        BackendManager(context).current()

    @Provides fun scanner(@ApplicationContext context: Context): PackageScanner =
        PackageScanner { context.packageManager.getInstalledPackages(0).map { it.packageName } }

    @Provides fun targetFinder(shell: PrivilegedShell, scanner: PackageScanner): TargetFinder =
        TargetLocator(shell, scanner)

    @Provides fun extractor(shell: PrivilegedShell): Extractor =
        DbExtractor(SourceReader { path -> shell.openFile(path) })

    @Provides fun dumper(): Dumper = DbDumper()
    @Provides fun manifestWriter(): ManifestWriter = ManifestWriter()
    @Provides fun normalizer(): Normalizer = Normalizer()
}
```

- [ ] **Step 5: `di/RepositoryModule.kt`**

```kotlin
package com.jhon.mihealthreader.di

import com.jhon.mihealthreader.data.ParsedRepository
import com.jhon.mihealthreader.local.LocalStore
import com.jhon.mihealthreader.local.MiHealthDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    @Provides @Singleton
    fun localStore(db: MiHealthDatabase): LocalStore = LocalStore(db)

    @Provides @Singleton
    fun parsedRepository(localStore: LocalStore): ParsedRepository = ParsedRepository(localStore)
}
```

- [ ] **Step 6: `ExtractionViewModel` → `@HiltViewModel`**

```kotlin
@HiltViewModel
class ExtractionViewModel @Inject constructor(
    private val rootShell: PrivilegedShell,
    private val targetFinder: TargetFinder,
    private val extractor: Extractor,
    private val dumper: Dumper,
    private val manifestWriter: ManifestWriter,
    private val localStore: LocalStore,
    @IoDispatcher private val io: CoroutineDispatcher,
    @ApplicationContext context: Context,
) : ViewModel() {
    private val androidVersion = Build.VERSION.SDK_INT
    private val appVersion = BuildConfig.VERSION_NAME
    private val exportsDir = File(context.filesDir, "exports")
    // ...body unchanged...
}
```
Enable `buildConfig = true` in `app/build.gradle.kts` `buildFeatures` (add it next to `compose`/`aidl`).

- [ ] **Step 7: `MainActivity` + `AppNav`**

`MainActivity` becomes:
```kotlin
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MiHealthTheme {
                AppNav()
            }
        }
    }
}
```
`AppNav` (no args) obtains its dependencies with `hiltViewModel()`:
```kotlin
@Composable
fun AppNav(
    extractionViewModel: ExtractionViewModel = hiltViewModel(),
    parsedRepository: ParsedRepository = hiltViewModel<RepositoryViewModel>().repository,
) { /* existing NavHost body unchanged */ }
```
If `AppNav` also needs `exportsDir`, expose it from `ExtractionViewModel` (`val exportsDir: File`) and read it there, or add a small `@HiltViewModel` holder — pick the smallest change that keeps `AppNav`'s body identical.

- [ ] **Step 8: Build + unit tests**

Run: `./gradlew :app:assembleDebug :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL; existing tests green.

- [ ] **Step 9: Commit**

```bash
git add app/src/main/java/com/jhon/mihealthreader/App.kt \
        app/src/main/java/com/jhon/mihealthreader/MainActivity.kt \
        app/src/main/java/com/jhon/mihealthreader/ui/ExtractionViewModel.kt \
        app/src/main/java/com/jhon/mihealthreader/ui/AppNav.kt \
        app/src/main/java/com/jhon/mihealthreader/di app/build.gradle.kts
git commit -m "refactor: migrate manual wiring to hilt"
```

---

## Task 3: Pure Health Connect mappers (TDD)

**Files:**
- Create: `app/src/main/java/com/jhon/mihealthreader/data/health/HealthMappings.kt`
- Test: `app/src/test/java/com/jhon/mihealthreader/data/health/HealthMappingsTest.kt`

**Interfaces:**
- Produces: `fun sleepStageType(state: Int): Int`; `fun workoutExerciseType(typeKey: String): Int`; `fun stepsByLocalDay(samples: List<StepSample>): Map<String, Int>`.

- [ ] **Step 1: Write the failing test**

```kotlin
package com.jhon.mihealthreader.data.health

import androidx.health.connect.client.records.SleepStageRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import com.jhon.mihealthreader.parse.StepSample
import org.junit.Assert.assertEquals
import org.junit.Test

class HealthMappingsTest {
    @Test fun `sleep stages map like the parser`() {
        assertEquals(SleepStageRecord.STAGE_TYPE_AWAKE, sleepStageType(1))
        assertEquals(SleepStageRecord.STAGE_TYPE_AWAKE, sleepStageType(5))
        assertEquals(SleepStageRecord.STAGE_TYPE_DEEP, sleepStageType(2))
        assertEquals(SleepStageRecord.STAGE_TYPE_LIGHT, sleepStageType(3))
        assertEquals(SleepStageRecord.STAGE_TYPE_REM, sleepStageType(4))
        assertEquals(SleepStageRecord.STAGE_TYPE_UNKNOWN, sleepStageType(99))
    }

    @Test fun `workout types map to health connect`() {
        assertEquals(ExerciseSessionRecord.EXERCISE_TYPE_WALKING, workoutExerciseType("outdoor_walking"))
        assertEquals(ExerciseSessionRecord.EXERCISE_TYPE_RUNNING, workoutExerciseType("running"))
        assertEquals(ExerciseSessionRecord.EXERCISE_TYPE_BIKING, workoutExerciseType("indoor_riding"))
        assertEquals(ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING, workoutExerciseType("strength_training"))
        assertEquals(ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_POOL, workoutExerciseType("swimming"))
        assertEquals(ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT, workoutExerciseType("motion_sensing_game"))
        assertEquals(ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT, workoutExerciseType("unknown_key"))
    }

    @Test fun `steps aggregate per local day`() {
        val samples = listOf(
            StepSample("s", 0L, 0, 100, 0, 0),
            StepSample("s", 60_000L, 0, 250, 0, 0),
            StepSample("s", 86_400_000L, 0, 40, 0, 0),
        )
        val byDay = stepsByLocalDay(samples)
        assertEquals(350, byDay.values.first())
        assertEquals(40, byDay.values.last())
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.jhon.mihealthreader.data.health.HealthMappingsTest"`
Expected: FAIL to compile — `sleepStageType` / `workoutExerciseType` / `stepsByLocalDay` unresolved.

- [ ] **Step 3: Implement `HealthMappings.kt`**

```kotlin
package com.jhon.mihealthreader.data.health

import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.SleepStageRecord
import com.jhon.mihealthreader.parse.StepSample
import java.time.Instant
import java.time.ZoneId

fun sleepStageType(state: Int): Int = when (state) {
    1, 5 -> SleepStageRecord.STAGE_TYPE_AWAKE
    2 -> SleepStageRecord.STAGE_TYPE_DEEP
    3 -> SleepStageRecord.STAGE_TYPE_LIGHT
    4 -> SleepStageRecord.STAGE_TYPE_REM
    else -> SleepStageRecord.STAGE_TYPE_UNKNOWN
}

fun workoutExerciseType(typeKey: String): Int = when (typeKey) {
    "outdoor_walking", "walking" -> ExerciseSessionRecord.EXERCISE_TYPE_WALKING
    "outdoor_running", "indoor_running", "running" -> ExerciseSessionRecord.EXERCISE_TYPE_RUNNING
    "outdoor_riding", "indoor_riding" -> ExerciseSessionRecord.EXERCISE_TYPE_BIKING
    "climbing" -> ExerciseSessionRecord.EXERCISE_TYPE_HIKING
    "strength_training", "dumbbell_training", "free_training" -> ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING
    "swimming" -> ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_POOL
    "yoga" -> ExerciseSessionRecord.EXERCISE_TYPE_YOGA
    "elliptical" -> ExerciseSessionRecord.EXERCISE_TYPE_ELLIPTICAL
    "basketball" -> ExerciseSessionRecord.EXERCISE_TYPE_BASKETBALL
    "football" -> ExerciseSessionRecord.EXERCISE_TYPE_FOOTBALL_AMERICAN
    "badminton" -> ExerciseSessionRecord.EXERCISE_TYPE_BADMINTON
    "pingpong" -> ExerciseSessionRecord.EXERCISE_TYPE_TABLE_TENNIS
    "rope_skipping" -> ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT
    else -> ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT
}

fun stepsByLocalDay(samples: List<StepSample>, zone: ZoneId = ZoneId.systemDefault()): Map<String, Int> =
    samples.groupBy { Instant.ofEpochMilli(it.time).atZone(zone).toLocalDate().toString() }
        .mapValues { (_, s) -> s.sumOf { it.steps } }
```

- [ ] **Step 4: Run to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "com.jhon.mihealthreader.data.health.HealthMappingsTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/jhon/mihealthreader/data/health/HealthMappings.kt \
        app/src/test/java/com/jhon/mihealthreader/data/health/HealthMappingsTest.kt
git commit -m "feat(health): pure health connect mapping functions"
```

---

## Task 4: HealthConnectManager + permissions

**Files:**
- Create: `app/src/main/java/com/jhon/mihealthreader/data/health/HealthConnectManager.kt`
- Create: `app/src/main/java/com/jhon/mihealthreader/ui/health/PermissionsRationaleActivity.kt`
- Modify: `app/src/main/AndroidManifest.xml`

**Interfaces:**
- Consumes: Task 3's mappers; `LocalStore` (Task 2).
- Produces: `HealthConnectManager` (`@Singleton`) with `isAvailable()`, `hasAllPermissions()`, `sync()` (writes all metrics from `LocalStore`), `permissions`.

- [ ] **Step 1: Manager**

```kotlin
package com.jhon.mihealthreader.data.health

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.*
import com.jhon.mihealthreader.local.LocalStore
import com.jhon.mihealthreader.parse.MetricTypes
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HealthConnectManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val localStore: LocalStore,
) {
    val permissions: Set<String> = setOf(
        HealthPermission.getWritePermission(StepsRecord::class),
        HealthPermission.getWritePermission(HeartRateRecord::class),
        HealthPermission.getWritePermission(RestingHeartRateRecord::class),
        HealthPermission.getWritePermission(OxygenSaturationRecord::class),
        HealthPermission.getWritePermission(SleepSessionRecord::class),
        HealthPermission.getWritePermission(ExerciseSessionRecord::class),
    )

    fun isAvailable(): Boolean =
        HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE

    private fun client(): HealthConnectClient? =
        if (isAvailable()) HealthConnectClient.getOrCreate(context) else null

    suspend fun hasAllPermissions(): Boolean {
        val client = client() ?: return false
        return client.permissionController.getGrantedPermissions().containsAll(permissions)
    }

    /** Best-effort write of every stored metric; returns the number of records written. */
    suspend fun sync(): Int {
        val client = client() ?: return 0
        var written = 0
        suspend fun insert(records: List<Record>) {
            if (records.isEmpty()) return
            records.chunked(200).forEach { batch ->
                val ok = runCatching { client.insertRecords(batch) }
                    .onFailure { Log.w(TAG, "HC insert failed", it) }
                    .isSuccess
                if (ok) written += batch.size
            }
        }
        // Steps (per local day), heart rate, resting HR, SpO2, sleep, workouts — build records
        // from localStore.stepSamples()/sleepSessions()/workouts() and metric flows read via
        // byType(); use Task 3 mappers and Metadata.manualEntry(clientRecordId = "mihealth:<type>:<key>").
        return written
    }

    private companion object { const val TAG = "HealthConnectManager" }
}
```
Fill `sync()` using `localStore.stepSamples()`, `localStore.sleepSessions()`, `localStore.workouts()`; for HR/RestingHR/SpO2 use `localStore` metric accessors (add `fun metricSamples(type): List<MetricSample>` if missing). Build:
- `StepsRecord(startTime, startZoneOffset, endTime, endZoneOffset, count, metadata)` per local day.
- `HeartRateRecord(..., samples = List<HeartRateRecord.Sample>)` per local day (batch).
- `RestingHeartRateRecord(..., beatsPerMinute = Long)` per day.
- `OxygenSaturationRecord(..., percentage = Percentage)` per sample.
- `SleepSessionRecord(..., stages = List<SleepStageRecord>)` per session; `SleepStageRecord(startTime, startZoneOffset, endTime, endZoneOffset, stageType, metadata)`.
- `ExerciseSessionRecord(start, startOffset, end, endOffset, metadata, exerciseType, title)` per workout (see hackqing/Kenko note: the primary constructor is `internal`; use the public positional secondary constructor).

Verify each constructor against the SDK by compiling; adjust parameter names/required args per the compiler (as done for Kenko).

- [ ] **Step 2: Rationale activity + manifest**

Create `PermissionsRationaleActivity` (same minimal pattern as Kenko) and in `AndroidManifest.xml` add the six `WRITE_*` health permissions and the `ViewPermissionUsageActivity` alias + rationale activity. Add the string `label_health_connect_rationale`.

- [ ] **Step 3: Build**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/jhon/mihealthreader/data/health/HealthConnectManager.kt \
        app/src/main/java/com/jhon/mihealthreader/ui/health/PermissionsRationaleActivity.kt \
        app/src/main/AndroidManifest.xml app/src/main/res/values/strings.xml
git commit -m "feat(health): health connect manager and permissions"
```

---

## Task 5: Trigger HC writes + Settings UI

**Files:**
- Modify: `app/src/main/java/com/jhon/mihealthreader/ui/ExtractionViewModel.kt`
- Modify: `app/src/main/java/com/jhon/mihealthreader/ui/screens/SettingsScreen.kt`

**Interfaces:**
- Consumes: `HealthConnectManager` (Task 4).

- [ ] **Step 1: Auto-write after extraction**

Inject `HealthConnectManager` into `ExtractionViewModel`; after `localStore.save(...)` in `start()`, call `healthConnectManager.sync()` inside `runCatching` (never fail the extraction).

- [ ] **Step 2: Settings UI**

In `SettingsScreen`, when `healthConnectManager.isAvailable()` add a row: a switch (request `PermissionController.createRequestPermissionResultContract()` — NOT `RequestMultiplePermissions`) that on grant enables + calls `sync()`, and a "立即同步" action showing the written count. Persist an enabled flag in `BackendPreferences` (or a small `SettingsPreferences`), mirroring the existing prefs helper.

- [ ] **Step 3: Build + tests**

Run: `./gradlew :app:assembleDebug :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL; tests green.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/jhon/mihealthreader/ui/ExtractionViewModel.kt \
        app/src/main/java/com/jhon/mihealthreader/ui/screens/SettingsScreen.kt
git commit -m "feat(health): sync after extraction and manual sync in settings"
```

---

## Task 6: MD3 fallback colors

**Files:**
- Modify: `app/src/main/java/com/jhon/mihealthreader/ui/theme/Theme.kt`

- [ ] **Step 1: Replace the teal fallback with a Material baseline pair**

```kotlin
private val LightFallback = lightColorScheme()
private val DarkFallback = darkColorScheme()
```
(Keep the dynamic-color branch unchanged.) This makes pre-Android-12 devices use the Material 3 baseline instead of the custom teal.

- [ ] **Step 2: Build**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/jhon/mihealthreader/ui/theme/Theme.kt
git commit -m "style: use material baseline fallback colors"
```

---

## Task 7: CI

**Files:**
- Create: `.github/workflows/ci.yml`

- [ ] **Step 1: Add the workflow**

```yaml
name: CI
on: [push, pull_request]
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { distribution: temurin, java-version: '17' }
      - uses: gradle/actions/setup-gradle@v3
      - run: ./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

- [ ] **Step 2: Commit**

```bash
git add .github/workflows/ci.yml
git commit -m "ci: run unit tests, lint and assemble"
```

---

## Task 8: Verification

- [ ] **Step 1:** `./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug` → all pass.
- [ ] **Step 2:** Manual (device/emulator, never a data-holding device without a backup): extract once → HC sync writes the expected counts; re-sync does not duplicate; with HC absent the app works unchanged.
- [ ] **Step 3:** Update `docs/manual-verification.md` with the results; commit.

## Self-Review Notes

- Spec coverage: Hilt→Task 2; HC writes→Tasks 3-5; MD3→Task 6; export unchanged (no task, by design); CI/tests→Tasks 3,7,8; version alignment→Task 1.
- The `sync()` record construction in Task 4 is deliberately specified against the SDK's actual constructors (compile-driven), because Health Connect's constructor visibility differs per record type.
- `AppNav` signature change is the riskiest wiring step; keep its body identical and only change how dependencies arrive.
