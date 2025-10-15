
package com.ptthaiti.audio

import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import java.io.File

class Recorder {
    private var rec: MediaRecorder? = null
    private var startMs: Long = 0

    fun start(outFile: File) {
        stop()
        val r = MediaRecorder()
        r.setAudioSource(MediaRecorder.AudioSource.MIC)
        if (Build.VERSION.SDK_INT >= 31) {
            r.setOutputFormat(MediaRecorder.OutputFormat.WEBM)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.OPUS)
        } else {
            r.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        }
        r.setAudioEncodingBitRate(12000)
        r.setAudioSamplingRate(16000)
        r.setOutputFile(outFile.absolutePath)
        r.prepare()
        r.start()
        rec = r
        startMs = System.currentTimeMillis()
    }

    fun stop(): Double {
        val r = rec ?: return 0.0
        return try {
            r.stop()
            r.reset()
            r.release()
            rec = null
            val dur = (System.currentTimeMillis() - startMs) / 1000.0
            dur
        } catch (e: Exception) {
            rec = null
            0.0
        }
    }
}

object Player {
    private var mp: MediaPlayer? = null
    fun play(file: File) {
        stop()
        mp = MediaPlayer().apply {
            setDataSource(file.absolutePath)
            prepare()
            start()
        }
    }
    fun stop() { try { mp?.stop(); mp?.release() } catch (_: Exception) {} finally { mp = null } }
}
