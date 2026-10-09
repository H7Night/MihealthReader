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
