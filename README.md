# HyperStatus 0.3.0

一个针对 HyperOS 4 环境的极简状态栏调校工具：独立 APK + Root。

## 当前功能

- Notch Bar Killer：按 Iconify 对应资源方案处理 framework Cutout 预留区域
- 状态栏高度：12–160 dp
- 左侧状态栏间距：0–120 dp
- 右侧状态栏间距：0–120 dp
- 只读兼容性检测：ROM、SystemUI APK、资源、Overlay、V3 Runtime 引用
- 一键恢复：只停用/卸载 HyperStatus 自己的两个 Overlay
- 日志可选中、长按复制，并提供“复制全部”

## UI

使用 Miuix Android 组件构建，采用 HyperOS 风格的 Scaffold / TopAppBar / Card / Preference / SliderPreference。Miuix 官方文档定位为遵循 Xiaomi HyperOS 视觉与交互设计的 Compose UI 库。

## 安全边界

运行时只允许两个 target package：

- `android`
- `com.android.systemui`

明确禁止：

- LSPosed / Xposed Hook
- 修改或删除 `/system_ext/framework/hyperos.rustruntime.*`
- 修改 V3 Runtime 权限/XML 注册
- 任意第三方目标包
- 未通过资源存在性检查的 Overlay

构建流程先生成并验证全部 Overlay，再安装/启用；任何一步失败都会停止后续启用，并自动停用本工具自己的 Overlay。

这仍然不能保证所有 OEM/移植 ROM 都完全兼容，因为资源类型和 Overlay 策略可能不同。因此建议第一次只启用一个项目，并保留“恢复默认”。

## AAPT2 修复

此前的运行时错误：

`FileNotFoundException: Tools/aapt2-arm64-v8a`

本版本不再从 `assets/Tools/...` 读取 AAPT2，而是把 arm64-v8a AAPT2 放入 `res/raw/aapt2_arm64_v8a`，运行时通过 Android Resources API 提取，然后复制到 root 临时目录并 `chmod 755`。

设备 ABI 会在运行时检查。当前内置版本仅针对 `arm64-v8a`。

```

构建产物：`HyperStatus-debug-v0.3.0`
