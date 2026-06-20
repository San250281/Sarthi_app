package com.aistudio.saarthi.speech

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
    private val TAG = "SAARTHI_VOICE"

    fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.e(TAG, "Speech recognition is NOT available on this device.")
            onError("Speech recognition is not available on this device. Please type your message instead!")
            return
        }

        try {
            Log.d(TAG, "Initializing speech recognizer...")
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        Log.i(TAG, "Voice recognition channel ready. Speak now.")
                    }
                    override fun onBeginningOfSpeech() {
                        Log.d(TAG, "User started speaking...")
                    }
                    override fun onRmsChanged(rmsdB: Float) {
                        onRmsChanged(rmsdB)
                    }
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {
                        Log.d(TAG, "User finished speaking.")
                    }
                    override fun onError(error: Int) {
                        val message = when (error) {
                            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error. Please check your mic."
                            SpeechRecognizer.ERROR_CLIENT -> "Voice client transaction failed."
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is required."
                            SpeechRecognizer.ERROR_NETWORK -> "Network connection error."
                            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network connection timed out."
                            SpeechRecognizer.ERROR_NO_MATCH -> "No speech understood. Tap mic and try again!"
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Voice assistant is busy. Please try again."
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected. Tap mic to speak!"
                            else -> "Voice input error. Try again!"
                        }
                        Log.e(TAG, "Speech Recognizer error code: $error ($message)")
                        onError(message)
                    }
                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull() ?: ""
                        Log.i(TAG, "Recognized output: $text")
                        onResult(text)
                    }
                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull() ?: ""
                        if (text.isNotBlank()) {
                            Log.d(TAG, "Partial recognized output: $text")
                            onPartialResult(text)
                        }
                    }
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val defaultLangTag = Locale.getDefault().toLanguageTag()
            Log.d(TAG, "Speech active language: $defaultLangTag, configuring additional Hinglish/Hindi rules packs.")

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, defaultLangTag)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, defaultLangTag)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                
                // Expose additional Indian Hinglish or Hindi linguistic fallback rules
                val fallbackLanguages = arrayListOf("hi-IN", "en-IN", "en-US")
                putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", fallbackLanguages.toTypedArray())
            }

            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting voice recognition: ${e.message}", e)
            onError("Voice recognition failed to start. Please type.")
        }
    }

    fun stopListening() {
        try {
            Log.d(TAG, "Stopping voice recognition channel...")
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up speech recognizer: ${e.message}")
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
    private val TAG = "SAARTHI_TTS"

    init {
        try {
            Log.d(TAG, "Initializing TextToSpeech Service...")
            tts = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val defaultLocale = Locale.getDefault()
                    Log.i(TAG, "TextToSpeech successfully initialized. Testing local constraints...")
                    try {
                        val result = tts?.setLanguage(defaultLocale)
                        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                            Log.w(TAG, "Default locale missing/not supported. Defaulting to US English.")
                            tts?.setLanguage(Locale.US)
                        }
                    } catch (ex: Exception) {
                        Log.e(TAG, "Error setting default locale in TextToSpeech", ex)
                        tts?.setLanguage(Locale.US)
                    }
                    isInitialized = true
                    tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            Log.d(TAG, "Audio synthesis starting for id: $utteranceId")
                            onStart()
                        }
                        override fun onDone(utteranceId: String?) {
                            Log.d(TAG, "Audio synthesis complete.")
                            onDone()
                        }
                        @Deprecated("Deprecated")
                        override fun onError(utteranceId: String?) {
                            Log.e(TAG, "Audio synthesis error on layout.")
                            onDone()
                        }
                        override fun onError(utteranceId: String?, errorCode: Int) {
                            Log.e(TAG, "Audio synthesis error code: $errorCode")
                            onDone()
                        }
                    })
                } else {
                    Log.e(TAG, "TTS Initialization failed with status code: $status")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Critical failure during TTS creation: ${e.message}", e)
        }
    }

    fun speak(text: String, detectedLang: String = "English") {
        if (!isInitialized || tts == null) {
            Log.w(TAG, "TTS speech invoked but system is not initialized/available.")
            return
        }

        try {
            // Dynamic Language adaptation for speech playback
            val hasDevanagari = text.any { it.code in 0x0900..0x097F }
            val targetLocale = if (hasDevanagari || detectedLang == "Hindi") {
                Log.d(TAG, "Devanagari text detected. Swapping speech synthesis to Hindi Locale.")
                Locale("hi", "IN")
            } else {
                Locale.getDefault()
            }

            Log.d(TAG, "Playing TTS synthesized output (Language = ${targetLocale.toLanguageTag()}). Length = ${text.length} characters.")
            val langResult = tts?.setLanguage(targetLocale)
            if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.w(TAG, "Preferred Language pack for ${targetLocale.displayName} is missing. Falling back safely.")
                tts?.setLanguage(Locale.US)
            }

            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "saarthi_voice_output")
        } catch (e: Exception) {
            Log.e(TAG, "TTS failed to synthesize Speech output: ${e.message}", e)
        }
    }

    fun stop() {
        try {
            Log.d(TAG, "Stopping TTS playback channel...")
            tts?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping TTS: ${e.message}")
        }
    }

    fun shutdown() {
        try {
            Log.d(TAG, "Shutting down TTS playback channel...")
            tts?.shutdown()
        } catch (e: Exception) {
            Log.e(TAG, "Error shutting down TTS: ${e.message}")
        }
    }
}
