# MihealthReader 更多指标设计（Phase 3）

- 日期：2026-10-09
- 状态：已实现并真机验证（2026-10-09；记录见 `docs/manual-verification.md` §9）
- 前置：本地存储（`mihealth.db` v1）已完成
- 实现记录：DB v2 由**破坏式迁移改为增量迁移**（见 §3）

## 1. 目标与范围

在睡眠/步数之外，新增四类指标：**心率、静息心率、血氧、运动记录、体重/体成分**（不含 VO2max）。
解析 → 增量入库 → 界面展示，复用现有增量与启动即读机制。

## 2. 数据源映射（真机确认）

- `hr_record`：`key='heart_rate'`，`value={bpm,...}`；`key='resting_heart_rate'`，`value={bpm,date_time}`。
  列：`sid,time,value,isDeleted`。
- `spo2_record`：`key IN ('spo2','single_spo2')`，`value={spo2}`（本机为空）。
- `sport_report`：`key`=运动类型；`value` 含 `start_time,end_time,duration,calories,avg_hrm,max_hrm,min_hrm`（`distance` 可选）。
- `weight_item_record`：独立列 `weight,bmi,bodyFatRate,moistureRate,boneMass,basalMetabolism,muscleMass,proteinRate,visceralFat`；回退 `value` JSON。

## 3. 表结构（DB v2，增量迁移）

**点指标统一表**（单值时间序列，可扩展）：

```
metric_sample(metricType TEXT, sourceId TEXT, time INTEGER, value REAL, PRIMARY KEY(metricType, sourceId, time))
```
- `metricType` 取值：`heart_rate`、`resting_heart_rate`、`spo2`（后续压力/体温可直接复用，无需改表）。
- 写入 `OnConflictStrategy.IGNORE`（append-only）。

**复合记录表**（多字段，各自独立）：

```
workout(sourceId TEXT, start INTEGER, end INTEGER, durationSec INTEGER, calories INTEGER,
        avgHr INTEGER, maxHr INTEGER, minHr INTEGER, distanceMeters INTEGER,
        type TEXT, typeName TEXT, PRIMARY KEY(sourceId, start))          -- REPLACE

body_measurement(sourceId TEXT, time INTEGER, weightKg REAL, bmi REAL, bodyFatRate REAL,
        muscleKg REAL, waterPct REAL, boneKg REAL, bmrKcal REAL, visceralFat REAL,
        proteinRate REAL, PRIMARY KEY(sourceId, time))                    -- REPLACE
```

- 现有 `step_sample`/`sleep_session`/`sleep_stage` 不变。
- 数据库 `version = 2`，`exportSchema = true`：schema 导出到
  `app/schemas/com.jhon.mihealthreader.local.MiHealthDatabase/2.json`（`ksp { arg("room.schemaLocation", ...) }`），
  作为迁移基线纳入版本控制。
- 升级用**增量迁移**而非破坏式迁移：`local/Migrations.kt` 的 `MIGRATION_1_2` 只 `CREATE TABLE` 三张新表
  （`metric_sample`/`workout`/`body_measurement`），`MiHealthDatabase.build` 注册 `addMigrations(*ALL_MIGRATIONS)`，
  **不调用 `fallbackToDestructiveMigration()`**。理由：快照是用户自己的健康历史，schema 不匹配必须显式失败，
  不允许静默清空、逼迫重新提取。
- 迁移 SQL 以 `internal const val` 常量暴露，便于纯 JVM 测试逐条对照生成的 schema；
  `Migration1To2Test`（Robolectric）断言 v1 数据在迁移后保留、新表可写，且**缺少迁移时打开 v1 库会抛错而非清库**。

## 4. 领域模型与解析

`parse/ParsedModels.kt` 新增：

- `MetricSample(metricType, sourceId, time, value: Double)`
- `Workout(sourceId, start, end, durationSec, calories, avgHr?, maxHr?, minHr?, distanceMeters?, type, typeName)`
- `BodyMeasurement(sourceId, time, weightKg?, bmi?, bodyFatRate?, muscleKg?, waterPct?, boneKg?, bmrKcal?, visceralFat?, proteinRate?)`
- `object MetricTypes { HEART_RATE, RESTING_HEART_RATE, SPO2 }`

解析器（`parse/`）：

- `HeartRateParser.parse(db): List<MetricSample>`（heart_rate + resting_heart_rate）
- `Spo2Parser.parse(db): List<MetricSample>`
- `WorkoutParser.parse(db): List<Workout>`（含类型名映射，未知回退 key）
- `BodyCompositionParser.parse(db): List<BodyMeasurement>`（独立列优先，回退 value JSON）

`Normalizer.normalize` 返回的 `ParsedData` 增加 `metricSamples`、`workouts`、`bodyMeasurements`；
`normalized.json` 增加 `records.heart_rate`/`resting_heart_rate`/`spo2`/`workouts`/`body_measurements`。

## 5. 入库与摘要

- `LocalStore.save(data: ParsedData, sourcePackage, at): SaveResult`
  - 点指标 `IGNORE`、运动/体重 `REPLACE`；`SaveResult` 增加 `newMetrics/newWorkouts/newBody`。
- `LocalSummary` 增加 `heartRateSamples/spo2Samples/workouts/bodyMeasurements` 计数。
- `ExtractionViewModel` 改为 `localStore.save(parsed, sourcePackage, at)`；`newRecords` 汇总所有新增。

## 6. 界面

- 首页导航新增：`心率`、`血氧`、`运动`、`体重`（保留活动/睡眠/浏览）。
- `ParsedRepository` 增加 `heartRate()`/`restingHeartRate()`/`spo2()`/`workouts()`/`bodyMeasurements()`（均 `Flow`）。
- 各一个列表页（倒序）：
  - 心率：静息心率摘要 + 最近心率样本（时间/bpm）。
  - 血氧：样本列表（空态提示）。
  - 运动：日期/类型/时长/热量/平均心率。
  - 体重：日期/体重/BMI/体脂率/肌肉量/水分/骨量/基础代谢/内脏脂肪。

## 7. 错误处理

- 表缺失 / 为空 → 对应列表为空数组，不报错。
- 单行 `value` 解析失败 → 跳过。
- 缺 `sid` → `sourceId` 用 `""` 兜底。

## 8. 测试策略

- 各 parser：纯函数 + 临时库测试（含缺列/坏 JSON 容错）。
- Room DAO：`metric_sample` IGNORE 幂等、`workout`/`body_measurement` REPLACE。
- `LocalStore.save` 增量计数、`summary` 计数。
- 真机：提取一次 → 各指标界面有数据；重启秒读；再提取"无新数据"。
- 交付前 `./gradlew :app:testDebugUnitTest` 与 `:app:lintDebug`。

## 9. 任务拆分（每个任务独立提交）

- **P3-1** 领域模型 + Room v2（metric_sample/workout/body_measurement）+ DAO 测试。
- **P3-2** `HeartRateParser` + `Spo2Parser` + 测试。
- **P3-3** `WorkoutParser` + `BodyCompositionParser` + 测试。
- **P3-4** `Normalizer`/`ParsedData` 扩展 + 信封 + 测试。
- **P3-5** `LocalStore`/`LocalSummary` 扩展 + VM 接入 + 测试。
- **P3-6** UI（导航 + 四个列表页）+ `ParsedRepository` + 真机复测。
