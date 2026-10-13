# MihealthReader 设计文档

- 日期：2026-10-08
- 状态：已实现并真机验证（2026-10-09；记录见 `docs/manual-verification.md` §6）
- 路径：方案一（逆向读取小米运动健康 / Mi Fitness 私有数据库）
- 实现记录：见本文 §10（含与设计的差异，如调试浏览器后续被移除）

## 1. 目标与背景

构建一个**个人使用的 Android 应用**（Kotlin + Jetpack Compose），运行在**已 root 的手机**上，
读取「小米运动健康 / Mi Fitness」应用私有目录中的 SQLite 数据库，把数据以**原始快照 + JSON**
两种形式记录到应用内部存储，并在应用内浏览。

目的：把小米运动健康的数据导出给自己分析与长期记录。用户已 root 手机，因此采用应用内 root
提取方案，而非云端 API 逆向。

### 功能范围

1. **读取**：通过 root 复制目标应用数据库，导出为 JSON。
2. **显示**：应用内以"调试浏览器"形式查看数据库表与行数据。

### 成功标准（阶段 1）

- 应用可在 rooted 真机上安装、运行。
- 能检测 root 状态并提示。
- 能自动定位目标应用及其数据库目录，或允许手动指定包名。
- 能把源数据库完整复制为带时间戳的原始快照。
- 能把每个数据库的每张表导出为 JSON，并生成 `manifest.json`。
- 应用内能列出所有表及行数，点击可查看行数据。
- 提取失败（root 被拒、包不存在、数据库加密）时给出明确错误，且不崩溃。

## 2. 技术栈

- 语言：Kotlin
- UI：Jetpack Compose（Material 3）
- 架构：单 Gradle 模块 `:app`，包内按职责分层（后续需要再拆多模块）
- 构建：Gradle Kotlin DSL + `libs.versions.toml` 版本目录；引入 JitPack 仓库
- root：`com.github.topjohnwu.libsu:core`（root shell 封装）
- 签名：使用默认 debug keystore（`signingConfigs.debug`），无需自建签名
- `applicationId`：`com.jhon.mihealthreader`
- SDK：`minSdk 26`，`compileSdk 35`，`targetSdk 35`
- 异步：Kotlin 协程 + `Flow` / `StateFlow`

## 3. 架构与模块划分

单模块，包结构：

```
com.jhon.mihealthreader
├─ ui/            Compose 界面与 ViewModel
├─ root/          RootShell（libsu 封装）
├─ discover/      TargetLocator（包与数据库定位）
├─ extract/       DbExtractor（root 复制生成快照）
├─ dump/          DbDumper（SQLite -> JSON）
└─ data/          ExportRepository（快照/JSON 文件管理）
```

各单元职责：

- **UI（Compose）**
  - 提取页：root 状态、目标应用、开始按钮、进度/日志。
  - 表列表页：数据库与表清单、行数。
  - 表详情页：行数据（键值列表或原始 JSON）。
- **ViewModel**
  - `ExtractionViewModel`：驱动提取流程，暴露 `StateFlow<ExtractionState>`。
  - `BrowserViewModel`：加载快照表清单与行数据。
- **RootShell**（`root/`）：检测 root、执行 `su` 命令、返回 stdout/stderr/退出码。
- **TargetLocator**（`discover/`）：内置候选包名 + 扫描已安装包（名称含 `xiaomi`/`mi`/`health`/`wearable`），
  用 root 校验 `/data/data/<pkg>/databases` 是否存在。
- **DbExtractor**（`extract/`）：创建带时间戳的快照目录，用 root 复制 `.db` 及 `-wal`/`-shm`。
- **DbDumper**（`dump/`）：只读打开快照数据库，遍历 `sqlite_master` 的表，导出行到 JSON。
- **ExportRepository**（`data/`）：管理快照与 JSON 文件，提供列表与读取。

单元之间通过明确接口通信；`RootShell` 与 `TargetLocator` 抽象为接口，便于用假实现测试。

## 4. 数据流

1. 用户点"开始提取"。
2. `RootShell` 检测 root：执行 `su -c id`，要求返回 `uid=0`；被拒则提示并终止。
3. `TargetLocator` 定位目标包与 `databases/` 目录；找不到则展示候选，允许手动输入包名。
4. `DbExtractor` 新建快照目录，用 root 复制该目录下所有数据库文件（含 `-wal`/`-shm`）。
5. `DbDumper` 对快照中每个 `.db`：
   - 校验文件头为 `SQLite format 3\0`，否则标记为"加密/不支持"，仍保留原始快照。
   - 读取 `sqlite_master` 获取表清单。
   - 每表 `SELECT *` 导出行到 JSON；写 `manifest.json`。
6. UI 经 `StateFlow` 刷新表列表；点击表加载行数据。

## 5. 存储布局与 JSON 格式

存储位置：**应用内部私有存储**（`context.filesDir`）：

```
/data/data/com.jhon.mihealthreader/files/exports/
  <时间戳>/
    raw/<源包名>/databases/*.db, -wal, -shm   # 原始快照，100% 保真
    json/<db名>/<表名>.json                    # 每表一个文件
    manifest.json                             # 元数据 + 表清单 + 行数
```

- 保留**原始快照 + JSON** 两套：快照可重导、零解析风险；JSON 用于分析。
- 表 JSON：对象数组，列名 -> 值；数字存数字，`null` 存 `null`，BLOB 转 base64。
- 大表默认全量导出，提供可配置的行数上限。
- `manifest.json` 结构：
  ```json
  {
    "exportedAt": "2026-10-08T22:54:00Z",
    "sourcePackage": "com.xiaomi.wearable",
    "androidVersion": 34,
    "appVersion": "0.1.0",
    "databases": [
      {
        "name": "xxx.db",
        "path": "raw/com.xiaomi.wearable/databases/xxx.db",
        "tables": [
          { "name": "t_sleep", "rowCount": 123, "jsonFile": "json/xxx.db/t_sleep.json" }
        ]
      }
    ]
  }
  ```

### 数据取出方式

内部私有存储无法通过 USB 直接访问。因应用为 **debug 可调试**，可用：

```
adb exec-out run-as com.jhon.mihealthreader tar -c files/exports | tar -x
```

后续可在应用内增加分享/导出功能（阶段 2）。

## 6. 错误处理

- root 被拒：明确提示 + 重试入口。
- 未找到目标包：展示扫描候选，允许手动改包名。
- 数据库非 SQLite 或被加密：校验文件头，不支持时保留原始快照并报"已加密/不支持"。
- 复制失败：展示 su 的 stderr。
- 大表内存压力：IO 线程流式写、可配行数上限。
- 重复触发：单飞，提取期间禁用按钮。

## 7. 测试策略

- `DbDumper`、JSON 序列化：Robolectric + 临时 SQLite 数据库做单元测试。
- `TargetLocator`/`RootShell`：接口化，设备相关部分做手动/集成验证。
- 交付前执行 `./gradlew lint` 与 `./gradlew test`。
- rooted 真机手动验证清单：提取 -> JSON 可打开 -> 行数与源库一致。

## 8. 阶段划分

- **阶段 0**：`git init`、Gradle/Compose 脚手架、debug 签名、root 状态页。
  交付：能装能跑并显示 root 状态。
- **阶段 1（本 spec 目标）**：包/库发现 + 快照 + 通用 JSON 导出 + 调试浏览器。
- **阶段 2（本次不做）**：睡眠/步数定向解析、界面美化、应用内分享导出、定时提取。

## 9. 风险与待确认

- 目标包名（候选：`com.xiaomi.wearable`、`com.mi.health`、`com.xiaomi.hm.health`）与数据库是否加密，
  需真机探针确认；设计上通过运行时发现 + 手动兜底降低风险。
- 手机 Android 版本未知，影响路径与行为。
- 我无法直接访问该 rooted 手机，逆向探针需由用户在设备上执行并反馈结果。

## 10. 实现记录（与设计的差异）

阶段 1 已于 2026-10-09 在真机完成（`docs/manual-verification.md` §6）：

- **待确认项已解决**：目标包为 **`com.mi.health`**（非 `com.xiaomi.wearable`），数据库为**明文 SQLite 未加密**；
  真机为 OnePlus 15 / **Android 16（SDK 36）**、root 为 KernelSU。
- **关键实现差异**：root shell 必须运行在**全局挂载命名空间**，否则看不到其他 App 数据目录；
  已用 libsu `FLAG_MOUNT_MASTER`（等价 `su --mount-master`）解决（`87a4071`）。
- 真实健康数据位于嵌套目录 `databases/<账号UID>/cn/{fitness_data,fitness_summary}`，发现逻辑需递归
  （`d240c63`）。
- **快照 `-shm` 处理**：抽取时不复制过时 `-shm`，打开副本前删除旧 `-shm`，打开后再清理 SQLite 自动生成的
  `-shm`，避免 SIGBUS（`5d5c790`、`491f815`）。
- **调试浏览器后续被移除**：`BrowserViewModel`/`TableListScreen`/`TableDetailScreen`/`ExportRepository`
  在后续 UI 阶段删除（`493b7b6`）；快照与 `normalized.json` 仍在生成，只是不再有表浏览器入口。
- **特权通道抽象**：`RootShell` 后续重构为 `PrivilegedShell`，新增 Shizuku 后端（见
  `2026-10-09-mihealthreader-shizuku-design.md`）。
