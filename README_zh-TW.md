# 唱K · KTV Karaoke App

一個可即用的卡拉OK（唱K）應用，包含兩個版本：

| 版本 | 位置 | 說明 |
|------|------|------|
| 🌐 網頁版 | `index.html`（倉庫根目錄） | 單檔案、手機瀏覽器即開即用，開啟 GitHub Pages 後可直接訪問 |
| 🤖 Android 版 | `android/` | 原生 Kotlin 工程原始碼，用 Android Studio 編譯出 APK |

## 網頁版（建議先玩這個）

開啟 `index.html` 即可使用；部署到 GitHub Pages 後手機瀏覽器直接訪問，無需安裝。

**功能**
- 卡拉OK歌詞：支援標準 LRC 與逐字高亮 LRC（`<00:20.00>你<00:20.50>好`），目前句漸變色逐字掃亮
- 內置示範曲（Web Audio 即時合成伴奏，無版權問題），開箱即唱
- 上傳本地音訊（mp3 / ogg / wav / m4a / flac）
- 匯入 `.lrc` 歌詞檔案或直接貼上歌詞文字
- 變速 0.5×–1.5×、移調 ±6 半音（男女聲切換）
- 麥克風跟唱：瀏覽器 Web Audio 即時混音回放（**請戴耳機**，iOS 需 Safari 開啟且關閉側邊靜音鍵）

**快速部署到 GitHub Pages**
1. 進入倉庫 → Settings → Pages
2. Source 選擇 `Deploy from a branch` → 分支選 `main` → 根目錄（`/`）
3. 儲存後訪問 `https://<你的使用者名稱>.github.io/karaoke-app/`

## Android 版

原生 Kotlin 工程（零第三方依賴），功能與網頁版對齊：

- 系統檔案選擇器選歌曲 / 選 `.lrc` 歌詞 / 貼上歌詞
- 卡拉OK歌詞自訂 View（目前句漸變逐字高亮 + 下一句預覽）
- `MediaPlayer` + `PlaybackParams` 變速（0.5×–1.5×）與移調（±6 半音）
- `AudioRecord` → `AudioTrack` 麥克風迴環跟唱（需麥克風權限，戴耳機使用）

**在 Windows 上編譯（已在你的機器驗證過同款建置鏈）**
- Android Studio 直接開啟 `android/` 資料夾（首次同步會自動下載 Gradle 8.14.2，或複用本機快取）
- 若提示缺少 gradle wrapper 指令碼，在 Android Studio 裡點 Sync 即可自動產生
- 建置配置：AGP 8.10.0 · Kotlin 2.3.21 · compileSdk 36 · minSdk 26 · JDK 17
- 建置 APK：`Build` → `Build Bundle(s)/APK(s)` → `Build APK(s)`
- 產物路徑：`android/app/build/outputs/apk/debug/app-debug.apk`

## 目錄結構

```
karaoke-app/
├── index.html          # 網頁版（單檔案自包含）
├── android/            # Android 原始碼工程
│   └── app/src/main/java/com/example/karaoke/
│       ├── MainActivity.kt        # 播放器 + 介面邏輯
│       ├── LrcParser.kt           # LRC 解析（標準 + 逐字）
│       ├── KaraokeTextView.kt     # 卡拉OK歌詞高亮 View
│       └── MicLoopback.kt         # 麥克風迴環混音
└── README.md
```

## 注意事項

- 請只使用自己有版權的音訊與歌詞檔案，本工具不附帶任何受版權保護的歌曲內容
- 麥克風混音會經由揚聲器外放，務必戴耳機避免嘯叫
- 網頁版移調基於 Web Audio 重新取樣，效果適合跟唱；專業音質要求建議用 Android 版或專業軟體
