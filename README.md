# MihealthReader

个人用的 Android 应用：在**已 root（或 Shizuku）**的手机上，读取「小米运动健康 / Mi Fitness」（`com.mi.health`）的私有 SQLite 数据库，把数据解析、持久化到本地，并用 Material 3 图表展示、导出为 JSON，供自己分析与记录。

> 非官方、仅个人使用。与小米无隶属关系。数据来源为非公开的本地数据库结构，随 App/固件版本可能变化。

## 功能

- **两种数据获取通道**：`Root`（libsu，全局挂载命名空间）或 `Shizuku`（UserService/AIDL，需 Shizuku 以 root 启动）。
- **提取 + 快照**：复制源数据库（`db` + `-wal`，避开过时 `-shm`）为带时间戳的原始快照。
- **解析指标**：睡眠（阶段）、步数/活动、心率、静息心率、血氧、运动记录、体重/体成分。
- **本地持久化（Room）**：提取一次后入库；**启动即读**，无需每次重新提取；再次提取**增量幂等**（有新数据则新增，否则提示"无新数据"）。
- **Material 3 界面**（Monet 动态取色）：
  - 底部导航：**今天 / 视图 / 设置**。
  - 今天：平面卡片网格（步数/睡眠/心率/运动/血氧/体重），近 7 次迷你图。
  - 视图：按**月份**查看趋势（面积折线 / 圆角柱 / 堆叠柱），带坐标轴与最小/平均/最大。
  - 睡眠详情：睡眠分析、睡眠解读、全天睡眠横向图（hypnogram）、入睡时间规律性。
- **导出**：规范化 JSON（`normalized.json`）经系统 SAF 保存到任意位置。

## 技术栈

| 项 | 版本 |
| --- | --- |
| 语言 / UI | Kotlin 2.0.21 · Jetpack Compose (Material 3) |
| 本地库 | Room 2.6.1 + KSP |
| 特权通道 | libsu 5.2.2 · Shizuku 13.1.5（api + provider） |
| 构建 | Gradle 8.9 · AGP 8.7.3 · JDK 17+（开发环境用 21） |
| SDK | minSdk 26 · compileSdk/targetSdk 35 |
| applicationId | `com.jhon.mihealthreader` |

## 数据来源

- 目标包：**`com.mi.health`**（小米运动健康）。
- 主数据库：`/data/data/com.mi.health/databases/<账号UID>/cn/fitness_data`（明文 SQLite，含 `-wal`）。
- 解析的表/键（`parse/`）：

| 指标 | 表 | 说明 |
| --- | --- | --- |
| 睡眠 | `sleep_segment` | `key='sleep'`，`value.items[]` 阶段；`2=deep,3=light,5=awake` |
| 步数 | `step_record` | `value={steps,distance,calories}`，按本地日聚合 + 分钟切片去重 |
| 心率 / 静息 | `hr_record` | `heart_rate` / `resting_heart_rate`，`value.bpm` |
| 血氧 | `spo2_record` | `value.spo2` |
| 运动 | `sport_report` | `value={start_time,end_time,duration,calories,avg_hrm,...}` |
| 体重/体成分 | `weight_item_record` | 独立列 `weight/bmi/bodyFatRate/...` |

## 构建

前置：JDK 17+、Android SDK（platform-tools、platforms;android-35、build-tools;35.0.0）。

```bash
# 指向本机 SDK（local.properties，未纳入版本控制）
echo "sdk.dir=/path/to/android-sdk" > local.properties

./gradlew :app:assembleDebug        # 产出 app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:testDebugUnitTest    # 单元测试
./gradlew :app:lintDebug            # 静态检查
```

安装到设备：

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## 使用

1. 打开应用 → **设置** → 选择数据源（`Root` 或 `Shizuku`）。
   - Shizuku 需先安装并以 **root** 启动，首次会请求授权（ADB 启动的 Shizuku 权限不足，读不了其他 App 数据）。
2. 点 **提取数据**（首次会写入本地库；之后增量更新）。
3. 返回 **今天 / 视图** 查看数据；无数据时会弹出提示。
4. **设置 → 导出数据（JSON）** 保存 `normalized.json`。

## 项目结构

```
app/src/main/java/com/jhon/mihealthreader/
├─ root/       特权通道：PrivilegedShell + LibsuRootShell / ShizukuShell（AIDL UserService）
├─ discover/   目标包与数据库定位（find -type f）
├─ extract/    复制源库生成快照（SourceReader）
├─ dump/       通用 SQLite→JSON + manifest（SqliteSupport 处理 -shm）
├─ parse/      指标解析：Sleep/Activity/HeartRate/Spo2/Workout/BodyComposition + Normalizer
├─ local/      Room 实体/DAO/LocalStore（增量入库）+ 数据源偏好
├─ data/       图表聚合（ChartData）、睡眠摘要（SleepSummary）、导出（ExportManager）
└─ ui/         Compose：nav（今天/视图/设置）、screens、charts（自绘 Canvas）、theme（Monet）
```

## 文档

- 设计与实现计划：`docs/superpowers/specs/`、`docs/superpowers/plans/`
- 真机验证记录：`docs/manual-verification.md`

## 免责声明

仅供个人数据访问与记录，不用于诊断或治疗。请妥善保管导出的健康数据。仅可用于你有权访问的账户与设备。
