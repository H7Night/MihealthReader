# MihealthReader 更多指标实现计划（Phase 3）

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans. Steps use checkbox (`- [ ]`).

**Goal:** 新增心率/静息心率/血氧/运动/体重体成分的解析、增量入库与界面。

**Architecture:** `parse/` 加四个解析器与模型；`local/` 加 `metric_sample`/`workout`/`body_measurement` 三表与 DAO；`LocalStore`/`ParsedData`/`Normalizer` 扩展；UI 加四个列表页。

**Tech Stack:** Kotlin、Compose、Room 2.6.1 + KSP、JUnit4 + Robolectric。

**Spec:** `docs/superpowers/specs/2026-10-09-mihealthreader-metrics-design.md`

## Global Constraints

- DB `version=2`，`fallbackToDestructiveMigration()`。
- 点指标 `metric_sample` 用 `IGNORE`；`workout`/`body_measurement` 用 `REPLACE`。
- `metricType`：`heart_rate`/`resting_heart_rate`/`spo2`。
- 交付前通过 `./gradlew :app:testDebugUnitTest` 与 `:app:lintDebug`。
- 每个任务独立提交。

## Review Focus

1. 缺 `sid`/坏 JSON/缺列 → 跳过或兜底，不崩溃。测试 P3-2/P3-3。
2. `metric_sample` 重复写入 → 第二次新增 0。测试 P3-1/P3-5。
3. 运动/体重同主键修订 → 覆盖不重复。测试 P3-1。
4. 表为空（如本机 spo2）→ 空列表 + 空态。测试 P3-3/P3-6。
5. 旧库缺新表 → v2 破坏式迁移后可正常建表。测试 P3-1。

---

### Task P3-1: 领域模型 + Room v2 + DAO

**Files:**
- Modify: `parse/ParsedModels.kt`, `local/Entities.kt`, `local/Daos.kt`, `local/MiHealthDatabase.kt`
- Test: `local/LocalDaoTest.kt`

**Interfaces:**
- Produces: `MetricSample(metricType,sourceId,time,value:Double)`；`Workout(...)`；`BodyMeasurement(...)`；`object MetricTypes`；实体 `MetricSampleEntity`/`WorkoutEntity`/`BodyMeasurementEntity`；`MetricDao.insertIgnore/byType/byTypeFlow/countByType`；`WorkoutDao.upsert/all/allFlow/count`；`BodyMeasurementDao.upsert/all/allFlow/count`；DB v2。

- [ ] **Step 1: 写失败测试**（`LocalDaoTest` 增加）：`metric_sample` IGNORE 幂等；`workout` 同 `(sourceId,start)` REPLACE；`body_measurement` 同 `(sourceId,time)` REPLACE；`byType` 过滤。
- [ ] **Step 2: 运行确认失败。**
- [ ] **Step 3: 实现模型/实体/DAO/DB（version=2 + fallbackToDestructiveMigration）。**
- [ ] **Step 4: 运行通过。**
- [ ] **Step 5: 提交** `feat: add metric/workout/body room tables`。

---

### Task P3-2: HeartRateParser + Spo2Parser

**Files:**
- Create: `parse/HeartRateParser.kt`, `parse/Spo2Parser.kt`
- Test: `parse/HeartRateParserTest.kt`, `parse/Spo2ParserTest.kt`

**Interfaces:**
- Produces: `HeartRateParser.parse(db): List<MetricSample>`（heart_rate + resting_heart_rate）；`Spo2Parser.parse(db): List<MetricSample>`。

- [ ] **Step 1: 写失败测试**：临时库建 `hr_record(value,key,sid,isDeleted,time)`，插入 heart_rate/resting_heart_rate/isDeleted 行，断言类型/值/数量；`spo2_record` 同理；坏 JSON 跳过。
- [ ] **Step 2: 运行确认失败。**
- [ ] **Step 3: 实现。**
- [ ] **Step 4: 运行通过。**
- [ ] **Step 5: 提交** `feat: parse heart rate and spo2`。

---

### Task P3-3: WorkoutParser + BodyCompositionParser

**Files:**
- Create: `parse/WorkoutParser.kt`, `parse/BodyCompositionParser.kt`
- Test: `parse/WorkoutParserTest.kt`, `parse/BodyCompositionParserTest.kt`

**Interfaces:**
- Produces: `WorkoutParser.parse(db): List<Workout>`；`BodyCompositionParser.parse(db): List<BodyMeasurement>`；类型名映射 `WorkoutTypes.name(key)`。

- [ ] **Step 1: 写失败测试**：`sport_report` 插入一条 `value` JSON，断言 start/end/duration/calories/hr/type；`weight_item_record` 独立列一行 + value JSON 回退一行，断言字段；缺列容错。
- [ ] **Step 2: 运行确认失败。**
- [ ] **Step 3: 实现。**
- [ ] **Step 4: 运行通过。**
- [ ] **Step 5: 提交** `feat: parse workouts and body composition`。

---

### Task P3-4: Normalizer / ParsedData 扩展

**Files:**
- Modify: `parse/ParsedModels.kt`, `parse/Normalizer.kt`
- Test: `parse/NormalizeIntegrationTest.kt`

**Interfaces:**
- Produces: `ParsedData` 增加 `metricSamples/workouts/bodyMeasurements`；`normalize` 解析并写 `normalized.json` 的 `records.heart_rate/resting_heart_rate/spo2/workouts/body_measurements`。

- [ ] **Step 1: 更新测试**：fixture 增加 `hr_record`/`sport_report`/`weight_item_record`，断言 `ParsedData` 与信封。
- [ ] **Step 2: 运行确认失败。**
- [ ] **Step 3: 实现。**
- [ ] **Step 4: 运行通过。**
- [ ] **Step 5: 提交** `feat: normalize additional metrics`。

---

### Task P3-5: LocalStore / LocalSummary 扩展 + VM 接入

**Files:**
- Modify: `local/LocalStore.kt`, `ui/ExtractionViewModel.kt`
- Test: `local/LocalStoreTest.kt`, `ui/ExtractionViewModelTest.kt`

**Interfaces:**
- Produces: `LocalStore.save(data: ParsedData, sourcePackage, at): SaveResult`（`SaveResult` 加 `newMetrics/newWorkouts/newBody`）；`metricSamplesFlow(type)`/`workoutsFlow()`/`bodyMeasurementsFlow()`；`LocalSummary` 加计数。

- [ ] **Step 1: 更新测试**：`save(ParsedData(...))` 计数；幂等；`summary` 各计数。
- [ ] **Step 2: 运行确认失败。**
- [ ] **Step 3: 实现并改 VM 调用 `save(parsed, ...)`。**
- [ ] **Step 4: 运行通过。**
- [ ] **Step 5: 提交** `feat: persist additional metrics incrementally`。

---

### Task P3-6: UI + ParsedRepository + 真机复测

**Files:**
- Modify: `data/ParsedRepository.kt`, `ui/AppNav.kt`, `ui/screens/ExtractionScreen.kt`
- Create: `ui/screens/HeartRateScreen.kt`, `Spo2Screen.kt`, `WorkoutScreen.kt`, `BodyScreen.kt`
- Test: `data/ParsedRepositoryTest.kt`

**Interfaces:**
- Produces: `ParsedRepository.heartRate()/restingHeartRate()/spo2()/workouts()/bodyMeasurements()`（Flow）；四个 Screen；首页新增导航。

- [ ] **Step 1: 更新 `ParsedRepositoryTest`。**
- [ ] **Step 2: 实现 ParsedRepository + 四个 Screen + 导航按钮/路由。**
- [ ] **Step 3: 构建 + lint + 全部单测通过。**
- [ ] **Step 4: 真机复测**：提取一次 → 心率/运动/体重界面有数据、血氧空态；重启秒读；再提取"无新数据"。
- [ ] **Step 5: 提交** `feat: metric screens`。

---

## Self-Review

- **Spec coverage**：表结构→P3-1；解析→P3-2/3；模型/信封→P3-4；入库/摘要→P3-5；界面→P3-6；容错→P3-2/3；迁移→P3-1。
- **类型一致性**：`MetricSample`/`Workout`/`BodyMeasurement`/`SaveResult`/`LocalSummary` 跨任务一致。
- **Review Focus**：5 项均有测试。
- **规模**：以接口与断言为主。
