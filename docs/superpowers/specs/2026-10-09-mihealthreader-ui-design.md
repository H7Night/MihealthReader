# MihealthReader UI 重构设计（导航 + 图表 + 设置）

- 日期：2026-10-09
- 状态：待实现计划
- 前置：数据层/本地库/Shizuku 已完成

## 1. 目标与范围

- 底部 **NavigationBar**：`今天 / 视图 / 设置`。
- **隐藏**现有列表视图（活动/睡眠/心率/血氧/运动/体重），改为**图表**呈现。
- 图表：**自绘 Canvas**，贴合 Material 3 / Google Health 风格。
- **提取 / 导出** 移到**设置页**；设置页含数据源（root/Shizuku）。
- 原始表**调试浏览器**移到设置页「高级/调试」保留。

不在本次：更复杂图表交互（缩放/十字线）、定时后台提取、正式多语言。

## 2. 导航结构

`AppNav` 改为 `Scaffold` + `NavigationBar`（3 项），`NavHost` 内含：

- `today` → `TodayScreen`（今天）
- `charts` → `ChartsScreen`（视图）
- `settings` → `SettingsScreen`（设置）
- `browse` → 现有 `TableListScreen`/`TableDetailScreen`（从设置进入，底部栏隐藏）

底部栏仅在 `today/charts/settings` 显示（按当前路由判断）。

删除：`ExtractionScreen`、`ActivityScreen`、`SleepScreen`、`HeartRateScreen`、`Spo2Screen`、`WorkoutScreen`、`BodyScreen`。

## 3. 今天（TodayScreen）

卡片式今日概览（Material 3 Card，大数字 + 小标签 + 迷你趋势）：

- 步数（今日 steps/distance/kcal）
- 睡眠（最近一晚：时长、深睡/浅睡）
- 心率（最近 bpm + 静息心率）
- 运动（今日次数/总时长）
- 血氧（最近值）、体重（最近值）
- 数据为空时卡片显示"暂无数据"，并提供"去设置提取"提示。

数据来源：`ParsedRepository` 各 Flow 取最新一条/当日聚合。

## 4. 视图（ChartsScreen）

- 顶部指标选择（Chips）：`步数 / 睡眠 / 心率 / 血氧 / 运动 / 体重`。
- 主图区（Canvas）+ 说明（范围、最新值）。
- 图表类型：
  - 步数/运动时长：近 30 天 **柱状图**。
  - 睡眠：近 30 天**堆叠柱状图**（深睡/浅睡/清醒）。
  - 心率/血氧/体重：**折线图**（按日聚合）。
- 聚合：按本地日期分组求均值/求和（纯函数，可测）。

## 5. 设置（SettingsScreen）

- **数据源**：`Root / Shizuku` 选择 + Shizuku 授权（沿用现有逻辑；切换后重启生效）。
- **提取数据**：按钮 + 状态（运行中/新增 N 条/无新数据/错误）+ 上次同步信息。
- **导出数据**：把 `<快照>/parsed/normalized.json` 通过 **SAF（ACTION_CREATE_DOCUMENT）** 保存到用户选择位置（默认文件名 `mihealth-normalized.json`）；无快照时提示先提取。
- **高级 / 调试**：进入原始表浏览器（`browse`）。
- **关于**：应用名/版本、数据来源说明。

## 6. 图表组件（自绘 Canvas）

`ui/charts/` 新增可复用 Compose 组件：

- `LineChart(values: List<Float>, modifier, color)` — 折线 + 基线，自动缩放。
- `BarChart(values: List<Float>, modifier, color)` — 竖柱。
- `StackedBarChart(rows: List<List<StackSegment>>, modifier)` — 睡眠阶段堆叠。

约束：使用 `MaterialTheme.colorScheme` 取色；空数据画占位；尺寸自适应。

## 7. 数据聚合

`data/ChartData.kt` 纯函数：

- `dailySteps(activity): List<Pair<date, Float>>`
- `dailyMetricAvg(samples, zoneDefault): List<Pair<date, Float>>`（按本地日求均值）
- `sleepDurations(sessions): List<SleepBar>`（深/浅/清醒分钟）
- `workoutMinutesPerDay(workouts): List<Pair<date, Float>>`
- 取最近 N 天（默认 30），缺失日补 0。

## 8. 错误处理

- 本地库为空 → 今天卡片空态 + 视图空态；设置里提示提取。
- 导出时无 `normalized.json` → 提示"请先提取数据"。
- SAF 取消 → 静默返回。
- Shizuku 未授权 → 设置页提示并引导。

## 9. 测试策略

- `ChartData` 纯函数（聚合/补零/取窗口）单测。
- `SettingsScreen`/导航为 Compose UI，逻辑部分（导出文件名、路由判断）抽成可测函数。
- 既有测试保持通过；真机验证：三页导航、今天卡片、视图图表、设置提取/导出/数据源、调试浏览器。
- 交付前 `./gradlew :app:testDebugUnitTest` 与 `:app:lintDebug`。

## 10. 任务拆分（每个任务独立提交）

- **U1** 导航重构（Scaffold+NavigationBar 三页）+ 删除旧列表页 + 占位页 + 迁移提取到设置。
- **U2** 设置页完整（数据源/提取/导出/高级/关于）。
- **U3** 今天页卡片。
- **U4** Canvas 图表组件 + `ChartData` 聚合 + 视图页。
- **U5** 真机复测。
