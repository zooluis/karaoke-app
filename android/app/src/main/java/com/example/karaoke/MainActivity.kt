package com.example.karaoke

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import kotlin.math.pow

/**
 * 唱K App 主界面：歌曲播放 + LRC 歌词同步高亮 + 变速移调 + 麦克风跟唱。
 * 全部原生控件，无第三方依赖；minSdk 26。
 */
class MainActivity : Activity() {

    companion object {
        private const val REQ_SONG = 1001
        private const val REQ_LRC = 1002
        private const val REQ_MIC = 1003
    }

    private lateinit var songName: TextView
    private lateinit var karaoke: KaraokeTextView
    private lateinit var seek: SeekBar
    private lateinit var curTime: TextView
    private lateinit var durTime: TextView
    private lateinit var playBtn: Button
    private lateinit var speedVal: TextView
    private lateinit var pitchVal: TextView
    private lateinit var speedSeek: SeekBar
    private lateinit var pitchSeek: SeekBar
    private lateinit var micBtn: Button
    private lateinit var micVol: SeekBar

    private var player: MediaPlayer? = null
    private var mic: MicLoopback? = null
    private var micStarting = false

    private val handler = Handler(Looper.getMainLooper())

    private val ticker = object : Runnable {
        override fun run() {
            val p = player
            if (p != null) {
                val pos = p.currentPosition.toLong()
                seek.progress = pos.toInt()
                curTime.text = fmt(pos)
                karaoke.setProgressMillis(pos)
            }
            handler.postDelayed(this, 150)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        songName = findViewById(R.id.songName)
        karaoke = findViewById(R.id.karaokeView)
        seek = findViewById(R.id.seek)
        curTime = findViewById(R.id.curTime)
        durTime = findViewById(R.id.durTime)
        playBtn = findViewById(R.id.playBtn)
        speedVal = findViewById(R.id.speedVal)
        pitchVal = findViewById(R.id.pitchVal)
        speedSeek = findViewById(R.id.speedSeek)
        pitchSeek = findViewById(R.id.pitchSeek)
        micBtn = findViewById(R.id.micBtn)
        micVol = findViewById(R.id.micVol)

        playBtn.setOnClickListener { playPause() }
        findViewById<Button>(R.id.restartBtn).setOnClickListener {
            player?.let { p ->
                p.seekTo(0)
                if (!p.isPlaying) {
                    p.start()
                    playBtn.text = "暂停"
                }
            }
        }

        speedSeek.setOnSeekBarChangeListener(simpleSeek { applyParams() })
        pitchSeek.setOnSeekBarChangeListener(simpleSeek { applyParams() })

        micBtn.setOnClickListener { toggleMic() }
        micVol.setOnSeekBarChangeListener(simpleSeek {
            mic?.setVolume(micVol.progress / 100f)
        })

        findViewById<Button>(R.id.btnSong).setOnClickListener { pickSong() }
        findViewById<Button>(R.id.btnLrc).setOnClickListener { pickLrc() }
        findViewById<Button>(R.id.btnPaste).setOnClickListener { pasteLrc() }

        seek.setOnSeekBarChangeListener(simpleSeek {
            player?.let { p ->
                val target = seek.progress
                p.seekTo(target)
                karaoke.setProgressMillis(target.toLong())
                curTime.text = fmt(target.toLong())
            }
        })

        handler.post(ticker)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        player?.release()
        player = null
        mic?.stop()
        mic = null
    }

    /* ============ 播放 ============ */

    private fun playPause() {
        val p = player ?: run {
            toast("先选择一首歌曲")
            return
        }
        if (p.isPlaying) {
            p.pause()
            playBtn.text = "播放"
        } else {
            p.start()
            playBtn.text = "暂停"
        }
    }

    private fun pickSong() {
        val i = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "audio/*"
        }
        startActivityForResult(i, REQ_SONG)
    }

    private fun pickLrc() {
        val i = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/*"
        }
        startActivityForResult(i, REQ_LRC)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK || data?.data == null) return
        val uri = data.data!!
        when (requestCode) {
            REQ_SONG -> loadSong(uri)
            REQ_LRC -> loadLrc(uri)
        }
    }

    private fun loadSong(uri: Uri) {
        try {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (_: Exception) {
            // 部分来源不支持持久授权，忽略即可
        }
        songName.text = displayName(uri)
        player?.release()
        player = null

        val p = MediaPlayer.create(this, uri) ?: run {
            toast("无法播放该文件")
            return
        }
        player = p
        p.setOnPreparedListener {
            seek.max = p.duration
            durTime.text = fmt(p.duration.toLong())
            applyParams()
            p.start()
            playBtn.text = "暂停"
        }
        p.setOnCompletionListener {
            playBtn.text = "播放"
            seek.progress = 0
            curTime.text = fmt(0)
            karaoke.reset()
        }
    }

    private fun displayName(uri: Uri): String {
        contentResolver.query(uri, null, null, null, null)?.use { c ->
            val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && c.moveToFirst()) {
                val name = c.getString(idx)
                if (!name.isNullOrBlank()) return name
            }
        }
        return uri.lastPathSegment ?: "歌曲"
    }

    /* ============ 变速 / 移调 ============ */

    private fun applyParams() {
        val p = player ?: return
        val sp = speedSeek.progress / 100f              // 50..100 → 0.5..1.5
        val st = pitchSeek.progress - 6                 // 0..12 → -6..+6 半音
        val pitch = 2f.pow(st / 12f)                    // 半音 → 倍率
        try {
            p.playbackParams = p.playbackParams.setSpeed(sp).setPitch(pitch)
            speedVal.text = String.format("%.2f×", sp)
            pitchVal.text = if (st > 0) "+$st" else "$st"
        } catch (_: Exception) {
            // 未 prepare 时部分机型会抛 IllegalStateException，忽略
        }
    }

    /* ============ 麦克风 ============ */

    private fun toggleMic() {
        if (mic != null) {
            mic?.stop()
            mic = null
            micBtn.text = "麦克风 关"
            micVol.isEnabled = false
            return
        }
        if (micStarting) return
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            micStarting = true
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), REQ_MIC)
            return
        }
        startMic()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_MIC) {
            micStarting = false
            if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
                startMic()
            } else {
                toast("没有麦克风权限")
            }
        }
    }

    private fun startMic() {
        val m = MicLoopback()
        m.start()
        m.setVolume(micVol.progress / 100f)
        mic = m
        micBtn.text = "麦克风 开"
        micVol.isEnabled = true
        toast("麦克风已开启，请戴耳机")
    }

    /* ============ 歌词 ============ */

    private fun loadLrc(uri: Uri) {
        try {
            val txt = contentResolver.openInputStream(uri)
                ?.bufferedReader(Charsets.UTF_8)
                ?.use { it.readText() }
                ?: return
            applyLrc(txt)
        } catch (_: Exception) {
            toast("歌词文件读取失败")
        }
    }

    private fun applyLrc(txt: String) {
        val r = LrcParser.parse(txt)
        if (r.lines.isEmpty()) {
            toast("没解析到歌词行")
            return
        }
        karaoke.setLrc(r.lines)
        toast("已载入歌词 ${r.lines.size} 行")
    }

    private fun pasteLrc() {
        val et = EditText(this)
        et.gravity = Gravity.TOP or Gravity.START
        et.hint = "[00:12.00]第一句歌词\n[00:20.00]<00:20.00>你<00:20.50>好"
        AlertDialog.Builder(this)
            .setTitle("粘贴 LRC 歌词")
            .setView(et)
            .setPositiveButton("应用") { _, _ -> applyLrc(et.text.toString()) }
            .setNegativeButton("取消", null)
            .show()
    }

    /* ============ 工具 ============ */

    private fun fmt(ms: Long): String {
        val s = ms / 1000
        return String.format("%02d:%02d", s / 60, s % 60)
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    private fun simpleSeek(onChange: () -> Unit) = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            if (fromUser) onChange()
        }

        override fun onStartTrackingTouch(seekBar: SeekBar?) {}

        override fun onStopTrackingTouch(seekBar: SeekBar?) {}
    }
}
