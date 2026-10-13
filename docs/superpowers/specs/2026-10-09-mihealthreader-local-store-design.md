# MihealthReader 本地存储与增量同步设计（阶段 3）

- 日期：2026-10-09
- 状态：已实现并真机验证（2026-10-09；记录见 `docs/manual-verification.md` §8）
- 前置：阶段 1/2 完成并真机验证
- 实现记录：库升至 **v2**（见 `2026-10-09-mihealthreader-metrics-design.md`），v2 起改为 `exportSchema=true` + 增量迁移，详见 §2/§9

## 1. 目标与范围

让 App 不再"每次打开都要提取"：

- 提取一次后，把解析结果**持久化到 App 自带数据库**；
- **启动即读**本地库，秒级显示上次结果，无需 root；
- 手动"提取"时**增量写入**：有新记录则新增，无则显示"无新数据"。

范围（本次）：

- 新增 Jetpack Room 本地库（原始样本 append-only）。
- 活动/睡眠界面改为读本地库；日汇总按需聚合。
- 首页常驻导航 + 上次同步信息。

不在本次：更多健康指标（阶段 3+ 扩展）、Health Connect 方案、定时后台提取。

## 2. 本地库设计（Room，`mihealth.db`，version 1；v2 见 metrics 设计）

| 表 | 列 | 主键 | 写入策略 |
| --- | --- | --- | --- |
| `step_sample` | `sourceId, time, zoneOffsetSec, steps, distanceMeters, calories` | `(sourceId, time)` | append-only `IGNORE` |
| `sleep_session` | `start, end, durationMinutes, deepMinutes, lightMinutes, avgHr, minHr, maxHr` | `start` | 覆盖为最新 `REPLACE` |
| `sleep_stage` | `sessionStart, start, end, state, stateName` | `(sessionStart, start)` | append-only `IGNORE` |
| `sync_state` | `id=1, lastExtractedAt, sourcePackage, lastNewCount` | `id` | 每次提取后更新 |

- `OnConflictStrategy.IGNORE` 的去重键为自然键；`REPLACE` 用于睡眠会话修订。
- 新增行数统计：`@Insert` 返回的 rowId 列表中 `!= -1` 的个数。

## 3. 解析与入库

- `Normalizer.normalize(snapshotRoot)` 除写 `parsed/normalized.json` 外，**返回解析数据 `ParsedData`**
  （`sleep: List<SleepSession>`、`samples: List<StepSample>`、`activity: List<DailyActivity>`、`skipped`）。
- `StepSample` 增加 `sourceId`（来源设备 sid），作为去重键；`ActivityParser.parseSamples` 读取 `sid`。
- `LocalStore.save(data: ParsedData, sourcePackage, at): SaveResult`（Phase 3 起改为接收整包 `ParsedData`）
  - 睡眠会话/运动/体重 `REPLACE`；阶段/步数样本/点指标 `IGNORE`；
  - `newRecords` 汇总所有新增（阶段 + 样本 + 点指标 + 运动 + 体重）；更新 `sync_state`。
- 提取流程（`ExtractionViewModel`）：root → 定位 → 快照 → 通用 JSON → `normalize` → `LocalStore.save` → 刷新摘要。
- `ExtractionState.Success` 改为报告 `newRecords`（0 时 UI 显示"无新数据"）。

## 4. 读取与界面

- `LocalStore` 提供 `stepSamples()`、`sleepSessions()`、`summary()`。
- 活动视图：`ActivityParser.aggregate(stepSamples())` 得到每日汇总。
- 睡眠视图：直接展示会话（含阶段）。
- 首页（提取页）：
  - 顶部显示"上次同步时间 · 步数样本 N · 睡眠 M 段"（`summary`）；
  - 按钮常驻：`提取数据` / `活动视图` / `睡眠视图` / `浏览数据`（浏览仍读快照）；
  - 状态区显示 Running/Success("新增 X 条"/"无新数据")/Error。
- 启动时 `ExtractionViewModel` 读取本地摘要；有无数据都能进视图。

## 5. 兼容与保留

- 快照目录 + `normalized.json` 继续生成，调试浏览器不受影响。
- 首次升级（旧快照存在但本地库为空）：启动显示"本地库为空，请点击提取"；手动提取后入库。

## 6. 错误处理

- 本地库为空 / 无同步记录 → 摘要显示"尚未同步"，视图空态。
- 提取失败（root/未找到/加密）→ 本地库不变，错误照常显示。
- 重复提取无新行 → `newRecords = 0`，显示"无新数据"。
- 解析返回空 → 入库 0 条，摘要不变。

## 7. 测试策略

- Room DAO（Robolectric 内存库）：`IGNORE` 幂等去重、`REPLACE` 覆盖、`sync_state` 更新。
- `LocalStore`：`save` 返回的新增计数正确；重复 save 第二次为 0。
- `ActivityParser`：`sourceId` 读取与聚合不受影响。
- `Normalizer`：返回解析数据。
- `ExtractionViewModel`：入库后 `Success.newRecords` 正确；无新数据时 0。
- 真机：提取一次 → 杀掉重启 → 秒读（不点提取也有数据）；再提取 → "无新数据"；新增一天数据后提取 → 只新增差量。
- 交付前 `./gradlew :app:testDebugUnitTest` 与 `:app:lintDebug`。

## 8. 任务拆分（每个任务独立提交）

- **3-1** Room 依赖 + 实体/DAO/数据库 + `sync_state` + DAO 测试。
- **3-2** `StepSample.sourceId` + `ActivityParser` 更新 + 测试。
- **3-3** `Normalizer` 返回解析数据（`ParsedData`）+ 测试。
- **3-4** `LocalStore`（save/summary/读取）+ 测试。
- **3-5** 接入 `ExtractionViewModel`（增量入库、摘要、Success.newRecords）+ 测试。
- **3-6** UI 改版（首页常驻导航 + 上次同步；活动/睡眠读本地库）+ 真机复测。

## 9. 技术选型

- Room `2.6.1` + KSP（Kotlin `2.0.21` 对应 KSP `2.0.21-1.0.25`）。
- 数据库文件名 `mihealth.db`；v1 时 `exportSchema=false`。v2 起改为 `exportSchema=true`：
  schema 导出到 `app/schemas/`，并改用增量迁移（无破坏式回退），详见 metrics 设计 §3。
- 读取聚合在调用线程完成（数据量小）；后续可迁到 Flow/后台。

## 10. 实现记录

- 已实现并真机验证（`docs/manual-verification.md` §8）：提取一次后入库，杀进程重启**启动即读**，
  再次提取「无新数据」，增量幂等。
- 读取后续改为 **Room `Flow`**（`LocalStore.stepSamplesFlow()`/`sleepSessionsFlow()`），避免主线程访问数据库崩溃。
- `Normalizer.normalize` 返回 `ParsedData`；`LocalStore.save(data: ParsedData, sourcePackage, at)`。
- 本地库**升至 v2**：`exportSchema=true`，schema 导出到 `app/schemas/…/2.json`；新增 `MIGRATION_1_2`
  增量迁移，**取消破坏式迁移**（`11c679b`，详见 `docs/manual-verification.md` §22）。
