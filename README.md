# 蓝牙键鼠遥控 · btkeys-remote

把 Android 手机变成电脑的**无线键盘 + 鼠标 + 遥控器**。电脑端不需要安装任何软件 —— 手机通过系统自带的
Bluetooth HID Device 协议把自己注册成标准蓝牙键盘，配对后即插即用。

**无广告 · 无会员 · 免登录 · 无需联网**，安装包只有 **57 KB**。

```
minSdk 28 (Android 9)   targetSdk 35   纯 Java + 系统 API   零第三方依赖
```

---

## 功能

| 模块 | 能力 |
|---|---|
| 设备连接 | 已配对设备列表、附近设备搜索、配对 / 连接 / 断开、连接状态实时显示 |
| 触控板 | 单指滑动移动光标、单击左键、双指滚动、长按拖动、三指中键、双击、光标颜色、灵敏度 |
| 键盘 | 完整 QWERTY、Shift/Ctrl/Alt/Win 修饰键、退格长按连发、常用快捷键、F1–F12 与方向键扩展面板 |
| 数字键盘 | 数字、`+ - * /`、退格、Tab、Esc |
| 翻页笔 | 大按钮上下翻页、F5 放映、Esc 退出、B 黑屏、方向键 |
| 媒体遥控 | 播放/暂停、上一首/下一首、停止、音量增减、静音、主页、返回、亮度、全屏 |
| 自定义按键 | 按键自由拖动摆位、改名称 / 改动作 / 改大小 / 复制 / 删除，多套档案可切换 |
| 设置 | 振动强度、触控板光标颜色、屏幕常亮、恢复默认档案 |

自定义按键内置三套默认档案：**办公**（复制粘贴、撤销、锁屏、截图…）、**游戏**（WASD、跳、Shift…）、
**影音**（播放、音量、全屏…），也可以自建任意套。

---

## 安装

下载 [btkeys-v1.0.0.apk](btkeys-v1.0.0.apk) 传到手机安装（首次需允许「安装未知应用」）。

> APK 使用本仓库构建时生成的调试证书签名。如果你自己重新构建，签名会不同，
> 覆盖安装前需要先卸载旧版本。

## 使用

1. 手机打开蓝牙；电脑打开蓝牙 → 添加设备。
2. 电脑搜索到手机后配对（电脑会把它识别为一个**蓝牙键盘**，部分系统还会识别出鼠标）。
3. 打开 App → 在「已配对设备」里点 **连接**。
4. 进入各功能页即可遥控电脑。

> 中文输入依赖**电脑端输入法**，本应用只发送按键码，行为与真实蓝牙键盘完全一致。

---

## 技术实现

### 自己是 HID 设备

`Hid.java` 通过 `BluetoothHidDevice`（API 28+）把手机注册成一个复合 HID 设备，自定义的 Report
Descriptor 同时提供三种报告：

| Report ID | 类型 | 长度 | 内容 |
|---|---|---|---|
| 1 | 键盘 | 8 字节 | 修饰键位图 + 保留字节 + 6 个按键码 |
| 2 | 相对位移鼠标 | 4 字节 | 按键位图 + dx + dy + 滚轮 |
| 3 | 多媒体键 | 2 字节 | 16 位 Consumer Usage（小端） |

关键时序（顺序错了就连不上）：

```java
adapter.getProfileProxy(ctx, listener, BluetoothProfile.HID_DEVICE);
// onServiceConnected → proxy = (BluetoothHidDevice) p
proxy.registerApp(sdp, null, null, ctx.getMainExecutor(), callback);
// onAppStatusChanged(dev, registered=true) → 此时才允许 connect()
proxy.connect(target);
```

- 注册失败或代理迟迟不来时，会把待连接设备**暂存**，注册成功后自动续连；
- 未配对的设备先 `createBond()`，监听 `ACTION_BOND_STATE_CHANGED` 再连；
- 离开页面、长按手势结束时强制释放按键与鼠标按钮，避免电脑端「按键卡住」。

### 为什么只有 57 KB

界面全部用代码构建（`Ui.java`），不依赖 AppCompat / Material / Kotlin 运行时，
没有任何广告或统计 SDK。APK 里只有 8 个条目：清单、资源表、`classes.dex`、图标和签名文件。

---

## 自己编译

不需要 Gradle。需要 JDK 17、Android build-tools（`aapt2`/`d8`/`zipalign`/`apksigner`）
和任意一个平台的 `android.jar`。脚本会依次从 `$env:BTKEYS_TOOLCHAIN`、`$env:ANDROID_HOME`、
`$env:ANDROID_SDK_ROOT`、`..\toolchain` 里查找。

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File build.ps1
```

流程：`aapt2 compile` → `aapt2 link` → `javac` → `jar` → `d8` → 塞入 `classes.dex`
→ `zipalign` → `apksigner`（v1+v2+v3）→ 输出 `btkeys-v1.0.0.apk`。

`debug.keystore` 首次运行时自动生成，**不纳入版本控制**。

---

## 目录结构

```
AndroidManifest.xml          清单（权限、8 个 Activity）
build.ps1                    无 Gradle 构建脚本
palette.json                 配色表（用于 WCAG 对比度自检）
res/                         图标与字符串
src/com/dsh/btkeys/
  Hid.java                   BluetoothHidDevice 引擎 + Report Descriptor
  Usages.java                HID 键码常量
  Prefs.java                 设置 + 按键档案持久化
  Ui.java                    代码化 UI 工具集（配色、圆角、按钮）
  BaseActivity.java          页面骨架（返回栏 + 连接状态胶囊）
  MainActivity.java          首页：连接、设备列表、功能入口
  TouchpadActivity.java      触控板（手势解析）
  KeyboardActivity.java      键盘
  NumpadActivity.java        数字键盘
  PptActivity.java           翻页笔
  MediaActivity.java         媒体遥控
  CustomKeysActivity.java    自定义按键（拖动、档案管理）
  SettingsActivity.java      设置
  Haptics.java               振动反馈
```

## 已知限制

- 只支持 **Android 9+**，且手机必须支持蓝牙 HID Device 角色（绝大多数机型支持，少数定制 ROM 不支持，
  App 会在启动 6 秒后给出提示）。
- 未在真实设备上做过完整回归测试：本项目是在无模拟器环境下开发的，UI 与 HID 报文经过静态校验。
- 电脑端识别为键盘还是键鼠复合设备，取决于电脑的蓝牙栈，Windows / macOS / Linux 均无需驱动。

## 许可

[MIT](LICENSE)
