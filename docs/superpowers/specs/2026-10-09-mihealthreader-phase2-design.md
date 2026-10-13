# MihealthReader 阶段 2 设计文档（定向解析 + 界面）

- 日期：2026-10-09
- 状态：已实现并真机验证（2026-10-09；记录见 `docs/manual-verification.md` §7）
- 前置：阶段 1 已完成并真机验证（见 `2026-10-08-mihealthreader-design.md` 与 `docs/manual-verification.md`）

## 1. 目标与范围

在阶段 1「通用全库导出 + 调试浏览器」之上，增加**睡眠**与**步数/活动**的**定向规范化解析**，
并新增对应的友好界面。

范围（本次）：

- **2A 解析**：从快照中的原始库 `fitness_data` 解析 `sleep_segment`、`step_record`，
  产出规范化 JSON `parsed/normalized.json`。
- **2B 界面**：新增"活动"和"睡眠"两个友好视图；保留调试浏览器。

不在本次：应用内分享导出、定时提取（列为 2C/2D）。

## 2. 数据源映射（真机确认）

源文件位于快照 `raw/com.mi.health/databases/<用户id>/cn/fitness_data`（含 `-wal`/`-shm`）。
用户 id 目录名可变，解析时按文件名 `fitness_data` 在快照内定位。

### `sleep_segment`
行含 `key`（多为 `sleep`）、`sid`、`time`、`isComplete`、`value`（内嵌 JSON）。`value` 示例字段：
`avg_hr`、`max_hr`、`min_hr`、`bedtime`、`sleep_deep_duration`、`sleep_light_duration`、`duration`、
`device_bedtime`、`device_wake_up_time`、`bed_timestamp`、`out_bed_timestamp`、`items`。
`items[]` 为阶段序列 `{start_time, end_time, state}`。时长字段单位为**分钟**。

### `step_record`
行含 `key`（多为 `steps`）、`time`、`value`（内嵌 JSON `{time, steps, distance, calories}`）、
`zoneOffsetInSec`、`isDeleted`。`distance` 单位为**米**，`calories` 单位为 **kcal**。

## 3. 规范化模型与输出

写入 `<快照>/parsed/normalized.json`，采用可互通信封（借鉴 mi_fitness_data_bridge 的格式）：

```json
{
  "schema_version": "1.0",
  "source": "mihealthreader",
  "generated_at": "<UTC ISO-8601>",
  "records": {
    "sleep": [
      {
        "start": 1781542380, "end": 1781565120,
        "start_local": "2026-06-12T01:33:00+08:00",
        "duration_min": 379, "deep_min": 82, "light_min": 297,
        "avg_hr": 50, "min_hr": 45, "max_hr": 67,
        "stages": [ { "start": 1781542380, "end": 1781545920, "state": 3, "state_name": "deep" } ]
      }
    ],
    "daily_activity": [
      { "date": "2026-06-12", "steps": 8342, "distance_m": 5210, "active_kcal": 311 }
    ]
  }
}
```

字段规则：

- `sleep.start`/`end` 取 `value.bedtime`/`value.device_wake_up_time`（回退 `bed_timestamp`/`out_bed_timestamp`），单位 epoch 秒。
- `sleep.date`（可选）取唤醒时刻的**本地日期**，用于展示。
- 阶段码 `state_name` 映射（**真机 + HCUnlock 交叉校准**）：`1=awake`、`2=deep`、`3=light`、`4=rem`、`5=awake`、其它=`unknown`；
  已用应用自带的 `sleep_deep_duration`/`sleep_light_duration` 与各 state 分钟数交叉验证。
  **同时保留原始 `state` 数值**。
- `daily_activity.date` 为**本地日期**（`time + zoneOffsetInSec` 换算为 UTC 日期字符串 `yyyy-MM-dd`）。
- 跳过 `step_record.isDeleted == 1` 的行；跳过无法解析 `value` 的行。

## 4. 聚合与去重

- 步数按本地日聚合：`steps`/`distance`/`calories` 求和。
- **分钟切片去重**：同一本地分钟内若存在多条记录（多设备重叠），该分钟保留 `steps` **较大**的一条，
  再对去重后的分钟求和，避免双设备重复计步（规则借鉴 mi_fitness_data_bridge）。
- 睡眠：一条 `key=sleep` 且 `value` 可解析的行对应一个会话；`items` 直接映射为阶段。

## 5. 架构

新增包 `parse/`：

```
com.jhon.mihealthreader.parse
├─ ParsedModels.kt   SleepStage/SleepSession/DailyActivity/StepSample
├─ SleepParser.kt    parse(db): List<SleepSession>；parseValue(json): SleepSession?（纯函数）
├─ ActivityParser.kt parseSamples(db): List<StepSample>；aggregate(samples, dedup): List<DailyActivity>（纯函数）
└─ Normalizer.kt     normalize(snapshotRoot): ParsedData（定位 fitness_data、解析、写 normalized.json）
```

- `Normalizer` 依赖 `Dumper` 邻近的只读 SQLite 打开方式，定位 `fitness_data` 后读取两个表。
- `ExtractionViewModel` 在导出 JSON 之后调用 `Normalizer`，把 `normalized.json` 与统计写入结果；
  `ExtractionState.Success` 增加解析得到的睡眠会话数与活动天数（可选）。
- 窗口 UI 通过新的 `ParsedRepository`（或扩展 `ExportRepository`）读取 `normalized.json`。

## 6. 界面（2B）

- **活动视图**：按日期倒序列出每日 `steps`/`distance_m`/`active_kcal`，顶部显示最近一天汇总。
- **睡眠视图**：按日期倒序列出会话，显示起止时间、总时长、深睡/浅睡分钟、平均/最低/最高心率，
  并可展开阶段时间线（用不同颜色/文字表示 wake/light/deep/rem）。
- 首页增加导航入口（活动 / 睡眠 / 调试），保留提取流程。

## 7. 错误处理

- 快照中找不到 `fitness_data` → 解析步骤返回空结果并提示"未找到 fitness_data"，不影响通用导出。
- `sleep_segment`/`step_record` 表缺失 → 对应数据集为空数组，不报错。
- 单行 `value` JSON 解析失败 → 跳过该行并计数，`normalized.json` 中记录 `skipped` 统计。
- 空数据 → 输出空数组。

## 8. 测试策略

- 纯函数单测：`SleepParser.parseValue`、`ActivityParser.aggregate`（含分钟去重、跨本地日、isDeleted 过滤）。
- Robolectric 单测：构造 `fitness_data` 临时库，写入 `sleep_segment`/`step_record`，验证解析与 `Normalizer` 输出。
- 交付前跑 `./gradlew :app:testDebugUnitTest` 与 `:app:lintDebug`。
- 真机复测：对既有快照重新解析，核对 `normalized.json` 的睡眠会话数、每日步数与源库量级一致。

## 9. 任务拆分（每个任务独立提交）

- **2A-1** 模型 + `normalized.json` 信封写出（`ParsedModels` + `Normalizer` 骨架）。
- **2A-2** `SleepParser`（纯解析 + 读库）+ 测试。
- **2A-3** `ActivityParser`（读库 + 每日聚合 + 分钟去重）+ 测试。
- **2A-4** `Normalizer` 完整接入提取流程 + 真机复测。
- **2B-1** 活动视图 + 导航接入。
- **2B-2** 睡眠视图（含阶段展示）。

## 10. 风险与待确认

- 睡眠阶段码 `1/2/3/4/5` 映射为 awake/deep/light/rem/awake（`1/4` 来自 HCUnlock 交叉验证，`2/3` 由真机 deep/light 字段确认）；输出保留原始 `state` 以兜底。
- `fitness_summary.daily_report` 可能含官方日汇总，可作日后校验（本次以 `step_record` 聚合为准）。
- 时区：使用行内 `zoneOffsetInSec` 换算本地日；无该字段时回退设备时区。

## 11. 实现记录（与设计的差异）

- 睡眠阶段码经真机 + HCUnlock 交叉校准并修正：`2=deep`、`3=light`、`5=awake`（`0629847`），
  随后补全 `1/4/5` 映射（`4093e41`），与源库自带的深睡/浅睡分钟数逐段吻合。
- `-shm` 处理：抽取不复制过时 `-shm`，打开副本前删除、打开后清理自动生成的 `-shm`（`5d5c790`、`491f815`）。
- `Normalizer.normalize` 返回值后续由 `NormalizeResult` 改为 `ParsedData`（见本地存储设计 §3）。
- 阶段 2 的首页导航入口（活动/睡眠/调试）后续被 UI 重构取代（底部导航「今天/视图/设置」，
  见 `2026-10-09-mihealthreader-ui-design.md`），活动/睡眠改由图表与睡眠详情呈现。
