# MihealthReader UI 重构实现计划（导航 + 图表 + 设置）

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans. Steps use checkbox (`- [ ]`).

**Goal:** 底部导航（今天/视图/设置）+ 自绘 Canvas 图表 + 设置页（提取/导出/数据源），隐藏旧列表页。

**Architecture:** `AppNav` 用 `Scaffold`+`NavigationBar`；新增 `TodayScreen`/`ChartsScreen`/`SettingsScreen`；`ui/charts/` 自绘组件；`data/ChartData.kt` 聚合纯函数；导出用 SAF。

**Tech Stack:** Kotlin、Compose Material 3、Canvas、SAF、JUnit4 + Robolectric。

**Spec:** `docs/superpowers/specs/2026-10-09-mihealthreader-ui-design.md`

## Global Constraints

- 底部栏仅 `today/charts/settings` 显示。
- 图表自绘 Canvas，取色用 `MaterialTheme.colorScheme`。
- 导出 = 规范化 JSON 经 SAF 保存。
- 删除旧列表页与 `ExtractionScreen`。
- 交付前通过 `./gradlew :app:testDebugUnitTest` 与 `:app:lintDebug`。

## Review Focus

1. 本地库为空 → 今天/视图空态不崩溃。测试 U3/U4。
2. 导出无 normalized.json → 提示先提取，不崩溃。测试 U2。
3. 图表空数据 → 占位不崩溃。测试 U4。
4. 聚合按本地日、缺失补零、窗口正确。测试 U4。
5. 底部栏在 browse 页隐藏。测试 U1（路由判断函数）。

---

### Task U1: 导航重构 + 设置骨架

**Files:**
- Create: `ui/screens/TodayScreen.kt`, `ChartsScreen.kt`, `SettingsScreen.kt`, `ui/nav/Routes.kt`
- Modify: `ui/AppNav.kt`, `MainActivity.kt`
- Delete: `ui/screens/ExtractionScreen.kt`, `ActivityScreen.kt`, `SleepScreen.kt`, `HeartRateScreen.kt`, `Spo2Screen.kt`, `WorkoutScreen.kt`, `BodyScreen.kt`
- Test: `ui/nav/RoutesTest.kt`

**Interfaces:**
- Produces: `object Routes { TODAY, CHARTS, SETTINGS, BROWSE }`；`fun showsBottomBar(route: String?): Boolean`；三个 Screen 骨架。

- [ ] **Step 1: 写失败测试**：`showsBottomBar` 仅对三主页返回 true。
- [ ] **Step 2: 运行确认失败。**
- [ ] **Step 3: 实现 Scaffold+NavigationBar+NavHost；删除旧页；`SettingsScreen` 暂放提取+数据源（迁移自 ExtractionScreen）；Today/Charts 占位。**
- [ ] **Step 4: 构建 + 全部单测通过。**
- [ ] **Step 5: 提交** `refactor: bottom navigation with today/charts/settings`。

---

### Task U2: 设置页完整（数据源/提取/导出/高级/关于）

**Files:**
- Modify: `ui/screens/SettingsScreen.kt`
- Create: `data/ExportManager.kt`
- Test: `data/ExportManagerTest.kt`

**Interfaces:**
- Produces: `ExportManager.latestNormalized(exportsDir): File?`；`defaultExportName(): String`；设置页含数据源、提取、导出（SAF）、高级（browse）、关于。

- [ ] **Step 1: 写失败测试**（`latestNormalized` 找最新快照的 normalized.json；无则 null）。
- [ ] **Step 2: 运行确认失败。**
- [ ] **Step 3: 实现 `ExportManager` + 设置页导出（`rememberLauncherForActivityResult(CreateDocument)` 写 Uri）。**
- [ ] **Step 4: 构建 + 单测通过。**
- [ ] **Step 5: 提交** `feat: settings with data source, extract and export`。

---

### Task U3: 今天页卡片

**Files:**
- Modify: `ui/screens/TodayScreen.kt`

**Interfaces:**
- Consumes: `ParsedRepository`、`LocalSummary`。
- Produces: 卡片（步数/睡眠/心率/运动/血氧/体重），空态提示。

- [ ] **Step 1: 实现 `TodayScreen`（收集各 Flow，取最新/当日）。**
- [ ] **Step 2: 构建 + 单测通过。**
- [ ] **Step 3: 提交** `feat: today overview cards`。

---

### Task U4: Canvas 图表 + 聚合 + 视图页

**Files:**
- Create: `ui/charts/LineChart.kt`, `BarChart.kt`, `StackedBarChart.kt`, `data/ChartData.kt`
- Modify: `ui/screens/ChartsScreen.kt`
- Test: `data/ChartDataTest.kt`

**Interfaces:**
- Produces: `LineChart(values, modifier, color)`、`BarChart(...)`、`StackedBarChart(rows, modifier)`；`ChartData.dailySteps/dailyMetricAvg/sleepDurations/workoutMinutesPerDay`（取最近 N 天、补零）。

- [ ] **Step 1: 写失败测试**（聚合：按本地日、补零、窗口长度）。
- [ ] **Step 2: 运行确认失败。**
- [ ] **Step 3: 实现聚合 + 三个 Canvas 组件 + `ChartsScreen`（指标 chips + 图表 + 空态）。**
- [ ] **Step 4: 构建 + lint + 单测通过。**
- [ ] **Step 5: 提交** `feat: canvas charts and charts screen`。

---

### Task U5: 真机复测

- [ ] **Step 1: 真机**：底部三页导航；今天卡片；视图切换指标与图表；设置数据源/提取/导出（保存 JSON）；高级调试浏览器。
- [ ] **Step 2: 记录结果到 `docs/manual-verification.md` 并提交** `docs: record ui verification`。

---

## Self-Review

- **Spec coverage**：导航→U1；设置/导出→U2；今天→U3；图表/聚合→U4；真机→U5。
- **类型一致性**：`Routes`/`showsBottomBar`/`ExportManager`/`ChartData` 跨任务一致。
- **Review Focus**：5 项均有测试/真机覆盖。
