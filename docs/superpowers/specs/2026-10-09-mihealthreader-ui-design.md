# MihealthReader UI 重构设计（导航 + 图表 + 设置）

- 日期：2026-10-09
- 状态：已实现并真机验证（2026-10-09；记录见 `docs/manual-verification.md` §11–§21）
- 前置：数据层/本地库/Shizuku 已完成
- 实现记录：见本文 §11（含与设计的差异：调试浏览器被移除、图表改为按月份等）

## 1. 目标与范围

- 底部 **NavigationBar**：`今天 / 视图 / 设置`。
- **隐藏**现有列表视图（活动/睡眠/心率/血氧/运动/体重），改为**图表**呈现。
- 图表：**自绘 Canvas**，贴合 Material 3 / Google Health 风格。
- **提取 / 导出** 移到**设置页**；设置页含数据源（root/Shizuku）。
- ~~原始表**调试浏览器**移到设置页「高级/调试」保留~~ → **后续整体移除**（见 §11）。

不在本次：更复杂图表交互（缩放/十字线）、定时后台提取、正式多语言。

## 2. 导航结构

`AppNav` 改为 `Scaffold` + `NavigationBar`（3 项），`NavHost` 内含：

- `today` → `TodayScreen`（今天）
- `charts` → `ChartsScreen`（视图）
- `settings` → `SettingsScreen`（设置）

底部栏仅在 `today/charts/settings` 显示（按当前路由判断）。

删除：`ExtractionScreen`、`ActivityScreen`、`SleepScreen`、`HeartRateScreen`、`Spo2Screen`、`WorkoutScreen`、`BodyScreen`。

> 实现差异（见 §11）：`browse` 调试浏览器路由与 `TableListScreen`/`TableDetailScreen`/`BrowserViewModel`/`ExportRepository`
> 在后续提交中**整体移除**，未保留在设置页；同时导航跳转动画被禁用（直接进入主界面）。

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
- 图表类型（**实现调整为按月份**，见 §11）：
  - 步数/运动时长：近 30 天 **柱状图**。
  - 睡眠：近 30 天**堆叠柱状图**（深睡/浅睡/清醒）。
  - 心率/血氧/体重：**折线图**（按日聚合）。
- 聚合：按本地日期分组求均值/求和（纯函数，可测）。

## 5. 设置（SettingsScreen）

- **数据源**：`Root / Shizuku` 选择 + Shizuku 授权（沿用现有逻辑；切换后重启生效）。
- **提取数据**：按钮 + 状态（运行中/新增 N 条/无新数据/错误）+ 上次同步信息。
- **导出数据**：把 `<快照>/parsed/normalized.json` 通过 **SAF（ACTION_CREATE_DOCUMENT）** 保存到用户选择位置（默认文件名 `mihealth-normalized.json`）；无快照时提示先提取。
- ~~**高级 / 调试**：进入原始表浏览器（`browse`）~~ → **后续移除**（见 §11）。
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

## 11. 实现记录（与设计的差异，2026-10-09 起）

本节记录设计落地后由后续提交引入的调整，均已在 `docs/manual-verification.md` §11–§21 真机确认：

- **调试浏览器移除**：`browse` 路由与 `TableListScreen`/`TableDetailScreen`/`BrowserViewModel`/`ExportRepository`
  整体删除，设置页不再有「高级/调试」入口（`493b7b6`）。
- **导航动画禁用**：NavHost 进入/退出/回退过渡设为 `None`，冷启动直接进入「今天」。
- **平面排版**：参考 Google Health，卡片浅灰底 `surfaceVariant`、24dp 圆角、无阴影分割线；今天页改为
  2 列卡片网格 + 近 7 天迷你图（`820eb0e`、`7d02771`）。
- **莫奈动态取色**：Android 12+ 用 `dynamicLight/DarkColorScheme`，低版本回退青绿静态配色（`044f180`）。
- **图表按月份**：移除「最近 N 次」，默认本月并用 `‹ 2026 年 10 月 ›` 居中切换；本月无数据时回退最近 10 次
  （`58f2692`、`bd91fb8`）；补齐坐标轴与最小/平均/最大，折线改平滑面积、柱状改圆角（`a2e471d`、`26a77a5`）。
- **今天卡片可点击进详情**；新增 `SleepDetailScreen`（睡眠分析/解读/全天睡眠 hypnogram/入睡规律性，`f6d99a2`、
  `47d9600`、`39fbed9`）与 `WorkoutListScreen`/`WorkoutDetailScreen`（彩色头部 + 运动心率曲线，`ee8081e`）。
- **睡眠修复**：午睡不再覆盖主睡眠；今天页/全天睡眠显示「主睡眠 + 午睡」并按会话分组绘制时间标记
  （`ea19579`、`9115647`、`f5314b1`、`b5ce412`）。
- **其他**：空数据由整页替换改为 AlertDialog（`965a199`、`d0ce21e`）；冷启动 Splash 无缝修复（`d897429`）；
  自适应应用图标（`74dd6a7`）。
