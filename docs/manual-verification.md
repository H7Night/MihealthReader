# MihealthReader 真机验证清单（阶段 1）

前置条件：

- 一台已 root（Magisk）的 Android 手机，Android 8.0+（minSdk 26）。
- 手机上已安装并同步过「小米运动健康 / Mi Fitness」。
- 电脑已安装 `adb`（platform-tools）。

## 1. 构建与安装

```bash
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## 2. 应用内验证

1. 打开 MihealthReader，确认首页显示 "root 状态"。
2. 首次会弹出 Magisk 的 root 授权请求，选择"允许"。
3. 点"开始提取"。观察状态依次经过：检测 root → 定位数据库 → 复制快照 → 导出 JSON。
4. 成功时显示"完成：共 N 张表"。若为 0 或报错，记录错误文本。
5. 点"浏览数据"，应看到表清单（表名 / 所属 db / 行数）。
6. 点任意表，应看到该表的行（JSON 形式）。

## 3. 取出并校验 JSON（应用为 debug 可调试）

```bash
adb exec-out run-as com.jhon.mihealthreader tar -c files/exports | tar -x
```

- 校验 `exports/<时间戳>/raw/<包名>/databases/*.db` 存在且可被 SQLite 打开。
- 校验 `exports/<时间戳>/json/<db>/<表>.json` 与 `manifest.json` 存在。
- 用源库对照：`manifest.json` 中的 `rowCount` 应与源库对应表一致。

## 4. 需要回填到 spec 第 9 节的信息

- 实际命中的目标包名（例如 `com.xiaomi.wearable` / `com.mi.health` / `com.xiaomi.hm.health`）。
- 数据库是否为明文 SQLite（`isSqlite=true`）还是被加密（`isSqlite=false` 且 `error=unsupported/encrypted`）。
- 睡眠 / 步数对应的表名与列结构，供阶段 2 定向解析使用。
- 手机 Android 版本。

## 5. 阶段 1 通过标准

- 应用能装、能跑、能显示 root 状态。
- 能在应用内完成一次提取并浏览到数据表与行。
- `manifest.json` 的行数与源库一致（至少睡眠/步数相关表）。
- 失败路径（root 被拒 / 未找到包 / 加密库）都有明确提示且不崩溃。

## 6. 实际结果（2026-10-09，阶段 1 通过）

- 设备：OnePlus 15（CPH2747），**Android 16（SDK 36）**，root 为 **KernelSU**。
- 目标包：**`com.mi.health`**（小米运动健康），数据库为**明文 SQLite，未加密**。
- 真实健康数据在嵌套目录：`databases/2476836171/cn/{fitness_data, fitness_summary}`（含 `-wal`/`-shm`）。
- 关键结论：**必须让 root shell 运行在全局挂载命名空间**，否则 App 命名空间看不到其他 App 数据目录
  （命令 `exit=1` 且无输出）。已通过 libsu `FLAG_MOUNT_MASTER`（等价 `su --mount-master`）解决。
- 提取结果：**共 112 张表**。其中 `fitness_data`（65 表）含：
  - `sleep_segment` 96 行（睡眠阶段、时长、心率）
  - `step_record` 6334 行（步数/距离/卡路里）
  - `hr_record` 7266 行、`calorie_record` 12305 行
  `fitness_summary` 含 `daily_report` 358 行等。
- 导出产物：`/data/user/0/com.jhon.mihealthreader/files/exports/2026-10-09T11:41:11.569595/`
  （`raw/` 原始快照约 40MB、`json/` 每表一个文件、`manifest.json`）。
- 浏览功能验证：表格列表显示 112 张表及行数；点表后详情页正确渲染行 JSON。

### 待阶段 2 使用

- 睡眠：`fitness_data.sleep_segment`（`value` 字段内嵌 JSON，含 `items[].state`/`start_time`/`end_time`、`sleep_deep_duration` 等）。
- 步数：`fitness_data.step_record`（`value` 内嵌 `{time,steps,distance,calories}`）。
- 日汇总：`fitness_summary.daily_report`。

## 7. 阶段 2 实际结果（2026-10-09，通过）

- 规范化输出：`<快照>/parsed/normalized.json`（信封 `schema_version=1.0`、`source=mihealthreader`）。
- 结果量级：**睡眠 97 段**、**活动 65 天**（总步数 345,796）。
- 睡眠阶段码经真机 + HCUnlock 交叉校准：`1=awake`、`2=deep(深睡)`、`3=light(浅睡)`、`4=rem`、`5=awake`；
  与源库自带 `sleep_deep_duration`/`sleep_light_duration` 分钟数完全一致（例：某会话深睡 82m、浅睡 297m，逐段求和吻合）。
- 步数按本地日聚合，并做分钟切片去重（同分钟取较大 `steps`）。
- **`-shm` 处理**：抽取时不复制 `-shm`，打开副本前先删除过时 `-shm`（避免 SQLite 信任过期 wal-index 导致 SIGBUS），
  打开后再清理 SQLite 自动生成的 `-shm`；真机确认 raw 快照只保留 `db + -wal`。
- 界面：活动视图（65 天列表 + 最近一天汇总）、睡眠视图（97 段 + 阶段时间线，标签 清醒/浅睡/深睡）均真机渲染正常。

## 8. 阶段 3 实际结果（2026-10-09，通过）

- 本地库：Room `mihealth.db`，表 `step_sample`/`sleep_session`/`sleep_stage`/`sync_state`。
- 提取一次后入库：首次提取报告"新增 7114 条记录"，摘要显示"上次同步 · 步数样本 6414 · 睡眠 97 段"。
- **启动即读**：杀进程重启后，首页直接显示上次同步时间与计数，活动/睡眠视图**无需再次提取**即可打开（读本地库）。
- **增量幂等**：再次提取同一数据 → 显示"无新数据"，计数不变。
- 修复：活动/睡眠视图改经 Room `Flow` 读取，避免主线程访问数据库崩溃。

## 9. Phase 3（更多指标）实际结果（2026-10-09，通过）

- 新增指标：**心率、静息心率、血氧、运动记录、体重/体成分**（不含 VO2max）。
- 表结构：点指标统一 `metric_sample(metricType,sourceId,time,value)`；复合 `workout`、`body_measurement`。DB v2（破坏式迁移）。
- 真机结果（提取一次后）：心率 16266 条（静息心率 54 bpm）、运动 60 次（含类型/时长/热量/平均心率）、体重/体成分 1 条、血氧 2 条。
- 各指标界面（心率/血氧/运动/体重）均正常渲染；首页导航新增对应入口。
- **增量幂等**：连续两次提取（无新同步）第二次显示"无新数据"，计数不变；期间 App 有新同步时正确报告"新增 N 条"。
- 66 个单元测试全绿，`lintDebug`/`assembleDebug` 通过。

## 10. Shizuku 数据层实际结果（2026-10-09，通过）

- 特权通道抽象为 `PrivilegedShell`（`run`/`openFile`/`isGranted`），实现 `LibsuRootShell` 与 `ShizukuShell`。
- Shizuku 通过 **UserService（AIDL）** 在 Shizuku 进程执行命令与 `openFile`（`ParcelFileDescriptor`，避免大文件 binder 限制）。
- 数据源可在首页临时切换 root/Shizuku（正式设置页后续做）；切换后重启 App 生效。
- 真机（`shizuku_server` 以 root 运行）：Shizuku 授权后 **提取成功**（112 表，新增 9536 条），再提取"无新数据"；root 后端同样正常。
- `isGranted()` 要求实际 uid=0，可识别"Shizuku 非 root 启动"。
- 67 个单元测试全绿，`lintDebug`/`assembleDebug` 通过。

## 11. UI 重构实际结果（2026-10-09，通过）

- 底部 NavigationBar：**今天 / 视图 / 设置**。
- **今天**：卡片式概览（步数/睡眠/心率/运动/血氧/体重），空库时提示"去设置提取"。
- **视图**：指标 Chips（步数/睡眠/心率/血氧/运动/体重）+ **自绘 Canvas 图表**（柱状/堆叠柱状/折线），按日聚合近 30 天。
- **设置**：数据源（Root/Shizuku）、提取数据、**导出规范化 JSON（SAF）**、关于。
- 真机导出：`导出数据（JSON）` → 系统保存对话框 → `/sdcard/Download/mihealth-normalized.json`（2.09MB），提示"已导出到所选位置"。
- 旧列表视图（活动/睡眠/心率/血氧/运动/体重）已移除，改由图表呈现。
- 75 个单元测试全绿，`lintDebug`/`assembleDebug` 通过。

## 12. 平面排版重排（参考 Google Health，2026-10-09，通过）

- 参考 Google Health 截图，采用**平面排版**：卡片浅灰底（`surfaceVariant`）、24dp 圆角、**无阴影无分割线**。
- **今天**：`主要指標` 式 **2 列卡片网格**，每卡 = 小标签 → 大数值+单位 → **近 7 天迷你图** → 白色状态胶囊。
  - 迷你图：步数/睡眠/运动用**圆角胶囊柱**；心率/血氧/体重用**点线图**（新增 `CapsuleBars`/`DottedLine`）。
- **视图**：指标 Chips + 图表放入平面卡片。
- **设置**：去分割线，分区标题 + 平面按钮。
- 底部导航选中项有胶囊高亮（Material 3）。
- 真机截图对比：今天/视图/设置三页与 Google Health 平面风格一致。

## 13. 取色 / 详情 / 坐标轴 / 时间切换（2026-10-09，通过）

- **莫奈动态取色**：Android 12+ 使用 `dynamicLight/DarkColorScheme`（真机显示为壁纸取色的蓝色系），低版本回退青绿静态配色。
- **今天卡片可点击**：点击进入对应指标**详情页**（真机点步数卡 → 步数详情）。
- **视图坐标轴**：纵轴显示数值（如 9235 / 56.0），横轴显示日期标签（06-06 / 10-05 / 10-09）。
- **时间视图**：图表**只按月份显示**，默认**本月**，用 `‹ 2026 年 10 月 ›` 切换月份（已移除"最近10次"选项）；
  月份选择**居中**；**若本月无数据则回退显示最近 10 次**（标题注明"本月无数据，显示最近10次"）。
- 77 个单元测试全绿，`lintDebug`/`assembleDebug` 通过。

## 14. 启动动画与调试页移除（2026-10-09）

- **禁用导航跳转动画**：NavHost 进入/退出/回退过渡均设为 `None`，打开程序直接进入主界面。
- **移除原调试页**：删除"浏览原始表（调试）"入口及其相关代码（`TableListScreen`/`TableDetailScreen`/`BrowserViewModel`/`ExportRepository`）。
- 74 个单元测试全绿，`lintDebug`/`assembleDebug` 通过。

## 17. 启动"跳转页面"修复（2026-10-09，通过）

- 现象：冷启动时先出现一页（默认安卓机器人图标）再进入主界面。
- 根因（证据：冷启动帧截图）：Android 12+ **系统 Splash 屏**默认使用 App 图标；本应用未设置 `android:icon`、主题也无 splash 配置，故显示默认机器人图标。
- 修复：`values-v31/themes.xml` 设置 `windowSplashScreenBackground` = 应用背景色、`windowSplashScreenAnimatedIcon` = 透明；`windowBackground` = 应用背景色。
- 验证：冷启动帧现为**纯背景（无图标）**，随后无缝进入"今天"。

## 18. 空数据提示改为弹窗（2026-10-09，通过）

- 去掉"暂无数据"整页替换；打开 App **始终进入主页卡片网格**，无数据时卡片显示"—"。
- 无数据时弹出 **AlertDialog**："暂无数据 / 请先在设置中提取「小米运动健康」数据"，按钮 `取消` / `去设置`。
- VM 新增 `dataLoaded` 标记，避免加载完成前误弹。
- 真机（root 清空数据后）：主页空白网格 + 弹窗正常。

## 19. 运动界面优化（参考小米运动健康 + MD3，2026-10-09，通过）

- 新增 `WorkoutListScreen`（按会话：类型彩色点 + 日期/时长/卡路里/平均心率）与 `WorkoutDetailScreen`。
- `WorkoutDetailScreen`（参考小米运动详情）：
  - **彩色头部**（按运动类型取色，圆角底）+ 运动名 + 日期时间。
  - **运动汇总**：大卡路里 + 运动时长 / 平均心率 / 最大心率（有距离则显示）。
  - **心率 (BPM)**：平均/最大 + **运动期间心率折线图**（用 `hr_record` 在 `[start,end]` 的样本，带坐标轴）。
- 入口：今天"运动"卡 → 运动列表 → 点选进详情；视图"运动"加"查看运动列表"按钮。
- 真机确认：力量训练详情 `498 kcal / 1h38m / 平均106 / 最大148` + 心率曲线（12:35→14:04）。

## 20. 午睡覆盖主睡眠修复（2026-10-09，通过）

- 现象：午睡后，今天页睡眠卡/睡眠详情显示的是午睡（较短），把主睡眠覆盖了。
- 根因：今天页用 `sleep.firstOrNull()`、详情用 `index=0`，而会话按 `start` 倒序 → 午睡（start 更晚）排最前。
- 修复：`SleepSummary.mainSleep`（夜间最长者为主睡眠）+ `sameDayNaps`/`napsOnDay`（同唤醒日的午睡作为补充）。
  - 今天页睡眠卡：显示**当日总睡眠（主睡眠 + 午睡）**，状态显示"深睡 Xm"或"含午睡 Xm"。
  - 睡眠详情：默认打开**主睡眠**；若有午睡，在睡眠分析中显示"含午睡 X 分钟（N 次）"。
- 真机确认：今天页与详情均显示主睡眠 `2026-10-09 / 9 小时 13 分钟`。
- 82 个单元测试全绿。

## 21. 全天睡眠含午睡 + 数据差异排查（2026-10-10，通过）

- **数据差异排查**：拉取本地库快照（10-10 19:23）发现睡眠数据与小米**完全一致**：10-10 主睡眠 `00:51→08:54`(483m) + 午睡 `13:22→16:13`(171m) = **654m = 10h54m**。
  差异根因是**显示逻辑**：`mainSleep` 取的是全局最长夜间睡眠（10-09 的 9h13m），而非"最新一天"的。
- **修复**：`mainSleep` 改为**最新一天**的主睡眠；`daySessions` 汇总当日会话。
  - 今天页睡眠卡：当日总睡眠（主睡眠 + 午睡），如 `10h 54m · 含午睡 171m`（与小米一致）。
  - 全天睡眠：显示当日总时长；hypnogram 在同一时间轴画出**主睡眠 + 午睡**（午睡用独立色块）；**时间标记按会话分组、时间在上/状态在下**（`00:51 入睡 / 08:54 清醒 / 13:22 入睡 / 16:13 清醒`），不再重叠。
- 真机确认：全天睡眠 `10 小时 54 分钟`，主睡眠+午睡块 + 4 个时间标记。

## 15. 视图图表视觉优化（参考小米运动健康 + MD3，2026-10-09，通过）

- **头部大数值**：区间代表值（最新）+ 单位。
- **折线类（心率/血氧/体重）**：改为**平滑面积折线**（曲线 + 底部渐变填充，MD3 主题色）。
- **柱状类（步数/运动）**：**圆角柱**。
- **睡眠**：堆叠柱（深/浅/清醒）。
- **汇总行**：最小 / 平均 / 最大。
- 坐标轴（纵轴 min/max、横轴日期）、居中月份选择保留。
- 真机确认：心率视图显示 `61.2 bpm` + 蓝色平滑面积折线 + `最小/平均/最大` + `2026 年 10 月`。

## 16. 睡眠详情页（参考小米运动健康 + MD3，2026-10-09，通过）

- 新增 `SleepDetailScreen`（今天页"睡眠"卡 / 视图页"查看每晚睡眠详情"进入），卡片顺序：
  **睡眠分析 → 睡眠解读（整合原"睡眠详情"与"解读"）→ 全天睡眠 → 入睡时间规律性**。
  - **每晚切换** `‹ 2026-10-09 ›`（居中）。
  - **睡眠分析**：睡眠总时长 / 入睡时间 / 深度睡眠比率 / 清醒次数（2×2）。
  - **文字描述**：由 `SleepSummary.describe` 生成（含质量评价、时长、入睡/起床时间、深睡占比）。
  - **全天睡眠**：大时长 + 图例（深睡/浅睡/清醒）+ **横向 hypnogram**（`Hypnogram` 组件：按阶段连续填充 + 虚线网格 + 时间轴）。
  - **入睡时间规律性**：近 14 晚夜间入睡的平均时间、波动（标准差分钟）与评价（规律/较规律/不规律）+ 点线图；**只统计夜间入睡（18:00–06:00），排除白天小睡**；点线图带**坐标轴**（纵轴=入睡时刻，横轴=日期）。
- 颜色用 MD3 主题（深睡=primary、浅睡=tertiary、清醒=error）。
- 真机确认：`9 小时 13 分钟 / 00:17 入睡 / 深睡 23% / 清醒 3 次`，描述与横向睡眠图均正常。
- 78 个单元测试全绿，`lintDebug`/`assembleDebug` 通过。

## 22. 安全 Room 迁移（2026-10-12，通过）

- 背景：DB v2 原先用 `fallbackToDestructiveMigration()`——schema 不匹配即**清空本地库**、需重新提取。
- 变更（`11c679b`）：改为**增量迁移** `MIGRATION_1_2`（`local/Migrations.kt`，仅 `CREATE TABLE`
  `metric_sample`/`workout`/`body_measurement`），`MiHealthDatabase.build` 注册 `addMigrations(*ALL_MIGRATIONS)`，
  **不再注册破坏式回退**；缺少迁移时必须显式失败，绝不静默丢弃用户的健康历史。
- 迁移基线：`exportSchema=true`，schema 导出到
  `app/schemas/com.jhon.mihealthreader.local.MiHealthDatabase/2.json`（纳入版本控制）。
- 迁移 SQL 以常量暴露，`Migration1To2Test`（Robolectric）验证：
  - v1 数据（`step_sample`/`sleep_session`/`sync_state`）迁移后**保留**，三张新表可正常写入；
  - **缺少迁移时打开 v1 库抛 `IllegalStateException`，而非清库**。
- 交付：运行 `./gradlew :app:testDebugUnitTest`（含 `Migration1To2Test`）。

## 23. 复测（2026-10-13，通过）

对当前实现做一次完整真机复测（OnePlus 15 / Android 16 / KernelSU）：

- 构建：`./gradlew :app:testDebugUnitTest` **84 项全绿**，`lintDebug`/`assembleDebug` 通过。
- **增量提取（Shizuku）**：新增 480 条（步数样本 17523→17593、睡眠 332→334）。
- **幂等**：再次提取显示「无新数据」，计数不变。
- **Root 数据源**：重启后生效，提取同样「无新数据」，与 Shizuku 后端结果一致。
- **导出 JSON（SAF）**：写出 `/sdcard/Download/mihealth-normalized.json`（2.18MB），内容校验通过
  （`schema_version=1.0`；sleep 334、daily_activity 216、heart_rate 25331、resting 132、spo2 2、workouts 62、body_measurements 1）。
- 界面：今天（启动即读）、视图（按月份图表）、设置、睡眠详情、心率详情 均正常渲染。
