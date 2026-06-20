package com.example.speech

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File

class MediaAudioRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null

    @Suppress("DEPRECATION")
    fun startRecording(outputFile: File) {
        try {
            recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(64000)
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }
            Log.d("MediaAudioRecorder", "Recording started successfully: ${outputFile.absolutePath}")
        } catch (e: Exception) {
            Log.e("MediaAudioRecorder", "Failed to start recording using MediaRecorder", e)
            recorder = null
            throw e
        }
    }

    fun stopRecording() {
        try {
            recorder?.apply {
                stop()
                release()
            }
            Log.d("MediaAudioRecorder", "Recording stopped successfully")
        } catch (e: Exception) {
            Log.e("MediaAudioRecorder", "Error stopping recorder or recording too short", e)
        } finally {
            recorder = null
        }
    }

    fun getMaxAmplitude(): Int {
        return try {
            recorder?.maxAmplitude ?: 0
        } catch (e: Exception) {
            0
        }
    }
}
