# MihealthReader — Hilt 迁移 · Health Connect 写入 · MD3 对齐 · 技术栈对齐

Date: 2026-10-13
Status: Draft (待评审)
Scope: MihealthReader 单模块 `:app`。与 Kenko / hackqing 相互独立，不抽公共框架。
基底：本设计基于分支 `feat/spec-sync`（已把各 spec 同步为已实现状态，并记录 2026-10-13 复测）。与既有 specs（`docs/superpowers/specs/2026-10-*.md`）一致。

## 1. 目标

在**不改动特权读取通道（libsu root / Shizuku）与现有解析逻辑**的前提下：

1. 技术栈对齐（item 1）
2. 手写依赖装配迁移到 Hilt（item 1）
3. 把已解析/入库的指标写入 Health Connect（item 2）：Steps / HeartRate / RestingHeartRate / SpO2 / Sleep / Workout
4. Material Design 3 一致性收口（item 5）
5. 保留 `normalized.json` 自有导出 schema（item 6，不做跨端导入）

非目标：改变 `PrivilegedShell`/解析表结构；跨端导入；Kenko / hackqing。

## 2. 目标技术栈（对齐 Kenko）

Kotlin 2.2.21 · AGP 8.13.2 · Compose BOM 2025.11.00 · Material3 1.5.0-alpha08 · Room 2.8.3 · Hilt 2.57.2 · KSP 2.2.21-2.0.4 · targetSdk 36 / compileSdk 36 / minSdk 26。

- 保留 `libsu 5.2.2`、`Shizuku 13.1.5`（api + provider）与 AIDL `IUserService`。
- JDK/JVM target 17。
- 现有 `libs.versions.toml` 基础上调版本；Room 从 2.6.1 → 2.8.3。
- Room 迁移：**已实现**为安全增量迁移（`11c679b`：`MIGRATION_1_2` + `ALL_MIGRATIONS`，`exportSchema=true`，缺迁移显式失败、绝不静默清库；`Migration1To2Test` 断言）。升级 Room 版本时保持该约定。

## 3. 依赖注入：Hilt 迁移

当前在 `MainActivity.kt` 手写装配（`App(...)`、各 `fun interface` 实现、`BackendManager`、`LocalStore`、`ParsedRepository`）。

- `@HiltAndroidApp` Application；`@AndroidEntryPoint` MainActivity；`ExtractionViewModel` 及屏幕 VM 改 Hilt。
- 模块：
  - `di/DatabaseModule`（`MiHealthDatabase` + 6 DAO）
  - `di/RepositoryModule`（`ParsedRepository`、`LocalStore`）
  - `di/PrivilegedModule`：提供 `BackendManager` 选出的 `PrivilegedShell`（`LibsuRootShell` 或 `ShizukuShell`）、`TargetLocator`、`PackageScanner`、`Extractor`、`DbDumper`、`Normalizer`。
  - `di/AppModule`（`@IoDispatcher`、`@ApplicationScope`）。
- 保留现有 `fun interface` 抽象（便于单测的 fake 不被破坏）。
- 后端切换需重启的既有约束保留（`BackendManager.current()` 启动时采样）。

## 4. Health Connect 写入（item 2）

新增 `data/health/HealthConnectManager.kt`（`@Singleton`），从 Room 读取已入库数据写入 HC；**只写**，不改读取通道。

| 指标 | Room 来源 | HC 记录 | 备注 |
|---|---|---|---|
| 步数 | `step_sample`（按本地日聚合） | `StepsRecord`（区间 count） | 按本地日 start/end + 当日总步数 |
| 心率 | `metric_sample[HEART_RATE]` | `HeartRateRecord` + `Sample` 列表 | 批量写入；同区间聚合 |
| 静息心率 | `metric_sample[RESTING_HEART_RATE]` | `RestingHeartRateRecord`（按日） | 每日一条 |
| 血氧 | `metric_sample[SPO2]` | `OxygenSaturationRecord` | 瞬时百分比 |
| 睡眠 | `sleep_session` + `sleep_stage` | `SleepSessionRecord` + `SleepStageRecord` | 阶段映射：1&5=清醒(AWAKE)、2=深睡(DEEP)、3=浅睡(LIGHT)、4=REM |
| 运动 | `workout` | `ExerciseSessionRecord` | 运动类型键 → HC `ExerciseSessionType`；含时长/卡路里/距离 |

- **触发**：提取完成入库后自动写新增数据；首次授权后**回填历史** + 设置页「立即同步」；全部 `clientRecordId = "mihealth:<type>:<自然键>"` 幂等。
- **权限**（manifest + rationale `activity-alias`，照抄 hackqing/Kenko 的 `VIEW_PERMISSION_USAGE` 模式）：`WRITE_STEPS/WRITE_HEART_RATE/WRITE_RESTING_HEART_RATE/WRITE_OXYGEN_SATURATION/WRITE_SLEEP/WRITE_EXERCISE`。
- **失败处理**：`runCatching` + 日志，绝不阻断提取/UI。
- 安装 HC 与否自行判定（`HealthConnectClient.getSdkStatus`）；不可用则不显示 HC 设置项。

## 5. MD3 一致性（item 5）

- 已有 Monet 动态色（12+）；补 **Android 12 以下基线色板兜底**（当前为静态 teal 回退，统一为 Material Theme Builder 基线），并统一 tokens。
- 保留自绘 Canvas 图表（`ui/charts/`）；仅统一配色来源为 `MaterialTheme.colorScheme`。
- 不强制新增主题设置项（可选：与 Kenko 一样的配色预设，见 §8 决策）。

## 6. 导出（item 6）

- 保持 `normalized.json`（`schema_version="1.0"`）自有 schema 与 SAF 导出；不做跨端导入。可视情况把 schema_version 升位以标记新增字段（如有）。本项基本无改动。

## 7. 测试与 CI

- 现有 24 文件 / 84 单测（JUnit4 + Robolectric）保持绿。
- 新增：HC DTO 映射纯函数单测（睡眠阶段映射、运动类型映射、步数按日聚合、CardioMinutes 类似聚合）；各记录映射的边界（空样本、跨日）。
- 新增 GitHub Actions：`testDebugUnitTest` + `lintDebug` + `assembleDebug`（当前无 CI）。
- **不在持有真实数据的设备上跑 instrumented 测试**（避免污染真实库；用模拟器）。

## 8. 需评审拍板的决策

1. **HC 写入触发**：提取后自动 + 手动回填（默认，推荐）还是仅手动。
2. **步数粒度**：按本地日聚合（默认）还是写入分时样本。
3. **心率体量**：大批量分页/限流策略（按日均批写）。
4. **MD3 主题设置**：仅 Monet/兜底（默认）还是加配色预设。

## 9. 风险

- **HR/Steps 数据量大**：HC 写入需分批，避免 OOM/超时。
- **睡眠阶段映射**：需与现有 `SleepParser.stateName`（1&5 awake / 2 deep / 3 light / 4 rem）保持一致，避免二次映射分歧。
- **Room 版本跳变**：2.6.1 → 2.8.3 需回归迁移测试；`exportSchema` 已开。
- 特权通道不变，但 Hilt 装配要保证 `PrivilegedShell` 选择与失败回退语义不变。

## 10. 非目标

- 跨端导入；改变解析/特权读取；Kenko / hackqing。
