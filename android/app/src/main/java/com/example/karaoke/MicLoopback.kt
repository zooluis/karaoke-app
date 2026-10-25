package com.example.karaoke

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder

/**
 * 麦克风回环：把手机麦克风实时混入扬声器播放（K 歌跟唱用）。
 * 需要 RECORD_AUDIO 权限；请戴耳机使用避免啸叫。
 */
class MicLoopback {

    private var record: AudioRecord? = null
    private var track: AudioTrack? = null
    private var thread: Thread? = null
    @Volatile private var running = false
    private var volume = 0.6f

    private val sampleRate = 44100

    fun start() {
        if (running) return
        val minIn = AudioRecord.getMinBufferSize(
            sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        val minOut = AudioTrack.getMinBufferSize(
            sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        val bufSize = maxOf(minIn, minOut) * 2

        val rec = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufSize
        )
        if (rec.state != AudioRecord.STATE_INITIALIZED) {
            rec.release()
            return
        }
        val tr = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        record = rec
        track = tr
        rec.startRecording()
        tr.play()
        tr.setVolume(volume)

        running = true
        thread = Thread {
            val buf = ByteArray(bufSize)
            while (running) {
                val n = rec.read(buf, 0, buf.size)
                if (n > 0) tr.write(buf, 0, n)
            }
        }
        thread?.start()
    }

    fun stop() {
        running = false
        thread?.join(400)
        thread = null
        try {
            record?.stop()
            record?.release()
        } catch (_: Exception) {
        }
        try {
            track?.stop()
            track?.release()
        } catch (_: Exception) {
        }
        record = null
        track = null
    }

    fun setVolume(v: Float) {
        volume = v.coerceIn(0f, 1f)
        track?.setVolume(volume)
    }
}
