# MihealthReader Shizuku 数据层实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans. Steps use checkbox (`- [ ]`).

**Goal:** 把 root 特权通道抽象为可切换的 `PrivilegedShell`，新增 Shizuku 后端，root/Shizuku 二选一。

**Architecture:** `root/` 定义 `PrivilegedShell`（run/openFile/isGranted）+ `LibsuRootShell`/`ShizukuShell`；`BackendPreferences`/`BackendManager` 选择；调用点（TargetLocator/DbExtractor/VM/MainActivity）改用抽象。

**Tech Stack:** Kotlin、libsu、Shizuku 13.1.5、JUnit4 + Robolectric。

**Spec:** `docs/superpowers/specs/2026-10-09-mihealthreader-shizuku-design.md`

## Global Constraints

- `PrivilegedShell.isGranted()` 必须同时满足"通道可用"与"实际 uid=0"。
- Shizuku 依赖 `dev.rikka.shizuku:api/provider:13.1.5`。
- 切换后端需重启 App 生效（临时）。
- 交付前通过 `./gradlew :app:testDebugUnitTest` 与 `:app:lintDebug`。
- 每个任务独立提交。

## Review Focus

1. Shizuku 非 root 启动 → `isGranted()=false`。测试 S2（用假输出）。
2. 未安装/未授权 Shizuku → 不崩溃，提示授权。测试 S2/S3。
3. `openFile` 二进制安全（不按文本处理）。测试 S1。
4. 切换后端后调用点仍工作。测试 S3 + 真机。
5. 既有解析/入库在抽象重构后不受影响。测试 S1。

---

### Task S1: PrivilegedShell 抽象重构

**Files:**
- Modify: `root/RootShell.kt` → `root/PrivilegedShell.kt`, `root/LibsuRootShell.kt`, `discover/TargetLocator.kt`, `extract/DbExtractor.kt`, `extract/SuSourceReader.kt`(删), `ui/ExtractionViewModel.kt`, `MainActivity.kt`
- Test: `root/RootShellTest.kt`→`root/PrivilegedShellTest.kt`, `discover/TargetLocatorTest.kt`, `extract/DbExtractorTest.kt`, `ui/ExtractionViewModelTest.kt`

**Interfaces:**
- Produces: `interface PrivilegedShell { run(vararg): ShellOutput; openFile(path): InputStream; isGranted(): Boolean }`；`outputIndicatesRoot(ShellOutput)`；`buildShellCommand(...)`；`LibsuRootShell : PrivilegedShell`。

- [ ] **Step 1: 改测试**：`PrivilegedShellTest` 测 `buildShellCommand`/`outputIndicatesRoot`；`TargetLocatorTest` 的假 shell 增加 `openFile`；`DbExtractorTest`/`ExtractionViewModelTest` 的假实现同步。
- [ ] **Step 2: 运行确认失败。**
- [ ] **Step 3: 实现抽象 + `LibsuRootShell`（run 用 libsu、openFile 用 SuFileInputStream、isGranted 用 outputIndicatesRoot）；删除 `SuSourceReader`；`DbExtractor` 保留 `SourceReader`，由调用方从 `openFile` 构造；调用点改名。**
- [ ] **Step 4: 运行全部单测通过。**
- [ ] **Step 5: 提交** `refactor: abstract privileged shell`。

---

### Task S2: ShizukuShell + 依赖 + Manifest

**Files:**
- Modify: `gradle/libs.versions.toml`, `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml`
- Create: `root/ShizukuShell.kt`, `root/ShizukuPermission.kt`
- Test: `root/ShizukuCommandTest.kt`（纯逻辑，不依赖 binder）

**Interfaces:**
- Produces: `ShizukuShell : PrivilegedShell`；`ShizukuPermission.isGranted()/request()`。

- [ ] **Step 1: 加依赖**（api/provider 13.1.5）；Manifest 加权限与 `ShizukuProvider`。
- [ ] **Step 2: 写失败测试**：对 `ShizukuShell` 的可测纯逻辑（如 `isGranted` 基于注入的 ping/permission/id 结果）做单测。
- [ ] **Step 3: 实现 `ShizukuShell`（newProcess 执行命令、并发读 stderr、openFile=cat）与 `ShizukuPermission`。**
- [ ] **Step 4: 运行通过 + `assembleDebug`。**
- [ ] **Step 5: 提交** `feat: add shizuku backend`。

---

### Task S3: 后端选择 + 首页临时选择 + 真机验证

**Files:**
- Create: `local/BackendPreferences.kt`, `root/BackendManager.kt`
- Modify: `MainActivity.kt`, `ui/screens/ExtractionScreen.kt`, `ui/AppNav.kt`
- Test: `local/BackendPreferencesTest.kt`

**Interfaces:**
- Produces: `enum BackendType { ROOT, SHIZUKU }`；`BackendPreferences.get/set`；`BackendManager.current()`；首页"数据源"选择 + 授权入口。

- [ ] **Step 1: 写失败测试**（`BackendPreferences` 读写、默认 root）。
- [ ] **Step 2: 运行确认失败。**
- [ ] **Step 3: 实现偏好/管理器；`MainActivity` 按偏好构建后端并注入；首页加数据源切换与 Shizuku 授权。**
- [ ] **Step 4: 构建 + lint + 全部单测通过。**
- [ ] **Step 5: 真机验证**：root 提取成功；切 Shizuku（root 启动）授权后提取成功；再提取"无新数据"。
- [ ] **Step 6: 提交** `feat: selectable root/shizuku backend`。

---

## Self-Review

- **Spec coverage**：抽象→S1；Shizuku→S2；选择→S3；错误处理→S2/S3；测试→各任务。
- **类型一致性**：`PrivilegedShell`/`ShellOutput`/`BackendType` 跨任务一致。
- **Review Focus**：5 项均有测试/真机覆盖。
