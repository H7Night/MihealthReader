# MihealthReader 本地存储与增量同步实现计划（阶段 3）

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax.

**Goal:** 提取结果持久化到 App 本地 Room 库；启动即读；手动提取只做增量去重写入。

**Architecture:** 新增 `local/` 包（Room 实体/DAO/DB/`LocalStore`）。`Normalizer` 返回解析数据，`ExtractionViewModel` 调用 `LocalStore.save` 增量入库并刷新摘要；活动/睡眠界面改为读本地库，调试浏览器仍读快照。

**Tech Stack:** Kotlin、Jetpack Compose、Room 2.6.1 + KSP、JUnit4 + Robolectric。

**Spec:** `docs/superpowers/specs/2026-10-09-mihealthreader-local-store-design.md`

## Global Constraints

- 数据库 `mihealth.db`，version 1，`exportSchema=false`。
- `step_sample`/`sleep_stage` 用 `IGNORE`（append-only），`sleep_session` 用 `REPLACE`（覆盖最新），`sync_state` 每次提取更新。
- `StepSample` 增加 `sourceId`。
- 保留快照 + `normalized.json` 与调试浏览器。
- 交付前通过 `./gradlew :app:testDebugUnitTest` 与 `:app:lintDebug`。
- 每个任务独立提交。

## Review Focus

1. 重复保存同一批数据 → 第二次新增为 0（幂等）。测试在 3-1/3-4。
2. 同一睡眠 `start` 修订 → 覆盖不重复。测试在 3-1。
3. 本地库为空时启动 → 空态不崩溃。测试在 3-5/3-6。
4. 提取无新增 → `Success.newRecords == 0`，显示"无新数据"。测试在 3-5。
5. `sourceId` 缺失的历史样本 → 去重键兜底不冲突。测试在 3-2。

---

### Task 3-1: Room 库 + 实体/DAO + 测试

**Files:**
- Modify: `gradle/libs.versions.toml`, `build.gradle.kts`, `app/build.gradle.kts`
- Create: `app/src/main/java/com/jhon/mihealthreader/local/Entities.kt`, `Daos.kt`, `MiHealthDatabase.kt`
- Test: `app/src/test/java/com/jhon/mihealthreader/local/LocalDaoTest.kt`

**Interfaces:**
- Produces: `StepSampleEntity(sourceId,time,zoneOffsetSec,steps,distanceMeters,calories)`；`SleepSessionEntity(start,end,durationMinutes,deepMinutes,lightMinutes,avgHr,minHr,maxHr)`；`SleepStageEntity(sessionStart,start,end,state,stateName)`；`SyncStateEntity(id=1,lastExtractedAt,sourcePackage,lastNewCount)`；`MiHealthDatabase.build(context)`；DAO：`StepSampleDao.insertIgnore/all/count`、`SleepDao.upsertSessions/insertStages/sessions/stages`、`SyncStateDao.put/get`。

- [ ] **Step 1: 加依赖**：版本目录加 `room=2.6.1`、`ksp=2.0.21-1.0.25`、libraries `androidx-room-runtime/ktx/testing`、plugin `ksp`；根 `build.gradle.kts` 加 `alias(libs.plugins.ksp) apply false`；app 加 `alias(libs.plugins.ksp)` 与 room 依赖（`ksp(libs.androidx.room.compiler)`）。
- [ ] **Step 2: 写失败测试** `LocalDaoTest`（Robolectric 内存库）：`insertIgnore` 同主键第二次返回 `-1`；`upsertSessions` 同 `start` 覆盖；`SyncStateDao` 覆盖更新；`all/count/sessions/stages` 正确。
- [ ] **Step 3: 运行确认失败** `./gradlew :app:testDebugUnitTest --tests "*LocalDaoTest"`。
- [ ] **Step 4: 实现实体/DAO/DB。**
- [ ] **Step 5: 运行确认通过。**
- [ ] **Step 6: 提交** `feat: add Room local database`.

---

### Task 3-2: StepSample.sourceId + ActivityParser

**Files:**
- Modify: `app/src/main/java/com/jhon/mihealthreader/parse/ParsedModels.kt`, `ActivityParser.kt`
- Test: `app/src/test/java/com/jhon/mihealthreader/parse/ActivityParserTest.kt`, `NormalizeIntegrationTest.kt`

**Interfaces:**
- Consumes: `StepSample` 加 `sourceId: String`。
- Produces: `ActivityParser.parseSamples(db)` 读取 `sid`；`aggregate` 语义不变（忽略 `sourceId`）。

- [ ] **Step 1: 更新测试**：`StepSample` 构造加 `sourceId`；`parseSamples` 断言读到的 `sourceId`。
- [ ] **Step 2: 运行确认失败。**
- [ ] **Step 3: 实现**：`StepSample(sourceId, time, zoneOffsetSec, steps, distanceMeters, calories)`；`parseSamples` 查询 ``sid` 列并填入；`aggregate` 不变。
- [ ] **Step 4: 运行全部单测通过（含 `NormalizeIntegrationTest` 的构造更新）。**
- [ ] **Step 5: 提交** `feat: add source id to step samples`.

---

### Task 3-3: Normalizer 返回解析数据

**Files:**
- Modify: `app/src/main/java/com/jhon/mihealthreader/parse/Normalizer.kt`, `ParsedModels.kt`
- Test: `app/src/test/java/com/jhon/mihealthreader/parse/NormalizeIntegrationTest.kt`

**Interfaces:**
- Produces: `data class ParsedData(val sleep: List<SleepSession>, val samples: List<StepSample>, val activity: List<DailyActivity>, val skipped: Int)`；`Normalizer.normalize(...): ParsedData`（仍写 `normalized.json`；`ParsedData` 替代原 `NormalizeResult`，删除旧类型）。

- [ ] **Step 1: 更新测试**：断言 `normalize` 返回的 `sleep`/`samples`/`activity` 非空且 `outputFile` 存在（`outputFile` 通过 `ParsedData` 的伴生方法或保留字段提供）。
- [ ] **Step 2: 运行确认失败。**
- [ ] **Step 3: 实现**：`normalize` 收集解析结果并返回 `ParsedData`；保留写信封逻辑。
- [ ] **Step 4: 运行确认通过。**
- [ ] **Step 5: 提交** `refactor: return parsed data from normalizer`.

---

### Task 3-4: LocalStore

**Files:**
- Create: `app/src/main/java/com/jhon/mihealthreader/local/LocalStore.kt`
- Test: `app/src/test/java/com/jhon/mihealthreader/local/LocalStoreTest.kt`

**Interfaces:**
- Produces:
  - `data class SaveResult(val newSamples: Int, val newStages: Int, val totalSessions: Int)`
  - `data class LocalSummary(val lastExtractedAt: Long?, val sourcePackage: String?, val stepSamples: Int, val sleepSessions: Int)`
  - `class LocalStore(db) { fun save(sleep, samples, sourcePackage, at): SaveResult; fun stepSamples(): List<StepSample>; fun sleepSessions(): List<SleepSession>; fun summary(): LocalSummary }`

- [ ] **Step 1: 写失败测试**：首次 `save` 新增计数正确；第二次 `save` 相同数据 `newSamples/newStages == 0`；`sleepSessions()` 合并阶段正确；`summary()` 计数正确；空库 `summary()` 返回 `null` 时间与 0 计数。
- [ ] **Step 2: 运行确认失败。**
- [ ] **Step 3: 实现 `LocalStore`**（映射实体 <-> 领域模型；`insertIgnore` 返回 rowId 统计 `!= -1`）。
- [ ] **Step 4: 运行确认通过。**
- [ ] **Step 5: 提交** `feat: add local store with incremental save`.

---

### Task 3-5: 接入 ExtractionViewModel

**Files:**
- Modify: `app/src/main/java/com/jhon/mihealthreader/ui/ExtractionViewModel.kt`, `ExtractionState.kt`
- Modify: `app/src/test/java/com/jhon/mihealthreader/ui/ExtractionViewModelTest.kt`

**Interfaces:**
- Consumes: `LocalStore`、`ParsedData`。
- Produces: VM 构造新增 `localStore: LocalStore`；`val summary: StateFlow<LocalSummary>`；`ExtractionState.Success(snapshot, tableCount, newRecords)`。

- [ ] **Step 1: 更新测试**：入库后 `Success.newRecords` 正确；重复数据时 `newRecords == 0`；`summary` 初始读取。
- [ ] **Step 2: 运行确认失败。**
- [ ] **Step 3: 实现**：`normalize` 后 `localStore.save(...)`；刷新 `_summary`；`Success` 带 `newRecords`。
- [ ] **Step 4: 运行确认通过。**
- [ ] **Step 5: 提交** `feat: persist extraction results incrementally`.

---

### Task 3-6: UI 改版 + 真机复测

**Files:**
- Modify: `app/src/main/java/com/jhon/mihealthreader/data/ParsedRepository.kt`, `ui/AppNav.kt`, `ui/screens/ExtractionScreen.kt`, `MainActivity.kt`, `App.kt`(如需)
- Test: `app/src/test/java/com/jhon/mihealthreader/data/ParsedRepositoryTest.kt`

**Interfaces:**
- Consumes: `LocalStore`。
- Produces: `ParsedRepository(localStore)`：`dailyActivity() = ActivityParser.aggregate(localStore.stepSamples())`；`sleepSessions() = localStore.sleepSessions()`。首页显示 `summary` 与常驻按钮。

- [ ] **Step 1: 更新 `ParsedRepository` 测试**（基于 `LocalStore` 或内存库）。
- [ ] **Step 2: 改 UI**：首页显示"上次同步/样本数/睡眠数"，按钮 `提取数据`/`活动视图`/`睡眠视图`/`浏览数据` 常驻；`Success` 显示"新增 X 条"/"无新数据"；`MainActivity` 构建 `MiHealthDatabase`/`LocalStore` 并注入。
- [ ] **Step 3: 构建 + lint + 全部单测通过。**
- [ ] **Step 4: 真机复测**：提取一次 → 杀进程重启 → 不点提取也能看活动/睡眠；再提取 → "无新数据"；解锁新数据后提取 → 只新增差量。
- [ ] **Step 5: 提交** `feat: read from local store and incremental extract ui`.

---

## Self-Review

- **Spec coverage**：Room 表→3-1；增量语义→3-1/3-4；`sourceId`→3-2；Normalizer 返回→3-3；入库→3-4/3-5；启动即读/首页→3-6；错误处理→3-4/3-5/3-6；测试→各任务。
- **类型一致性**：`StepSample.sourceId`、`ParsedData`、`SaveResult`、`LocalSummary`、`Success.newRecords` 跨任务一致。
- **Review Focus**：5 项均有测试。
- **规模**：以接口与断言为主。
