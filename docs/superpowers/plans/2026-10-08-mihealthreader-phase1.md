# MihealthReader 实现计划（阶段 1）

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在已 root 的 Android 手机上，用 Kotlin + Jetpack Compose 应用把「小米运动健康」私有数据库提取为原始快照 + JSON，并在应用内浏览。

**Architecture:** 单 Gradle 模块 `:app`，包内按职责分层：`root`（libsu 封装）、`discover`（包/库定位）、`extract`（root 读取生成快照）、`dump`（SQLite→JSON + manifest）、`data`（导出文件管理）、`ui`（Compose + ViewModel）。所有设备相关依赖抽象为接口，便于用假实现做 JVM 单元测试。

**Tech Stack:** Kotlin 2.0.21、Jetpack Compose (Material 3)、libsu 5.2.2、AGP 8.7.3、Gradle 8.9、JUnit4 + Robolectric 4.14.1、协程。

**Spec:** `docs/superpowers/specs/2026-10-08-mihealthreader-design.md`

## Global Constraints

- 语言 Kotlin；UI 用 Jetpack Compose（Material 3）；单模块 `:app`。
- root 封装使用 `com.github.topjohnwu.libsu:core:5.2.2`（JitPack 仓库）。
- 签名使用默认 debug keystore（`signingConfigs.debug`），不自建签名。
- `applicationId = "com.jhon.mihealthreader"`；`minSdk 26`、`compileSdk 35`、`targetSdk 35`。
- 存储位置：应用内部私有存储 `context.filesDir/exports/<时间戳>/`。
- 每次导出同时产出 `raw/`（原始快照）与 `json/`（每表一个 JSON）+ `manifest.json`。
- 异步用协程；IO 操作在 `Dispatchers.IO`。
- 交付前必须通过 `./gradlew :app:lintDebug` 与 `./gradlew :app:testDebugUnitTest`。

## Review Focus

以下输入/失败模式最容易在真机使用中咬人，spec 未逐条展开，但每个都必须在拥有该代码的任务里被测试钉住：

1. 数据库不是 SQLite 或被加密（如 SQLCipher）→ 不得崩溃；保留原始快照并标记"不支持"。测试在 Task 5。
2. root 被拒绝或未授予 → 明确错误提示，不崩溃。测试在 Task 3（检测逻辑）与 Task 8（编排）。
3. 表名/列名是 SQL 关键字或含引号/空格 → 标识符正确加双引号转义。测试在 Task 5。
4. 空数据库或零行表 → 输出空数组，不崩溃。测试在 Task 5。
5. 超大表 → `rowLimit` 生效，内存可控。测试在 Task 5。
6. 重复点击提取 → 单飞，不并发。测试在 Task 8。

## File Structure

```
settings.gradle.kts
build.gradle.kts
gradle.properties
gradle/libs.versions.toml
gradle/wrapper/gradle-wrapper.properties (+ jar)
gradlew / gradlew.bat
.gitignore
local.properties                      (本机 SDK 路径，不提交)
app/build.gradle.kts
app/proguard-rules.pro
app/src/main/AndroidManifest.xml
app/src/main/java/com/jhon/mihealthreader/
  MainActivity.kt
  root/RootShell.kt                   ShellOutput + RootShell 接口 + 默认 root 检测
  root/LibsuRootShell.kt              libsu 实现
  discover/PackageScanner.kt          已安装包列表抽象
  discover/TargetLocator.kt           TargetApp + 候选定位
  extract/SourceReader.kt             读取 root 文件的抽象
  extract/DbExtractor.kt              Snapshot + 复制生成快照
  extract/SuSourceReader.kt           SuFileInputStream 实现
  dump/DbDumper.kt                    TableDump/DbDumpResult + SQLite→JSON
  dump/ManifestWriter.kt              ManifestData + 写 manifest.json
  data/ExportRepository.kt            快照列表/表清单/行读取
  ui/ExtractionViewModel.kt           提取编排
  ui/ExtractionState.kt               状态模型
  ui/BrowserViewModel.kt              浏览
  ui/AppNav.kt                        Compose 导航
  ui/screens/ExtractionScreen.kt
  ui/screens/TableListScreen.kt
  ui/screens/TableDetailScreen.kt
app/src/test/java/com/jhon/mihealthreader/
  root/RootShellTest.kt
  discover/TargetLocatorTest.kt
  extract/DbExtractorTest.kt
  dump/DbDumperTest.kt
  dump/ManifestWriterTest.kt
  data/ExportRepositoryTest.kt
  ui/ExtractionViewModelTest.kt
```

---

### Task 1: 工具链 + 项目脚手架

**Files:**
- Create: `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `gradle/libs.versions.toml`, `.gitignore`, `app/build.gradle.kts`, `app/proguard-rules.pro`, `app/src/main/AndroidManifest.xml`, `app/src/main/java/com/jhon/mihealthreader/MainActivity.kt`
- Create: `local.properties`（写入本机 SDK 路径，加入 .gitignore）

**Interfaces:**
- Produces: 可编译的 `:app` 模块，`applicationId`、SDK 版本、libsu/Compose 依赖就绪，`MainActivity` 显示 Compose 文本。

- [ ] **Step 1: 安装 JDK 17**

Run: `apt-get update && apt-get install -y openjdk-17-jdk`
Expected: `java -version` 输出 17.x。

- [ ] **Step 2: 安装 Android 命令行工具与 SDK 组件**

```bash
mkdir -p /opt/android-sdk/cmdline-tools
curl -L -o /tmp/cmdline-tools.zip https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
unzip -q /tmp/cmdline-tools.zip -d /opt/android-sdk/cmdline-tools
mv /opt/android-sdk/cmdline-tools/cmdline-tools /opt/android-sdk/cmdline-tools/latest
yes | /opt/android-sdk/cmdline-tools/latest/bin/sdkmanager --licenses
/opt/android-sdk/cmdline-tools/latest/bin/sdkmanager "platform-tools" "platforms;android-35" "build-tools;35.0.0"
```
Expected: `/opt/android-sdk/platforms/android-35` 存在。

- [ ] **Step 3: 安装 Gradle 8.9 并生成 wrapper**

```bash
curl -L -o /tmp/gradle.zip https://services.gradle.org/distributions/gradle-8.9-bin.zip
unzip -q /tmp/gradle.zip -d /opt
/opt/gradle-8.9/bin/gradle wrapper --gradle-version 8.9
```
Expected: 生成 `gradlew`、`gradle/wrapper/gradle-wrapper.jar`、`gradle-wrapper.properties`。

- [ ] **Step 4: 写版本目录 `gradle/libs.versions.toml`**

```toml
[versions]
agp = "8.7.3"
kotlin = "2.0.21"
coreKtx = "1.13.1"
lifecycle = "2.8.7"
activityCompose = "1.9.3"
composeBom = "2024.10.01"
navigationCompose = "2.8.4"
libsu = "5.2.2"
junit = "4.13.2"
robolectric = "4.14.1"
coroutines = "1.9.0"

[libraries]
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "coreKtx" }
androidx-lifecycle-runtime-ktx = { group = "androidx.lifecycle", name = "lifecycle-runtime-ktx", version.ref = "lifecycle" }
androidx-lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycle" }
androidx-activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activityCompose" }
androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
androidx-ui = { group = "androidx.compose.ui", name = "ui" }
androidx-ui-graphics = { group = "androidx.compose.ui", name = "ui-graphics" }
androidx-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
androidx-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
androidx-material3 = { group = "androidx.compose.material3", name = "material3" }
androidx-navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigationCompose" }
libsu-core = { group = "com.github.topjohnwu.libsu", name = "core", version.ref = "libsu" }
junit = { group = "junit", name = "junit", version.ref = "junit" }
robolectric = { group = "org.robolectric", name = "robolectric", version.ref = "robolectric" }
kotlinx-coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutines" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
```

- [ ] **Step 5: 写 `settings.gradle.kts`**

```kotlin
pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral(); maven("https://jitpack.io") }
}
rootProject.name = "MihealthReader"
include(":app")
```

- [ ] **Step 6: 写根 `build.gradle.kts`、`gradle.properties`、`.gitignore`**

根 `build.gradle.kts` 用 `plugins { alias(...) apply false }` 声明三个插件。`gradle.properties` 设 `android.useAndroidX=true`、`org.gradle.jvmargs=-Xmx2g`。`.gitignore` 含 `local.properties`、`.gradle/`、`build/`、`.idea/`。

- [ ] **Step 7: 写 `app/build.gradle.kts`**

应用 android-application / kotlin-android / kotlin-compose 插件；`namespace` 与 `applicationId = "com.jhon.mihealthreader"`；`minSdk 26 / compileSdk 35 / targetSdk 35`；`buildFeatures { compose = true }`；`testOptions.unitTests.isIncludeAndroidResources = true`；依赖见版本目录（`implementation` core-ktx、lifecycle、activity-compose、compose bom、ui、material3、navigation-compose、libsu-core；`testImplementation` junit、robolectric、coroutines-test）。构建时读取 `local.properties` 的 `sdk.dir`（AGP 自动读取）。

- [ ] **Step 8: 写 `AndroidManifest.xml` 与 `MainActivity.kt`**

Manifest 声明 `MainActivity` 为 launcher，无特殊权限。`MainActivity` 用 `setContent { Text("MihealthReader") }`。

- [ ] **Step 9: 验证构建**

Run: `./gradlew :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`，产出 `app/build/outputs/apk/debug/app-debug.apk`。

- [ ] **Step 10: 提交**

```bash
git add -A
git commit -m "chore: scaffold Android Compose project with libsu"
```

---

### Task 2: RootShell 抽象与 root 检测

**Files:**
- Create: `app/src/main/java/com/jhon/mihealthreader/root/RootShell.kt`, `app/src/main/java/com/jhon/mihealthreader/root/LibsuRootShell.kt`
- Test: `app/src/test/java/com/jhon/mihealthreader/root/RootShellTest.kt`

**Interfaces:**
- Produces:
  - `data class ShellOutput(val exitCode: Int, val stdout: List<String>, val stderr: List<String>)`
  - `interface RootShell { fun run(vararg commands: String): ShellOutput; fun isRootGranted(): Boolean }`，`isRootGranted()` 默认实现为 `run("id")` 且退出码 0 且 stdout 任一行含 `uid=0`。
  - `class LibsuRootShell : RootShell`，`run` 用 `Shell.cmd(*commands).exec()` 取 `out/err/code`。

- [ ] **Step 1: 写失败测试**

```kotlin
class RootShellTest {
    private fun fake(output: ShellOutput) = object : RootShell {
        override fun run(vararg commands: String) = output
    }
    @Test fun `root granted when id shows uid 0`() {
        val shell = fake(ShellOutput(0, listOf("uid=0(root) gid=0(root)"), emptyList()))
        assertTrue(shell.isRootGranted())
    }
    @Test fun `not granted when id shows normal uid`() {
        val shell = fake(ShellOutput(0, listOf("uid=2000(shell)"), emptyList()))
        assertFalse(shell.isRootGranted())
    }
    @Test fun `not granted when command fails`() {
        val shell = fake(ShellOutput(1, emptyList(), listOf("permission denied")))
        assertFalse(shell.isRootGranted())
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `./gradlew :app:testDebugUnitTest --tests "*RootShellTest"`
Expected: 编译失败（`RootShell`/`ShellOutput` 未定义）。

- [ ] **Step 3: 实现 `RootShell.kt` 与 `LibsuRootShell.kt`**

按 Interfaces 定义。`LibsuRootShell.run` 返回 `ShellOutput(result.code, result.out, result.err)`。

- [ ] **Step 4: 运行测试确认通过**

Run: `./gradlew :app:testDebugUnitTest --tests "*RootShellTest"`
Expected: PASS。

- [ ] **Step 5: 提交**

```bash
git add app/src/main/java/com/jhon/mihealthreader/root app/src/test/java/com/jhon/mihealthreader/root
git commit -m "feat: add RootShell abstraction and libsu implementation"
```

---

### Task 3: TargetLocator（包与数据库定位）

**Files:**
- Create: `app/src/main/java/com/jhon/mihealthreader/discover/PackageScanner.kt`, `app/src/main/java/com/jhon/mihealthreader/discover/TargetLocator.kt`
- Test: `app/src/test/java/com/jhon/mihealthreader/discover/TargetLocatorTest.kt`

**Interfaces:**
- Consumes: `RootShell`（Task 2）。
- Produces:
  - `fun interface PackageScanner { fun installedPackages(): List<String> }`
  - `data class TargetApp(val packageName: String, val databasesPath: String, val dbFiles: List<String>)`
  - `interface TargetFinder { fun findCandidates(): List<TargetApp> }`
  - `class TargetLocator(private val rootShell: RootShell, private val scanner: PackageScanner, private val priority: List<String> = DEFAULT_PRIORITY) : TargetFinder`
    - `DEFAULT_PRIORITY = listOf("com.xiaomi.wearable", "com.mi.health", "com.xiaomi.hm.health")`
    - `NAME_HINTS = listOf("xiaomi", "health", "wearable", "mifit", "zepp")`
    - 逻辑：候选 = 优先包 + 已安装包中名称命中 `NAME_HINTS` 者，去重并保持"优先包在前"。对每个候选执行 `rootShell.run("ls", "-1", "/data/data/<pkg>/databases")`；退出码 0 且 stdout 非空则构成 `TargetApp`（`databasesPath = "/data/data/<pkg>/databases"`，`dbFiles = stdout`）。

- [ ] **Step 1: 写失败测试**

```kotlin
@RunWith(JUnit4::class)
class TargetLocatorTest {
    private fun shellReturning(dirExistsFor: Set<String>) = object : RootShell {
        override fun run(vararg commands: String): ShellOutput {
            val path = commands.last()
            val pkg = path.removePrefix("/data/data/").removeSuffix("/databases")
            return if (pkg in dirExistsFor)
                ShellOutput(0, listOf("mi.db", "mi.db-wal"), emptyList())
            else ShellOutput(1, emptyList(), listOf("No such file"))
        }
    }
    @Test fun `finds priority package with databases`() {
        val locator = TargetLocator(shellReturning(setOf("com.xiaomi.wearable")), { emptyList() })
        val result = locator.findCandidates()
        assertEquals(listOf("com.xiaomi.wearable"), result.map { it.packageName })
        assertEquals(listOf("mi.db", "mi.db-wal"), result.first().dbFiles)
    }
    @Test fun `finds installed package by name hint`() {
        val locator = TargetLocator(shellReturning(setOf("com.acme.health")), { listOf("com.acme.health", "com.other.app") })
        assertEquals(listOf("com.acme.health"), locator.findCandidates().map { it.packageName })
    }
    @Test fun `ignores packages without databases dir`() {
        val locator = TargetLocator(shellReturning(emptySet()), { listOf("com.acme.health") })
        assertTrue(locator.findCandidates().isEmpty())
    }
    @Test fun `priority package comes first and dedups`() {
        val locator = TargetLocator(shellReturning(setOf("com.xiaomi.wearable", "com.acme.health")), { listOf("com.xiaomi.wearable", "com.acme.health") })
        assertEquals(listOf("com.xiaomi.wearable", "com.acme.health"), locator.findCandidates().map { it.packageName })
    }
}
```

- [ ] **Step 2: 运行确认失败**

Run: `./gradlew :app:testDebugUnitTest --tests "*TargetLocatorTest"`
Expected: 编译失败。

- [ ] **Step 3: 实现 `PackageScanner.kt` 与 `TargetLocator.kt`**

`TargetLocator` 收集候选（`LinkedHashSet`）、逐个 `ls`、过滤、映射为 `TargetApp`。

- [ ] **Step 4: 运行确认通过**

Run: `./gradlew :app:testDebugUnitTest --tests "*TargetLocatorTest"`
Expected: PASS。

- [ ] **Step 5: 提交**

```bash
git add app/src/main/java/com/jhon/mihealthreader/discover app/src/test/java/com/jhon/mihealthreader/discover
git commit -m "feat: locate Mi Fitness package and databases dir"
```

---

### Task 4: DbExtractor（root 读取生成快照）

**Files:**
- Create: `app/src/main/java/com/jhon/mihealthreader/extract/SourceReader.kt`, `app/src/main/java/com/jhon/mihealthreader/extract/DbExtractor.kt`, `app/src/main/java/com/jhon/mihealthreader/extract/SuSourceReader.kt`
- Test: `app/src/test/java/com/jhon/mihealthreader/extract/DbExtractorTest.kt`

**Interfaces:**
- Consumes: `TargetApp`（Task 3）。
- Produces:
  - `fun interface SourceReader { fun open(path: String): InputStream }`
  - `data class Snapshot(val directory: File, val sourcePackage: String, val files: List<File>)`
  - `interface Extractor { fun extract(target: TargetApp, exportsDir: File): Snapshot }`
  - `class DbExtractor(private val sourceReader: SourceReader, private val timestamp: () -> String = { java.time.LocalDateTime.now().toString() }) : Extractor`
    - 生成 `exportsDir/<timestamp>/raw/<pkg>/databases/`，对每个 `target.dbFiles` 用 `sourceReader.open("$databasesPath/$file")` 读取并写入目标文件。
  - `class SuSourceReader : SourceReader { override fun open(path) = SuFileInputStream(path) }`（libsu）。

- [ ] **Step 1: 写失败测试**

```kotlin
class DbExtractorTest {
    @get:Rule val tmp = TemporaryFolder()
    @Test fun `copies every file into timestamped snapshot`() {
        val content = mapOf(
            "/data/data/com.xiaomi.wearable/databases/mi.db" to "DB".toByteArray(),
            "/data/data/com.xiaomi.wearable/databases/mi.db-wal" to "WAL".toByteArray(),
        )
        val reader = SourceReader { path -> ByteArrayInputStream(content.getValue(path)) }
        val target = TargetApp("com.xiaomi.wearable", "/data/data/com.xiaomi.wearable/databases", listOf("mi.db", "mi.db-wal"))
        val snap = DbExtractor(reader) { "T1" }.extract(target, tmp.root)
        assertEquals(2, snap.files.size)
        assertTrue(snap.directory.endsWith(java.io.File("raw/com.xiaomi.wearable/databases")))
        assertEquals("DB", snap.files.first { it.name == "mi.db" }.readText())
    }
    @Test fun `propagates read failure`() {
        val reader = SourceReader { throw java.io.IOException("nope") }
        val target = TargetApp("p", "/data/data/p/databases", listOf("x.db"))
        assertThrows(java.io.IOException::class.java) { DbExtractor(reader) { "T1" }.extract(target, tmp.root) }
    }
}
```

- [ ] **Step 2: 运行确认失败**

Run: `./gradlew :app:testDebugUnitTest --tests "*DbExtractorTest"`
Expected: 编译失败。

- [ ] **Step 3: 实现 `SourceReader.kt`、`DbExtractor.kt`、`SuSourceReader.kt`**

`extract` 建目录、逐文件 `sourceReader.open(...).use { it.copyTo(FileOutputStream(dest)) }`，返回 `Snapshot`。

- [ ] **Step 4: 运行确认通过**

Run: `./gradlew :app:testDebugUnitTest --tests "*DbExtractorTest"`
Expected: PASS。

- [ ] **Step 5: 提交**

```bash
git add app/src/main/java/com/jhon/mihealthreader/extract app/src/test/java/com/jhon/mihealthreader/extract
git commit -m "feat: extract root-owned databases into snapshot"
```

---

### Task 5: DbDumper（SQLite → JSON）

**Files:**
- Create: `app/src/main/java/com/jhon/mihealthreader/dump/DbDumper.kt`
- Test: `app/src/test/java/com/jhon/mihealthreader/dump/DbDumperTest.kt`

**Interfaces:**
- Produces:
  - `data class TableDump(val name: String, val rowCount: Int, val jsonFile: File)`
  - `data class DbDumpResult(val dbName: String, val isSqlite: Boolean, val tables: List<TableDump>, val error: String? = null)`
  - `interface Dumper { fun dump(dbFile: File, jsonDir: File): DbDumpResult }`
  - `class DbDumper(private val rowLimit: Int = Int.MAX_VALUE) : Dumper`
    - `fun isSqlite(dbFile: File): Boolean`：读前 16 字节，等于 `"SQLite format 3\u0000"`。
    - `dump`：非 SQLite → 返回 `isSqlite=false, error="unsupported/encrypted"`，不抛异常。
    - 用 `SQLiteDatabase.openDatabase(path, null, OPEN_READONLY)`；表清单来自 `SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'`；每表 `SELECT * FROM "<name>"` 取行（上限 `rowLimit`）；写 `jsonDir/<table>.json`（`JSONArray`，列为键，类型：NULL→`JSONObject.NULL`，INTEGER→Long，FLOAT→Double，STRING→String，BLOB→base64）。标识符用双引号包裹并转义内部 `"` 为 `""`。

- [ ] **Step 1: 写失败测试（Robolectric）**

```kotlin
@RunWith(RobolectricTestRunner::class)
class DbDumperTest {
    @get:Rule val tmp = TemporaryFolder()
    private fun makeDb(name: String, block: (SQLiteDatabase) -> Unit): File {
        val f = File(tmp.root, name)
        val db = SQLiteDatabase.openOrCreateDatabase(f, null)
        block(db); db.close(); return f
    }
    @Test fun `dumps table rows to json`() {
        val db = makeDb("a.db") { it.execSQL("CREATE TABLE t_sleep(id INTEGER, stage TEXT)"); it.execSQL("INSERT INTO t_sleep VALUES (1,'deep')") }
        val out = File(tmp.root, "json").also { it.mkdirs() }
        val result = DbDumper().dump(db, out)
        assertTrue(result.isSqlite)
        val arr = JSONArray(File(out, "t_sleep.json").readText())
        assertEquals(1, arr.length())
        assertEquals("deep", arr.getJSONObject(0).getString("stage"))
    }
    @Test fun `non sqlite file is reported not crash`() {
        val bad = File(tmp.root, "bad.db").apply { writeText("not a database") }
        val result = DbDumper().dump(bad, tmp.root)
        assertFalse(result.isSqlite)
        assertNotNull(result.error)
    }
    @Test fun `empty table yields empty array`() {
        val db = makeDb("e.db") { it.execSQL("CREATE TABLE t(a INTEGER)") }
        val out = File(tmp.root, "json").also { it.mkdirs() }
        DbDumper().dump(db, out)
        assertEquals(0, JSONArray(File(out, "t.json").readText()).length())
    }
    @Test fun `quotes keyword table and column names`() {
        val db = makeDb("k.db") { it.execSQL("CREATE TABLE \"select\"(\"order\" INTEGER)"); it.execSQL("INSERT INTO \"select\" VALUES (7)") }
        val out = File(tmp.root, "json").also { it.mkdirs() }
        DbDumper().dump(db, out)
        assertEquals(7, JSONArray(File(out, "select.json").readText()).getJSONObject(0).getInt("order"))
    }
    @Test fun `respects row limit`() {
        val db = makeDb("l.db") { it.execSQL("CREATE TABLE t(a INTEGER)"); repeat(10) { i -> it.execSQL("INSERT INTO t VALUES ($i)") } }
        val out = File(tmp.root, "json").also { it.mkdirs() }
        DbDumper(rowLimit = 3).dump(db, out)
        assertEquals(3, JSONArray(File(out, "t.json").readText()).length())
    }
}
```

- [ ] **Step 2: 运行确认失败**

Run: `./gradlew :app:testDebugUnitTest --tests "*DbDumperTest"`
Expected: 编译失败。

- [ ] **Step 3: 实现 `DbDumper.kt`**

按 Interfaces 实现；`openDatabase` 用 `try/finally` 关闭；捕获异常写入 `error` 字段。

- [ ] **Step 4: 运行确认通过**

Run: `./gradlew :app:testDebugUnitTest --tests "*DbDumperTest"`
Expected: PASS。

- [ ] **Step 5: 提交**

```bash
git add app/src/main/java/com/jhon/mihealthreader/dump/DbDumper.kt app/src/test/java/com/jhon/mihealthreader/dump/DbDumperTest.kt
git commit -m "feat: dump sqlite tables to json"
```

---

### Task 6: ManifestWriter

**Files:**
- Create: `app/src/main/java/com/jhon/mihealthreader/dump/ManifestWriter.kt`
- Test: `app/src/test/java/com/jhon/mihealthreader/dump/ManifestWriterTest.kt`

**Interfaces:**
- Consumes: `DbDumpResult`（Task 5）。
- Produces:
  - `data class ManifestData(val exportedAt: String, val sourcePackage: String, val androidVersion: Int, val appVersion: String, val databases: List<DbDumpResult>)`
  - `class ManifestWriter { fun write(data: ManifestData, dest: File) }`
  - 输出 JSON 结构：`exportedAt/sourcePackage/androidVersion/appVersion/databases[]`，每个 database 含 `name/isSqlite/error/tables[]`，每个 table 含 `name/rowCount/jsonFile`（`jsonFile` 为相对快照目录的路径）。

- [ ] **Step 1: 写失败测试**

```kotlin
@RunWith(RobolectricTestRunner::class)
class ManifestWriterTest {
    @get:Rule val tmp = TemporaryFolder()
    @Test fun `writes manifest with tables`() {
        val data = ManifestData("2026-10-08T00:00:00Z", "com.xiaomi.wearable", 34, "0.1.0",
            listOf(DbDumpResult("mi.db", true, listOf(TableDump("t_sleep", 3, File("json/mi.db/t_sleep.json"))))))
        val dest = File(tmp.root, "manifest.json")
        ManifestWriter().write(data, dest)
        val root = JSONObject(dest.readText())
        assertEquals("com.xiaomi.wearable", root.getString("sourcePackage"))
        assertEquals(3, root.getJSONArray("databases").getJSONObject(0).getJSONArray("tables").getJSONObject(0).getInt("rowCount"))
    }
    @Test fun `writes unsupported database entry`() {
        val data = ManifestData("t", "p", 34, "0.1.0", listOf(DbDumpResult("x.db", false, emptyList(), "unsupported/encrypted")))
        val dest = File(tmp.root, "manifest.json")
        ManifestWriter().write(data, dest)
        assertEquals("unsupported/encrypted", JSONObject(dest.readText()).getJSONArray("databases").getJSONObject(0).getString("error"))
    }
}
```

- [ ] **Step 2: 运行确认失败**

Run: `./gradlew :app:testDebugUnitTest --tests "*ManifestWriterTest"`
Expected: 编译失败。

- [ ] **Step 3: 实现 `ManifestWriter.kt`**

用 `org.json` 构造并 `dest.writeText(json.toString(2))`。

- [ ] **Step 4: 运行确认通过**

Run: `./gradlew :app:testDebugUnitTest --tests "*ManifestWriterTest"`
Expected: PASS。

- [ ] **Step 5: 提交**

```bash
git add app/src/main/java/com/jhon/mihealthreader/dump/ManifestWriter.kt app/src/test/java/com/jhon/mihealthreader/dump/ManifestWriterTest.kt
git commit -m "feat: write export manifest"
```

---

### Task 7: ExportRepository

**Files:**
- Create: `app/src/main/java/com/jhon/mihealthreader/data/ExportRepository.kt`
- Test: `app/src/test/java/com/jhon/mihealthreader/data/ExportRepositoryTest.kt`

**Interfaces:**
- Consumes: `ManifestData`（Task 6）。
- Produces:
  - `data class TableEntry(val dbName: String, val name: String, val rowCount: Int, val jsonFile: File)`
  - `class ExportRepository(private val exportsDir: File)`
    - `fun listSnapshots(): List<File>`：子目录按名倒序（最新在前）。
    - `fun latestSnapshot(): File?`
    - `fun tables(snapshot: File): List<TableEntry>`：解析 `manifest.json`，`jsonFile` 相对路径解析为绝对文件。
    - `fun rows(entry: TableEntry): List<JSONObject>`：读表 JSON 文件返回对象列表。

- [ ] **Step 1: 写失败测试**

```kotlin
@RunWith(RobolectricTestRunner::class)
class ExportRepositoryTest {
    @get:Rule val tmp = TemporaryFolder()
    private fun snapshot(name: String): File {
        val dir = File(tmp.root, name).apply { mkdirs() }
        File(dir, "json/mi.db").mkdirs()
        File(dir, "json/mi.db/t_sleep.json").writeText("""[{"id":1,"stage":"deep"}]""")
        File(dir, "manifest.json").writeText("""{"databases":[{"name":"mi.db","tables":[{"name":"t_sleep","rowCount":1,"jsonFile":"json/mi.db/t_sleep.json"}]}]}""")
        return dir
    }
    @Test fun `lists snapshots newest first`() {
        snapshot("2026-01-01T00-00-00"); snapshot("2026-02-01T00-00-00")
        val repo = ExportRepository(tmp.root)
        assertEquals("2026-02-01T00-00-00", repo.latestSnapshot()!!.name)
    }
    @Test fun `reads table entries and rows`() {
        val s = snapshot("2026-01-01T00-00-00")
        val repo = ExportRepository(tmp.root)
        val entry = repo.tables(s).single()
        assertEquals("t_sleep", entry.name)
        assertEquals(1, entry.rowCount)
        assertEquals("deep", repo.rows(entry).single().getString("stage"))
    }
}
```

- [ ] **Step 2: 运行确认失败**

Run: `./gradlew :app:testDebugUnitTest --tests "*ExportRepositoryTest"`
Expected: 编译失败。

- [ ] **Step 3: 实现 `ExportRepository.kt`**

解析 manifest 时用 `File(snapshot, relativePath)`；`rows` 用 `JSONArray(readText())` 转 `List<JSONObject>`。

- [ ] **Step 4: 运行确认通过**

Run: `./gradlew :app:testDebugUnitTest --tests "*ExportRepositoryTest"`
Expected: PASS。

- [ ] **Step 5: 提交**

```bash
git add app/src/main/java/com/jhon/mihealthreader/data app/src/test/java/com/jhon/mihealthreader/data
git commit -m "feat: add export repository for browsing snapshots"
```

---

### Task 8: ExtractionViewModel（编排）

**Files:**
- Create: `app/src/main/java/com/jhon/mihealthreader/ui/ExtractionState.kt`, `app/src/main/java/com/jhon/mihealthreader/ui/ExtractionViewModel.kt`
- Test: `app/src/test/java/com/jhon/mihealthreader/ui/ExtractionViewModelTest.kt`

**Interfaces:**
- Consumes: `RootShell`（T2）、`TargetFinder`/`TargetApp`（T3）、`Extractor`/`Snapshot`（T4）、`Dumper`/`DbDumpResult`（T5）、`ManifestWriter`/`ManifestData`（T6）、`ExportRepository`（T7）。
- Produces:
  - `sealed interface ExtractionState { object Idle; data class Running(val step: String); data class Success(val snapshot: File, val tableCount: Int); data class Error(val message: String) }`
  - `class ExtractionViewModel(rootShell, targetFinder, extractor, dumper, manifestWriter, repository, androidVersion: Int, appVersion: String, exportsDir: File, io: CoroutineDispatcher = Dispatchers.IO) : ViewModel()`
    - `val state: StateFlow<ExtractionState>`
    - `fun start()`：若已在运行则忽略（单飞）；流程 = 检测 root → 找候选（空则 Error）→ 取第一个 → `extractor.extract` → 对 `raw/<pkg>/databases/` 下每个 `.db` `dumper.dump` → `manifestWriter.write` → `Success`。任一步异常 → `Error(message)`。
    - 用 `viewModelScope.launch(io)`，状态经 `MutableStateFlow` 更新。

- [ ] **Step 1: 写失败测试**

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class ExtractionViewModelTest {
    @get:Rule val tmp = TemporaryFolder()
    private val main = StandardTestDispatcher()
    @Before fun setUp() = Dispatchers.setMain(main)
    @After fun tearDown() = Dispatchers.resetMain()

    private fun vm(root: RootShell, finder: TargetFinder, extractor: Extractor, dumper: Dumper, writer: ManifestWriter) =
        ExtractionViewModel(root, finder, extractor, dumper, writer, ExportRepository(tmp.root), 34, "0.1.0", tmp.root, main)

    @Test fun `errors when root denied`() = runTest(main) {
        val vm = vm(object : RootShell { override fun run(vararg c: String) = ShellOutput(1, emptyList(), listOf("denied")) }, { emptyList() }, { _, _ -> error("x") }, { _, _ -> error("x") }, ManifestWriter())
        vm.start(); advanceUntilIdle()
        assertTrue(vm.state.value is ExtractionState.Error)
    }
    @Test fun `errors when no target found`() = runTest(main) {
        val vm = vm(granted(), { emptyList() }, { _, _ -> error("x") }, { _, _ -> error("x") }, ManifestWriter())
        vm.start(); advanceUntilIdle()
        assertTrue(vm.state.value is ExtractionState.Error)
    }
    @Test fun `success writes manifest and reports table count`() = runTest(main) {
        val raw = File(tmp.root, "raw/com.xiaomi.wearable/databases").apply { mkdirs() }
        File(raw, "mi.db").writeText("x")
        val extractor = Extractor { _, _ -> Snapshot(raw.parentFile.parentFile, "com.xiaomi.wearable", listOf(File(raw, "mi.db"))) }
        val dumper = Dumper { db, _ -> DbDumpResult(db.name, true, listOf(TableDump("t", 2, File("json/${db.name}/t.json")))) }
        val vm = vm(granted(), { listOf(TargetApp("com.xiaomi.wearable", "/data/data/com.xiaomi.wearable/databases", listOf("mi.db"))) }, extractor, dumper, ManifestWriter())
        vm.start(); advanceUntilIdle()
        val s = vm.state.value as ExtractionState.Success
        assertEquals(1, s.tableCount)
        assertTrue(File(s.snapshot, "manifest.json").exists())
    }
    @Test fun `ignores duplicate start while running`() = runTest(main) {
        var calls = 0
        val finder = TargetFinder { calls++; emptyList() }
        val vm = vm(granted(), finder, { _, _ -> error("x") }, { _, _ -> error("x") }, ManifestWriter())
        vm.start(); vm.start(); advanceUntilIdle()
        assertEquals(1, calls)
    }
}
```

（`granted()` 为返回 `ShellOutput(0, listOf("uid=0(root)"), emptyList())` 的 `RootShell` 假实现。）

- [ ] **Step 2: 运行确认失败**

Run: `./gradlew :app:testDebugUnitTest --tests "*ExtractionViewModelTest"`
Expected: 编译失败。

- [ ] **Step 3: 实现 `ExtractionState.kt` 与 `ExtractionViewModel.kt`**

用 `AtomicBoolean`/`Job` 实现单飞；在 `Dispatchers.IO` 上执行阻塞 IO。

- [ ] **Step 4: 运行确认通过**

Run: `./gradlew :app:testDebugUnitTest --tests "*ExtractionViewModelTest"`
Expected: PASS。

- [ ] **Step 5: 提交**

```bash
git add app/src/main/java/com/jhon/mihealthreader/ui app/src/test/java/com/jhon/mihealthreader/ui
git commit -m "feat: orchestrate extraction in view model"
```

---

### Task 9: Compose UI 与装配

**Files:**
- Create: `app/src/main/java/com/jhon/mihealthreader/ui/AppNav.kt`, `app/src/main/java/com/jhon/mihealthreader/ui/BrowserViewModel.kt`, `app/src/main/java/com/jhon/mihealthreader/ui/screens/ExtractionScreen.kt`, `app/src/main/java/com/jhon/mihealthreader/ui/screens/TableListScreen.kt`, `app/src/main/java/com/jhon/mihealthreader/ui/screens/TableDetailScreen.kt`
- Modify: `app/src/main/java/com/jhon/mihealthreader/MainActivity.kt`

**Interfaces:**
- Consumes: `ExtractionViewModel`/`ExtractionState`（T8）、`ExportRepository`/`TableEntry`（T7）、`RootShell`/`LibsuRootShell`（T2）、`TargetLocator`/`PackageScanner`（T3）、`DbExtractor`/`SuSourceReader`（T4）、`DbDumper`（T5）、`ManifestWriter`（T6）。
- Produces:
  - `class BrowserViewModel(repository, snapshot: File) : ViewModel()`，暴露 `tables: StateFlow<List<TableEntry>>` 与 `fun rows(entry): List<JSONObject>`。
  - `AppNav()`：两个路由 `extract` 与 `browse/{snapshot}`，从提取页成功后进入浏览。
  - 三个 Screen：提取页（root 状态 + 开始按钮 + 状态/错误）、表列表页（表名/行数，点击进入详情）、表详情页（行键值列表）。

- [ ] **Step 1: 装配依赖与 MainActivity**

在 `MainActivity` 里构造真实依赖：`LibsuRootShell()`、`TargetLocator(shell, { packageManager.getInstalledPackages(0).map { it.packageName } })`、`DbExtractor(SuSourceReader())`、`DbDumper()`、`ManifestWriter()`、`ExportRepository(File(filesDir, "exports"))`，注入 `ExtractionViewModel`。

- [ ] **Step 2: 实现 `AppNav.kt` 与三个 Screen**

提取页订阅 `state`：`Idle` 显示开始按钮，`Running` 显示步骤，`Success` 显示表数量并可跳转浏览，`Error` 显示消息。列表/详情页从 `ExportRepository` 读取。

- [ ] **Step 3: 构建验证**

Run: `./gradlew :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`。

- [ ] **Step 4: 静态检查**

Run: `./gradlew :app:lintDebug`
Expected: `BUILD SUCCESSFUL`（无 error；warning 可接受）。

- [ ] **Step 5: 提交**

```bash
git add -A
git commit -m "feat: compose UI for extraction and table browsing"
```

---

### Task 10: 全量测试与真机验证清单

**Files:**
- Create: `docs/manual-verification.md`（真机验证清单）

- [ ] **Step 1: 跑全部单元测试**

Run: `./gradlew :app:testDebugUnitTest`
Expected: 全部 PASS。

- [ ] **Step 2: 写真机验证清单**

清单包含：安装 `app-debug.apk` → 打开显示 root 状态 → 点开始提取 → 授权 root → 看到表数量 → 浏览表/行 → 用
`adb exec-out run-as com.jhon.mihealthreader tar -c files/exports | tar -x` 取出并校验 JSON 行数与源库一致 →
记录目标包名与是否加密（回填 spec 第 9 节风险项）。

- [ ] **Step 3: 提交**

```bash
git add docs/manual-verification.md
git commit -m "docs: add on-device verification checklist"
```

---

## Self-Review

- **Spec coverage：** 目标/成功标准→T1、T9、T10；技术栈→T1；架构分层→T2–T9；数据流→T3–T8；存储/JSON/manifest→T4–T7；错误处理（root 拒绝→T2/T8；包未找到→T3/T8；非 SQLite/加密→T5；复制失败→T4；大表→T5；重复触发→T8）；测试策略→T2–T8 单测 + T10 真机；阶段 0→T1；阶段 1→T2–T10。无遗漏。
- **类型一致性：** `TargetApp`/`Snapshot`/`DbDumpResult`/`TableEntry`/`ManifestData` 在各任务间签名一致；`TargetFinder`/`Extractor`/`Dumper` 接口在 T3/T4/T5 定义、T8 消费。
- **Review Focus：** 6 项均已在对应任务加入测试（T5×5、T2/T8 root、T8 单飞）。
- **规模：** 计划以接口与测试断言为主，未逐行转录实现代码。
