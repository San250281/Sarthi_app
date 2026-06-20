package com.example.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

class SpeechSpeechRecognizer(
    private val context: Context,
    private val onResult: (String) -> Unit,
    private val onError: (String) -> Unit,
    private val onPartialResult: (String) -> Unit = {},
    private val onRmsChanged: (Float) -> Unit = {}
) {
    private var speechRecognizer: SpeechRecognizer? = null

    fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onError("Speech recognition is not available on this device. Please type your message instead!")
            return
        }

        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        Log.d("STT", "Ready for speech")
                    }
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {
                        onRmsChanged(rmsdB)
                    }
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {}
                    override fun onError(error: Int) {
                        val message = when (error) {
                            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                            SpeechRecognizer.ERROR_CLIENT -> "Client side error"
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is required"
                            SpeechRecognizer.ERROR_NETWORK -> "Network connection error"
                            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network connection timed out"
                            SpeechRecognizer.ERROR_NO_MATCH -> "No speech understood. Tap mic and try again!"
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Voice assistant is busy. Please try again"
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected. Tap mic to speak!"
                            else -> "Voice input error. Try again!"
                        }
                        onError(message)
                    }
                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull() ?: ""
                        onResult(text)
                    }
                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull() ?: ""
                        onPartialResult(text)
                    }
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }

            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e("STT", "Error initializing speech recognizer: ${e.message}", e)
            onError("Voice recognition failed to start. Please type.")
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.e("STT", "Error cleaning up speech recognizer: ${e.message}")
        }
        speechRecognizer = null
    }
}

class SpeechTextToSpeech(
    private val context: Context,
    private val onStart: () -> Unit = {},
    private val onDone: () -> Unit = {}
) {
    private var tts: TextToSpeech? = null
    private var isInitialized = false

    init {
        try {
            tts = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val locale = Locale.getDefault()
                    val result = tts?.setLanguage(locale)
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e("TTS", "Language missing/not supported. Defaulting to US locale.")
                        tts?.setLanguage(Locale.US)
                    }
                    isInitialized = true
                    tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            onStart()
                        }
                        override fun onDone(utteranceId: String?) {
                            onDone()
                        }
                        @Deprecated("Deprecated")
                        override fun onError(utteranceId: String?) {
                            onDone()
                        }
                        override fun onError(utteranceId: String?, errorCode: Int) {
                            Log.e("TTS", "TTS Error: $errorCode")
                            onDone()
                        }
                    })
                } else {
                    Log.e("TTS", "Initialization failed with status: $status")
                }
            }
        } catch (e: Exception) {
            Log.e("TTS", "Error starting TTS: ${e.message}", e)
        }
    }

    fun speak(text: String) {
        if (!isInitialized) {
            Log.w("TTS", "TTS system is not yet ready.")
            return
        }
        try {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "saarthi_voice_output")
        } catch (e: Exception) {
            Log.e("TTS", "Failed to speak: ${e.message}")
        }
    }

    fun stop() {
        try {
            tts?.stop()
        } catch (e: Exception) {
            Log.e("TTS", "Error stopping TTS: ${e.message}")
        }
    }

    fun shutdown() {
        try {
            tts?.shutdown()
        } catch (e: Exception) {
            Log.e("TTS", "Error shutting down TTS: ${e.message}")
        }
    }
}
