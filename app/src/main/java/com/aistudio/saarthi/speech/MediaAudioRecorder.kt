package com.aistudio.saarthi.speech

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File

class MediaAudioRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private val TAG = "SAARTHI_VOICE"

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
            Log.d(TAG, "Audio direct recording started: ${outputFile.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed start recording using MediaRecorder: ${e.message}", e)
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
            Log.d(TAG, "Audio recording completed successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping media recorder (recording may be too short): ${e.message}")
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
