# Google Health UI Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the stock Material 3 UI with a self-contained Google Health-style design system, Monet dynamic color, and rebuilt screens.

**Architecture:** Add `ui/theme` tokens (Monet + fallback), a `ui/component` library (cards, pills, segmented controls, ring, tiles, bottom bar), a right-axis chart layer, and a pure `ChartSeries` aggregation layer. Rewrite all screens against these; data/Room/Health Connect stay untouched.

**Tech Stack:** Kotlin 2.2.21, Jetpack Compose (BOM 2025.11.00), Material 3, Navigation Compose, Room, Hilt, Robolectric, JUnit 4.

**Spec:** `docs/superpowers/specs/2026-10-26-google-health-ui-redesign-design.md`

## Global Constraints

- Kotlin + Compose only; no XML layouts, no `View`.
- No external design-system dependency; theme/components live in this app.
- Default palette is **Monet** on API 31+; fixed fallback below 31.
- No `Color(0x…)` in screens; use theme tokens / `GhTone`.
- No hardcoded user-facing text; use `stringResource` with `values` + `values-zh`.
- Keep three tabs: 今天 / 视图 / 设置.
- Keep data contracts: `ParsedRepository`, `SleepSummary`, `ExtractionViewModel`.
- Edge-to-edge; bottom bar only on top-level routes.
- Do **not** run `git commit` unless the user explicitly asks.
- Windows commands use `.\gradlew.bat`.

## Review Focus

1. **Fallback color leak** — API 26–30 must never show Material default purple (Task 1 test).
2. **Dark-mode contrast** — tinted cards must use matching `on*Container` colors (Task 3 test).
3. **Cross-day/timezone aggregation** — midnight samples must bucket by local zone (Task 6 tests).
4. **Empty/loading flash** — `dataLoaded == false` shows placeholders, never empty states (Tasks 7–8; manual Task 14).
5. **Range boundaries** — Monday week start, full current month/year, “next” disabled at today (Tasks 6/9; manual Task 14).

---

### Task 1: Theme tokens, typography, shapes, window effects

**Files:**
- Create: `app/src/main/java/com/jhon/mihealthreader/ui/theme/Color.kt`
- Create: `app/src/main/java/com/jhon/mihealthreader/ui/theme/Type.kt`
- Create: `app/src/main/java/com/jhon/mihealthreader/ui/theme/Shape.kt`
- Modify: `app/src/main/java/com/jhon/mihealthreader/ui/theme/Theme.kt`
- Modify: `app/src/main/java/com/jhon/mihealthreader/MainActivity.kt`
- Modify: `app/src/main/java/com/jhon/mihealthreader/ui/health/PermissionsRationaleActivity.kt`
- Modify: `app/src/main/res/values/colors.xml`, `app/src/main/res/values-night/colors.xml`
- Modify: `gradle/libs.versions.toml`, `app/build.gradle.kts`
- Test: `app/src/test/java/com/jhon/mihealthreader/ui/theme/GhFallbackColorsTest.kt`

**Interfaces:** `GhTheme(...)`, `GhWindowEffects()`, `GhLightFallback`, `GhDarkFallback`, `GhTypography`, `GhShapes`.

- [ ] **Step 1: Write the failing fallback test**

```kotlin
package com.jhon.mihealthreader.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GhFallbackColorsTest {
    @Test fun `light fallback uses google health surfaces`() {
        assertEquals(Color(0xFFF0F4F9), GhLightFallback.background)
        assertEquals(Color(0xFFFFFFFF), GhLightFallback.surface)
        assertEquals(Color(0xFF1157CE), GhLightFallback.primary)
        assertEquals(Color(0xFFF0F4F9), GhLightFallback.surfaceContainer)
    }

    @Test fun `dark fallback uses deep blue surfaces`() {
        assertEquals(Color(0xFF0F141A), GhDarkFallback.background)
        assertEquals(Color(0xFF171C22), GhDarkFallback.surface)
        assertEquals(Color(0xFF1B2129), GhDarkFallback.surfaceContainer)
        assertEquals(Color(0xFFA8C7FA), GhDarkFallback.primary)
    }
}
```

- [ ] **Step 2: Run it and verify failure**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "com.jhon.mihealthreader.ui.theme.GhFallbackColorsTest"`
Expected: FAIL — `GhLightFallback` unresolved.

- [ ] **Step 3: Add icons dependency**

`gradle/libs.versions.toml`:

```toml
androidx-compose-material-icons-extended = { group = "androidx.compose.material", name = "material-icons-extended" }
```

`app/build.gradle.kts`:

```kotlin
implementation(libs.androidx.compose.material.icons.extended)
```

- [ ] **Step 4: Implement fallback schemes**

`ui/theme/Color.kt`:

```kotlin
package com.jhon.mihealthreader.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

val GhLightFallback = lightColorScheme(
    primary = Color(0xFF1157CE), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFC2E7FF), onPrimaryContainer = Color(0xFF004A77),
    secondary = Color(0xFF00948A), onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD9F2EF), onSecondaryContainer = Color(0xFF003D38),
    tertiary = Color(0xFF5A2F90), onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFEDDCFF), onTertiaryContainer = Color(0xFF2A0B4E),
    error = Color(0xFFBA1A1A), onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF0F4F9), onBackground = Color(0xFF1F1F1F),
    surface = Color(0xFFFFFFFF), onSurface = Color(0xFF1F1F1F),
    surfaceVariant = Color(0xFFE7EDF4), onSurfaceVariant = Color(0xFF444746),
    outline = Color(0xFFC4C7C5), outlineVariant = Color(0xFFDDE3EA),
    surfaceContainerLowest = Color(0xFFFFFFFF), surfaceContainerLow = Color(0xFFF7F9FC),
    surfaceContainer = Color(0xFFF0F4F9), surfaceContainerHigh = Color(0xFFE7EDF4),
    surfaceContainerHighest = Color(0xFFDDE3EA),
    inverseSurface = Color(0xFF2F3033), inverseOnSurface = Color(0xFFF1F0F4),
    inversePrimary = Color(0xFFA8C7FA), surfaceTint = Color(0xFF1157CE),
)

val GhDarkFallback = darkColorScheme(
    primary = Color(0xFFA8C7FA), onPrimary = Color(0xFF002F6C),
    primaryContainer = Color(0xFF28405F), onPrimaryContainer = Color(0xFFD3E3FD),
    secondary = Color(0xFF4CB4AD), onSecondary = Color(0xFF003733),
    secondaryContainer = Color(0xFF1B4B47), onSecondaryContainer = Color(0xFFB8EDE6),
    tertiary = Color(0xFFD0BCFF), onTertiary = Color(0xFF3B2166),
    tertiaryContainer = Color(0xFF4A3275), onTertiaryContainer = Color(0xFFEADDFF),
    error = Color(0xFFF2B8B5), onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18), onErrorContainer = Color(0xFFF9DEDC),
    background = Color(0xFF0F141A), onBackground = Color(0xFFE3E6EA),
    surface = Color(0xFF171C22), onSurface = Color(0xFFE3E6EA),
    surfaceVariant = Color(0xFF232A33), onSurfaceVariant = Color(0xFFA9B0B8),
    outline = Color(0xFF8C929A), outlineVariant = Color(0xFF3C4652),
    surfaceContainerLowest = Color(0xFF0C1015), surfaceContainerLow = Color(0xFF141920),
    surfaceContainer = Color(0xFF1B2129), surfaceContainerHigh = Color(0xFF232A33),
    surfaceContainerHighest = Color(0xFF2C343F),
    inverseSurface = Color(0xFFE3E6EA), inverseOnSurface = Color(0xFF2F3033),
    inversePrimary = Color(0xFF1157CE), surfaceTint = Color(0xFFA8C7FA),
)
```

- [ ] **Step 5: Add typography and shapes**

`ui/theme/Type.kt`:

```kotlin
package com.jhon.mihealthreader.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private fun style(size: Int, weight: FontWeight, line: Int, tracking: Double = 0.0) =
    TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = weight,
        fontSize = size.sp,
        lineHeight = line.sp,
        letterSpacing = tracking.sp,
    )

val GhTypography = Typography(
    displayLarge = style(56, FontWeight.Bold, 60, -1.0),
    displayMedium = style(44, FontWeight.Bold, 48, -0.8),
    displaySmall = style(36, FontWeight.Bold, 40, -0.6),
    headlineLarge = style(32, FontWeight.Bold, 38, -0.5),
    headlineMedium = style(28, FontWeight.Bold, 34, -0.3),
    headlineSmall = style(24, FontWeight.SemiBold, 30),
    titleLarge = style(22, FontWeight.SemiBold, 28),
    titleMedium = style(18, FontWeight.Medium, 24),
    titleSmall = style(16, FontWeight.Medium, 22),
    bodyLarge = style(16, FontWeight.Normal, 24),
    bodyMedium = style(14, FontWeight.Normal, 20),
    bodySmall = style(12, FontWeight.Normal, 18),
    labelLarge = style(14, FontWeight.Medium, 20),
    labelMedium = style(12, FontWeight.Medium, 16),
    labelSmall = style(11, FontWeight.Medium, 14),
)
```

`ui/theme/Shape.kt`:

```kotlin
package com.jhon.mihealthreader.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val GhShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp),
)
```

- [ ] **Step 6: Rewrite the theme entry point**

`ui/theme/Theme.kt`:

```kotlin
package com.jhon.mihealthreader.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
fun GhTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val context = LocalContext.current
    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> GhDarkFallback
        else -> GhLightFallback
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = GhTypography,
        shapes = GhShapes,
        content = content,
    )
}

@Composable
fun GhWindowEffects() {
    val view = LocalView.current
    if (view.isInEditMode) return
    val window = (view.context as? Activity)?.window ?: return
    val dark = isSystemInDarkTheme()
    SideEffect {
        val controller = WindowCompat.getInsetsController(window, view)
        controller.isAppearanceLightStatusBars = !dark
        controller.isAppearanceLightNavigationBars = !dark
    }
}
```

- [ ] **Step 7: Point MainActivity and the rationale activity at the new theme**

`MainActivity`:

```kotlin
setContent {
    GhTheme {
        GhWindowEffects()
        AppNav(extractionViewModel)
    }
}
```

`PermissionsRationaleActivity`: replace the `MiHealthTheme` import/call with `GhTheme` (keep the existing Surface/Column layout for now; Task 3 components are not available yet):

```kotlin
setContent {
    GhTheme {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.padding(24.dp)) {
                Text(getString(R.string.label_health_connect_rationale),
                    style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
```

- [ ] **Step 8: Align the splash/window background**

`res/values/colors.xml`:

```xml
<color name="app_background">#F0F4F9</color>
<color name="ic_launcher_background">#006A60</color>
```

`res/values-night/colors.xml`:

```xml
<color name="app_background">#0F141A</color>
```

- [ ] **Step 9: Run test and build**

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.jhon.mihealthreader.ui.theme.GhFallbackColorsTest"
.\gradlew.bat :app:assembleDebug
```

Expected: PASS / SUCCESS.

- [ ] **Step 10: Checkpoint**

```powershell
git add app/src/main/java/com/jhon/mihealthreader/ui/theme app/src/main/java/com/jhon/mihealthreader/MainActivity.kt app/src/test/java/com/jhon/mihealthreader/ui/theme app/build.gradle.kts gradle/libs.versions.toml app/src/main/res/values/colors.xml app/src/main/res/values-night/colors.xml
# commit only if the user asked
```

---

### Task 2: String resources and metric labels

**Files:**
- Modify: `app/src/main/res/values/strings.xml`
- Create: `app/src/main/res/values-zh/strings.xml`
- Modify: `app/src/main/java/com/jhon/mihealthreader/ui/Metric.kt`
- Test: `app/src/test/java/com/jhon/mihealthreader/StringsParityTest.kt`

**Interfaces:** `Metric(@StringRes labelRes, @StringRes unitRes, chartKind, sumBuckets)`, `MetricChartKind.Bars/Dotted/Area`; all string keys for later tasks.

- [ ] **Step 1: Write the failing parity test**

```kotlin
package com.jhon.mihealthreader

import org.junit.Assert.assertEquals
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class StringsParityTest {
    private fun keys(path: String): Set<String> {
        val file = listOf(File(path), File("app/$path")).firstOrNull { it.exists() } ?: error("missing $path")
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val nodes = doc.getElementsByTagName("string")
        return (0 until nodes.length).map { (nodes.item(it) as Element).getAttribute("name") }.toSet()
    }

    @Test fun `values and values-zh have identical keys`() {
        assertEquals(keys("src/main/res/values/strings.xml"), keys("src/main/res/values-zh/strings.xml"))
    }
}
```

- [ ] **Step 2: Run it and verify failure**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "com.jhon.mihealthreader.StringsParityTest"`
Expected: FAIL — `values-zh/strings.xml` missing.

- [ ] **Step 3: Replace English strings**

`res/values/strings.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">MihealthReader</string>
    <string name="label_health_connect_rationale">MihealthReader writes parsed steps, heart rate, SpO2, sleep and workouts to Health Connect. All data stays on this device.</string>
    <string name="nav_today">Today</string>
    <string name="nav_charts">Insights</string>
    <string name="nav_settings">Settings</string>
    <string name="action_back">Back</string>
    <string name="action_more">More</string>
    <string name="action_retry">Retry</string>
    <string name="action_cancel">Cancel</string>
    <string name="action_confirm">OK</string>
    <string name="action_extract">Extract data</string>
    <string name="action_extracting">Extracting…</string>
    <string name="action_extract_again">Extract again</string>
    <string name="action_sync">Sync now</string>
    <string name="action_sync_health_connect">Sync Health Connect</string>
    <string name="action_go_settings">Open settings</string>
    <string name="action_export">Export JSON</string>
    <string name="action_request_shizuku">Grant Shizuku</string>
    <string name="action_today">Today</string>
    <string name="action_previous">Previous</string>
    <string name="action_next">Next</string>
    <string name="label_no_data">No data</string>
    <string name="label_loading">Loading…</string>
    <string name="label_unknown">—</string>
    <string name="unit_steps">steps</string>
    <string name="unit_bpm">bpm</string>
    <string name="unit_percent">%</string>
    <string name="unit_kg">kg</string>
    <string name="unit_kcal">kcal</string>
    <string name="unit_minutes">min</string>
    <string name="status_updated">Updated %1$s</string>
    <string name="title_today">Today</string>
    <string name="title_charts">Insights</string>
    <string name="title_settings">Settings</string>
    <string name="section_main_metrics">Key metrics</string>
    <string name="section_categories">Categories</string>
    <string name="section_recent">Recent</string>
    <string name="section_stats">This period</string>
    <string name="section_records">Records</string>
    <string name="category_activity">Activity</string>
    <string name="category_sleep">Sleep</string>
    <string name="category_body">Body</string>
    <string name="tracked_metrics">%1$d tracked</string>
    <string name="metric_steps">Steps</string>
    <string name="metric_sleep">Sleep</string>
    <string name="metric_hr">Heart rate</string>
    <string name="metric_spo2">SpO2</string>
    <string name="metric_workout">Workout</string>
    <string name="metric_body">Weight</string>
    <string name="range_day">Day</string>
    <string name="range_week">Week</string>
    <string name="range_month">Month</string>
    <string name="range_three_months">3 months</string>
    <string name="range_year">Year</string>
    <string name="range_separator"> – </string>
    <string name="stat_min">Min</string>
    <string name="stat_avg">Avg</string>
    <string name="stat_max">Max</string>
    <string name="empty_metric">No %1$s data yet</string>
    <string name="today_ring_label">This week</string>
    <string name="today_ring_progress">%1$d/%2$d</string>
    <string name="today_workout_minutes">%1$d min this week</string>
    <string name="today_status_running">Updating…</string>
    <string name="today_greeting">How’s your day?</string>
    <string name="today_steps_status_distance">%1$s km</string>
    <string name="today_sleep_deep">Deep %1$d min</string>
    <string name="today_sleep_nap">Incl. nap %1$d min</string>
    <string name="today_hr_resting">Resting %1$s</string>
    <string name="today_recent_workout">%1$s · %2$s</string>
    <string name="today_recent_sleep">Sleep %1$s</string>
    <string name="today_recent_weight">Weight %1$s kg</string>
    <string name="today_no_data_title">No data yet</string>
    <string name="today_no_data_message">Extract your Mi Fitness data once in Settings.</string>
    <string name="detail_no_range_data">No data in this period</string>
    <string name="detail_avg_hr">Avg HR</string>
    <string name="detail_max_hr">Max HR</string>
    <string name="detail_duration">Duration</string>
    <string name="detail_distance">Distance</string>
    <string name="detail_calories">Calories</string>
    <string name="detail_records">Records</string>
    <string name="sleep_title">Sleep</string>
    <string name="sleep_total">Total sleep</string>
    <string name="sleep_bedtime">Bedtime</string>
    <string name="sleep_wake">Wake</string>
    <string name="sleep_deep_ratio">Deep sleep</string>
    <string name="sleep_awake_count">Awake</string>
    <string name="sleep_deep">Deep</string>
    <string name="sleep_light">Light</string>
    <string name="sleep_awake">Awake</string>
    <string name="sleep_nap">Nap</string>
    <string name="sleep_rem">REM</string>
    <string name="sleep_analysis">Sleep analysis</string>
    <string name="sleep_interpretation">Insights</string>
    <string name="sleep_timeline">Timeline</string>
    <string name="sleep_regularity">Bedtime regularity</string>
    <string name="sleep_avg_bedtime">Avg bedtime</string>
    <string name="sleep_variation">Variation</string>
    <string name="sleep_rating">Rating</string>
    <string name="sleep_regular">Regular</string>
    <string name="sleep_somewhat_regular">Fairly regular</string>
    <string name="sleep_irregular">Irregular</string>
    <string name="sleep_recent_nights">Last %1$d nights</string>
    <string name="sleep_naps_summary">Incl. %1$d min of naps (%2$d)</string>
    <string name="sleep_no_stage">No stage data</string>
    <string name="sleep_no_data">No sleep records yet</string>
    <string name="workout_title">Workouts</string>
    <string name="workout_count">%1$d sessions</string>
    <string name="workout_all">All</string>
    <string name="workout_summary">Summary</string>
    <string name="workout_heart_rate">Heart rate (BPM)</string>
    <string name="workout_no_samples">No heart-rate samples</string>
    <string name="workout_empty">No workouts yet</string>
    <string name="settings_subtitle">Sources, extraction &amp; sync</string>
    <string name="settings_source">Data source</string>
    <string name="settings_source_current">Current: %1$s (%2$s)</string>
    <string name="settings_source_granted">Granted</string>
    <string name="settings_source_not_granted">Not granted</string>
    <string name="settings_source_checking">Checking…</string>
    <string name="settings_source_restart">Restart the app after switching sources</string>
    <string name="settings_shizuku_hint">Shizuku must run as root to read data</string>
    <string name="settings_extraction">Extraction</string>
    <string name="settings_export">Export</string>
    <string name="settings_export_hint">Export normalized JSON (normalized.json)</string>
    <string name="settings_export_empty">Extract data first</string>
    <string name="settings_export_done">Exported to the selected location</string>
    <string name="settings_export_failed">Export failed</string>
    <string name="settings_health_connect">Health Connect</string>
    <string name="settings_sync_hc">Sync to Health Connect</string>
    <string name="settings_about">About</string>
    <string name="settings_about_body">Reads the local Mi Fitness database for on-device records and analysis.</string>
    <string name="settings_version">Version %1$s</string>
    <string name="settings_last_sync">Last sync: %1$s</string>
    <string name="settings_never_synced">Never synced</string>
    <string name="settings_summary">%1$s · %2$d step samples · %3$d sleep sessions</string>
    <string name="settings_extract_new">%1$d new records</string>
    <string name="settings_extract_none">No new data</string>
    <string name="settings_error">Error: %1$s</string>
    <string name="settings_source_root">Root</string>
    <string name="settings_source_shizuku">Shizuku</string>
    <string name="settings_hc_unavailable">Health Connect isn’t available on this device</string>
</resources>
```

- [ ] **Step 4: Add Chinese strings**

`res/values-zh/strings.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">MihealthReader</string>
    <string name="label_health_connect_rationale">MihealthReader 会把已解析的步数、心率、血氧、睡眠与运动写入 Health Connect，数据仅保存在本机。</string>
    <string name="nav_today">今天</string>
    <string name="nav_charts">视图</string>
    <string name="nav_settings">设置</string>
    <string name="action_back">返回</string>
    <string name="action_more">更多</string>
    <string name="action_retry">重试</string>
    <string name="action_cancel">取消</string>
    <string name="action_confirm">确定</string>
    <string name="action_extract">提取数据</string>
    <string name="action_extracting">提取中…</string>
    <string name="action_extract_again">再次提取</string>
    <string name="action_sync">立即同步</string>
    <string name="action_sync_health_connect">同步 Health Connect</string>
    <string name="action_go_settings">去设置</string>
    <string name="action_export">导出数据（JSON）</string>
    <string name="action_request_shizuku">请求 Shizuku 权限</string>
    <string name="action_today">今天</string>
    <string name="action_previous">上一段</string>
    <string name="action_next">下一段</string>
    <string name="label_no_data">暂无数据</string>
    <string name="label_loading">读取中…</string>
    <string name="label_unknown">—</string>
    <string name="unit_steps">步</string>
    <string name="unit_bpm">bpm</string>
    <string name="unit_percent">%</string>
    <string name="unit_kg">kg</string>
    <string name="unit_kcal">kcal</string>
    <string name="unit_minutes">分钟</string>
    <string name="status_updated">上次更新 %1$s</string>
    <string name="title_today">今天</string>
    <string name="title_charts">视图</string>
    <string name="title_settings">设置</string>
    <string name="section_main_metrics">主要指标</string>
    <string name="section_categories">分类</string>
    <string name="section_recent">最近记录</string>
    <string name="section_stats">本区间统计</string>
    <string name="section_records">记录</string>
    <string name="category_activity">活动</string>
    <string name="category_sleep">睡眠</string>
    <string name="category_body">身体</string>
    <string name="tracked_metrics">已追踪 %1$d 项</string>
    <string name="metric_steps">步数</string>
    <string name="metric_sleep">睡眠</string>
    <string name="metric_hr">心率</string>
    <string name="metric_spo2">血氧</string>
    <string name="metric_workout">运动</string>
    <string name="metric_body">体重</string>
    <string name="range_day">天</string>
    <string name="range_week">周</string>
    <string name="range_month">月</string>
    <string name="range_three_months">3 个月</string>
    <string name="range_year">年</string>
    <string name="range_separator"> – </string>
    <string name="stat_min">最小</string>
    <string name="stat_avg">平均</string>
    <string name="stat_max">最大</string>
    <string name="empty_metric">还没有%1$s数据</string>
    <string name="today_ring_label">本周运动</string>
    <string name="today_ring_progress">%1$d/%2$d</string>
    <string name="today_workout_minutes">本周 %1$d 分钟</string>
    <string name="today_status_running">更新中…</string>
    <string name="today_greeting">今天过得怎么样？</string>
    <string name="today_steps_status_distance">%1$s 公里</string>
    <string name="today_sleep_deep">深睡 %1$d 分钟</string>
    <string name="today_sleep_nap">含午睡 %1$d 分钟</string>
    <string name="today_hr_resting">静息 %1$s</string>
    <string name="today_recent_workout">%1$s · %2$s</string>
    <string name="today_recent_sleep">睡眠 %1$s</string>
    <string name="today_recent_weight">体重 %1$s kg</string>
    <string name="today_no_data_title">还没有数据</string>
    <string name="today_no_data_message">请先在设置中提取一次「小米运动健康」数据。</string>
    <string name="detail_no_range_data">本区间暂无数据</string>
    <string name="detail_avg_hr">平均心率</string>
    <string name="detail_max_hr">最大心率</string>
    <string name="detail_duration">运动时长</string>
    <string name="detail_distance">距离</string>
    <string name="detail_calories">消耗</string>
    <string name="detail_records">记录</string>
    <string name="sleep_title">睡眠</string>
    <string name="sleep_total">睡眠总时长</string>
    <string name="sleep_bedtime">入睡时间</string>
    <string name="sleep_wake">起床时间</string>
    <string name="sleep_deep_ratio">深睡比例</string>
    <string name="sleep_awake_count">清醒次数</string>
    <string name="sleep_deep">深睡</string>
    <string name="sleep_light">浅睡</string>
    <string name="sleep_awake">清醒</string>
    <string name="sleep_nap">午睡</string>
    <string name="sleep_rem">快速眼动</string>
    <string name="sleep_analysis">睡眠分析</string>
    <string name="sleep_interpretation">睡眠解读</string>
    <string name="sleep_timeline">睡眠时间轴</string>
    <string name="sleep_regularity">入睡规律</string>
    <string name="sleep_avg_bedtime">平均入睡</string>
    <string name="sleep_variation">波动</string>
    <string name="sleep_rating">评价</string>
    <string name="sleep_regular">规律</string>
    <string name="sleep_somewhat_regular">较规律</string>
    <string name="sleep_irregular">不规律</string>
    <string name="sleep_recent_nights">近 %1$d 晚</string>
    <string name="sleep_naps_summary">含午睡 %1$d 分钟（%2$d 次）</string>
    <string name="sleep_no_stage">无阶段数据</string>
    <string name="sleep_no_data">还没有睡眠记录</string>
    <string name="workout_title">运动</string>
    <string name="workout_count">%1$d 次</string>
    <string name="workout_all">全部</string>
    <string name="workout_summary">运动汇总</string>
    <string name="workout_heart_rate">心率 (BPM)</string>
    <string name="workout_no_samples">无逐点心率数据</string>
    <string name="workout_empty">还没有运动记录</string>
    <string name="settings_subtitle">数据源、提取与同步</string>
    <string name="settings_source">数据源</string>
    <string name="settings_source_current">当前：%1$s（%2$s）</string>
    <string name="settings_source_granted">已授权</string>
    <string name="settings_source_not_granted">未授权</string>
    <string name="settings_source_checking">检测中…</string>
    <string name="settings_source_restart">切换数据源后请重启 App 生效</string>
    <string name="settings_shizuku_hint">Shizuku 需以 root 启动才能读取数据</string>
    <string name="settings_extraction">数据提取</string>
    <string name="settings_export">数据导出</string>
    <string name="settings_export_hint">导出规范化 JSON（normalized.json）</string>
    <string name="settings_export_empty">请先提取数据</string>
    <string name="settings_export_done">已导出到所选位置</string>
    <string name="settings_export_failed">导出失败</string>
    <string name="settings_health_connect">Health Connect</string>
    <string name="settings_sync_hc">同步到 Health Connect</string>
    <string name="settings_about">关于</string>
    <string name="settings_about_body">读取「小米运动健康」本地数据库并做本地记录与分析。</string>
    <string name="settings_version">版本 %1$s</string>
    <string name="settings_last_sync">上次同步：%1$s</string>
    <string name="settings_never_synced">尚未同步</string>
    <string name="settings_summary">%1$s · 步数样本 %2$d · 睡眠 %3$d 段</string>
    <string name="settings_extract_new">新增 %1$d 条记录</string>
    <string name="settings_extract_none">无新数据</string>
    <string name="settings_error">错误：%1$s</string>
    <string name="settings_source_root">Root</string>
    <string name="settings_source_shizuku">Shizuku</string>
    <string name="settings_hc_unavailable">此设备不支持 Health Connect</string>
</resources>
```

- [ ] **Step 5: Update `Metric.kt`**

```kotlin
package com.jhon.mihealthreader.ui

import androidx.annotation.StringRes
import com.jhon.mihealthreader.R

enum class MetricChartKind { Bars, Dotted, Area }

enum class Metric(
    @StringRes val labelRes: Int,
    @StringRes val unitRes: Int,
    val chartKind: MetricChartKind,
    val sumBuckets: Boolean,
) {
    STEPS(R.string.metric_steps, R.string.unit_steps, MetricChartKind.Bars, true),
    SLEEP(R.string.metric_sleep, R.string.unit_minutes, MetricChartKind.Bars, true),
    HR(R.string.metric_hr, R.string.unit_bpm, MetricChartKind.Dotted, false),
    SPO2(R.string.metric_spo2, R.string.unit_percent, MetricChartKind.Dotted, false),
    WORKOUT(R.string.metric_workout, R.string.unit_minutes, MetricChartKind.Bars, true),
    BODY(R.string.metric_body, R.string.unit_kg, MetricChartKind.Area, false),
}
```

- [ ] **Step 6: Fix the two current `metric.label` usages**

In `ChartsScreen.kt`: `label = { Text(stringResource(m.labelRes)) }`.
In `MetricDetailScreen.kt`: `Text(stringResource(metric.labelRes), ...)`.

- [ ] **Step 7: Run the parity test**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "com.jhon.mihealthreader.StringsParityTest"`
Expected: PASS.

- [ ] **Step 8: Checkpoint**

```powershell
git add app/src/main/res/values/strings.xml app/src/main/res/values-zh app/src/main/java/com/jhon/mihealthreader/ui/Metric.kt app/src/test/java/com/jhon/mihealthreader/StringsParityTest.kt
# commit only if the user asked
```

---

### Task 3: Core surfaces and buttons

**Files:**
- Create: `app/src/main/java/com/jhon/mihealthreader/ui/component/GhSurfaces.kt`
- Create: `app/src/main/java/com/jhon/mihealthreader/ui/component/GhButtons.kt`
- Test: `app/src/test/java/com/jhon/mihealthreader/ui/component/GhToneTest.kt`

**Interfaces:** `GhTone`, `ghToneContainer(ColorScheme, GhTone)`, `ghToneContent(ColorScheme, GhTone)`, `GhCard`, `GhTintedCard`, `GhSectionHeader`, `GhPageTitle`, `GhIconPill`, `GhStatusPill`, `GhPillButton`, `GhIconActionButton`.

- [ ] **Step 1: Write the failing tone test**

```kotlin
package com.jhon.mihealthreader.ui.component

import com.jhon.mihealthreader.ui.theme.GhDarkFallback
import com.jhon.mihealthreader.ui.theme.GhLightFallback
import org.junit.Assert.assertEquals
import org.junit.Test

class GhToneTest {
    @Test fun `light tones use matching containers`() {
        assertEquals(GhLightFallback.primaryContainer, ghToneContainer(GhLightFallback, GhTone.Primary))
        assertEquals(GhLightFallback.secondaryContainer, ghToneContainer(GhLightFallback, GhTone.Secondary))
        assertEquals(GhLightFallback.tertiaryContainer, ghToneContainer(GhLightFallback, GhTone.Tertiary))
        assertEquals(GhLightFallback.surfaceContainerHigh, ghToneContainer(GhLightFallback, GhTone.Neutral))
    }

    @Test fun `dark tones use matching on-colors`() {
        assertEquals(GhDarkFallback.onPrimaryContainer, ghToneContent(GhDarkFallback, GhTone.Primary))
        assertEquals(GhDarkFallback.onSecondaryContainer, ghToneContent(GhDarkFallback, GhTone.Secondary))
        assertEquals(GhDarkFallback.onTertiaryContainer, ghToneContent(GhDarkFallback, GhTone.Tertiary))
        assertEquals(GhDarkFallback.onSurface, ghToneContent(GhDarkFallback, GhTone.Neutral))
    }
}
```

- [ ] **Step 2: Run it and verify failure**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "com.jhon.mihealthreader.ui.component.GhToneTest"`
Expected: FAIL — `GhTone` unresolved.

- [ ] **Step 3: Implement surfaces**

`ui/component/GhSurfaces.kt`:

```kotlin
package com.jhon.mihealthreader.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

enum class GhTone { Primary, Secondary, Tertiary, Neutral }

fun ghToneContainer(scheme: ColorScheme, tone: GhTone): Color = when (tone) {
    GhTone.Primary -> scheme.primaryContainer
    GhTone.Secondary -> scheme.secondaryContainer
    GhTone.Tertiary -> scheme.tertiaryContainer
    GhTone.Neutral -> scheme.surfaceContainerHigh
}

fun ghToneContent(scheme: ColorScheme, tone: GhTone): Color = when (tone) {
    GhTone.Primary -> scheme.onPrimaryContainer
    GhTone.Secondary -> scheme.onSecondaryContainer
    GhTone.Tertiary -> scheme.onTertiaryContainer
    GhTone.Neutral -> scheme.onSurface
}

@Composable
fun GhCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    color: Color = MaterialTheme.colorScheme.surfaceContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    content: @Composable ColumnScope.() -> Unit,
) {
    val body: @Composable () -> Unit = { Column(Modifier.padding(16.dp), content = content) }
    if (onClick != null) {
        Surface(onClick, modifier, shape = MaterialTheme.shapes.large, color = color, contentColor = contentColor) { body() }
    } else {
        Surface(modifier, shape = MaterialTheme.shapes.large, color = color, contentColor = contentColor) { body() }
    }
}

@Composable
fun GhTintedCard(
    tone: GhTone,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    GhCard(modifier, onClick, ghToneContainer(scheme, tone), ghToneContent(scheme, tone), content)
}

@Composable
fun GhSectionHeader(title: String, modifier: Modifier = Modifier, action: (@Composable RowScope.() -> Unit)? = null) {
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        action?.let { Row(content = it) }
    }
}

@Composable
fun GhPageTitle(title: String, modifier: Modifier = Modifier, subtitle: String? = null) {
    Column(modifier) {
        Text(title, style = MaterialTheme.typography.headlineLarge)
        subtitle?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
fun GhIconPill(
    icon: ImageVector,
    contentDescription: String?,
    container: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(container),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, contentDescription, tint = contentColor, modifier = Modifier.size(24.dp)) }
}

@Composable
fun GhStatusPill(
    text: String,
    modifier: Modifier = Modifier,
    container: Color = MaterialTheme.colorScheme.surfaceContainerLowest,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Surface(color = container, contentColor = contentColor, shape = CircleShape, modifier = modifier) {
        Text(text, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
    }
}
```

- [ ] **Step 4: Implement buttons**

`ui/component/GhButtons.kt`:

```kotlin
package com.jhon.mihealthreader.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun GhPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    emphasized: Boolean = false,
) {
    val scheme = MaterialTheme.colorScheme
    val container = if (emphasized) scheme.primary else scheme.primaryContainer
    val content = if (emphasized) scheme.onPrimary else scheme.onPrimaryContainer
    Surface(onClick, enabled = enabled, modifier = modifier.heightIn(min = 48.dp),
        color = container, contentColor = content, shape = CircleShape) {
        Row(
            Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            icon?.let { Icon(it, contentDescription = null, modifier = Modifier.size(20.dp)) }
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun GhIconActionButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    container: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Surface(onClick, enabled = enabled, modifier = modifier.size(48.dp),
        color = container, contentColor = contentColor, shape = CircleShape) {
        Box(contentAlignment = Alignment.Center) { Icon(icon, contentDescription, modifier = Modifier.size(22.dp)) }
    }
}
```

- [ ] **Step 5: Run test and build**

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.jhon.mihealthreader.ui.component.GhToneTest"
.\gradlew.bat :app:assembleDebug
```

- [ ] **Step 6: Checkpoint**

```powershell
git add app/src/main/java/com/jhon/mihealthreader/ui/component app/src/test/java/com/jhon/mihealthreader/ui/component
# commit only if the user asked
```

---

### Task 4: Ring, navigation, rows, metric tile

**Files:**
- Create: `ui/component/GhRing.kt`
- Create: `ui/component/GhNavigation.kt`
- Create: `ui/component/GhRows.kt`
- Create: `ui/component/GhMetricTile.kt`
- Test: `app/src/test/java/com/jhon/mihealthreader/ui/component/GhRingTest.kt`

**Interfaces:** `ghRingProgress(Int, Int): Float`, `GhRing`, `GhTab(route, @StringRes labelRes, icon)`, `GhBottomBar`, `GhTopBar`, `GhSegment<T>`, `GhSegmentedControl`, `GhRangeHeader`, `GhListRow`, `GhSwitchRow`, `GhEmptyState`, `GhMetricTile`.

- [ ] **Step 1: Write the failing ring test**

```kotlin
package com.jhon.mihealthreader.ui.component

import org.junit.Assert.assertEquals
import org.junit.Test

class GhRingTest {
    @Test fun `zero total is empty`() = assertEquals(0f, ghRingProgress(3, 0), 0f)
    @Test fun `partial progress is clamped`() {
        assertEquals(3f / 7f, ghRingProgress(3, 7), 0.0001f)
        assertEquals(1f, ghRingProgress(9, 7), 0f)
        assertEquals(0f, ghRingProgress(-1, 7), 0f)
    }
}
```

- [ ] **Step 2: Run it and verify failure**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "com.jhon.mihealthreader.ui.component.GhRingTest"`
Expected: FAIL — `ghRingProgress` unresolved.

- [ ] **Step 3: Implement `GhRing`**

```kotlin
package com.jhon.mihealthreader.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

fun ghRingProgress(completed: Int, total: Int): Float =
    if (total <= 0) 0f else (completed.toFloat() / total).coerceIn(0f, 1f)

@Composable
fun GhRing(progress: Float, centerValue: String, centerLabel: String?, modifier: Modifier = Modifier) {
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f), label = "ring")
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    val color = MaterialTheme.colorScheme.primary
    Box(modifier.aspectRatio(1f), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 28.dp.toPx()
            val inset = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(track, 0f, 360f, false, Offset(inset, inset), arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round))
            drawArc(color, -90f, 360f * animated, false, Offset(inset, inset), arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(centerValue, style = MaterialTheme.typography.displayMedium)
            centerLabel?.let {
                Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
```

- [ ] **Step 4: Implement navigation components**

`ui/component/GhNavigation.kt`:

```kotlin
package com.jhon.mihealthreader.ui.component

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jhon.mihealthreader.R

data class GhTab(val route: String, @StringRes val labelRes: Int, val icon: ImageVector)

@Composable
fun GhBottomBar(tabs: List<GhTab>, currentRoute: String?, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    Surface(color = MaterialTheme.colorScheme.background, modifier = modifier.fillMaxWidth()) {
        Row(
            Modifier.navigationBarsPadding().padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEach { tab ->
                val selected = currentRoute == tab.route
                Column(
                    Modifier.weight(1f).clip(CircleShape).clickable { onSelect(tab.route) }.padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier.clip(CircleShape)
                            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                    ) {
                        Icon(
                            tab.icon,
                            contentDescription = stringResource(tab.labelRes),
                            tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        stringResource(tab.labelRes),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
fun GhTopBar(title: String, onBack: () -> Unit, modifier: Modifier = Modifier,
             actions: @Composable RowScope.() -> Unit = {}) {
    Row(modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back))
        }
        Text(title, style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.weight(1f).padding(start = 4.dp))
        actions()
    }
}

data class GhSegment<T>(val value: T, val label: String)

@Composable
fun <T> GhSegmentedControl(
    options: List<GhSegment<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    unselectedContainer: Color = MaterialTheme.colorScheme.surfaceContainer,
) {
    Row(modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            val isSelected = option.value == selected
            Surface(
                onClick = { onSelect(option.value) },
                color = if (isSelected) MaterialTheme.colorScheme.primary else unselectedContainer,
                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                shape = CircleShape,
            ) {
                Text(option.label, style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp))
            }
        }
    }
}

@Composable
fun GhRangeHeader(label: String, onPrevious: () -> Unit, onNext: () -> Unit, onToday: () -> Unit,
                  modifier: Modifier = Modifier, nextEnabled: Boolean = true) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
        GhIconActionButton(Icons.Filled.ChevronLeft, stringResource(R.string.action_previous), onPrevious,
            container = MaterialTheme.colorScheme.surfaceContainer)
        Spacer(Modifier.width(8.dp))
        GhIconActionButton(Icons.Filled.ChevronRight, stringResource(R.string.action_next), onNext,
            enabled = nextEnabled, container = MaterialTheme.colorScheme.surfaceContainer)
        Spacer(Modifier.width(8.dp))
        GhIconActionButton(Icons.Filled.Today, stringResource(R.string.action_today), onToday,
            container = MaterialTheme.colorScheme.surfaceContainer)
    }
}
```

- [ ] **Step 5: Implement rows and empty state**

`ui/component/GhRows.kt`:

```kotlin
package com.jhon.mihealthreader.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun GhListRow(label: String, value: String, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    GhCard(modifier.fillMaxWidth(), onClick) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text(value, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun GhSwitchRow(title: String, subtitle: String?, checked: Boolean,
                onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    GhCard(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                subtitle?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
fun GhEmptyState(title: String, message: String? = null, icon: ImageVector? = null,
                 modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    Column(modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        icon?.let { Icon(it, null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.outline) }
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        message?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
        action?.invoke()
    }
}
```

- [ ] **Step 6: Implement the metric tile**

`ui/component/GhMetricTile.kt`:

```kotlin
package com.jhon.mihealthreader.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jhon.mihealthreader.R

@Composable
fun GhMetricTile(
    title: String,
    value: String,
    unit: String?,
    status: String?,
    tone: GhTone,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    chart: @Composable BoxScope.() -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Surface(onClick, modifier = modifier, color = ghToneContainer(scheme, tone),
        contentColor = ghToneContent(scheme, tone), shape = MaterialTheme.shapes.large) {
        Column(
            Modifier.padding(16.dp).heightIn(min = 200.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, style = MaterialTheme.typography.displaySmall)
                unit?.let {
                    Text(" $it", style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 6.dp))
                }
            }
            Box(Modifier.fillMaxWidth().height(56.dp), content = chart)
            val pill = status ?: if (value == "—") stringResource(R.string.label_no_data) else null
            pill?.let {
                GhStatusPill(it, container = scheme.surfaceContainerLowest,
                    contentColor = scheme.onSurfaceVariant)
            }
        }
    }
}
```

- [ ] **Step 7: Run test and build**

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.jhon.mihealthreader.ui.component.GhRingTest"
.\gradlew.bat :app:assembleDebug
```

- [ ] **Step 8: Checkpoint**

```powershell
git add app/src/main/java/com/jhon/mihealthreader/ui/component app/src/test/java/com/jhon/mihealthreader/ui/component/GhRingTest.kt
# commit only if the user asked
```

---

### Task 5: Right-axis charts and axis math

**Files:**
- Create: `app/src/main/java/com/jhon/mihealthreader/ui/charts/GhChart.kt`
- Test: `app/src/test/java/com/jhon/mihealthreader/ui/charts/GhAxisTest.kt`

**Interfaces:** `GhAxis(min, max, ticks)`, `ghAxis(List<Float>, fromZero): GhAxis`, `ghLabelIndices(Int, Int = 7): List<Int>`, `GhChartStyle`, `GhChart`, `GhStackedBars`.

- [ ] **Step 1: Write the failing axis test**

```kotlin
package com.jhon.mihealthreader.ui.charts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GhAxisTest {
    @Test fun `empty values produce a safe axis`() {
        val axis = ghAxis(emptyList(), fromZero = true)
        assertEquals(0f, axis.min, 0f)
        assertEquals(1f, axis.max, 0f)
        assertEquals(3, axis.ticks.size)
    }

    @Test fun `zero based axis starts at zero`() {
        val axis = ghAxis(listOf(0f, 120f, 60f), fromZero = true)
        assertEquals(0f, axis.min, 0f)
        assertEquals(120f, axis.max, 0f)
        assertEquals(listOf(120f, 60f, 0f), axis.ticks)
    }

    @Test fun `non zero axis keeps the minimum`() {
        val axis = ghAxis(listOf(61f, 72f, 80f), fromZero = false)
        assertEquals(61f, axis.min, 0f)
        assertEquals(80f, axis.max, 0f)
    }

    @Test fun `label indices include first and last`() {
        val indices = ghLabelIndices(30, maxLabels = 7)
        assertEquals(7, indices.size)
        assertEquals(0, indices.first())
        assertEquals(29, indices.last())
        assertTrue(indices.zipWithNext().all { (a, b) -> a < b })
    }

    @Test fun `short series shows every label`() {
        assertEquals(listOf(0, 1, 2), ghLabelIndices(3))
    }
}
```

- [ ] **Step 2: Run it and verify failure**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "com.jhon.mihealthreader.ui.charts.GhAxisTest"`
Expected: FAIL — `ghAxis` unresolved.

- [ ] **Step 3: Implement `GhChart.kt`**

```kotlin
package com.jhon.mihealthreader.ui.charts

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.min
import kotlin.math.roundToInt

enum class GhChartStyle { Bars, DottedLine, AreaLine }

data class GhAxis(val min: Float, val max: Float, val ticks: List<Float>)

fun ghAxis(values: List<Float>, fromZero: Boolean): GhAxis {
    if (values.isEmpty()) return GhAxis(0f, 1f, listOf(1f, 0.5f, 0f))
    val max = values.max().coerceAtLeast(1f)
    val min = if (fromZero) 0f else values.min().coerceAtMost(max)
    val safeMax = if (max == min) min + 1f else max
    return GhAxis(min, safeMax, listOf(safeMax, (safeMax + min) / 2f, min))
}

fun ghLabelIndices(size: Int, maxLabels: Int = 7): List<Int> {
    if (size <= 0) return emptyList()
    if (size <= maxLabels) return (0 until size).toList()
    val step = (size - 1).toFloat() / (maxLabels - 1)
    return (0 until maxLabels).map { (it * step).roundToInt() }.distinct()
}

@Composable
fun GhChart(
    values: List<Float>,
    labels: List<String>,
    modifier: Modifier = Modifier,
    style: GhChartStyle,
    color: Color = MaterialTheme.colorScheme.secondary,
    selectedIndex: Int? = null,
    valueFormatter: (Float) -> String = { it.toInt().toString() },
) {
    val axis = remember(values, style) { ghAxis(values, fromZero = style == GhChartStyle.Bars) }
    val animated by animateFloatAsState(if (values.isEmpty()) 0f else 1f, label = "chart")
    val surface = MaterialTheme.colorScheme.surface
    Column(modifier) {
        Row(Modifier.fillMaxWidth()) {
            Box(Modifier.weight(1f).height(180.dp)) {
                when (style) {
                    GhChartStyle.Bars -> BarsCanvas(values, axis, color, selectedIndex, surface, animated)
                    GhChartStyle.DottedLine -> DottedCanvas(values, axis, color, selectedIndex, surface, animated)
                    GhChartStyle.AreaLine -> AreaCanvas(values, axis, color, animated)
                }
            }
            Spacer(Modifier.width(8.dp))
            Column(
                Modifier.height(180.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End,
            ) {
                axis.ticks.forEach {
                    Text(valueFormatter(it), style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth().padding(end = 40.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            ghLabelIndices(labels.size).forEach { i ->
                Text(labels.getOrElse(i) { "" }, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun BarsCanvas(values: List<Float>, axis: GhAxis, color: Color, selected: Int?,
                       surface: Color, progress: Float) {
    Canvas(Modifier.fillMaxSize()) {
        if (values.isEmpty()) {
            drawLine(color.copy(alpha = 0.2f), Offset(0f, size.height), Offset(size.width, size.height), 2.dp.toPx())
            return@Canvas
        }
        val range = (axis.max - axis.min).takeIf { it > 0f } ?: 1f
        val gap = size.width * 0.035f
        val barWidth = (size.width - gap * (values.size - 1)) / values.size
        values.forEachIndexed { i, v ->
            val h = (((v - axis.min) / range).coerceIn(0f, 1f)) * size.height * progress
            val radius = min(barWidth / 2f, h / 2f)
            drawRoundRect(color, Offset(i * (barWidth + gap), size.height - h),
                Size(barWidth, h), CornerRadius(radius, radius))
        }
        selected?.let { i ->
            if (i in values.indices) {
                val x = i * (barWidth + gap) + barWidth / 2f
                val h = (((values[i] - axis.min) / range).coerceIn(0f, 1f)) * size.height * progress
                drawLine(surface, Offset(x, 0f), Offset(x, size.height), 1.dp.toPx())
                drawCircle(surface, 5.dp.toPx(), Offset(x, size.height - h))
                drawCircle(color, 3.dp.toPx(), Offset(x, size.height - h))
            }
        }
    }
}

@Composable
private fun DottedCanvas(values: List<Float>, axis: GhAxis, color: Color, selected: Int?,
                         surface: Color, progress: Float) {
    Canvas(Modifier.fillMaxSize()) {
        if (values.isEmpty()) {
            drawLine(color.copy(alpha = 0.2f), Offset(0f, size.height), Offset(size.width, size.height), 2.dp.toPx())
            return@Canvas
        }
        val range = (axis.max - axis.min).takeIf { it > 0f } ?: 1f
        val stepX = if (values.size > 1) size.width / (values.size - 1) else 0f
        val points = values.mapIndexed { i, v ->
            val y = size.height - ((v - axis.min) / range) * size.height * progress
            Offset(stepX * i, y)
        }
        for (i in 0 until points.size - 1) drawLine(color, points[i], points[i + 1], 2.dp.toPx())
        points.forEach { drawCircle(color, 3.5.dp.toPx(), it) }
        selected?.let { i ->
            points.getOrNull(i)?.let {
                drawCircle(surface, 6.dp.toPx(), it)
                drawCircle(color, 4.dp.toPx(), it)
            }
        }
    }
}

@Composable
private fun AreaCanvas(values: List<Float>, axis: GhAxis, color: Color, progress: Float) {
    Canvas(Modifier.fillMaxSize()) {
        if (values.size < 2) {
            drawLine(color.copy(alpha = 0.2f), Offset(0f, size.height), Offset(size.width, size.height), 2.dp.toPx())
            return@Canvas
        }
        val range = (axis.max - axis.min).takeIf { it > 0f } ?: 1f
        val stepX = size.width / (values.size - 1)
        val points = values.mapIndexed { i, v ->
            Offset(stepX * i, size.height - ((v - axis.min) / range) * size.height * progress)
        }
        val line = Path().apply {
            moveTo(points.first().x, points.first().y)
            for (i in 1 until points.size) {
                val prev = points[i - 1]
                val cur = points[i]
                val midX = (prev.x + cur.x) / 2f
                cubicTo(midX, prev.y, midX, cur.y, cur.x, cur.y)
            }
        }
        val area = Path().apply {
            addPath(line)
            lineTo(points.last().x, size.height)
            lineTo(points.first().x, size.height)
            close()
        }
        drawPath(area, Brush.verticalGradient(listOf(color.copy(alpha = 0.30f), color.copy(alpha = 0.02f))))
        drawPath(line, color, style = Stroke(width = 3.dp.toPx()))
    }
}

@Composable
fun GhStackedBars(rows: List<List<StackSegment>>, labels: List<String>, modifier: Modifier = Modifier) {
    val maxTotal = remember(rows) {
        rows.maxOfOrNull { row -> row.sumOf { it.value.toDouble() }.toFloat() } ?: 1f
    }
    Column(modifier) {
        Row(Modifier.fillMaxWidth()) {
            Box(Modifier.weight(1f).height(160.dp)) {
                StackedBarChart(rows, Modifier.fillMaxSize())
            }
            Spacer(Modifier.width(8.dp))
            Column(Modifier.height(160.dp), verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End) {
                Text(maxTotal.toInt().toString(), style = MaterialTheme.typography.labelSmall)
                Text("0", style = MaterialTheme.typography.labelSmall)
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth().padding(end = 40.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            ghLabelIndices(labels.size).forEach { i ->
                Text(labels.getOrElse(i) { "" }, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
```

- [ ] **Step 4: Run the test**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "com.jhon.mihealthreader.ui.charts.GhAxisTest"`
Expected: PASS.

- [ ] **Step 5: Checkpoint**

```powershell
git add app/src/main/java/com/jhon/mihealthreader/ui/charts/GhChart.kt app/src/test/java/com/jhon/mihealthreader/ui/charts/GhAxisTest.kt
# commit only if the user asked
```

---

### Task 6: `ChartSeries` range aggregation

**Files:**
- Create: `app/src/main/java/com/jhon/mihealthreader/data/ChartSeries.kt`
- Test: `app/src/test/java/com/jhon/mihealthreader/data/ChartSeriesTest.kt`

**Interfaces:** `ChartRange`, `ChartBucket`, `ChartWindow`, `ChartSeries.shift/window/steps/workouts/heartRate/spo2/weight`.

- [ ] **Step 1: Write the failing aggregation tests**

```kotlin
package com.jhon.mihealthreader.data

import com.jhon.mihealthreader.parse.DailyActivity
import com.jhon.mihealthreader.parse.MetricSample
import com.jhon.mihealthreader.parse.Workout
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class ChartSeriesTest {
    private val zone = ZoneId.of("UTC")
    private val anchor = LocalDate.of(2026, 10, 21) // Wednesday

    @Test fun `week window starts on monday`() {
        val (start, end) = ChartSeries.window(ChartRange.WEEK, anchor)
        assertEquals(LocalDate.of(2026, 10, 19), start)
        assertEquals(LocalDate.of(2026, 10, 25), end)
    }

    @Test fun `three month window includes the current month`() {
        val (start, end) = ChartSeries.window(ChartRange.THREE_MONTHS, anchor)
        assertEquals(LocalDate.of(2026, 8, 1), start)
        assertEquals(LocalDate.of(2026, 10, 31), end)
    }

    @Test fun `year window covers the calendar year`() {
        val (start, end) = ChartSeries.window(ChartRange.YEAR, anchor)
        assertEquals(LocalDate.of(2026, 1, 1), start)
        assertEquals(LocalDate.of(2026, 12, 31), end)
    }

    @Test fun `week steps sum per day`() {
        val window = ChartSeries.steps(listOf(
            DailyActivity("2026-10-19", 1000, 800, 40),
            DailyActivity("2026-10-20", 2000, 1600, 80),
        ), ChartRange.WEEK, anchor)
        assertEquals(2, window.buckets.size)
        assertEquals(1000f, window.buckets[0].value, 0f)
        assertEquals(2000f, window.buckets[1].value, 0f)
    }

    @Test fun `month steps stay inside the month`() {
        val window = ChartSeries.steps(listOf(
            DailyActivity("2026-09-30", 9000, 7000, 300),
            DailyActivity("2026-10-01", 1000, 800, 40),
            DailyActivity("2026-10-31", 2000, 1600, 80),
        ), ChartRange.MONTH, anchor)
        assertEquals(2, window.buckets.size)
        assertEquals(3000f, window.buckets.sumOf { it.value.toDouble() }.toFloat(), 0f)
    }

    @Test fun `day heart rate buckets by hour`() {
        val day = anchor.atStartOfDay(zone).toEpochSecond()
        val window = ChartSeries.heartRate(listOf(
            MetricSample("heart_rate", "dev", day + 8 * 3600, 60.0),
            MetricSample("heart_rate", "dev", day + 8 * 3600 + 600, 80.0),
            MetricSample("heart_rate", "dev", day + 9 * 3600, 70.0),
        ), ChartRange.DAY, anchor, zone)
        assertEquals(2, window.buckets.size)
        assertEquals(8, window.buckets[0].hour)
        assertEquals(70f, window.buckets[0].value, 0f)
    }

    @Test fun `year heart rate averages by month`() {
        val jan = LocalDate.of(2026, 1, 10).atStartOfDay(zone).toEpochSecond()
        val feb = LocalDate.of(2026, 2, 10).atStartOfDay(zone).toEpochSecond()
        val window = ChartSeries.heartRate(listOf(
            MetricSample("heart_rate", "dev", jan, 60.0),
            MetricSample("heart_rate", "dev", jan + 86400, 80.0),
            MetricSample("heart_rate", "dev", feb, 70.0),
        ), ChartRange.YEAR, anchor, zone)
        assertEquals(2, window.buckets.size)
        assertEquals(70f, window.buckets[0].value, 0f)
        assertEquals(70f, window.buckets[1].value, 0f)
    }

    @Test fun `week workouts sum minutes`() {
        val start = LocalDate.of(2026, 10, 19).atStartOfDay(zone).toEpochSecond()
        val window = ChartSeries.workouts(listOf(
            Workout("dev", start, start + 600, 600, 100, null, null, null, null, "running", "Running"),
            Workout("dev", start + 3600, start + 4200, 600, 100, null, null, null, null, "running", "Running"),
        ), ChartRange.WEEK, anchor, zone)
        assertEquals(20f, window.buckets.single().value, 0f)
    }

    @Test fun `empty data produces no buckets`() {
        assertEquals(0, ChartSeries.steps(emptyList(), ChartRange.WEEK, anchor).buckets.size)
    }

    @Test fun `shift moves by the range unit`() {
        assertEquals(LocalDate.of(2026, 10, 28), ChartSeries.shift(ChartRange.WEEK, anchor, 1))
        assertEquals(LocalDate.of(2026, 11, 21), ChartSeries.shift(ChartRange.MONTH, anchor, 1))
        assertEquals(LocalDate.of(2027, 1, 21), ChartSeries.shift(ChartRange.THREE_MONTHS, anchor, 1))
    }
}
```

- [ ] **Step 2: Run them and verify failure**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "com.jhon.mihealthreader.data.ChartSeriesTest"`
Expected: FAIL — `ChartSeries` unresolved.

- [ ] **Step 3: Implement `ChartSeries.kt`**

```kotlin
package com.jhon.mihealthreader.data

import com.jhon.mihealthreader.parse.BodyMeasurement
import com.jhon.mihealthreader.parse.DailyActivity
import com.jhon.mihealthreader.parse.MetricSample
import com.jhon.mihealthreader.parse.Workout
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

enum class ChartRange { DAY, WEEK, MONTH, THREE_MONTHS, YEAR }

data class ChartBucket(
    val start: LocalDate,
    val end: LocalDate,
    val value: Float,
    val hour: Int? = null,
)

data class ChartWindow(val start: LocalDate, val end: LocalDate, val buckets: List<ChartBucket>)

object ChartSeries {

    fun shift(range: ChartRange, anchor: LocalDate, delta: Long): LocalDate = when (range) {
        ChartRange.DAY -> anchor.plusDays(delta)
        ChartRange.WEEK -> anchor.plusWeeks(delta)
        ChartRange.MONTH -> anchor.plusMonths(delta)
        ChartRange.THREE_MONTHS -> anchor.plusMonths(delta * 3)
        ChartRange.YEAR -> anchor.plusYears(delta)
    }

    fun window(range: ChartRange, anchor: LocalDate,
               firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY): Pair<LocalDate, LocalDate> = when (range) {
        ChartRange.DAY -> anchor to anchor
        ChartRange.WEEK -> {
            val start = anchor.with(TemporalAdjusters.previousOrSame(firstDayOfWeek))
            start to start.plusDays(6)
        }
        ChartRange.MONTH -> anchor.withDayOfMonth(1) to anchor.withDayOfMonth(anchor.lengthOfMonth())
        ChartRange.THREE_MONTHS -> {
            val end = anchor.withDayOfMonth(anchor.lengthOfMonth())
            end.withDayOfMonth(1).minusMonths(2) to end
        }
        ChartRange.YEAR -> anchor.withDayOfYear(1) to anchor.withDayOfYear(anchor.lengthOfYear())
    }

    fun steps(activity: List<DailyActivity>, range: ChartRange, anchor: LocalDate,
              firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY): ChartWindow = aggregateDaily(
        activity.associate { LocalDate.parse(it.date) to it.steps.toFloat() },
        range, anchor, firstDayOfWeek, sum = true,
    )

    fun workouts(workouts: List<Workout>, range: ChartRange, anchor: LocalDate,
                 zone: ZoneId = ZoneId.systemDefault(),
                 firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY): ChartWindow {
        val daily = workouts.groupBy { instant(it.start, zone).toLocalDate() }
            .mapValues { (_, list) -> list.sumOf { it.durationSec }.toFloat() / 60f }
        return aggregateDaily(daily, range, anchor, firstDayOfWeek, sum = true)
    }

    fun heartRate(samples: List<MetricSample>, range: ChartRange, anchor: LocalDate,
                  zone: ZoneId = ZoneId.systemDefault(),
                  firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY): ChartWindow =
        samplesWindow(samples, range, anchor, zone, firstDayOfWeek)

    fun spo2(samples: List<MetricSample>, range: ChartRange, anchor: LocalDate,
             zone: ZoneId = ZoneId.systemDefault(),
             firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY): ChartWindow =
        samplesWindow(samples, range, anchor, zone, firstDayOfWeek)

    fun weight(measurements: List<BodyMeasurement>, range: ChartRange, anchor: LocalDate,
               zone: ZoneId = ZoneId.systemDefault(),
               firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY): ChartWindow {
        val daily = measurements
            .mapNotNull { m -> m.weightKg?.let { instant(m.time, zone).toLocalDate() to it.toFloat() } }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, list) -> list.average().toFloat() }
        return aggregateDaily(daily, range, anchor, firstDayOfWeek, sum = false)
    }

    private fun samplesWindow(samples: List<MetricSample>, range: ChartRange, anchor: LocalDate,
                              zone: ZoneId, firstDayOfWeek: DayOfWeek): ChartWindow {
        if (range == ChartRange.DAY) {
            val byHour = samples
                .filter { instant(it.time, zone).toLocalDate() == anchor }
                .groupBy { instant(it.time, zone).hour }
                .toSortedMap()
                .map { (hour, list) ->
                    ChartBucket(anchor, anchor, list.map { it.value }.average().toFloat(), hour)
                }
            return ChartWindow(anchor, anchor, byHour)
        }
        val daily = samples.groupBy { instant(it.time, zone).toLocalDate() }
            .mapValues { (_, list) -> list.map { it.value }.average().toFloat() }
        return aggregateDaily(daily, range, anchor, firstDayOfWeek, sum = false)
    }

    private fun aggregateDaily(values: Map<LocalDate, Float>, range: ChartRange, anchor: LocalDate,
                               firstDayOfWeek: DayOfWeek, sum: Boolean): ChartWindow {
        val (start, end) = window(range, anchor, firstDayOfWeek)
        val inWindow = values.filterKeys { it in start..end }
        val buckets = when (range) {
            ChartRange.DAY -> listOfNotNull(inWindow[anchor]?.let { ChartBucket(anchor, anchor, it) })
            ChartRange.WEEK, ChartRange.MONTH -> inWindow.entries.sortedBy { it.key }
                .map { (date, value) -> ChartBucket(date, date, value) }
            ChartRange.THREE_MONTHS -> inWindow.entries
                .groupBy { weekStart(it.key, firstDayOfWeek) }.toSortedMap()
                .map { (week, entries) ->
                    ChartBucket(week, minOf(week.plusDays(6), end), combine(entries.map { it.value }, sum))
                }
            ChartRange.YEAR -> inWindow.entries
                .groupBy { YearMonth.from(it.key) }.toSortedMap()
                .map { (month, entries) -> ChartBucket(month.atDay(1), month.atEndOfMonth(), combine(entries.map { it.value }, sum)) }
        }
        return ChartWindow(start, end, buckets)
    }

    private fun combine(values: List<Float>, sum: Boolean): Float =
        if (sum) values.sum() else values.average().toFloat()

    private fun weekStart(date: LocalDate, firstDayOfWeek: DayOfWeek): LocalDate =
        date.with(TemporalAdjusters.previousOrSame(firstDayOfWeek))

    private fun instant(epochSeconds: Long, zone: ZoneId) =
        Instant.ofEpochSecond(epochSeconds).atZone(zone)
}
```

- [ ] **Step 4: Run the tests**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "com.jhon.mihealthreader.data.ChartSeriesTest"`
Expected: PASS.

- [ ] **Step 5: Checkpoint**

```powershell
git add app/src/main/java/com/jhon/mihealthreader/data/ChartSeries.kt app/src/test/java/com/jhon/mihealthreader/data/ChartSeriesTest.kt
# commit only if the user asked
```

---

### Task 7: Today screen

**Files:**
- Create: `app/src/main/java/com/jhon/mihealthreader/data/TodaySummary.kt`
- Modify: `app/src/main/java/com/jhon/mihealthreader/ui/screens/TodayScreen.kt`
- Modify: `app/src/main/java/com/jhon/mihealthreader/ui/AppNav.kt` (TodayScreen call)
- Test: `app/src/test/java/com/jhon/mihealthreader/data/TodaySummaryTest.kt`

**Interfaces:** `TodaySummary.weeklyWorkouts(...): WeeklyWorkouts(days, minutes)`. Consumes `GhRing`, `GhTintedCard`, `GhPillButton`, `GhStatusPill`, `GhListRow`, `CapsuleBars`, `DottedLine`.

- [ ] **Step 1: Write the failing summary test**

```kotlin
package com.jhon.mihealthreader.data

import com.jhon.mihealthreader.parse.Workout
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class TodaySummaryTest {
    private val zone = ZoneId.of("UTC")
    private val today = LocalDate.of(2026, 10, 21)

    private fun workout(day: LocalDate, minutes: Int): Workout {
        val start = day.atStartOfDay(zone).toEpochSecond()
        return Workout("dev", start, start + minutes * 60L, minutes * 60, 100,
            null, null, null, null, "running", "Running")
    }

    @Test fun `counts distinct days and total minutes`() {
        val result = TodaySummary.weeklyWorkouts(listOf(
            workout(LocalDate.of(2026, 10, 19), 30),
            workout(LocalDate.of(2026, 10, 19), 20),
            workout(LocalDate.of(2026, 10, 21), 40),
            workout(LocalDate.of(2026, 10, 12), 60),
        ), today, zone)
        assertEquals(2, result.days)
        assertEquals(90, result.minutes)
    }

    @Test fun `empty workouts are zero`() {
        val result = TodaySummary.weeklyWorkouts(emptyList(), today, zone)
        assertEquals(0, result.days)
        assertEquals(0, result.minutes)
    }
}
```

- [ ] **Step 2: Run it and verify failure**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "com.jhon.mihealthreader.data.TodaySummaryTest"`
Expected: FAIL — `TodaySummary` unresolved.

- [ ] **Step 3: Implement `TodaySummary`**

```kotlin
package com.jhon.mihealthreader.data

import com.jhon.mihealthreader.parse.Workout
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

object TodaySummary {
    data class WeeklyWorkouts(val days: Int, val minutes: Int)

    fun weeklyWorkouts(
        workouts: List<Workout>,
        today: LocalDate = LocalDate.now(),
        zone: ZoneId = ZoneId.systemDefault(),
        firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
    ): WeeklyWorkouts {
        val start = today.with(TemporalAdjusters.previousOrSame(firstDayOfWeek))
        val end = start.plusDays(6)
        val inWeek = workouts.filter {
            Instant.ofEpochSecond(it.start).atZone(zone).toLocalDate() in start..end
        }
        val days = inWeek.map { Instant.ofEpochSecond(it.start).atZone(zone).toLocalDate() }.distinct().size
        return WeeklyWorkouts(days, inWeek.sumOf { it.durationSec } / 60)
    }
}
```

- [ ] **Step 4: Rewrite `TodayScreen.kt`**

Replace the whole file with the following (add imports for Compose layout/Material3 and the `ui.component` symbols):

```kotlin
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TodayScreen(
    parsedRepository: ParsedRepository,
    summary: LocalSummary,
    dataLoaded: Boolean,
    extractionState: ExtractionState,
    onOpenDetail: (Metric) -> Unit,
    onGoSettings: () -> Unit,
    onStartExtraction: () -> Unit = {},
    onSyncHealthConnect: () -> Unit = {},
) {
    val activity by parsedRepository.dailyActivity().collectAsState(initial = emptyList())
    val sleep by parsedRepository.sleepSessions().collectAsState(initial = emptyList())
    val heart by parsedRepository.heartRate().collectAsState(initial = emptyList())
    val resting by parsedRepository.restingHeartRate().collectAsState(initial = emptyList())
    val workouts by parsedRepository.workouts().collectAsState(initial = emptyList())
    val body by parsedRepository.bodyMeasurements().collectAsState(initial = emptyList())

    val today = LocalDate.now()
    val todayActivity = activity.firstOrNull { it.date == today.toString() }
    val mainSleep = remember(sleep) { SleepSummary.mainSleep(sleep) }
    val naps = remember(sleep) { SleepSummary.sameDayNaps(sleep) }
    val napMinutes = naps.sumOf { it.durationMinutes }
    val weekly = remember(workouts, today) { TodaySummary.weeklyWorkouts(workouts, today) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding(),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, top = 16.dp, end = 20.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.title_today), style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.weight(1f))
            GhStatusPill(
                if (!dataLoaded) stringResource(R.string.label_loading)
                else updateStatusLine(extractionState, summary.lastExtractedAt),
            )
        }

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                GhRing(
                    progress = ghRingProgress(weekly.days, 7),
                    centerValue = stringResource(R.string.today_ring_progress, weekly.days, 7),
                    centerLabel = stringResource(R.string.today_ring_label),
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                )
                Text(
                    stringResource(R.string.today_workout_minutes, weekly.minutes),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                HeroCard(
                    tone = GhTone.Primary,
                    icon = Icons.Filled.Walking,
                    title = stringResource(R.string.metric_steps),
                    value = todayActivity?.steps?.toString() ?: stringResource(R.string.label_unknown),
                    unit = stringResource(R.string.unit_steps),
                    status = todayActivity?.let {
                        stringResource(R.string.today_steps_status_distance, "%.1f".format(it.distanceMeters / 1000f))
                    },
                    onClick = { onOpenDetail(Metric.STEPS) },
                )
                HeroCard(
                    tone = GhTone.Tertiary,
                    icon = Icons.Filled.Nightlight,
                    title = stringResource(R.string.metric_sleep),
                    value = mainSleep?.let { formatMinutes(it.durationMinutes + napMinutes) }
                        ?: stringResource(R.string.label_unknown),
                    unit = null,
                    status = when {
                        mainSleep == null -> null
                        napMinutes > 0 -> stringResource(R.string.today_sleep_nap, napMinutes)
                        else -> stringResource(R.string.today_sleep_deep, mainSleep.deepMinutes)
                    },
                    onClick = { onOpenDetail(Metric.SLEEP) },
                )
                HeroCard(
                    tone = GhTone.Secondary,
                    icon = Icons.Filled.Favorite,
                    title = stringResource(R.string.metric_hr),
                    value = heart.firstOrNull()?.value?.toInt()?.toString() ?: stringResource(R.string.label_unknown),
                    unit = stringResource(R.string.unit_bpm),
                    status = stringResource(
                        R.string.today_hr_resting,
                        resting.firstOrNull()?.value?.toInt()?.toString() ?: stringResource(R.string.label_unknown),
                    ),
                    onClick = { onOpenDetail(Metric.HR) },
                )
            }
        }

        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.Center) {
            repeat(3) { index ->
                Box(
                    Modifier.padding(horizontal = 3.dp)
                        .width(if (index == 0) 18.dp else 8.dp)
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(
                            if (index == 0) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.surfaceContainerHighest
                        ),
                )
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GhPillButton(stringResource(R.string.action_extract), onStartExtraction,
                Modifier.weight(1f), icon = Icons.Filled.Refresh)
            GhPillButton(stringResource(R.string.action_sync_health_connect), onSyncHealthConnect,
                Modifier.weight(1f), icon = Icons.Filled.Sync)
            GhIconActionButton(Icons.Filled.Settings, stringResource(R.string.nav_settings), onGoSettings)
        }

        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(stringResource(R.string.today_greeting), style = MaterialTheme.typography.headlineMedium)

                if (dataLoaded && summary.lastExtractedAt == null) {
                    GhTintedCard(GhTone.Primary) {
                        Text(stringResource(R.string.today_no_data_title), style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.today_no_data_message),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))
                        GhPillButton(stringResource(R.string.action_go_settings), onGoSettings)
                    }
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Metric.entries.forEach { metric ->
                        Surface(onClick = { onOpenDetail(metric) },
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            shape = RoundedCornerShape(50)) {
                            Text(stringResource(metric.labelRes), style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
                        }
                    }
                }

                Text(stringResource(R.string.section_recent), style = MaterialTheme.typography.titleLarge)
                workouts.firstOrNull()?.let {
                    GhListRow(
                        label = it.typeName,
                        value = stringResource(R.string.today_recent_workout,
                            formatDateTime(it.start), formatDurationSeconds(it.durationSec)),
                        onClick = { onOpenDetail(Metric.WORKOUT) },
                    )
                }
                sleep.firstOrNull()?.let {
                    GhListRow(
                        label = stringResource(R.string.today_recent_sleep),
                        value = formatMinutes(it.durationMinutes),
                        onClick = { onOpenDetail(Metric.SLEEP) },
                    )
                }
                body.firstOrNull()?.weightKg?.let {
                    GhListRow(
                        label = stringResource(R.string.metric_body),
                        value = stringResource(R.string.today_recent_weight, it.toString()),
                        onClick = { onOpenDetail(Metric.BODY) },
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroCard(
    tone: GhTone,
    icon: ImageVector,
    title: String,
    value: String,
    unit: String?,
    status: String?,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    GhTintedCard(tone = tone, onClick = onClick) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            GhIconPill(icon, null, ghToneContent(scheme, tone).copy(alpha = 0.10f), ghToneContent(scheme, tone))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.labelMedium)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(value, style = MaterialTheme.typography.headlineSmall)
                    unit?.let { Text(" $it", style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(bottom = 2.dp)) }
                }
                status?.let { Text(it, style = MaterialTheme.typography.labelSmall) }
            }
        }
    }
}

@Composable
private fun updateStatusLine(state: ExtractionState, lastExtractedAt: Long?): String = when {
    state is ExtractionState.Running -> stringResource(R.string.today_status_running)
    lastExtractedAt != null -> stringResource(
        R.string.status_updated,
        Instant.ofEpochMilli(lastExtractedAt).atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("HH:mm")),
    )
    else -> stringResource(R.string.label_no_data)
}
```

- [ ] **Step 5: Update the `TodayScreen` call in `AppNav.kt`**

```kotlin
TodayScreen(
    parsedRepository = parsedRepository,
    summary = summary,
    dataLoaded = dataLoaded,
    extractionState = extractionState,
    onOpenDetail = { metric ->
        when (metric) {
            Metric.SLEEP -> navController.navigate("sleep_detail")
            Metric.WORKOUT -> navController.navigate("workout_list")
            else -> navController.navigate("detail/${metric.name}")
        }
    },
    onGoSettings = { navController.navigateSingleTop(Routes.SETTINGS) },
    onStartExtraction = { extractionViewModel.start() },
    onSyncHealthConnect = { extractionViewModel.syncHealthConnect() },
)
```

- [ ] **Step 6: Run tests and build**

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.jhon.mihealthreader.data.TodaySummaryTest"
.\gradlew.bat :app:assembleDebug
```

- [ ] **Step 7: Checkpoint**

```powershell
git add app/src/main/java/com/jhon/mihealthreader/data/TodaySummary.kt app/src/main/java/com/jhon/mihealthreader/ui/screens/TodayScreen.kt app/src/main/java/com/jhon/mihealthreader/ui/AppNav.kt app/src/test/java/com/jhon/mihealthreader/data/TodaySummaryTest.kt
# commit only if the user asked
```

---

### Task 8: Views screen (metric grid + categories)

**Files:**
- Create: `app/src/main/java/com/jhon/mihealthreader/ui/screens/MetricDestination.kt`
- Modify: `app/src/main/java/com/jhon/mihealthreader/ui/screens/ChartsScreen.kt`
- Modify: `app/src/main/java/com/jhon/mihealthreader/ui/AppNav.kt` (pass `onOpenDetail`)
- Test: `app/src/test/java/com/jhon/mihealthreader/ui/screens/MetricDestinationTest.kt`

**Interfaces:** `MetricDestination { Sleep, WorkoutList, Detail(metric) }`, `metricDestination(Metric)`.

- [ ] **Step 1: Write the failing destination test**

```kotlin
package com.jhon.mihealthreader.ui.screens

import com.jhon.mihealthreader.ui.Metric
import org.junit.Assert.assertEquals
import org.junit.Test

class MetricDestinationTest {
    @Test fun `sleep and workout have dedicated screens`() {
        assertEquals(MetricDestination.Sleep, metricDestination(Metric.SLEEP))
        assertEquals(MetricDestination.WorkoutList, metricDestination(Metric.WORKOUT))
    }

    @Test fun `other metrics open the detail screen`() {
        assertEquals(MetricDestination.Detail(Metric.STEPS), metricDestination(Metric.STEPS))
        assertEquals(MetricDestination.Detail(Metric.HR), metricDestination(Metric.HR))
        assertEquals(MetricDestination.Detail(Metric.SPO2), metricDestination(Metric.SPO2))
        assertEquals(MetricDestination.Detail(Metric.BODY), metricDestination(Metric.BODY))
    }
}
```

- [ ] **Step 2: Run it and verify failure**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "com.jhon.mihealthreader.ui.screens.MetricDestinationTest"`
Expected: FAIL — unresolved.

- [ ] **Step 3: Implement the destination helper**

```kotlin
package com.jhon.mihealthreader.ui.screens

import com.jhon.mihealthreader.ui.Metric

sealed interface MetricDestination {
    data object Sleep : MetricDestination
    data object WorkoutList : MetricDestination
    data class Detail(val metric: Metric) : MetricDestination
}

fun metricDestination(metric: Metric): MetricDestination = when (metric) {
    Metric.SLEEP -> MetricDestination.Sleep
    Metric.WORKOUT -> MetricDestination.WorkoutList
    else -> MetricDestination.Detail(metric)
}
```

- [ ] **Step 4: Rewrite `ChartsScreen.kt`**

Signature: `fun ChartsScreen(parsedRepository, onOpenDetail: (Metric) -> Unit, onOpenSleepDetail: () -> Unit, onOpenWorkoutList: () -> Unit)`.

```kotlin
@Composable
fun ChartsScreen(
    parsedRepository: ParsedRepository,
    onOpenDetail: (Metric) -> Unit,
    onOpenSleepDetail: () -> Unit,
    onOpenWorkoutList: () -> Unit,
) {
    val activity by parsedRepository.dailyActivity().collectAsState(initial = emptyList())
    val sleep by parsedRepository.sleepSessions().collectAsState(initial = emptyList())
    val heart by parsedRepository.heartRate().collectAsState(initial = emptyList())
    val spo2 by parsedRepository.spo2().collectAsState(initial = emptyList())
    val workouts by parsedRepository.workouts().collectAsState(initial = emptyList())
    val body by parsedRepository.bodyMeasurements().collectAsState(initial = emptyList())

    val scheme = MaterialTheme.colorScheme
    val steps = remember(activity) { ChartData.lastN(ChartData.stepsSeries(activity), 7).map { it.value } }
    val sleepBars = remember(sleep) { ChartData.lastN(ChartData.sleepSeries(sleep), 7).map { it.total } }
    val hr = remember(heart) { ChartData.lastN(ChartData.metricSeries(heart), 7).map { it.value } }
    val spo = remember(spo2) { ChartData.lastN(ChartData.metricSeries(spo2), 7).map { it.value } }
    val workoutBars = remember(workouts) { ChartData.lastN(ChartData.workoutSeries(workouts), 7).map { it.value } }
    val weights = remember(body) { ChartData.lastN(ChartData.bodySeries(body), 7).map { it.value } }
    val mainSleep = remember(sleep) { SleepSummary.mainSleep(sleep) }

    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState()).statusBarsPadding().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        GhPageTitle(stringResource(R.string.title_charts))
        GhSectionHeader(stringResource(R.string.section_main_metrics))

        Metric.entries.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { metric ->
                    val (value, unit, status, series, tone) = when (metric) {
                        Metric.STEPS -> MetricTileData(
                            activity.firstOrNull()?.steps?.toString() ?: "—",
                            stringResource(R.string.unit_steps),
                            activity.firstOrNull()?.let { stringResource(R.string.today_steps_status_distance, "%.1f".format(it.distanceMeters / 1000f)) },
                            steps, GhTone.Primary)
                        Metric.SLEEP -> MetricTileData(
                            mainSleep?.let { formatMinutes(it.durationMinutes) } ?: "—",
                            null, mainSleep?.let { stringResource(R.string.today_sleep_deep, it.deepMinutes) },
                            sleepBars, GhTone.Tertiary)
                        Metric.HR -> MetricTileData(
                            heart.firstOrNull()?.value?.toInt()?.toString() ?: "—",
                            stringResource(R.string.unit_bpm), null, hr, GhTone.Secondary)
                        Metric.SPO2 -> MetricTileData(
                            spo2.firstOrNull()?.value?.toInt()?.toString() ?: "—",
                            stringResource(R.string.unit_percent), null, spo, GhTone.Neutral)
                        Metric.WORKOUT -> MetricTileData(
                            workouts.count { it.start >= LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toEpochSecond() }.toString(),
                            null, null, workoutBars, GhTone.Primary)
                        Metric.BODY -> MetricTileData(
                            body.firstOrNull()?.weightKg?.toString() ?: "—",
                            stringResource(R.string.unit_kg),
                            body.firstOrNull()?.bodyFatRate?.let { "%.1f%%".format(it) },
                            weights, GhTone.Neutral)
                    }
                    GhMetricTile(
                        title = stringResource(metric.labelRes),
                        value = value,
                        unit = unit,
                        status = status,
                        tone = tone,
                        onClick = { onOpenDetail(metric) },
                        modifier = Modifier.weight(1f).height(220.dp),
                    ) {
                        if (metric.chartKind == MetricChartKind.Bars) {
                            CapsuleBars(series, Modifier.fillMaxSize(), ghToneContent(scheme, tone).copy(alpha = 0.75f))
                        } else {
                            DottedLine(series, Modifier.fillMaxSize(), ghToneContent(scheme, tone).copy(alpha = 0.75f))
                        }
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }

        GhSectionHeader(stringResource(R.string.section_categories))
        CategoryCard(
            icon = Icons.Filled.DirectionsRun,
            title = stringResource(R.string.category_activity),
            subtitle = stringResource(R.string.tracked_metrics, 2),
            onClick = { onOpenDetail(Metric.STEPS) },
        )
        CategoryCard(
            icon = Icons.Filled.Bedtime,
            title = stringResource(R.string.category_sleep),
            subtitle = stringResource(R.string.tracked_metrics, 1),
            onClick = onOpenSleepDetail,
        )
        CategoryCard(
            icon = Icons.Filled.MonitorWeight,
            title = stringResource(R.string.category_body),
            subtitle = stringResource(R.string.tracked_metrics, 1),
            onClick = { onOpenDetail(Metric.BODY) },
        )
    }
}

private data class MetricTileData(
    val value: String,
    val unit: String?,
    val status: String?,
    val series: List<Float>,
    val tone: GhTone,
)

@Composable
private fun CategoryCard(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    GhCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GhIconPill(icon, null, scheme.primaryContainer, scheme.onPrimaryContainer)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
```

`MetricTileData` destructuring requires component1..5; data class provides. For `Metric.WORKOUT`, pass `unit = null` and use today's workout count as the value.

Keep the existing `internal fun MonthSelector(...)` at the bottom of `ChartsScreen.kt` for now: the old `MetricDetailScreen` still calls it until Task 9 rewrites that screen. Delete it in Task 9 after the rewrite.

- [ ] **Step 5: Update `AppNav.kt`**

```kotlin
ChartsScreen(
    parsedRepository = parsedRepository,
    onOpenDetail = { metric ->
        when (val destination = metricDestination(metric)) {
            MetricDestination.Sleep -> navController.navigate("sleep_detail")
            MetricDestination.WorkoutList -> navController.navigate("workout_list")
            is MetricDestination.Detail -> navController.navigate("detail/${destination.metric.name}")
        }
    },
    onOpenSleepDetail = { navController.navigate("sleep_detail") },
    onOpenWorkoutList = { navController.navigate("workout_list") },
)
```

- [ ] **Step 6: Run tests and build**

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.jhon.mihealthreader.ui.screens.MetricDestinationTest"
.\gradlew.bat :app:assembleDebug
```

- [ ] **Step 7: Checkpoint**

```powershell
git add app/src/main/java/com/jhon/mihealthreader/ui/screens/MetricDestination.kt app/src/main/java/com/jhon/mihealthreader/ui/screens/ChartsScreen.kt app/src/main/java/com/jhon/mihealthreader/ui/AppNav.kt app/src/test/java/com/jhon/mihealthreader/ui/screens/MetricDestinationTest.kt
# commit only if the user asked
```

---

### Task 9: Metric detail screen

**Files:**
- Create: `app/src/main/java/com/jhon/mihealthreader/ui/screens/MetricRangeUi.kt`
- Modify: `app/src/main/java/com/jhon/mihealthreader/ui/screens/MetricDetailScreen.kt`
- Delete: `app/src/main/java/com/jhon/mihealthreader/ui/screens/MetricChartView.kt`
- Create: `app/src/test/java/com/jhon/mihealthreader/ui/screens/MetricRangeUiTest.kt`

**Interfaces:** `canMoveNext(end: LocalDate, today: LocalDate = LocalDate.now()): Boolean`. Consumes `ChartRange`, `ChartSeries`, `GhSegmentedControl`, `GhRangeHeader`, `GhChart`, `GhListRow`, `GhEmptyState`, `GhStatusPill`.

- [ ] **Step 1: Write the failing range test**

```kotlin
package com.jhon.mihealthreader.ui.screens

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class MetricRangeUiTest {
    private val today = LocalDate.of(2026, 10, 21)

    @Test fun `past windows can move forward`() {
        assertTrue(canMoveNext(LocalDate.of(2026, 10, 20), today))
        assertTrue(canMoveNext(LocalDate.of(2026, 10, 18), today))
    }

    @Test fun `current and future windows cannot`() {
        assertFalse(canMoveNext(today, today))
        assertFalse(canMoveNext(today.plusDays(1), today))
    }
}
```

- [ ] **Step 2: Run it and verify failure**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "com.jhon.mihealthreader.ui.screens.MetricRangeUiTest"`
Expected: FAIL — `canMoveNext` unresolved.

- [ ] **Step 3: Implement `MetricRangeUi.kt`**

```kotlin
package com.jhon.mihealthreader.ui.screens

import java.time.LocalDate

fun canMoveNext(end: LocalDate, today: LocalDate = LocalDate.now()): Boolean = end < today
```

- [ ] **Step 4: Rewrite `MetricDetailScreen.kt`**

```kotlin
@Composable
fun MetricDetailScreen(metric: Metric, parsedRepository: ParsedRepository, onBack: () -> Unit) {
    var range by rememberSaveable { mutableStateOf(ChartRange.WEEK) }
    var anchor by rememberSaveable { mutableStateOf(LocalDate.now()) }
    val activity by parsedRepository.dailyActivity().collectAsState(initial = emptyList())
    val heart by parsedRepository.heartRate().collectAsState(initial = emptyList())
    val spo2 by parsedRepository.spo2().collectAsState(initial = emptyList())
    val workouts by parsedRepository.workouts().collectAsState(initial = emptyList())
    val body by parsedRepository.bodyMeasurements().collectAsState(initial = emptyList())

    val window = remember(metric, range, anchor, activity, heart, spo2, workouts, body) {
        when (metric) {
            Metric.STEPS -> ChartSeries.steps(activity, range, anchor)
            Metric.HR -> ChartSeries.heartRate(heart, range, anchor)
            Metric.SPO2 -> ChartSeries.spo2(spo2, range, anchor)
            Metric.WORKOUT -> ChartSeries.workouts(workouts, range, anchor)
            Metric.BODY -> ChartSeries.weight(body, range, anchor)
            Metric.SLEEP -> ChartWindow(anchor, anchor, emptyList())
        }
    }
    val values = window.buckets.map { it.value }
    val labels = window.buckets.map { bucketLabel(range, it) }
    val style = when (metric.chartKind) {
        MetricChartKind.Bars -> GhChartStyle.Bars
        MetricChartKind.Dotted -> GhChartStyle.DottedLine
        MetricChartKind.Area -> GhChartStyle.AreaLine
    }
    val unit = stringResource(metric.unitRes)
    val latest = values.lastOrNull()

    LazyColumn(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding().navigationBarsPadding().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { GhTopBar(stringResource(metric.labelRes), onBack) }
        item {
            GhSegmentedControl(
                options = listOf(
                    ChartRange.DAY to R.string.range_day,
                    ChartRange.WEEK to R.string.range_week,
                    ChartRange.MONTH to R.string.range_month,
                    ChartRange.THREE_MONTHS to R.string.range_three_months,
                    ChartRange.YEAR to R.string.range_year,
                ).map { GhSegment(it.first, stringResource(it.second)) },
                selected = range,
                onSelect = { range = it; anchor = LocalDate.now() },
            )
        }
        item {
            GhRangeHeader(
                label = rangeLabel(window),
                onPrevious = { anchor = ChartSeries.shift(range, anchor, -1) },
                onNext = { anchor = ChartSeries.shift(range, anchor, 1) },
                onToday = { anchor = LocalDate.now() },
                nextEnabled = canMoveNext(window.end),
            )
        }
        item {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(latest?.let(::formatChartValue) ?: "—",
                    style = MaterialTheme.typography.displayMedium)
                Text(" $unit", style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp))
            }
        }
        item {
            when {
                values.size >= 2 -> GhChart(
                    values = values,
                    labels = labels,
                    style = style,
                    selectedIndex = values.lastIndex,
                    valueFormatter = ::formatChartValue,
                )
                values.isEmpty() -> GhEmptyState(stringResource(R.string.detail_no_range_data))
            }
        }
        if (values.isNotEmpty()) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val pillContainer = MaterialTheme.colorScheme.surfaceContainer
                    GhStatusPill(stringResource(R.string.stat_min) + " " + formatChartValue(values.min()),
                        container = pillContainer)
                    GhStatusPill(stringResource(R.string.stat_avg) + " " + formatChartValue(values.average().toFloat()),
                        container = pillContainer)
                    GhStatusPill(stringResource(R.string.stat_max) + " " + formatChartValue(values.max()),
                        container = pillContainer)
                }
            }
            item {
                GhSectionHeader(stringResource(R.string.section_records))
            }
            items(window.buckets.reversed()) { bucket ->
                GhListRow(
                    label = bucketLabel(range, bucket),
                    value = formatChartValue(bucket.value) + " " + unit,
                )
            }
        }
    }
}

private fun LocalDate.toJavaDate(): java.util.Date =
    java.util.Date.from(atStartOfDay(ZoneId.systemDefault()).toInstant())

@Composable
private fun rangeLabel(window: ChartWindow): String {
    val fmt = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault())
    return if (window.start == window.end) {
        fmt.format(window.start.toJavaDate())
    } else {
        fmt.format(window.start.toJavaDate()) + stringResource(R.string.range_separator) +
            fmt.format(window.end.toJavaDate())
    }
}

@Composable
private fun bucketLabel(range: ChartRange, bucket: ChartBucket): String {
    val locale = Locale.getDefault()
    return when {
        bucket.hour != null -> "%02d:00".format(bucket.hour)
        range == ChartRange.DAY -> stringResource(R.string.action_today)
        range == ChartRange.WEEK -> bucket.start.dayOfWeek.getDisplayName(TextStyle.SHORT, locale)
        range == ChartRange.MONTH || range == ChartRange.THREE_MONTHS ->
            DateTimeFormatter.ofPattern("M/d", locale).format(bucket.start)
        else -> DateTimeFormatter.ofPattern("MMM", locale).format(bucket.start)
    }
}

private fun formatChartValue(value: Float): String =
    if (value == value.toInt().toFloat()) value.toInt().toString() else "%.1f".format(value)
```

Note: `LazyColumn` needs `items` import; `rememberSaveable` needs `androidx.compose.runtime.saveable.rememberSaveable`.

- [ ] **Step 5: Delete the old chart view and selector**

```powershell
Remove-Item app/src/main/java/com/jhon/mihealthreader/ui/screens/MetricChartView.kt
# Then remove the now-unused internal fun MonthSelector(...) from ChartsScreen.kt
```

- [ ] **Step 6: Run tests and build**

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.jhon.mihealthreader.ui.screens.MetricRangeUiTest"
.\gradlew.bat :app:assembleDebug
```

- [ ] **Step 7: Checkpoint**

```powershell
git add app/src/main/java/com/jhon/mihealthreader/ui/screens/MetricRangeUi.kt app/src/main/java/com/jhon/mihealthreader/ui/screens/MetricDetailScreen.kt app/src/main/java/com/jhon/mihealthreader/ui/screens/MetricChartView.kt app/src/test/java/com/jhon/mihealthreader/ui/screens/MetricRangeUiTest.kt
# commit only if the user asked
```

---

### Task 10: Sleep detail screen

**Files:**
- Create: `app/src/main/java/com/jhon/mihealthreader/ui/screens/SleepUi.kt`
- Modify: `app/src/main/java/com/jhon/mihealthreader/ui/screens/SleepDetailScreen.kt`
- Modify: `app/src/main/java/com/jhon/mihealthreader/ui/charts/Hypnogram.kt` (grid color parameter)
- Test: `app/src/test/java/com/jhon/mihealthreader/ui/screens/SleepUiTest.kt`

**Interfaces:** `SleepBreakdown(deep, light, awake)`, `sleepStageBreakdown(SleepSession): SleepBreakdown`.

- [ ] **Step 1: Write the failing breakdown test**

```kotlin
package com.jhon.mihealthreader.ui.screens

import com.jhon.mihealthreader.parse.SleepSession
import org.junit.Assert.assertEquals
import org.junit.Test

class SleepUiTest {
    @Test fun `stage percentages sum to one hundred`() {
        val session = SleepSession(0, 0, 100, 20, 70, null, null, null, emptyList())
        val b = sleepStageBreakdown(session)
        assertEquals(20, b.deep)
        assertEquals(70, b.light)
        assertEquals(10, b.awake)
        assertEquals(100, b.deep + b.light + b.awake)
    }

    @Test fun `zero duration is safe`() {
        val session = SleepSession(0, 0, 0, 0, 0, null, null, null, emptyList())
        val b = sleepStageBreakdown(session)
        assertEquals(100, b.deep + b.light + b.awake)
    }
}
```

- [ ] **Step 2: Run it and verify failure**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "com.jhon.mihealthreader.ui.screens.SleepUiTest"`
Expected: FAIL — `sleepStageBreakdown` unresolved.

- [ ] **Step 3: Implement `SleepUi.kt`**

```kotlin
package com.jhon.mihealthreader.ui.screens

import androidx.annotation.StringRes
import com.jhon.mihealthreader.R
import com.jhon.mihealthreader.parse.SleepSession

data class SleepBreakdown(val deep: Int, val light: Int, val awake: Int)

fun sleepStageBreakdown(session: SleepSession): SleepBreakdown {
    val total = session.durationMinutes.coerceAtLeast(1)
    val deep = (session.deepMinutes * 100 / total).coerceIn(0, 100)
    val light = (session.lightMinutes * 100 / total).coerceIn(0, 100 - deep)
    return SleepBreakdown(deep, light, 100 - deep - light)
}

@StringRes
fun regularityLabelRes(stdMinutes: Int): Int = when {
    stdMinutes <= 30 -> R.string.sleep_regular
    stdMinutes <= 60 -> R.string.sleep_somewhat_regular
    else -> R.string.sleep_irregular
}
```

- [ ] **Step 4: Add the grid color to `Hypnogram`**

Change the signature to include `gridColor: Color` and replace `Color.Gray.copy(alpha = 0.25f)` with `gridColor`.

```kotlin
@Composable
fun Hypnogram(
    segments: List<HypnoSegment>,
    modifier: Modifier,
    deepColor: Color,
    lightColor: Color,
    awakeColor: Color,
    napColor: Color,
    gridColor: Color,
)
```

- [ ] **Step 5: Rewrite `SleepDetailScreen.kt`**

Use `GhTopBar`, `GhRangeHeader`, `GhCard`, `GhTintedCard`, `GhStatusPill`, `GhChart`, `Hypnogram`. Keep the existing data selection (`SleepSummary.mainSleep`, session index, naps, regularity). The layout:

```kotlin
@Composable
fun SleepDetailScreen(parsedRepository: ParsedRepository, onBack: () -> Unit) {
    val sleep by parsedRepository.sleepSessions().collectAsState(initial = emptyList())
    val main = remember(sleep) { SleepSummary.mainSleep(sleep) }
    var index by remember(sleep) { mutableStateOf(sleep.indexOfFirst { it === main }.coerceAtLeast(0)) }
    val session = sleep.getOrNull(index)
    val scheme = MaterialTheme.colorScheme
    val deepColor = scheme.primary
    val lightColor = scheme.tertiary
    val awakeColor = scheme.error
    val napColor = scheme.secondaryContainer

    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState()).statusBarsPadding()
            .navigationBarsPadding().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        GhTopBar(stringResource(R.string.sleep_title), onBack)
        if (session == null) {
            GhEmptyState(stringResource(R.string.sleep_no_data))
            return
        }
        GhRangeHeader(
            label = formatNight(session.start),
            onPrevious = { if (index < sleep.size - 1) index += 1 },
            onNext = { if (index > 0) index -= 1 },
            onToday = { index = sleep.indexOfFirst { it === main }.coerceAtLeast(0) },
            nextEnabled = index > 0,
        )

        val pagePill = MaterialTheme.colorScheme.surfaceContainer
        Row(verticalAlignment = Alignment.Bottom) {
            Text(formatMinutesCn(session.durationMinutes), style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.width(8.dp))
            GhStatusPill(
                stringResource(R.string.sleep_deep_ratio) + " " + SleepSummary.deepRatioPct(session) + "%",
                container = pagePill,
            )
        }
        val dayNaps = remember(sleep, session) {
            SleepSummary.napsOnDay(sleep, SleepSummary.endDate(session))
        }
        if (dayNaps.isNotEmpty()) {
            GhStatusPill(
                stringResource(R.string.sleep_naps_summary,
                    dayNaps.sumOf { it.durationMinutes }, dayNaps.size),
                container = pagePill,
            )
        }

        GhCard {
            Text(stringResource(R.string.sleep_analysis), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatItem(stringResource(R.string.sleep_total), formatMinutesCn(session.durationMinutes))
                StatItem(stringResource(R.string.sleep_bedtime), SleepSummary.hhmm(session.start))
                StatItem(stringResource(R.string.sleep_deep_ratio), "${SleepSummary.deepRatioPct(session)}%")
                StatItem(stringResource(R.string.sleep_awake_count), "${SleepSummary.awakeCount(session)}")
            }
        }

        val day = SleepSummary.endDate(session)
        val daySessions = remember(sleep, day) { SleepSummary.daySessions(sleep, day).sortedBy { it.start } }
        val segments = remember(daySessions) {
            daySessions.flatMap { s ->
                if (s.stages.isNotEmpty()) s.stages.map { HypnoSegment(it.start, it.end, it.stateName) }
                else listOf(HypnoSegment(s.start, s.end, "nap"))
            }
        }

        GhCard {
            Text(stringResource(R.string.sleep_timeline), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LegendDot(deepColor, stringResource(R.string.sleep_deep))
                LegendDot(lightColor, stringResource(R.string.sleep_light))
                LegendDot(awakeColor, stringResource(R.string.sleep_awake))
                LegendDot(napColor, stringResource(R.string.sleep_nap))
            }
            Spacer(Modifier.height(12.dp))
            if (segments.isEmpty()) {
                Text(stringResource(R.string.sleep_no_stage))
            } else {
                Hypnogram(segments, Modifier.fillMaxWidth().height(140.dp),
                    deepColor, lightColor, awakeColor, napColor, scheme.outlineVariant)
            }
        }

        val b = sleepStageBreakdown(session)
        GhCard {
            Text(stringResource(R.string.sleep_total), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                StageBar(b.deep, deepColor, Modifier.weight(b.deep.coerceAtLeast(1).toFloat()))
                StageBar(b.light, lightColor, Modifier.weight(b.light.coerceAtLeast(1).toFloat()))
                StageBar(b.awake, awakeColor, Modifier.weight(b.awake.coerceAtLeast(1).toFloat()))
            }
            Spacer(Modifier.height(8.dp))
            Text("${b.deep}% / ${b.light}% / ${b.awake}%", style = MaterialTheme.typography.labelMedium)
        }

        GhTintedCard(GhTone.Tertiary) {
            Text(stringResource(R.string.sleep_interpretation), style = MaterialTheme.typography.titleLarge)
            Text(SleepSummary.interpret(session), style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = 6.dp))
            Text(SleepSummary.details(session), style = MaterialTheme.typography.bodyMedium)
        }

        val recent = remember(sleep) { sleep.filter { SleepSummary.isNightSleep(it) }.take(14) }
        val regularity = remember(recent) { SleepSummary.bedtimeRegularity(recent) }
        if (regularity != null) {
            GhCard {
                Text(stringResource(R.string.sleep_regularity), style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    StatItem(stringResource(R.string.sleep_avg_bedtime), SleepSummary.formatBedtime(regularity.averageMinutes))
                    StatItem(stringResource(R.string.sleep_variation), "±${regularity.stdMinutes} " + stringResource(R.string.unit_minutes))
                    StatItem(stringResource(R.string.sleep_rating), stringResource(regularityLabelRes(regularity.stdMinutes)))
                }
                val values = remember(recent) { recent.reversed().map { SleepSummary.bedtimeMinutes(it).toFloat() } }
                val labels = remember(recent) {
                    recent.reversed().map {
                        Instant.ofEpochSecond(it.start).atZone(ZoneId.systemDefault())
                            .format(DateTimeFormatter.ofPattern("M/d"))
                    }
                }
                if (values.size >= 2) {
                    GhChart(values, labels, Modifier.padding(top = 12.dp), GhChartStyle.DottedLine,
                        valueFormatter = { SleepSummary.formatBedtime(it.toInt()) })
                }
                Text(stringResource(R.string.sleep_recent_nights, regularity.nights),
                    style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column { Text(label, style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium) }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).background(color, CircleShape))
        Text(label, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(start = 6.dp))
    }
}

@Composable
private fun StageBar(percent: Int, color: Color, modifier: Modifier) {
    Box(modifier.height(10.dp).clip(CircleShape).background(color))
}
```

Keep the existing `formatNight` and `formatMinutesCn` helpers at the bottom.

- [ ] **Step 6: Run tests and build**

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.jhon.mihealthreader.ui.screens.SleepUiTest"
.\gradlew.bat :app:assembleDebug
```

- [ ] **Step 7: Checkpoint**

```powershell
git add app/src/main/java/com/jhon/mihealthreader/ui/screens/SleepUi.kt app/src/main/java/com/jhon/mihealthreader/ui/screens/SleepDetailScreen.kt app/src/main/java/com/jhon/mihealthreader/ui/charts/Hypnogram.kt app/src/test/java/com/jhon/mihealthreader/ui/screens/SleepUiTest.kt
# commit only if the user asked
```

---

### Task 11: Workout list and detail

**Files:**
- Create: `app/src/main/java/com/jhon/mihealthreader/ui/screens/WorkoutUi.kt`
- Modify: `app/src/main/java/com/jhon/mihealthreader/ui/screens/WorkoutListScreen.kt`
- Modify: `app/src/main/java/com/jhon/mihealthreader/ui/screens/WorkoutDetailScreen.kt`
- Test: `app/src/test/java/com/jhon/mihealthreader/ui/screens/WorkoutUiTest.kt`

**Interfaces:** `workoutTypeOptions(List<Workout>): List<String>`, `filterWorkouts(List<Workout>, String?): List<Workout>`.

- [ ] **Step 1: Write the failing filter test**

```kotlin
package com.jhon.mihealthreader.ui.screens

import com.jhon.mihealthreader.parse.Workout
import org.junit.Assert.assertEquals
import org.junit.Test

class WorkoutUiTest {
    private fun workout(type: String) = Workout(
        "dev", 0, 600, 600, 100, null, null, null, null, type, type,
    )

    @Test fun `types are distinct most frequent first`() {
        val options = workoutTypeOptions(listOf(
            workout("Running"), workout("Running"), workout("Walking"),
        ))
        assertEquals(listOf("Running", "Walking"), options)
    }

    @Test fun `null filter keeps everything`() {
        val all = listOf(workout("Running"), workout("Walking"))
        assertEquals(all, filterWorkouts(all, null))
    }

    @Test fun `type filter keeps matching sessions`() {
        val all = listOf(workout("Running"), workout("Walking"))
        assertEquals(listOf(all[1]), filterWorkouts(all, "Walking"))
    }
}
```

- [ ] **Step 2: Run it and verify failure**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "com.jhon.mihealthreader.ui.screens.WorkoutUiTest"`
Expected: FAIL — unresolved.

- [ ] **Step 3: Implement `WorkoutUi.kt`**

```kotlin
package com.jhon.mihealthreader.ui.screens

import com.jhon.mihealthreader.parse.Workout

fun workoutTypeOptions(workouts: List<Workout>): List<String> =
    workouts.groupingBy { it.typeName }.eachCount()
        .entries.sortedByDescending { it.value }
        .map { it.key }

fun filterWorkouts(workouts: List<Workout>, type: String?): List<Workout> =
    if (type == null) workouts else workouts.filter { it.typeName == type }
```

- [ ] **Step 4: Rewrite `WorkoutListScreen.kt`**

```kotlin
@Composable
fun WorkoutListScreen(parsedRepository: ParsedRepository, onOpen: (Int) -> Unit, onBack: () -> Unit) {
    val workouts by parsedRepository.workouts().collectAsState(initial = emptyList())
    var type by rememberSaveable { mutableStateOf<String?>(null) }
    val options = remember(workouts) { workoutTypeOptions(workouts) }
    val filtered = remember(workouts, type) { filterWorkouts(workouts, type) }
    val indexes = remember(workouts, filtered) { filtered.map { workouts.indexOf(it) } }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)
        .statusBarsPadding().navigationBarsPadding()) {
        GhTopBar(stringResource(R.string.workout_title), onBack,
            modifier = Modifier.padding(horizontal = 8.dp)) {
            GhStatusPill(stringResource(R.string.workout_count, workouts.size),
                modifier = Modifier.padding(end = 8.dp),
                container = MaterialTheme.colorScheme.surfaceContainer)
        }
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            GhFilterPill(stringResource(R.string.workout_all), type == null) { type = null }
            options.forEach { option ->
                GhFilterPill(option, type == option) { type = option }
            }
        }
        if (workouts.isEmpty()) {
            GhEmptyState(stringResource(R.string.workout_empty), icon = Icons.Filled.DirectionsRun)
            return@Column
        }
        LazyColumn(
            Modifier.fillMaxWidth().weight(1f).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            itemsIndexed(filtered) { i, workout ->
                GhCard(onClick = { onOpen(indexes[i]) }, modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        GhIconPill(
                            icon = Icons.Filled.DirectionsRun,
                            contentDescription = null,
                            container = SportColors.of(workout.type).copy(alpha = 0.16f),
                            contentColor = SportColors.of(workout.type),
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(workout.typeName, style = MaterialTheme.typography.titleMedium)
                            Text(
                                formatDateTime(workout.start) + " · " +
                                    formatDurationSeconds(workout.durationSec) + " · " +
                                    workout.calories + " kcal",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text("${workout.avgHr ?: "—"} bpm", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun GhFilterPill(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainer,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        shape = CircleShape,
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
    }
}
```

- [ ] **Step 5: Rewrite `WorkoutDetailScreen.kt`**

```kotlin
@Composable
fun WorkoutDetailScreen(parsedRepository: ParsedRepository, index: Int, onBack: () -> Unit) {
    val workouts by parsedRepository.workouts().collectAsState(initial = emptyList())
    val heart by parsedRepository.heartRate().collectAsState(initial = emptyList())
    val workout = workouts.getOrNull(index)
    val scheme = MaterialTheme.colorScheme

    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState()).statusBarsPadding()
            .navigationBarsPadding().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (workout == null) {
            GhTopBar(stringResource(R.string.workout_title), onBack)
            GhEmptyState(stringResource(R.string.label_no_data))
            return
        }
        GhTopBar(workout.typeName, onBack)
        val sport = SportColors.of(workout.type)
        Surface(
            color = sport.copy(alpha = 0.14f),
            contentColor = scheme.onSurface,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                GhIconPill(Icons.Filled.DirectionsRun, null, sport.copy(alpha = 0.20f), sport)
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(workout.typeName, style = MaterialTheme.typography.headlineSmall)
                    Text(formatDateTime(workout.start), style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(workout.calories.toString(), style = MaterialTheme.typography.displaySmall)
                        Text(" " + stringResource(R.string.unit_kcal), style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 6.dp))
                    }
                }
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile(stringResource(R.string.detail_duration), formatDurationSeconds(workout.durationSec), Modifier.weight(1f))
            StatTile(stringResource(R.string.detail_avg_hr), "${workout.avgHr ?: "—"} bpm", Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile(stringResource(R.string.detail_max_hr), "${workout.maxHr ?: "—"} bpm", Modifier.weight(1f))
            StatTile(stringResource(R.string.detail_distance),
                workout.distanceMeters?.let { "$it m" } ?: "—", Modifier.weight(1f))
        }

        val samples = remember(heart, workout) {
            heart.filter { it.time in workout.start..workout.end }.sortedBy { it.time }
        }
        GhCard {
            Text(stringResource(R.string.workout_heart_rate), style = MaterialTheme.typography.titleLarge)
            if (samples.size >= 2) {
                GhChart(
                    values = samples.map { it.value.toFloat() },
                    labels = samples.map { hhmm(it.time) },
                    style = GhChartStyle.DottedLine,
                    modifier = Modifier.padding(top = 12.dp),
                )
            } else {
                Text(stringResource(R.string.workout_no_samples),
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier) {
    GhCard(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleLarge)
    }
}
```

Keep the existing `hhmm` helper.

- [ ] **Step 6: Run tests and build**

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.jhon.mihealthreader.ui.screens.WorkoutUiTest"
.\gradlew.bat :app:assembleDebug
```

- [ ] **Step 7: Checkpoint**

```powershell
git add app/src/main/java/com/jhon/mihealthreader/ui/screens/WorkoutUi.kt app/src/main/java/com/jhon/mihealthreader/ui/screens/WorkoutListScreen.kt app/src/main/java/com/jhon/mihealthreader/ui/screens/WorkoutDetailScreen.kt app/src/test/java/com/jhon/mihealthreader/ui/screens/WorkoutUiTest.kt
# commit only if the user asked
```

---

### Task 12: Settings screen

**Files:**
- Modify: `app/src/main/java/com/jhon/mihealthreader/ui/screens/SettingsScreen.kt`
- Modify: `app/src/main/java/com/jhon/mihealthreader/ui/AppNav.kt` (settings call if signature changes)
- Test: existing `RoutesTest` + build; no new pure test required.

**Interfaces:** Keeps the existing `SettingsScreen` parameters. Uses `GhPageTitle`, `GhCard`, `GhSectionHeader`, `GhPillButton`, `GhStatusPill`, `GhSegmentedControl`, `GhSwitchRow`.

- [ ] **Step 1: Replace the settings layout**

Keep the two `rememberLauncherForActivityResult` blocks (Health Connect permissions and SAF export) unchanged. Replace the `Column` body:

```kotlin
Column(
    Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)
        .verticalScroll(rememberScrollState())
        .statusBarsPadding().padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(20.dp),
) {
    GhPageTitle(stringResource(R.string.title_settings), subtitle = stringResource(R.string.settings_subtitle))

    GhSectionHeader(stringResource(R.string.settings_source))
    GhCard {
        GhSegmentedControl(
            options = listOf(
                GhSegment(BackendType.ROOT, stringResource(R.string.settings_source_root)),
                GhSegment(BackendType.SHIZUKU, stringResource(R.string.settings_source_shizuku)),
            ),
            selected = backend,
            onSelect = onSelectBackend,
            unselectedContainer = MaterialTheme.colorScheme.surfaceContainerLowest,
        )
        Spacer(Modifier.height(12.dp))
        GhStatusPill(
            stringResource(
                R.string.settings_source_current,
                stringResource(if (backend == BackendType.ROOT) R.string.settings_source_root else R.string.settings_source_shizuku),
                backendStatus(rootGranted),
            ),
        )
        if (backend == BackendType.SHIZUKU && !shizukuGranted) {
            Spacer(Modifier.height(12.dp))
            GhPillButton(stringResource(R.string.action_request_shizuku), onRequestShizuku)
            Text(stringResource(R.string.settings_shizuku_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.settings_source_restart),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }

    GhSectionHeader(stringResource(R.string.settings_extraction))
    GhCard {
        Text(summaryText(summary), style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(12.dp))
        when (state) {
            ExtractionState.Idle -> GhPillButton(stringResource(R.string.action_extract), onStart)
            is ExtractionState.Running -> {
                GhPillButton(stringResource(R.string.action_extracting), {}, enabled = false)
                Text(state.step, style = MaterialTheme.typography.bodySmall)
            }
            is ExtractionState.Success -> {
                Text(
                    if (state.newRecords > 0) stringResource(R.string.settings_extract_new, state.newRecords)
                    else stringResource(R.string.settings_extract_none),
                    style = MaterialTheme.typography.bodyMedium,
                )
                GhPillButton(stringResource(R.string.action_extract_again), onStart)
            }
            is ExtractionState.Error -> {
                Text(stringResource(R.string.settings_error, state.message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error)
                GhPillButton(stringResource(R.string.action_retry), onStart)
            }
        }
    }

    GhSectionHeader(stringResource(R.string.settings_export))
    GhCard {
        Text(stringResource(R.string.settings_export_hint), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        GhPillButton(stringResource(R.string.action_export), onClick = {
            if (exportManager.latestNormalized() == null) exportMessage = context.getString(R.string.settings_export_empty)
            else exportLauncher.launch(exportManager.defaultExportName())
        })
        exportMessage?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
        }
    }

    if (healthConnectAvailable) {
        GhSectionHeader(stringResource(R.string.settings_health_connect))
        GhSwitchRow(
            title = stringResource(R.string.settings_sync_hc),
            subtitle = null,
            checked = healthConnectEnabled,
            onCheckedChange = { checked ->
                if (checked) healthPermissionLauncher.launch(healthConnectPermissions)
                else onToggleHealthConnect(false)
            },
        )
        if (healthConnectEnabled) {
            GhPillButton(stringResource(R.string.action_sync), onSyncHealthConnect)
        }
        healthConnectMessage?.let {
            Text(it, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        GhStatusPill(stringResource(R.string.settings_hc_unavailable),
            container = MaterialTheme.colorScheme.surfaceContainer)
    }

    GhSectionHeader(stringResource(R.string.settings_about))
    GhCard {
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(stringResource(R.string.settings_about_body),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp))
    }
}
```

Helpers:

```kotlin
@Composable
private fun backendStatus(granted: Boolean?): String = when (granted) {
    true -> stringResource(R.string.settings_source_granted)
    false -> stringResource(R.string.settings_source_not_granted)
    null -> stringResource(R.string.settings_source_checking)
}

@Composable
internal fun summaryText(summary: LocalSummary): String {
    val last = summary.lastExtractedAt
    val whenText = if (last == null) {
        stringResource(R.string.settings_never_synced)
    } else {
        stringResource(
            R.string.settings_last_sync,
            Instant.ofEpochMilli(last).atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
        )
    }
    return stringResource(R.string.settings_summary, whenText, summary.stepSamples, summary.sleepSessions)
}
```

`summaryText` was previously non-composable and may be used in tests — search shows only this file. If a test uses it, update the test to call it through a Compose test or remove the assertion.

- [ ] **Step 2: Build and lint**

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:lintDebug
```

- [ ] **Step 3: Checkpoint**

```powershell
git add app/src/main/java/com/jhon/mihealthreader/ui/screens/SettingsScreen.kt
# commit only if the user asked
```

---

### Task 13: Navigation chrome, edge-to-edge, cleanup

**Files:**
- Modify: `app/src/main/java/com/jhon/mihealthreader/ui/AppNav.kt`
- Modify: `app/src/main/java/com/jhon/mihealthreader/MainActivity.kt`
- Delete: `app/src/main/java/com/jhon/mihealthreader/ui/charts/AxisChart.kt` (after no references)
- Modify: `app/src/test/java/com/jhon/mihealthreader/ui/nav/RoutesTest.kt`

**Interfaces:** Uses `GhBottomBar`, `GhTab`, `showsBottomBar`.

- [ ] **Step 1: Extend the route test**

```kotlin
@Test fun `detail and workout routes hide the bar`() {
    assertFalse(showsBottomBar("sleep_detail"))
    assertFalse(showsBottomBar("workout_list"))
    assertFalse(showsBottomBar("workout_detail/0"))
}
```

- [ ] **Step 2: Replace the bottom bar in `AppNav.kt`**

```kotlin
private val tabs = listOf(
    GhTab(Routes.TODAY, R.string.nav_today, Icons.Filled.WbSunny),
    GhTab(Routes.CHARTS, R.string.nav_charts, Icons.Filled.Insights),
    GhTab(Routes.SETTINGS, R.string.nav_settings, Icons.Filled.Settings),
)

Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
    bottomBar = {
        if (showsBottomBar(currentRoute)) {
            GhBottomBar(
                tabs = tabs,
                currentRoute = currentRoute,
                onSelect = { route ->
                    navController.navigate(route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
            )
        }
    },
) { padding ->
    NavHost(
        navController = navController,
        startDestination = Routes.TODAY,
        modifier = Modifier.padding(padding),
        enterTransition = { fadeIn(tween(180)) + slideInHorizontally(tween(220)) { it / 10 } },
        exitTransition = { fadeOut(tween(120)) },
        popEnterTransition = { fadeIn(tween(180)) },
        popExitTransition = { fadeOut(tween(120)) + slideOutHorizontally(tween(220)) { it / 10 } },
    ) {
        // existing composable routes unchanged
    }
}
```

- [ ] **Step 3: Enable edge-to-edge in `MainActivity`**

```kotlin
import androidx.activity.enableEdgeToEdge

override fun onCreate(savedInstanceState: Bundle?) {
    enableEdgeToEdge()
    super.onCreate(savedInstanceState)
    setContent { GhTheme { GhWindowEffects(); AppNav(extractionViewModel) } }
}
```

- [ ] **Step 4: Delete the old axis chart if unused**

```powershell
rg -n "ChartWithAxes|StackedChartWithAxes|DottedChartWithAxes" app/src/main
# if no output:
Remove-Item app/src/main/java/com/jhon/mihealthreader/ui/charts/AxisChart.kt
```

- [ ] **Step 5: Run tests and build**

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.jhon.mihealthreader.ui.nav.RoutesTest"
.\gradlew.bat :app:assembleDebug
```

- [ ] **Step 6: Checkpoint**

```powershell
git add app/src/main/java/com/jhon/mihealthreader/ui/AppNav.kt app/src/main/java/com/jhon/mihealthreader/MainActivity.kt app/src/main/java/com/jhon/mihealthreader/ui/charts/AxisChart.kt app/src/test/java/com/jhon/mihealthreader/ui/nav/RoutesTest.kt
# commit only if the user asked
```

---

### Task 14: Full verification and device pass

**Files:**
- Modify: `docs/manual-verification.md` (append §26)

- [ ] **Step 1: Run the full suite**

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

Expected: all unit tests PASS, lint clean (no `MissingTranslation`, no hardcoded color/text), APK built.

- [ ] **Step 2: Install and launch**

```powershell
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am force-stop com.jhon.mihealthreader
adb shell am start -n com.jhon.mihealthreader/.MainActivity
```

- [ ] **Step 3: Light-mode screenshots**

For each screen, capture and visually verify:

```powershell
adb exec-out screencap -p > "$env:TEMP\opencode\mh_today.png"
```

Screens: 今天 / 视图 / 设置 / 指标详情（步数）/ 睡眠详情 / 运动列表 / 运动详情.
Check: no default purple, cards 28dp, right-axis charts, bottom bar pill, no clipped text, no empty flash.

- [ ] **Step 4: Dark-mode screenshots**

```powershell
adb shell cmd uimode night yes
adb shell am force-stop com.jhon.mihealthreader
adb shell am start -n com.jhon.mihealthreader/.MainActivity
```

Repeat the screenshots. Check contrast of tinted cards, text, charts, and bottom bar.

- [ ] **Step 5: Interaction checks**

- Switch tabs: state preserved, bottom bar stays visible.
- Open each detail: bottom bar hidden; back returns.
- Range control: 天/周/月/3个月/年; previous/next; “today” resets; next disabled at today.
- Settings: backend selection, extraction states, export, Health Connect toggle/sync.
- Empty data: clear app data (`adb shell pm clear com.jhon.mihealthreader`), reopen; inline no-data card appears (not a dialog), no crash.
- Rotation: activity recreates without losing range selection.

- [ ] **Step 6: Restore light mode**

```powershell
adb shell cmd uimode night no
```

- [ ] **Step 7: Record verification**

Append to `docs/manual-verification.md`:

```markdown
## 26. Google Health 风格界面重构（2026-10-26）

- 设计系统：`GhTheme`（Monet 动态色 + 低版本回退）、`GhCard`/`GhPillButton`/`GhSegmentedControl`/`GhRing`/`GhMetricTile`/`GhBottomBar`。
- 页面：今天（圆环 + 三色 hero + sheet）、视图（指标网格 + 分类）、指标详情（天/周/月/3个月/年 + 右轴图表 + 按桶列表）、睡眠详情、运动列表/详情、设置。
- 验证：`testDebugUnitTest`、`lintDebug`、`assembleDebug` 全绿；真机亮/暗两套截图通过。
```

- [ ] **Step 8: Final checkpoint**

```powershell
git add docs/manual-verification.md
# commit only if the user asked
```

---

## Plan Self-Review

**Spec coverage:**

- Monet + fallback colors → Task 1.
- Typography/shapes/spacing → Task 1.
- Component catalog → Tasks 3–4.
- Right-axis charts + mini charts → Tasks 4–5.
- Range aggregation → Task 6.
- Today screen (ring x/7, tinted hero, actions, sheet, chips, recent, inline empty) → Task 7.
- Views screen (metric grid + categories) → Task 8.
- Metric detail (segmented range, right axis, stats, bucket list) → Task 9.
- Sleep detail (analysis, timeline, breakdown, insights, regularity) → Task 10.
- Workout list/detail → Task 11.
- Settings (source, extraction, export, Health Connect, about) → Task 12.
- Three-tab nav, edge-to-edge, hidden bar on details → Task 13.
- Strings/i18n, no hardcoded colors/text → Tasks 1–2 + lint in Task 14.
- Verification → Task 14.

**Placeholder scan:** No TBD/TODO; every code step has concrete code or an exact command. Import lists for screen files are intentionally omitted (mechanical); the new component/data files include full imports.

**Type consistency:**

- `GhTone` used by `GhTintedCard`, `GhMetricTile`, `TodayScreen`, `SleepDetailScreen`.
- `ChartRange`, `ChartBucket`, `ChartWindow` defined in Task 6 and consumed in Task 9.
- `metric.chartKind` / `metric.unitRes` / `metric.labelRes` defined in Task 2 and consumed in Tasks 8–9.
- `GhChartStyle` defined in Task 5 and consumed in Tasks 9–10.
- `sleepStageBreakdown` / `regularityLabelRes` defined in Task 10.
- `workoutTypeOptions` / `filterWorkouts` defined in Task 11.
- `GhTab`/`GhBottomBar` defined in Task 4 and consumed in Task 13.

**Review Focus tests:**

1. Fallback color leak → `GhFallbackColorsTest` (Task 1).
2. Dark contrast → `GhToneTest` (Task 3) + dark screenshots (Task 14).
3. Timezone/cross-day → `ChartSeriesTest` (Task 6).
4. Empty/loading flash → Task 7 gating + Task 14 `pm clear` check.
5. Range boundaries → `ChartSeriesTest` + Task 9 `nextEnabled` + Task 14 interaction check.

