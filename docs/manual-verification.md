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
