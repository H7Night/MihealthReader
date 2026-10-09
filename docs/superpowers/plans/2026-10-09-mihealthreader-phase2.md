# MihealthReader 阶段 2 实现计划（定向解析 + 界面）

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 从快照原始库 `fitness_data` 解析睡眠与步数，产出规范化 JSON，并提供活动/睡眠友好界面。

**Architecture:** 新增 `parse/` 包（模型、SleepParser、ActivityParser、Normalizer），在 `ExtractionViewModel` 导出流程末尾调用 `Normalizer` 写 `parsed/normalized.json`；UI 经 `ParsedRepository` 读取并展示。

**Tech Stack:** Kotlin、Jetpack Compose、Android SQLite、org.json、JUnit4 + Robolectric。

**Spec:** `docs/superpowers/specs/2026-10-09-mihealthreader-phase2-design.md`

## Global Constraints

- 阶段 1 的 `RootShell`/`LibsuRootShell`（`FLAG_MOUNT_MASTER`）、`Snapshot`、`ExportRepository` 保持不变。
- 规范化输出写入 `<快照>/parsed/normalized.json`，信封含 `schema_version`（`1.0`）、`source`（`mihealthreader`）、`generated_at`、`records`。
- 睡眠阶段码映射 `1=awake/2=light/3=deep/4=rem/其它=unknown`，并保留原始 `state`。
- 步数按本地日聚合，先做分钟切片去重（同分钟取较大 `steps`）。
- 跳过 `step_record.isDeleted == 1` 及无法解析的行。
- 交付前必须通过 `./gradlew :app:testDebugUnitTest` 与 `:app:lintDebug`。
- 每个任务独立提交。

## Review Focus

1. `sleep_segment.value` 缺字段 → 返回 null 跳过，不崩溃。测试在 Task 2A-2。
2. `step_record` 同分钟多设备重叠 → 只计较大值。测试在 Task 2A-3。
3. 快照中不存在 `fitness_data` → Normalizer 返回 0 且不抛异常，通用导出不受影响。测试在 Task 2A-4。
4. 跨本地日/时区 → 用 `zoneOffsetInSec` 归入正确日期。测试在 Task 2A-3。
5. `normalized.json` 不存在（尚未解析）→ 界面显示空态。测试在 Task 2B-1。

---

### Task 2A-1: 模型与规范化信封

**Files:**
- Create: `app/src/main/java/com/jhon/mihealthreader/parse/ParsedModels.kt`, `app/src/main/java/com/jhon/mihealthreader/parse/Normalizer.kt`
- Test: `app/src/test/java/com/jhon/mihealthreader/parse/NormalizerTest.kt`

**Interfaces:**
- Produces:
  - `data class SleepStage(val start: Long, val end: Long, val state: Int, val stateName: String)`
  - `data class SleepSession(val start: Long, val end: Long, val durationMinutes: Int, val deepMinutes: Int, val lightMinutes: Int, val avgHr: Int?, val minHr: Int?, val maxHr: Int?, val stages: List<SleepStage>)`
  - `data class StepSample(val time: Long, val zoneOffsetSec: Int, val steps: Int, val distanceMeters: Int, val calories: Int)`
  - `data class DailyActivity(val date: String, val steps: Int, val distanceMeters: Int, val calories: Int)`
  - `data class NormalizeResult(val sleepSessions: Int, val activityDays: Int, val skipped: Int, val outputFile: File)`
  - `fun writeNormalizedEnvelope(sleep: List<SleepSession>, activity: List<DailyActivity>, skipped: Int, dest: File, generatedAt: String)`

- [ ] **Step 1: 写失败测试**

```kotlin
@RunWith(RobolectricTestRunner::class)
class NormalizerTest {
    @get:Rule val tmp = TemporaryFolder()
    @Test fun `writes envelope with sleep and activity`() {
        val dest = File(tmp.root, "parsed/normalized.json")
        writeNormalizedEnvelope(
            sleep = listOf(SleepSession(100, 200, 2, 1, 1, 60, 50, 70, listOf(SleepStage(100, 160, 3, "deep")))),
            activity = listOf(DailyActivity("2026-06-12", 100, 80, 5)),
            skipped = 1, dest = dest, generatedAt = "2026-06-12T00:00:00Z",
        )
        val root = JSONObject(dest.readText())
        assertEquals("1.0", root.getString("schema_version"))
        assertEquals("mihealthreader", root.getString("source"))
        assertEquals(1, root.getJSONObject("records").getJSONArray("sleep").length())
        assertEquals(100, root.getJSONObject("records").getJSONArray("daily_activity").getJSONObject(0).getInt("steps"))
    }
}
```

- [ ] **Step 2: 运行确认失败** — `./gradlew :app:testDebugUnitTest --tests "*NormalizerTest"`
- [ ] **Step 3: 实现 `ParsedModels.kt` 与 `Normalizer.kt` 的 `writeNormalizedEnvelope`**（org.json 写信封；`records.sleep[]` 字段 `start/end/duration_min/deep_min/light_min/avg_hr/min_hr/max_hr/stages[]`；`records.daily_activity[]` 字段 `date/steps/distance_m/active_kcal`；顶层 `skipped`）。
- [ ] **Step 4: 运行确认通过。**
- [ ] **Step 5: 提交** `feat: add parsed models and normalized envelope`

---

### Task 2A-2: SleepParser

**Files:**
- Create: `app/src/main/java/com/jhon/mihealthreader/parse/SleepParser.kt`
- Test: `app/src/test/java/com/jhon/mihealthreader/parse/SleepParserTest.kt`

**Interfaces:**
- Produces: `object SleepParser { fun stateName(state: Int): String; fun parseValue(valueJson: String): SleepSession?; fun parse(db: SQLiteDatabase): List<SleepSession> }`

- [ ] **Step 1: 写失败测试**

```kotlin
@RunWith(RobolectricTestRunner::class)
class SleepParserTest {
    @Test fun `parses value into session`() {
        val value = """{"avg_hr":50,"min_hr":45,"max_hr":67,"bedtime":1000,"device_wake_up_time":1060,"duration":1,"sleep_deep_duration":1,"sleep_light_duration":0,"items":[{"start_time":1000,"end_time":1060,"state":3}]}"""
        val s = SleepParser.parseValue(value)!!
        assertEquals(1000L, s.start); assertEquals(1060L, s.end)
        assertEquals("deep", s.stages.single().stateName)
        assertEquals(50, s.avgHr)
    }
    @Test fun `returns null on invalid json`() {
        assertNull(SleepParser.parseValue("not json"))
    }
    @Test fun `state name mapping`() {
        assertEquals("awake", SleepParser.stateName(1))
        assertEquals("light", SleepParser.stateName(2))
        assertEquals("deep", SleepParser.stateName(3))
        assertEquals("rem", SleepParser.stateName(4))
        assertEquals("unknown", SleepParser.stateName(9))
    }
}
```

- [ ] **Step 2: 运行确认失败。**
- [ ] **Step 3: 实现 `SleepParser`**（`parseValue` 容错，缺 `items` 时阶段为空；`parse(db)` 查询 `sleep_segment` 的 `value` 列，跳过 null/空/解析失败）。
- [ ] **Step 4: 运行确认通过。**
- [ ] **Step 5: 提交** `feat: parse sleep sessions`

---

### Task 2A-3: ActivityParser（每日聚合 + 分钟去重）

**Files:**
- Create: `app/src/main/java/com/jhon/mihealthreader/parse/ActivityParser.kt`
- Test: `app/src/test/java/com/jhon/mihealthreader/parse/ActivityParserTest.kt`

**Interfaces:**
- Produces: `object ActivityParser { fun localDate(time: Long, zoneOffsetSec: Int): String; fun parseSamples(db: SQLiteDatabase): List<StepSample>; fun aggregate(samples: List<StepSample>): List<DailyActivity> }`

- [ ] **Step 1: 写失败测试**

```kotlin
class ActivityParserTest {
    @Test fun `dedups same minute keeping larger steps`() {
        val out = ActivityParser.aggregate(listOf(
            StepSample(0, 0, 10, 5, 1),
            StepSample(0, 0, 30, 20, 2),      // same minute, keep this
            StepSample(60, 0, 5, 3, 1),
        ))
        assertEquals(1, out.size)
        assertEquals(35, out.single().steps)
    }
    @Test fun `separates local days by zone offset`() {
        // 1970-01-01T23:30:00Z with +8h offset -> local 1970-01-02
        val t = 23L * 3600 + 30 * 60
        assertEquals("1970-01-02", ActivityParser.localDate(t, 28800))
    }
}
```

- [ ] **Step 2: 运行确认失败。**
- [ ] **Step 3: 实现**：`aggregate` 先按 `minuteKey = (time + zoneOffsetSec)/60` 分组，取 `steps` 最大者，再按 `localDate` 求和 `steps/distance/calories`；结果按日期升序。`parseSamples(db)` 读 `step_record`，过滤 `isDeleted != 1`，解析 `value`。
- [ ] **Step 4: 运行确认通过。**
- [ ] **Step 5: 提交** `feat: aggregate daily activity with minute dedup`

---

### Task 2A-4: Normalizer 接入提取流程

**Files:**
- Modify: `app/src/main/java/com/jhon/mihealthreader/parse/Normalizer.kt`（加 `fun normalize(snapshotRoot: File): NormalizeResult`）
- Modify: `app/src/main/java/com/jhon/mihealthreader/ui/ExtractionViewModel.kt`
- Modify: `app/src/test/java/com/jhon/mihealthreader/ui/ExtractionViewModelTest.kt`
- Test: `app/src/test/java/com/jhon/mihealthreader/parse/NormalizeIntegrationTest.kt`

**Interfaces:**
- Consumes: `SleepParser`、`ActivityParser`、`writeNormalizedEnvelope`。
- Produces: `Normalizer.normalize(snapshotRoot): NormalizeResult`：在 `snapshotRoot/raw` 下查找名为 `fitness_data` 的文件；只读打开；解析两表；写 `snapshotRoot/parsed/normalized.json`；找不到文件时返回 `NormalizeResult(0,0,0, outputFile=不存在)` 且不抛异常。

- [ ] **Step 1: 写失败测试**（见 `NormalizeIntegrationTest`）：`normalize` 在无 `fitness_data` 的目录返回 0 不抛；在有 fixture 库时输出会话/天数。
- [ ] **Step 2: 运行确认失败。**
- [ ] **Step 3: 实现 `normalize` 并接入 VM**：VM 在写 `manifest.json` 后调用 `normalizer.normalize(snapshot.root)`，将 `NormalizeResult` 记入日志/可选状态；VM 构造新增 `normalizer: Normalizer = Normalizer()`（带默认值以兼容既有测试）。
- [ ] **Step 4: 运行全部单测确认通过。**
- [ ] **Step 5: 真机复测**：重装应用，用既有快照重新解析，核对 `normalized.json` 的睡眠会话数、活动天数与源库量级。
- [ ] **Step 6: 提交** `feat: wire normalizer into extraction`

---

### Task 2B-1: 活动视图

**Files:**
- Create: `app/src/main/java/com/jhon/mihealthreader/data/ParsedRepository.kt`, `app/src/main/java/com/jhon/mihealthreader/ui/screens/ActivityScreen.kt`
- Modify: `app/src/main/java/com/jhon/mihealthreader/ui/AppNav.kt`、`ExtractionScreen.kt`
- Test: `app/src/test/java/com/jhon/mihealthreader/data/ParsedRepositoryTest.kt`

**Interfaces:**
- Produces: `class ParsedRepository(exportsDir: File) { fun latestNormalized(): File?; fun dailyActivity(): List<DailyActivity>; fun sleepSessions(): List<SleepSession> }`；`ActivityScreen(activity: List<DailyActivity>, onBack)`。

- [ ] **Step 1: 写失败测试**（`ParsedRepository` 解析 `normalized.json` 返回活动/睡眠；缺文件返回空）。
- [ ] **Step 2: 运行确认失败。**
- [ ] **Step 3: 实现 `ParsedRepository` 与 `ActivityScreen`，导航新增 `activity` 路由，成功页加"活动"入口。**
- [ ] **Step 4: 运行确认通过 + `assembleDebug`。**
- [ ] **Step 5: 提交** `feat: daily activity screen`

---

### Task 2B-2: 睡眠视图

**Files:**
- Create: `app/src/main/java/com/jhon/mihealthreader/ui/screens/SleepScreen.kt`
- Modify: `app/src/main/java/com/jhon/mihealthreader/ui/AppNav.kt`、`ExtractionScreen.kt`

**Interfaces:**
- Produces: `SleepScreen(sessions: List<SleepSession>, onBack)`：列出会话（日期、时长、深睡/浅睡、心率），阶段时间线以文字/颜色区分。

- [ ] **Step 1: 实现 `SleepScreen` 并加 `sleep` 路由与"睡眠"入口。**
- [ ] **Step 2: `assembleDebug` + `lintDebug` + 全量单测通过。**
- [ ] **Step 3: 真机复测两个视图。**
- [ ] **Step 4: 提交** `feat: sleep screen`

---

## Self-Review

- **Spec coverage**：数据源映射→2A-2/2A-3；信封/字段→2A-1；睡眠阶段映射→2A-2；分钟去重→2A-3；架构接入→2A-4；界面→2B-1/2B-2；错误处理→2A-2/2A-4/2B-1；测试→各任务。
- **类型一致性**：`SleepSession`/`DailyActivity`/`StepSample`/`NormalizeResult` 在 2A-1 定义，2A-2/2A-3/2A-4/2B 消费，签名一致。
- **Review Focus**：5 项均有对应测试。
- **规模**：以接口、测试断言为主，未逐行转录实现。
