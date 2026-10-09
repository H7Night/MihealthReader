# MihealthReader Shizuku 数据层设计

- 日期：2026-10-09
- 状态：待实现计划
- 前置：本地存储与指标解析已完成

## 1. 目标与范围

让数据获取支持 **root 或 Shizuku 二选一**。把现有仅 root（libsu）的特权通道抽象为可切换后端。

**关键前提**：读 `fitness_data` 需要文件系统 root 权限。Shizuku 仅在**以 root 启动**时具备该权限
（ADB 启动的 Shizuku 为 shell uid=2000，读不了其他 App 的 `/data/data`）。本机 `shizuku_server` 已以 root 运行。

## 2. 特权通道抽象

`root/PrivilegedShell.kt`（替换 `RootShell`）：

```kotlin
data class ShellOutput(exitCode, stdout: List<String>, stderr: List<String>)
interface PrivilegedShell {
    fun run(vararg commands: String): ShellOutput
    fun openFile(path: String): InputStream   // 二进制安全读取
    fun isGranted(): Boolean                   // 且实际具备 root 权限
}
fun outputIndicatesRoot(output: ShellOutput): Boolean
fun buildShellCommand(commands: Array<out String>): String
```

实现：

- `LibsuRootShell`：`run` 用 `Shell.cmd(buildShellCommand(...))`；`openFile` = `SuFileInputStream.open`；
  `isGranted` = `outputIndicatesRoot(run("id"))`。
- `ShizukuShell`：`run` 用 `Shizuku.newProcess(["sh","-c",cmd], null, null)`（stdout/stderr 并发读取避免死锁）；
  `openFile` = `Shizuku.newProcess(["cat", path], null, null).inputStream`；
  `isGranted` = `Shizuku.pingBinder()` + 权限已授 + `outputIndicatesRoot(run("id"))`（识别"非 root 启动"）。

## 3. 依赖与 Manifest

- `dev.rikka.shizuku:api:13.1.5`、`dev.rikka.shizuku:provider:13.1.5`。
- Manifest：
  - `<uses-permission android:name="moe.shizuku.manager.permission.API_V23" />`
  - `<provider android:name="rikka.shizuku.ShizukuProvider" android:authorities="${applicationId}.shizuku" ... />`

## 4. 后端选择

- `local/BackendPreferences(context)`：`backend` 存 `root|shizuku`（SharedPreferences）。
- `root/BackendManager(context)`：`current(): PrivilegedShell`（按偏好返回 `LibsuRootShell` 或 `ShizukuShell`）。
- `ShizukuPermission`：`isGranted()` / `request()`（注册 `OnRequestPermissionResultListener`）。
- `MainActivity` 启动时按偏好构建后端并注入；选 Shizuku 时若未授权则发起授权请求。

## 5. 影响面

- `TargetLocator(rootShell)` → `PrivilegedShell`；`DbExtractor` 的 `SourceReader` 由 `backend.openFile` 提供
  （删除 `SuSourceReader`）。
- `ExtractionViewModel`：`rootShell.isRootGranted()` → `shell.isGranted()`。
- 首页加**临时"数据源"选择**（root/Shizuku）+ Shizuku 授权入口（正式设置页留到后续 UI 阶段）。

## 6. 错误处理

- Shizuku 未安装/未运行/未授权 → `isGranted()=false`，UI 提示并引导授权。
- Shizuku 非 root 启动 → `run("id")` 非 uid=0 → `isGranted()=false`，提示"Shizuku 需以 root 启动"。
- 切换后端后需重启 App 生效（临时方案）。

## 7. 测试策略

- 纯函数：`buildShellCommand`、`outputIndicatesRoot`。
- `BackendPreferences` 读写（Robolectric）。
- 既有解析/入库测试改用 `PrivilegedShell` 假实现（`openFile` 返回 `ByteArrayInputStream`）。
- 真机：root 提取成功；切 Shizuku（root）提取成功；均"无新数据"幂等。
- 交付前 `./gradlew :app:testDebugUnitTest` 与 `:app:lintDebug`。

## 8. 任务拆分（每个任务独立提交）

- **S1** `PrivilegedShell` 抽象重构（含 `openFile`、`outputIndicatesRoot`）+ 全量改调用点 + 测试。
- **S2** `ShizukuShell` + 依赖 + Manifest + 权限 helper。
- **S3** `BackendPreferences`/`BackendManager` + 首页临时选择 + 真机双后端验证。

## 9. 后续（本次不做）

导航（今天/视图/设置）、图表（Google Health 风格）、设置页（提取/导出）、正式的数据源设置项。
