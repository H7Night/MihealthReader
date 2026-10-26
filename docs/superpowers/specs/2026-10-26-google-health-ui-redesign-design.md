# Google Health 风格界面重构设计

- 日期：2026-10-26
- 状态：设计已确认，待实现
- 参考：Google Health（`com.fitbit.FitbitMobile`）真机截图，2026-10-26 逆向
- 前提：数据层、Room、Shizuku/Root、Health Connect 已完成，本次只改界面与展示聚合

## 1. 目标

把现有 Material 3 默认风格全面替换为 Google Health 风格：

- 浅色内容层 + 大圆角平面卡片 + 彩色语义卡 + 胶囊按钮/图表。
- 使用 **Monet 动态取色**（Android 12+ 跟随壁纸；低版本回退一套 Google Health 风格静态色板）。
- 新增自包含设计系统（`ui/theme` + `ui/component`），所有页面复用。
- 保留现有功能边界：三 tab（今天 / 视图 / 设置）、提取、导出、Health Connect、睡眠/运动详情。
- 提升信息密度与可读性：大数值、右轴图表、分段区间、按日列表。

**非目标**：新增健康指标、云同步、目标设置、社交、后台任务改造、数据层 schema 变更。

## 2. 设计原则

1. **平面、无阴影**：用底色、留白和色块分区，不用 1dp 分割线堆叠。
2. **大圆角**：卡片 28dp，sheet 顶部 32dp，按钮与状态 pill 全圆角。
3. **一个屏幕一个视觉焦点**：今天页是圆环，详情页是大数值 + 图表。
4. **数据用色语义化**：步数/睡眠/心率各用一套容器色；数据线用 `secondary`。
5. **所有颜色走 theme token**：屏幕代码不出现 `Color(0x…)`。
6. **Monet 优先**：动态色取不到时回退静态 GH 色板，不退回 Material 默认紫。

## 3. 颜色

### 3.1 取色策略

```kotlin
@Composable
fun GhTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
)
```

- `Build.VERSION.SDK_INT >= 31`：`dynamicLightColorScheme(context)` / `dynamicDarkColorScheme(context)`。
- 否则：`GhLightFallback` / `GhDarkFallback`。
- 默认不使用品牌 seed；Monet 即品牌。
- 所有页面通过 `MaterialTheme.colorScheme` 与 `GhTone` 取色。

### 3.2 语义映射

| 用途 | Monet role |
|---|---|
| 今天页底 / 底栏 | `background` |
| 视图、详情、设置页底 | `surface` |
| 顶部/内容 sheet | `surface` / `surfaceContainerLowest` |
| 普通卡片 | `surfaceContainer` |
| 中性卡片、状态底 | `surfaceContainerHigh` |
| 步数/活动卡 | `primaryContainer` + `onPrimaryContainer` |
| 心率卡 | `secondaryContainer` + `onSecondaryContainer` |
| 睡眠卡 | `tertiaryContainer` + `onTertiaryContainer` |
| 数据线/柱 | `secondary` |
| 选中分段/主操作 | `primary` + `onPrimary` |
| 次操作胶囊 | `primaryContainer` + `onPrimaryContainer` |
| 正文/次要文字 | `onSurface` / `onSurfaceVariant` |
| 描边/空状态 | `outline` / `outlineVariant` |

`GhTone` 枚举：`Primary`、`Secondary`、`Tertiary`、`Neutral`。卡片 tone 只决定容器色与内容色，不硬编码颜色。

页面底的具体用法：今天页用 `background`（浅蓝灰）作为页底，底部 sheet 用 `surface`；视图、详情、设置页用 `surface`（白）作为页底，卡片用 `surfaceContainer`，保证卡片与页底有对比。

### 3.3 回退色板（API 26–30）

亮色：

| Role | Value |
|---|---|
| primary | `#1157CE` |
| onPrimary | `#FFFFFF` |
| primaryContainer | `#C2E7FF` |
| onPrimaryContainer | `#004A77` |
| secondary | `#00948A` |
| onSecondary | `#FFFFFF` |
| secondaryContainer | `#D9F2EF` |
| onSecondaryContainer | `#003D38` |
| tertiary | `#5A2F90` |
| onTertiary | `#FFFFFF` |
| tertiaryContainer | `#EDDCFF` |
| onTertiaryContainer | `#2A0B4E` |
| background | `#F0F4F9` |
| onBackground | `#1F1F1F` |
| surface | `#FFFFFF` |
| onSurface | `#1F1F1F` |
| surfaceVariant | `#E7EDF4` |
| onSurfaceVariant | `#444746` |
| outline | `#C4C7C5` |
| outlineVariant | `#DDE3EA` |
| surfaceContainerLowest | `#FFFFFF` |
| surfaceContainerLow | `#F7F9FC` |
| surfaceContainer | `#F0F4F9` |
| surfaceContainerHigh | `#E7EDF4` |
| surfaceContainerHighest | `#DDE3EA` |
| error | `#BA1A1A` |

暗色：

| Role | Value |
|---|---|
| primary | `#A8C7FA` |
| onPrimary | `#002F6C` |
| primaryContainer | `#28405F` |
| onPrimaryContainer | `#D3E3FD` |
| secondary | `#4CB4AD` |
| onSecondary | `#003733` |
| secondaryContainer | `#1B4B47` |
| onSecondaryContainer | `#B8EDE6` |
| tertiary | `#D0BCFF` |
| onTertiary | `#3B2166` |
| tertiaryContainer | `#4A3275` |
| onTertiaryContainer | `#EADDFF` |
| background | `#0F141A` |
| onBackground | `#E3E6EA` |
| surface | `#171C22` |
| onSurface | `#E3E6EA` |
| surfaceVariant | `#232A33` |
| onSurfaceVariant | `#A9B0B8` |
| outline | `#8C929A` |
| outlineVariant | `#3C4652` |
| surfaceContainerLowest | `#0C1015` |
| surfaceContainerLow | `#141920` |
| surfaceContainer | `#1B2129` |
| surfaceContainerHigh | `#232A33` |
| surfaceContainerHighest | `#2C343F` |
| error | `#F2B8B5` |

## 4. 字体

- `FontFamily.SansSerif`（Android 系统 Roboto / 厂商无衬线），不引入版权字体。
- 数值：`displayLarge` 56sp Bold、`displayMedium` 44sp Bold、`displaySmall` 36sp Bold，负字距。
- 区块标题：`headlineLarge` 32sp Bold、`headlineMedium` 28sp Bold、`headlineSmall` 24sp SemiBold。
- 卡片标题：`titleMedium` 18sp Medium；正文 `bodyLarge` 16sp / `bodyMedium` 14sp / `bodySmall` 12sp。
- 标签：`labelLarge` 14sp Medium、`labelMedium` 12sp Medium、`labelSmall` 11sp Medium。
- 数值与单位：数值用 `displaySmall`/`displayMedium`，单位用 `titleMedium` 且基线对齐。

## 5. 形状与间距

- Shapes：xs 6、sm 10、md 16、lg 28、xl 32 dp。
- 卡片 28dp；tile 28dp；sheet 顶部 32dp；按钮/状态 pill 50%。
- 图标托底：48×48dp，圆角 16dp（或 52×72dp 竖胶囊用于 hero 卡）。
- 间距 4dp 网格：页面左右 16dp、块间 16–20dp、卡片内 16–20dp、行内 12dp。
- 触控目标不小于 48dp。

## 6. 组件清单（`ui/component`）

| 组件 | 职责 |
|---|---|
| `GhCard` | 可点击/不可点击的平面卡片，默认 `surfaceContainer` |
| `GhTintedCard` | 按 `GhTone` 取色的彩色卡 |
| `GhSectionHeader` | 大标题 + 可选右侧动作 |
| `GhPageTitle` | 页面大标题 + 可选副标题 |
| `GhPillButton` | 胶囊主/次操作，支持图标、禁用 |
| `GhIconActionButton` | 48dp 圆形图标按钮 |
| `GhIconPill` | 卡片左侧图标托底 |
| `GhStatusPill` | 白色/高亮状态胶囊 |
| `GhSegmentedControl` | 天/周/月/3个月/年 分段胶囊 |
| `GhRangeHeader` | 区间文案 + 上一段/下一段/今天 |
| `GhMetricTile` | 2 列指标 tile：标题、大值、迷你图、状态 |
| `GhRing` | 活动圆环（Canvas，圆头弧线） |
| `GhListRow` | 日期/名称 + 数值的卡片行 |
| `GhSwitchRow` | 标题/副标题 + Switch |
| `GhEmptyState` | 图标 + 文案 + 可选操作 |
| `GhBottomBar` | 贴底三 tab，选中态淡色胶囊 |
| `GhTopBar` | 详情页返回 + 标题 + 动作 |

图表组件（`ui/charts`）：

- `GhChart`：右轴、柱/点线/面积三种样式，支持选中高亮。
- `GhStackedBars`：睡眠阶段堆叠柱。
- `GhMiniBars` / `GhMiniLine`：tile 内 7 日迷你图。
- 复用现有 `Hypnogram`，颜色由参数传入。

## 7. 导航与页面框架

- 保留三 tab：`今天 / 视图 / 设置`。
- `GhBottomBar` 固定贴底、全宽；仅一级路由显示。
- 详情路由（`detail/*`、`sleep_detail`、`workout_list`、`workout_detail/*`）隐藏底栏。
- 系统栏：`enableEdgeToEdge()`；`GhWindowEffects()` 设置状态栏/导航栏图标明暗；所有页面用 `statusBarsPadding` / `navigationBarsPadding`。
- 转场：tab 切换淡入；详情页 slide + fade；返回反向。

## 8. 今天页

结构：

1. 顶部：`今天` 大标题 + 右侧“上次更新 HH:mm”状态 pill。
2. Hero：
   - 左：`GhRing`，本周运动天数 `x/7`，中心 `x/7`，下方“本周 N 分钟”。
   - 右：三张竖向排列的 `GhTintedCard`：
     - 步数（Primary）：今日步数 + 距离。
     - 睡眠（Tertiary）：主睡眠 + 午睡总时长 + 深睡/午睡 pill。
     - 心率（Secondary）：最新心率 + 静息心率。
3. 分页点（3 个，当前第 1 个）——装饰性，为未来多组 hero 预留。
4. 操作行：`提取数据`、`同步 Health Connect` 两个淡蓝胶囊 + 设置方形按钮。
5. 白色 sheet（顶部 32dp 圆角）：
   - 标题“今天过得怎么样？”
   - 六个指标 chips（步数/睡眠/心率/血氧/运动/体重），点击进详情。
   - “最近记录”卡片：最近运动、最近睡眠、最近体重。
6. 空数据：sheet 内淡蓝提示卡 + “去设置”，替代 AlertDialog。

数据：`ParsedRepository` 现有 Flow；圆环用 `TodaySummary.weeklyWorkouts(workouts)`；最近记录取各列表首条。

## 9. 视图页

1. `视图` 大标题。
2. `主要指标`：2 列 `GhMetricTile`，共 6 张（步数/睡眠/心率/血氧/运动/体重）。
   - 每张：标题、大值+单位、7 日迷你图、状态 pill。
   - 点击：睡眠 → 睡眠详情；运动 → 运动列表；其余 → 指标详情。
3. `分类`：三张整行/双列分类卡（活动 / 睡眠 / 身体），图标 + 已追踪指标数，点击进入对应详情。
4. 移除视图页的月份选择器和单图；月/3月/年浏览全部移到详情页分段控件。

## 10. 指标详情页

1. `GhTopBar`：返回 + 指标名 + 更多（暂无动作时隐藏）。
2. `GhSegmentedControl`：天 / 周 / 月 / 3个月 / 年，默认周。
3. `GhRangeHeader`：区间文案 + 上一段 / 下一段 / 今天。
4. 大数值 + 单位。
5. 图表（≥2 个桶时显示）：
   - 步数/运动：胶囊柱，基线 0。
   - 心率/血氧：点线。
   - 体重：平滑面积线。
   - 右轴 3 个刻度；X 轴最多 7 个标签；选中桶有竖线 + 圆环。
6. `本区间统计`：最小 / 平均 / 最大三枚 pill。
7. `记录`：按桶倒序的 `GhListRow` 列表（日期 + 数值）。
8. 空区间：`GhEmptyState` + “本区间暂无数据”。

范围聚合规则：

| Range | 窗口 | 桶 | 步数/运动 | 心率/血氧/体重 |
|---|---|---|---|---|
| 天 | 当天 | 小时（样本类）/ 单桶（日汇总类） | 求和 | 按小时平均 |
| 周 | 周一起 7 天 | 日 | 求和 | 按日平均 |
| 月 | 当月 | 日 | 求和 | 按日平均 |
| 3个月 | 当月 + 前两月 | 周 | 求和 | 按周平均 |
| 年 | 当年 | 月 | 求和 | 按月平均 |

- 缺失日不补 0；图表只画有数据的桶。
- 周起始日使用 `Locale` 对应首日（默认周一，可通过参数注入以便测试）。

## 11. 睡眠详情页

1. `GhTopBar` + 日期前后切换（`GhRangeHeader` 风格）。
2. 大号总时长 + 深睡比例 pill。
3. `睡眠分析` 卡：2×2 统计（总时长 / 入睡 / 深睡比例 / 清醒次数）。
4. `睡眠时间轴` 卡：图例胶囊 + `Hypnogram` + 时间标记。
5. 阶段占比：横向堆叠条 + 百分比图例（深睡/浅睡/清醒/午睡）。
6. `睡眠解读`：`tertiaryContainer` 卡，大号评价 + 详细描述。
7. `入睡规律`：平均入睡 / 波动 / 评价 + 点线图 + “近 N 晚”。
8. 午睡提示 pill。

## 12. 运动列表 / 详情

列表：

1. `GhTopBar`：返回 + `运动` + 次数 pill。
2. 类型筛选胶囊：`全部` + 实际出现类型。
3. 运动卡片：运动色图标托底、类型、日期时间、时长、kcal、平均心率。
4. 空状态：`GhEmptyState`。

详情：

1. `GhTopBar`：返回 + 类型名。
2. Hero：淡色运动卡（运动色作容器、深色文字），图标 + 类型 + 日期时间 + 大 kcal。
3. 2×2 统计 tile：运动时长、平均心率、最大心率、距离（无距离隐藏）。
4. `心率 (BPM)` 卡：右轴折线 + 最小/平均/最大 pill；无逐点数据时显示空状态。

## 13. 设置页

1. `GhPageTitle("设置", 副标题)`。
2. `数据源` 卡：Root/Shizuku 选择（`GhSegmentedControl` 或两枚可选 pill）、授权状态 pill、Shizuku 请求权限胶囊、重启提示。
3. `数据提取` 卡：上次同步摘要、状态（Idle/Running/Success/Error）、主胶囊按钮。
4. `数据导出` 卡：说明 + 胶囊按钮 + 结果提示。
5. `Health Connect` 卡：`GhSwitchRow` + `立即同步` 胶囊 + 状态文案；不可用时隐藏。
6. `关于` 卡：应用名、版本、用途说明。
7. 分组间距 20dp；卡片 28dp；不使用零散 `Button`。

## 14. 图表规范

- 布局：左内容 + 右轴（3 个刻度：max / mid / min）；X 轴在下方，最多 7 个标签。
- 柱：圆头胶囊，间距约 3.5% 宽；基线下有 1dp 浅色底。
- 点线：圆点直径 7dp，线宽 2dp；选中点外圈 3dp。
- 面积线：线宽 3dp，渐变填充从 30% 到 2%。
- 空数据：画一条浅色基线 + 居中“暂无数据” pill。
- 入场：从 0 到目标值 300ms `FastOutSlowInEasing`；范围切换淡入。
- 颜色：数据色 `secondary`；睡眠堆叠用 `primary`/`tertiary`/`outline`/`secondaryContainer`。

## 15. 状态与文案

- 加载：`dataLoaded == false` 时显示 skeleton 卡片（浅色块），绝不先闪空状态。
- 空：`GhEmptyState`；今天页空数据用 sheet 内提示卡。
- 错误：沿用 `ExtractionViewModel` 的 snackbar/message；UI 不直接 emit。
- 文案：全部抽到 `res/values/strings.xml`（英文）与 `res/values-zh/strings.xml`（中文），屏幕不使用硬编码文本。
- 日期：使用系统 `Locale` 的 `DateFormat.getDateInstance(DateFormat.MEDIUM)`；区间用 en dash。
- 数字：沿用现有 `formatMinutes` / `formatDurationSeconds`；图表轴用整数或 1 位小数。

## 16. 无障碍

- 所有图标有 `contentDescription`；装饰性图标设为 `null`。
- 触控目标 ≥48dp。
- 文字对比度：文字只用 `onSurface`/`onSurfaceVariant`/容器 on-color。
- 图表提供文本摘要（最新值 + 区间统计），不依赖图形单独传达信息。

## 17. 文件结构

```
ui/theme/Color.kt          # fallback schemes
ui/theme/Type.kt           # typography
ui/theme/Shape.kt          # shapes
ui/theme/Theme.kt          # GhTheme + GhWindowEffects
ui/component/GhSurfaces.kt # card/tone/section/page title/icon pill/status pill
ui/component/GhButtons.kt  # pill/icon buttons
ui/component/GhNavigation.kt # bottom bar/top bar/segmented/range header
ui/component/GhMetricTile.kt
ui/component/GhRing.kt
ui/component/GhRows.kt     # list row/switch row/empty state
ui/charts/GhChart.kt       # axis/chart/stacked/mini charts
ui/charts/Charts.kt        # existing primitives (kept, restyled)
ui/charts/Hypnogram.kt     # kept
ui/screens/*               # rewritten
ui/AppNav.kt               # GhBottomBar + transitions
MainActivity.kt            # edge-to-edge + GhTheme
data/ChartSeries.kt        # range windows + bucket aggregation
res/values/strings.xml     # English
res/values-zh/strings.xml  # Chinese
```

删除：`ui/screens/MetricChartView.kt`、`ui/charts/AxisChart.kt`（迁移完成后无引用）。

## 18. 测试策略

纯函数单测：

- `ChartSeriesTest`：天/周/月/3月/年窗口、求和 vs 平均、时区、空数据、周起始日。
- `GhChartAxisTest`：空/单值/全零/正常值的刻度。
- `GhRingTest`：进度 0、部分、满、超出、total=0。
- `TodaySummaryTest`：本周运动天数/分钟。
- `MetricUiTest`：指标 → 图表样式/单位/路由。
- `SleepUiTest`：阶段占比之和为 100。
- `WorkoutUiTest`：类型筛选去重与排序。
- `StringsParityTest`：`values` 与 `values-zh` key 集合一致。

构建与真机：

- `./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug`
- 安装后逐页截图：亮色/暗色 × 今天/视图/设置/指标详情/睡眠/运动列表/运动详情。
- 检查：底栏隐藏、edge-to-edge、加载不闪空、空数据、深色对比度、图表右轴。

## 19. 风险与取舍

- **Monet 不可控**：不同壁纸下卡片色调会变化，无法保证与参考截图完全一致；这是动态取色的预期行为。
- **Google Sans 不可用**：使用系统无衬线，字重与字号对齐参考，字形略有差异。
- **范围聚合新增逻辑**：只做展示聚合，不写数据库；通过纯函数单测覆盖。
- **图标依赖**：新增 `material-icons-extended` 以获取 chevron/health 图标；release 由 R8 裁剪。
- **字符串迁移面大**：一次性把现有中文硬编码抽到资源，后续新增文案必须走资源。
