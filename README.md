# 唱K · KTV Karaoke App

一个可即用的卡拉 OK（唱K）应用，包含两个版本：

| 版本 | 位置 | 说明 |
|------|------|------|
| 🌐 网页版 | `index.html`（仓库根目录） | 单文件、手机浏览器即开即用，开启 GitHub Pages 后可直接访问 |
| 🤖 Android 版 | `android/` | 原生 Kotlin 工程源码，用 Android Studio 编译出 APK |

## 网页版（推荐先玩这个）

打开 `index.html` 即可使用；部署到 GitHub Pages 后手机浏览器直接访问，无需安装。

**功能**
- 卡拉OK歌词：支持标准 LRC 与逐字高亮 LRC（`<00:20.00>你<00:20.50>好`），当前句渐变色逐字扫亮
- 内置演示曲（Web Audio 实时合成伴奏，无版权问题），开箱即唱
- 上传本地音频（mp3 / ogg / wav / m4a / flac）
- 导入 `.lrc` 歌词文件或直接粘贴歌词文本
- 变速 0.5×–1.5×、移调 ±6 半音（男女声切换）
- 麦克风跟唱：浏览器 Web Audio 实时混音回放（**请戴耳机**，iOS 需 Safari 打开且关闭侧边静音键）

**快速部署到 GitHub Pages**
1. 进入仓库 → Settings → Pages
2. Source 选择 `Deploy from a branch` → 分支选 `main` → 根目录（`/`）
3. 保存后访问 `https://<你的用户名>.github.io/karaoke-app/`

## Android 版

原生 Kotlin 工程（零第三方依赖），功能与网页版对齐：

- 系统文件选择器选歌曲 / 选 `.lrc` 歌词 / 粘贴歌词
- 卡拉OK歌词自定义 View（当前句渐变逐字高亮 + 下一句预览）
- `MediaPlayer` + `PlaybackParams` 变速（0.5×–1.5×）与移调（±6 半音）
- `AudioRecord` → `AudioTrack` 麦克风回环跟唱（需麦克风权限，戴耳机使用）

**在 Windows 上编译（已在你的机器验证过同款构建链）**
- Android Studio 直接打开 `android/` 文件夹（首次同步会自动下载 Gradle 8.14.2，或复用本机缓存）
- 若提示缺少 gradle wrapper 脚本，在 Android Studio 里点 Sync 即可自动生成
- 构建配置：AGP 8.10.0 · Kotlin 2.3.21 · compileSdk 36 · minSdk 26 · JDK 17
- 构建 APK：`Build` → `Build Bundle(s)/APK(s)` → `Build APK(s)`
- 产物路径：`android/app/build/outputs/apk/debug/app-debug.apk`

## 目录结构

```
karaoke-app/
├── index.html          # 网页版（单文件自包含）
├── android/            # Android 源码工程
│   └── app/src/main/java/com/example/karaoke/
│       ├── MainActivity.kt        # 播放器 + 界面逻辑
│       ├── LrcParser.kt           # LRC 解析（标准 + 逐字）
│       ├── KaraokeTextView.kt     # 卡拉OK歌词高亮 View
│       └── MicLoopback.kt         # 麦克风回环混音
└── README.md
```

## 注意事项

- 请只使用自己有版权的音频与歌词文件，本工具不附带任何受版权保护的歌曲内容
- 麦克风混音会经由扬声器外放，务必戴耳机避免啸叫
- 网页版移调基于 Web Audio 重采样，效果适合跟唱；专业音质要求建议用 Android 版或专业软件
