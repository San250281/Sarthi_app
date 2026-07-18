package com.aistudio.saarthi.viewmodel

import com.aistudio.saarthi.BuildConfig
import android.app.Application
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aistudio.saarthi.api.GeminiClient
import com.aistudio.saarthi.api.GeminiContent
import com.aistudio.saarthi.api.GeminiPart
import com.aistudio.saarthi.data.*
import com.aistudio.saarthi.speech.SpeechSpeechRecognizer
import com.aistudio.saarthi.speech.SpeechTextToSpeech
import com.aistudio.saarthi.speech.MediaAudioRecorder
import com.aistudio.saarthi.api.GeminiInlineData
import java.io.File
import android.util.Base64
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

@JsonClass(generateAdapter = true)
data class ExtractedMemory(
    val category: String,
    val key: String,
    val value: String
)

@JsonClass(generateAdapter = true)
data class ReflectionJson(
    val detectedFeelings: String,
    val keyTakeaways: String,
    val actionableSteps: String,
    val mainMoodEmoji: String
)

enum class SaarthiState {
    IDLE,
    LISTENING,
    THINKING,
    SPEAKING
}

class SaarthiViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "SaarthiViewModel"

    private val database by lazy {
        androidx.room.Room.databaseBuilder(
            getApplication(),
            SaarthiDatabase::class.java,
            "saarthi_db"
        ).fallbackToDestructiveMigration().build()
    }

    val repository by lazy {
        SaarthiRepository(database)
    }

    // UI States
    private val _companionState = MutableStateFlow(SaarthiState.IDLE)
    val companionState: StateFlow<SaarthiState> = _companionState.asStateFlow()

    private val _ttsEnabled = MutableStateFlow(true)
    val ttsEnabled: StateFlow<Boolean> = _ttsEnabled.asStateFlow()

    private val _partialSpeechInput = MutableStateFlow("")
    val partialSpeechInput: StateFlow<String> = _partialSpeechInput.asStateFlow()

    private val _voiceAmplitude = MutableStateFlow(0f)
    val voiceAmplitude: StateFlow<Float> = _voiceAmplitude.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Flow items from Repository
    val messages = repository.chatMessages.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val memories = repository.memoryItems.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val profile = repository.userProfile.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UserProfile()
    )

    val moodRecords = repository.moodRecords.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val reflectionSummaries = repository.reflectionSummaries.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _reflectionSpeechInput = MutableStateFlow("")
    val reflectionSpeechInput: StateFlow<String> = _reflectionSpeechInput.asStateFlow()

    private val _isGeneratingReflection = MutableStateFlow(false)
    val isGeneratingReflection: StateFlow<Boolean> = _isGeneratingReflection.asStateFlow()

    private val _isGeneratingReport = MutableStateFlow(false)
    val isGeneratingReport: StateFlow<Boolean> = _isGeneratingReport.asStateFlow()

    private val _growthReport = MutableStateFlow<String?>(null)
    val growthReport: StateFlow<String?> = _growthReport.asStateFlow()

    private val _isGeneratingReflectivePrompts = MutableStateFlow(false)
    val isGeneratingReflectivePrompts: StateFlow<Boolean> = _isGeneratingReflectivePrompts.asStateFlow()

    private val _reflectivePrompts = MutableStateFlow<List<String>>(listOf(
        "What is one goal you are working on today, and what is the smallest first step you can take to make progress?",
        "Looking back at your recent conversations, is there a specific thought or feeling that has been recurring?",
        "What is a supportive, kind perspective you can offer yourself regarding any challenge you're currently facing?",
        "Describe a simple habit or routine that made you feel balanced recently."
    ))
    val reflectivePrompts: StateFlow<List<String>> = _reflectivePrompts.asStateFlow()

    private val _detectedUserLanguage = MutableStateFlow("English")
    val detectedUserLanguage: StateFlow<String> = _detectedUserLanguage.asStateFlow()

    private val _isGratitudeLoggingActive = MutableStateFlow(false)
    val isGratitudeLoggingActive: StateFlow<Boolean> = _isGratitudeLoggingActive.asStateFlow()

    private val _useDirectAudioCapture = MutableStateFlow(false)
    val useDirectAudioCapture: StateFlow<Boolean> = _useDirectAudioCapture.asStateFlow()

    // --- NEW: Multiple AI Agents Features and Razorpay Wallet ---
    data class AIAgent(
        val id: String,
        val name: String,
        val pricePerMin: Int,
        val avatarEmoji: String,
        val description: String,
        val greeting: String,
        val systemPromptExtension: String
    )

    val agentsList = listOf(
        AIAgent(
            id = "saarthi",
            name = "Saarthi",
            pricePerMin = 5,
            avatarEmoji = "🌌",
            description = "Warm, empathetic and emotionally intelligent companion.",
            greeting = "Aapka swagat hai. I am Saarthi, your voice companion. My talking rate is ₹5 per minute. Aap mujhse kisi bhi vishay par baat kar sakte hain. How are you feeling today?",
            systemPromptExtension = "You are Saarthi, an emotionally intelligent guide. Be extremely warm, supportive, friendly, and non-judgmental. Listen and validate before suggesting positive baby steps."
        ),
        AIAgent(
            id = "kabir",
            name = "Kabir",
            pricePerMin = 10,
            avatarEmoji = "🧘",
            description = "Philosophical guide who speaks with couplets (dohas) and deep peace.",
            greeting = "Pranam, seeker of truth. I am Kabir, your philosophical mentor. My rate is ₹10 per minute. 'Bura jo dekhan main chala, bura na milya koy...' Life operates on subtle laws. What binds your thoughts today?",
            systemPromptExtension = "You are Saint Kabir, a spiritual and philosophical mentor. Speak with profound depth and detachment. Weave in simple parables, couplets, or traditional Indian philosophical insights with composure. Encourage self-reflection."
        ),
        AIAgent(
            id = "meera",
            name = "Meera",
            pricePerMin = 8,
            avatarEmoji = "🎨",
            description = "Creative coach helping you seek artistic flow and express yourself.",
            greeting = "Hello creative soul! I am Meera, your artistic spark. My rate is ₹8 per minute. Let's paint your thoughts today. What creative spark or aesthetic dream is calling you?",
            systemPromptExtension = "You are Meera, an enthusiastic creative coach and therapist. Be positive, expressive, artistic, encouraging, and eager to help the user spark imagination, write, or find creative flow in their emotional challenges."
        ),
        AIAgent(
            id = "shanti",
            name = "Shanti",
            pricePerMin = 12,
            avatarEmoji = "🏔️",
            description = "Zen mindfulness teacher specializing in slowing down and breathing.",
            greeting = "Deep breath in... and slow release. I am Shanti, your Zen coach. My rate is ₹12 per minute. Let's rest in the present moment together. What is current-moment reality showing you?",
            systemPromptExtension = "You are Shanti, a serene Zen teacher and mindfulness coach. Speak extremely slowly and with intense tranquility. Guide the user to practice relaxing, rhythmic breathing, and remind them to stay in the present moment."
        )
    )

    private val _selectedAgent = MutableStateFlow(agentsList[0])
    val selectedAgent: StateFlow<AIAgent> = _selectedAgent.asStateFlow()

    private val _walletBalance = MutableStateFlow(150) // starts with ₹150 demo credit
    val walletBalance: StateFlow<Int> = _walletBalance.asStateFlow()

    private var debitJob: kotlinx.coroutines.Job? = null

    fun selectAgent(agentId: String) {
        val found = agentsList.find { it.id == agentId } ?: return
        _selectedAgent.value = found
        
        // Add greeting message to database
        viewModelScope.launch(Dispatchers.IO) {
            val switchMsg = ChatMessage(
                role = "saarthi",
                text = "Switched companion to ${found.name} ${found.avatarEmoji}. ${found.greeting}"
            )
            repository.insertMessage(switchMsg)
            
            if (_ttsEnabled.value) {
                ttsEngine?.speak(found.greeting)
            }
        }
    }

    fun addWalletBalance(amount: Int, packName: String = "Top-up") {
        _walletBalance.value += amount
        viewModelScope.launch(Dispatchers.IO) {
            val successMsg = ChatMessage(
                role = "saarthi",
                text = "🎉 Razorpay recharge successful! Added ₹$amount to your wallet via $packName. Total balance: ₹${_walletBalance.value}."
            )
            repository.insertMessage(successMsg)
        }
    }

    // Subscription & Premium Plan activation logic
    private var pendingPlanPrice: Int? = null
    private var pendingPlanValue: Int? = null
    private var pendingPlanName: String? = null

    fun setPendingOrder(price: Int, value: Int, name: String) {
        pendingPlanPrice = price
        pendingPlanValue = value
        pendingPlanName = name
    }

    fun purchaseSubscription(planName: String, price: Int, talkTimeCredits: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val cur = repository.userProfile.first() ?: UserProfile()
            val durationMs = when (planName) {
                "Weekly Explorer" -> 7L * 24L * 60L * 60L * 1000L
                "Monthly Companion" -> 30L * 24L * 60L * 60L * 1000L
                "Yearly Zen" -> 365L * 24L * 60L * 60L * 1000L
                else -> 30L * 24L * 60L * 60L * 1000L
            }
            val expiry = System.currentTimeMillis() + durationMs
            
            val updated = cur.copy(
                subscriptionPlan = planName,
                subscriptionExpiry = expiry,
                subscriptionStatus = "Active"
            )
            repository.saveProfile(updated)
            
            if (talkTimeCredits > 0) {
                _walletBalance.value += talkTimeCredits
            }
            
            val successMsg = ChatMessage(
                role = "saarthi",
                text = "💳 **Subscription Activated:** Thank you for billing **$planName**!\n" +
                       "• Amount Paid: ₹$price\n" +
                       "• Talk-time credits: ${if (planName == "Yearly Zen") "Unlimited Zen Talk-Time enabled!" else "+₹$talkTimeCredits added to your wallet!"}\n" +
                       "• Status: Active until ${java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()).format(expiry)}"
            )
            repository.insertMessage(successMsg)
        }
    }

    fun onRazorpayPaymentSuccess(paymentId: String?) {
        val price = pendingPlanPrice ?: 0
        val value = pendingPlanValue ?: 0
        val name = pendingPlanName ?: "Top-up"

        if (name.contains("Pack")) {
            addWalletBalance(value, name)
        } else {
            purchaseSubscription(name, price, value)
        }

        pendingPlanPrice = null
        pendingPlanValue = null
        pendingPlanName = null
    }

    fun onRazorpayPaymentFailure(code: Int, response: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            val failedMsg = ChatMessage(
                role = "saarthi",
                text = "❌ **Razorpay Payment Failed:** Payment authorization was canceled or failed (Code $code).\nResponse: ${response ?: "User dismissed checkout."}"
            )
            repository.insertMessage(failedMsg)
        }
        
        pendingPlanPrice = null
        pendingPlanValue = null
        pendingPlanName = null
    }

    // -----------------------------------------------------------

    private var audioRecorder: MediaAudioRecorder? = null
    private var recordedAudioFile: File? = null

    // Voice Engine Managers
    private var ttsEngine: SpeechTextToSpeech? = null
    private var recognizerEngine: SpeechSpeechRecognizer? = null

    init {
        // App startup logs & API Key validation
        Log.i("SAARTHI_APP", "Saarthi App initialized. Using AWS Lambda backend.")

        // Build TTS Engine
        ttsEngine = SpeechTextToSpeech(
            context = getApplication(),
            onStart = {
                _companionState.value = SaarthiState.SPEAKING
            },
            onDone = {
                _companionState.value = SaarthiState.IDLE
            }
        )

        // Seed initial message if the history database is empty
        viewModelScope.launch(Dispatchers.IO) {
            val count = repository.chatMessages.first().size
            if (count == 0) {
                val timeGreeting = getIntroTimeGreeting()
                val welcomeMsg = ChatMessage(
                    role = "saarthi",
                    text = "$timeGreeting I am Saarthi, your emotionally intelligent AI guide. I'm here to listen, support, and help you move forward. How are you feeling tonight?"
                )
                repository.insertMessage(welcomeMsg)
            }
        }

        // Seed emotional logging checkpoints if empty
        viewModelScope.launch(Dispatchers.IO) {
            val count = repository.moodRecords.first().size
            if (count == 0) {
                val now = System.currentTimeMillis()
                val oneDay = 24 * 60 * 60 * 1000L
                repository.insertMoodRecord(MoodRecord(mood = "Normal", confidence = 4, clarity = 3, notes = "Quietly started tracking key ideas with Saarthi.", timestamp = now - 4 * oneDay))
                repository.insertMoodRecord(MoodRecord(mood = "Confused", confidence = 3, clarity = 5, notes = "Had some doubts about work-life focus.", timestamp = now - 3 * oneDay))
                repository.insertMoodRecord(MoodRecord(mood = "Stressed", confidence = 5, clarity = 4, notes = "Worrying about execution, but Saarthi helped break the problem down.", timestamp = now - 2 * oneDay))
                repository.insertMoodRecord(MoodRecord(mood = "Normal", confidence = 7, clarity = 7, notes = "Refined some positive goals and bedtime routines.", timestamp = now - oneDay))
                repository.insertMoodRecord(MoodRecord(mood = "Excited", confidence = 8, clarity = 9, notes = "Moving forward with robust, clear actionable plans!", timestamp = now))
            }
        }
    }

    fun setTtsEnabled(enabled: Boolean) {
        _ttsEnabled.value = enabled
        if (!enabled) {
            ttsEngine?.stop()
            if (_companionState.value == SaarthiState.SPEAKING) {
                _companionState.value = SaarthiState.IDLE
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    // Speech Input Trigger
    private var amplitudeJob: kotlinx.coroutines.Job? = null

    fun setUseDirectAudioCapture(enabled: Boolean) {
        _useDirectAudioCapture.value = enabled
    }

    fun startListening() {
        ttsEngine?.stop()
        _partialSpeechInput.value = ""
        _voiceAmplitude.value = 0f

        Log.d("SAARTHI_VOICE", "Attempting to start listening. Checking RECORD_AUDIO permission...")
        val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
            getApplication(),
            android.Manifest.permission.RECORD_AUDIO
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            Log.e("SAARTHI_VOICE", "Speech recording fails: Microphone permission is denied!")
            _companionState.value = SaarthiState.IDLE
            _errorMessage.value = "Microphone permission is required to use voice input."
            return
        }

        // --- Wallet Check ---
        val rate = _selectedAgent.value.pricePerMin
        if (_walletBalance.value < rate) {
            _companionState.value = SaarthiState.IDLE
            _errorMessage.value = "Insufficient wallet balance (Requires at least ₹$rate. Active Companion: ${_selectedAgent.value.name} at ₹$rate/min). Please add money via Razorpay!"
            return
        }

        _companionState.value = SaarthiState.LISTENING

        // Start dynamic debiting loop during active call/listening
        debitJob?.cancel()
        debitJob = viewModelScope.launch(Dispatchers.Default) {
            var secondsPassed = 0
            val costPerMin = _selectedAgent.value.pricePerMin
            val secondsPerRupee = if (costPerMin > 0) 60 / costPerMin else 12
            
            while (_companionState.value == SaarthiState.LISTENING) {
                kotlinx.coroutines.delay(1000)
                secondsPassed++
                if (secondsPassed >= secondsPerRupee) {
                    secondsPassed = 0
                    if (_walletBalance.value > 0) {
                        _walletBalance.value -= 1
                    } else {
                        // Ran out of funds! Stop listing immediately
                        launch(Dispatchers.Main) {
                            stopListening()
                            _errorMessage.value = "Call disconnected: Wallet balance reached zero! Please top up via Razorpay."
                        }
                        break
                    }
                }
            }
        }

        if (_useDirectAudioCapture.value) {
            try {
                recordedAudioFile = File(getApplication<Application>().cacheDir, "saarthi_capture.m4a")
                audioRecorder = MediaAudioRecorder(getApplication())
                audioRecorder?.startRecording(recordedAudioFile!!)

                amplitudeJob?.cancel()
                amplitudeJob = viewModelScope.launch(Dispatchers.Default) {
                    while (_companionState.value == SaarthiState.LISTENING) {
                        val amp = audioRecorder?.getMaxAmplitude() ?: 0
                        val normalized = (amp / 32767f).coerceIn(0f, 1f)
                        _voiceAmplitude.value = normalized
                        kotlinx.coroutines.delay(100)
                    }
                }
            } catch (e: Exception) {
                Log.e("SaarthiViewModel", "Failed to start MediaRecorder capture", e)
                _companionState.value = SaarthiState.IDLE
                _errorMessage.value = "Direct audio recording failed to start: ${e.message}"
            }
        } else {
            recognizerEngine = SpeechSpeechRecognizer(
                context = getApplication(),
                onResult = { resultText ->
                    _partialSpeechInput.value = ""
                    _voiceAmplitude.value = 0f
                    _companionState.value = SaarthiState.IDLE
                    debitJob?.cancel()
                    if (resultText.isNotBlank()) {
                        sendMessage(resultText, isVoice = true)
                    }
                },
                onError = { err ->
                    _companionState.value = SaarthiState.IDLE
                    _voiceAmplitude.value = 0f
                    debitJob?.cancel()
                    _errorMessage.value = err
                },
                onPartialResult = { partialText ->
                    _partialSpeechInput.value = partialText
                },
                onRmsChanged = { rmsdB ->
                    // Map speech RMS level (typically around -2dB to 10+dB) to 0.0 .. 1.0
                    val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                    _voiceAmplitude.value = normalized
                }
            )
            recognizerEngine?.startListening()
        }
    }

    fun stopListening() {
        _voiceAmplitude.value = 0f
        debitJob?.cancel()
        if (_useDirectAudioCapture.value) {
            amplitudeJob?.cancel()
            _companionState.value = SaarthiState.IDLE
            audioRecorder?.stopRecording()
            recordedAudioFile?.let { file ->
                sendAudioMessage(file)
            }
        } else {
            recognizerEngine?.stopListening()
            _companionState.value = SaarthiState.IDLE
        }
    }

    fun sendAudioMessage(file: File) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (!file.exists() || file.length() == 0L) {
                    _errorMessage.value = "Recorded file is empty or missing. Please try speaking again."
                    return@launch
                }

                if (!com.aistudio.saarthi.util.NetworkUtils.isNetworkAvailable(getApplication())) {
                    _companionState.value = SaarthiState.IDLE
                    _errorMessage.value = "Please check your internet connection and try again."
                    return@launch
                }
                
                _companionState.value = SaarthiState.THINKING
                
                // Show a localized user voice note in chat history
                val userMsg = ChatMessage(
                    role = "user", 
                    text = "🎤 Voice Note (Direct Audio Capture)", 
                    isVoice = true
                )
                repository.insertMessage(userMsg)

                val bytes = file.readBytes()
                val base64Data = Base64.encodeToString(bytes, Base64.NO_WRAP)

                // Build conversation history mapping previous messages
                val activeMessages = repository.chatMessages.first()
                val userPart = GeminiPart(
                    text = "Please listen to this audio note and respond with deep empathy and warmth.",
                    inlineData = GeminiInlineData(
                        mimeType = "audio/m4a",
                        data = base64Data
                    )
                )

                val history = activeMessages.takeLast(9).map { msg ->
                    val role = if (msg.role == "saarthi") "model" else "user"
                    GeminiContent(role = role, parts = listOf(GeminiPart(text = msg.text)))
                } + GeminiContent(role = "user", parts = listOf(userPart))

                // Build dynamic system prompt
                val activeUserProfile = repository.userProfile.first()
                val activeMemories = repository.memoryItems.first()
                val systemInstructionPrompt = buildSystemPrompt(activeUserProfile, activeMemories)

                // Call Gemini API which processes this direct audio note!
                val replyText = GeminiClient.generateResponse(history, systemInstructionPrompt)

                // Save companion response in database
                val responseMsg = ChatMessage(role = "saarthi", text = replyText, isVoice = false)
                repository.insertMessage(responseMsg)

                _companionState.value = SaarthiState.IDLE

                if (_ttsEnabled.value) {
                    val responseLang = detectLanguageOfInput(replyText)
                    ttsEngine?.speak(replyText, responseLang)
                }

                // Scan memories from output
                scanAndExtractMemory("Voice Note (Direct Audio Capture)", replyText)
            } catch (e: Exception) {
                Log.e("SaarthiViewModel", "Error in sendAudioMessage", e)
                _companionState.value = SaarthiState.IDLE
                _errorMessage.value = "Failed to process direct voice message: ${e.message}"
            }
        }
    }

    // Main Messaging Engine
    fun sendMessage(text: String, isVoice: Boolean = false) {
        if (text.isBlank()) return

        val rate = _selectedAgent.value.pricePerMin
        if (_walletBalance.value < rate) {
            _errorMessage.value = "Insufficient wallet balance (Requires at least ₹$rate. Active Companion: ${_selectedAgent.value.name} is ₹$rate/min). Please recharge via Razorpay!"
            return
        }

        // Deduct rate for message exchange
        _walletBalance.value = (_walletBalance.value - rate).coerceAtLeast(0)

        viewModelScope.launch(Dispatchers.IO) {
            _errorMessage.value = null

            if (!com.aistudio.saarthi.util.NetworkUtils.isNetworkAvailable(getApplication())) {
                _companionState.value = SaarthiState.IDLE
                _errorMessage.value = "Please check your internet connection and try again."
                return@launch
            }

            // 1. Insert user message in database
            val userMsg = ChatMessage(role = "user", text = text, isVoice = isVoice)
            repository.insertMessage(userMsg)

            // Detect and update language
            _detectedUserLanguage.value = detectLanguageOfInput(text)

            if (_isGratitudeLoggingActive.value) {
                _isGratitudeLoggingActive.value = false

                // Save gratitude MemoryItem
                repository.insertMemory(
                    MemoryItem(
                        category = "Gratitude",
                        key = "Grateful For",
                        value = text
                    )
                )

                // High-quality warm end-of-day closure response
                val systemPrompt = """
                    The user is practicing their Nightly Gratitude Logging.
                    What they are grateful for today: "$text"
                    
                    Respond with deep emotional intelligence and warmth. Acknowledge and reinforce their gratitude choice. 
                    Point out why this focus on appreciation helps calm the mind and prepare for restorative sleep.
                    Keep the response very brief (2 to 3 sentences max) and highly sound-friendly/voice-friendly. Do NOT ask any follow-up questions.
                """.trimIndent()

                val history = listOf(
                    GeminiContent(role = "user", parts = listOf(GeminiPart(text = text)))
                )

                val replyText = GeminiClient.generateResponse(history, systemPrompt)
                val responseMsg = ChatMessage(role = "saarthi", text = replyText, isVoice = false)
                repository.insertMessage(responseMsg)

                _companionState.value = SaarthiState.IDLE
                if (_ttsEnabled.value) {
                    val responseLang = detectLanguageOfInput(replyText)
                    ttsEngine?.speak(replyText, responseLang)
                }
                return@launch
            }

            // 2. Map state to thinking
            _companionState.value = SaarthiState.THINKING

            // 3. Collect conversation history mapping to Gemini content format
            val activeMessages = repository.chatMessages.first()
            val history = activeMessages.takeLast(10).map { msg ->
                val role = if (msg.role == "saarthi") "model" else "user"
                GeminiContent(role = role, parts = listOf(GeminiPart(text = msg.text)))
            }

            // 4. Construct System Instruction with dynamic details
            val activeUserProfile = repository.userProfile.first()
            val activeMemories = repository.memoryItems.first()
            val systemInstructionPrompt = buildSystemPrompt(activeUserProfile, activeMemories)

            // 5. Call Gemini client
            val replyText = GeminiClient.generateResponse(history, systemInstructionPrompt)

            // 6. Save response message in database
            val responseMsg = ChatMessage(role = "saarthi", text = replyText, isVoice = false)
            repository.insertMessage(responseMsg)

            _companionState.value = SaarthiState.IDLE

            // 7. Play Speech Synthesis if enabled
            if (_ttsEnabled.value) {
                val responseLang = detectLanguageOfInput(replyText)
                ttsEngine?.speak(replyText, responseLang)
            }

            // 8. Capture elements in the background to update memories automatically
            scanAndExtractMemory(text, replyText)
        }
    }

    // Manual profile save/update
    fun saveProfileName(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val cur = repository.userProfile.first() ?: UserProfile()
            repository.saveProfile(cur.copy(userName = name))
        }
    }

    fun setMood(mood: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val cur = repository.userProfile.first() ?: UserProfile()
            repository.saveProfile(cur.copy(currentMood = mood))
            
            // Map the selected mood to realistic initial confidence and clarity parameters (1 to 10 scale)
            val conf = when (mood) {
                "Excited" -> 9
                "Normal" -> 7
                "Confused", "Unmotivated" -> 4
                "Stressed" -> 3
                "Sad" -> 2
                else -> 5
            }
            val clar = when (mood) {
                "Excited" -> 8
                "Normal" -> 7
                "Confused" -> 3
                "Stressed" -> 4
                "Unmotivated" -> 4
                "Sad" -> 3
                else -> 5
            }
            
            repository.insertMoodRecord(
                MoodRecord(
                    mood = mood, 
                    confidence = conf, 
                    clarity = clar, 
                    notes = "Selected mood emoji check-in."
                )
            )

            // Mirror emotional shifts with a warm verbal greeting from Saarthi
            sendMessage("I'm feeling $mood today.")
        }
    }

    fun addMoodCheckIn(mood: String, confidence: Int, clarity: Int, notes: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val record = MoodRecord(mood = mood, confidence = confidence, clarity = clarity, notes = notes)
            repository.insertMoodRecord(record)
        }
    }

    fun clearAllMoodLogs() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearAllMoodRecords()
        }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val cur = repository.userProfile.first() ?: UserProfile()
            repository.saveProfile(cur.copy(themeMode = mode))
        }
    }

    // --- Secure Local Login & Authentication System ---
    fun secureSignUp(name: String, email: String, passcode: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (name.isBlank() || email.isBlank() || passcode.length < 4) {
            onError("Please enter all details. Passcode must be at least 4 digits.")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val hashed = com.aistudio.saarthi.util.SecurityUtils.hashPassword(passcode)
                val current = repository.userProfile.first() ?: UserProfile()
                val newProfile = current.copy(
                    userName = name,
                    userEmail = email.trim(),
                    passcodeHash = hashed,
                    isLoggedIn = true
                )
                repository.saveProfile(newProfile)
                launch(Dispatchers.Main) {
                    onSuccess()
                }
            } catch (e: Exception) {
                launch(Dispatchers.Main) {
                    onError("Sign up secure registration failed: ${e.message}")
                }
            }
        }
    }

    fun secureLogin(email: String, passcode: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (email.isBlank() || passcode.isBlank()) {
            onError("Email and passcode cannot be empty.")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val cur = repository.userProfile.first()
                if (cur == null || cur.passcodeHash.isBlank()) {
                    launch(Dispatchers.Main) {
                        onError("No registered user found. Please sign up first.")
                    }
                    return@launch
                }

                if (cur.userEmail.trim().lowercase() != email.trim().lowercase()) {
                    launch(Dispatchers.Main) {
                        onError("Incorrect email address.")
                    }
                    return@launch
                }

                val hashed = com.aistudio.saarthi.util.SecurityUtils.hashPassword(passcode)
                if (cur.passcodeHash == hashed) {
                    repository.saveProfile(cur.copy(isLoggedIn = true))
                    launch(Dispatchers.Main) {
                        onSuccess()
                    }
                } else {
                    launch(Dispatchers.Main) {
                        onError("Invalid passcode. Access Denied.")
                    }
                }
            } catch (e: Exception) {
                launch(Dispatchers.Main) {
                    onError("Secure authentication failure: ${e.message}")
                }
            }
        }
    }

    fun secureLogout() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val cur = repository.userProfile.first()
                if (cur != null) {
                    repository.saveProfile(cur.copy(isLoggedIn = false))
                }
            } catch (e: Exception) {
                Log.e("SaarthiViewModel", "Error logging out securely", e)
            }
        }
    }

    fun fullyResetSecureApp() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                clearChatLogs()
                val cur = repository.userProfile.first() ?: UserProfile()
                repository.saveProfile(UserProfile(id = 1))
            } catch (e: Exception) {
                Log.e("SaarthiViewModel", "Error fully resetting app securely", e)
            }
        }
    }

    fun addManualMemory(category: String, key: String, value: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val item = MemoryItem(category = category, key = key, value = value)
            repository.insertMemory(item)
        }
    }

    fun removeMemory(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteMemoryById(id)
        }
    }

    fun clearChatLogs() {
        viewModelScope.launch(Dispatchers.IO) {
            ttsEngine?.stop()
            repository.clearChatHistory()
            val welcomeMsg = ChatMessage(
                role = "saarthi",
                text = "Starting fresh! Talk to me about your startup, challenges, sleep habits, or feelings. I'll remember the highlights."
            )
            repository.insertMessage(welcomeMsg)
        }
    }

    private fun getIntroTimeGreeting(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when {
            hour in 6..10 -> "Good morning! ☀️"
            hour in 11..16 -> "Good afternoon! 😊"
            hour in 17..20 -> "Welcome back, good evening. 🌅"
            hour in 21..23 || hour < 1 -> "Hey there, absolute peaceful night. 🌌"
            else -> "Hello. Taking a quiet moment before sleep? 🕯️"
        }
    }

    fun getLocalTimeBasedTone(): String {
        val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when {
            h in 6..10 -> "MORNING (Energetic, positive, motivating. Respond with warm breakfast sunshine vibe)"
            h in 11..16 -> "AFTERNOON (Supportive, productive. Encourage breaks and steady small progress)"
            h in 17..20 -> "EVENING (Reflective, encouraging. Warm welcome back, appreciate getting through)"
            h in 21..23 || h < 1 -> "NIGHT (Calm, reflective, comforting. Gratitude, breathing, releasing burden)"
            else -> "BEFORE SLEEP / LATE NIGHT (Peaceful, reassuring. Remind them today is complete, breathe, rest well)"
        }
    }

    private fun buildSystemPrompt(profile: UserProfile?, memories: List<MemoryItem>): String {
        val userName = if (profile?.userName.isNullOrBlank()) "Friend" else profile?.userName
        val mood = profile?.currentMood ?: "Normal"
        val currentTone = getLocalTimeBasedTone()
        val currentAgent = _selectedAgent.value
        val activeDetectedLang = _detectedUserLanguage.value

        val memorySummary = if (memories.isEmpty()) {
            "- No personal memories recorded. Gently explore their goals and habits through conversation turns."
        } else {
            memories.joinToString("\n") { "- [${it.category}] ${it.key}: ${it.value}" }
        }

        return """
${currentAgent.systemPromptExtension}
${currentAgent.name} is not a chatbot. You are a trusted guide, companion, mentor, listener, and daily coach.
The experience should feel warm, human, supportive, inspiring, and deeply personal.

CORE BRAND MESSAGE:
"${currentAgent.name} — Helping You Seek Inner Balance & Grow."

PERSONALITY: Warm and friendly, motivational, emotionally intelligent, encouraging, calm, reassuring, positive but realistic, respectful, non-judgmental, thoughtful listener, growth-oriented.

VOICE RULES (CRITICAL):
- ALWAYS keep responses extremely concise: 1 to 3 sentences maximum.
- Speak naturally like a real person during a phone voice call.
- Avoid robotic, bulleted, or list-like language. Formulate short, voice-friendly comments.
- Ask exactly one thoughtful follow-up question at a time to keep the conversation conversational.
- Avoid long explanations unless specifically requested.
- Match the user's energy and tone (calm, enthusiastic, etc.).

ACTIVE DETECTED USER LANGUAGE CONTEXT (IMPORTANT):
- The user is currently communicating with you in: $activeDetectedLang
- You MUST mirror this choice. If English, respond in English. If Hindi, respond in standard modern Hindi (Devanagari script). If Hinglish (Hindi written in Roman letters like "main thik hu", "kaise ho?"), respond in Hinglish using Roman/Latin letters. Keep the language choice consistent during the conversation turn.

LANGUAGE ADAPTATION RULES:
- Automatically detect the user's language.
- If the user speaks in Hindi, respond in standard modern Hindi (using Hindi Devanagari script).
- If the user speaks in English, respond in English.
- If the user uses Hinglish (Hindi written in Roman/Latin transcript, e.g., "Aap kaise ho?", "mera startup idea hai"), respond in highly natural Hinglish.
- Always mirror the user's language and style. Switch languages immediately when the user switches.

CURRENT STATE & CONTEXT:
- Local Time Period: $currentTone
- User's Name: $userName
- Current User Mood: $mood

MEMORIES YOU HAVE GAINED ABOUT THE USER (Acknowledge and weave these naturally in chat when relevant):
$memorySummary

EMOTIONAL SUPPORT INSTRUCTIONS:
- Stressed: provide calm, grounded reassurance.
- Sad: listen, acknowledge emotion first, offer gentle support.
- Excited: celebrate and double down on positive energy!
- Confused: simplify, capture essence, help create clarity.
- Unmotivated: encourage small, low-friction micro-actions.

Always end with helping people move forward, one concise conversation at a time.
""".trimIndent()
    }

    private fun scanAndExtractMemory(userMessage: String, responseText: String) {
        viewModelScope.launch(Dispatchers.IO) {
            if (!com.aistudio.saarthi.util.NetworkUtils.isNetworkAvailable(getApplication())) {
                Log.w("SAARTHI_GEMINI", "Offline: skipping background memory extraction scan.")
                return@launch
            }

            val extractorPrompt = """
Analyze the dialogue between a user and an AI. Identify personal facts mentioned about the user:
- Goal (e.g., startup ideas, career, learning routines, fitness targets)
- Habit (e.g., waking early, coffee patterns, sleep schedules)
- Interest (e.g., love tech, hiking, chess)
- Preference (e.g., favorite subjects, hates noise)
- Milestone (e.g., finished thesis, got promoted)
- Gratitude (e.g., things the user expresses thankfulness, appreciation, or gratitude for, such as health, a loyal friend, or small daily blessings)
- Name (e.g., user states "Aap mujhe Raj bol sakte ho" or "My name is John")

Output ONLY a JSON array of objects. Keys must be "category" (from: Name, Goal, Habit, Interest, Preference, Milestone, Gratitude, Other), "key" (concept name, max 3 words), and "value" (full factual sentence).
Do NOT include markdown wrapping of JSON. If nothing new, output [].

User message: "$userMessage"
AI Response: "$responseText"
            """.trimIndent()

            val contents = listOf(
                GeminiContent(role = "user", parts = listOf(GeminiPart(text = extractorPrompt)))
            )

            try {
                val jsonResult = GeminiClient.generateResponse(contents, "You are a JSON parser. Output only valid JSON lists.")
                val cleanJson = jsonResult.trim()
                    .removePrefix("```json")
                    .removePrefix("```")
                    .removeSuffix("```")
                    .trim()

                if (cleanJson == "[]" || cleanJson.isBlank()) return@launch

                val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
                val listType = Types.newParameterizedType(List::class.java, ExtractedMemory::class.java)
                val adapter = moshi.adapter<List<ExtractedMemory>>(listType)

                val extracted = adapter.fromJson(cleanJson) ?: emptyList()
                for (item in extracted) {
                    if (item.category.lowercase(Locale.ROOT) == "name") {
                        val curr = repository.userProfile.first() ?: UserProfile()
                        repository.saveProfile(curr.copy(userName = item.value))
                    } else {
                        val mappedCategory = when (item.category.lowercase(Locale.ROOT)) {
                            "goal" -> "Goal"
                            "habit" -> "Habit"
                            "interest" -> "Interest"
                            "preference" -> "Preference"
                            "milestone" -> "Milestone"
                            "gratitude" -> "Gratitude"
                            else -> "Other"
                        }
                        repository.insertMemory(
                            MemoryItem(
                                category = mappedCategory,
                                key = item.key,
                                value = item.value
                            )
                        )
                    }
                    Log.d(TAG, "Memory Extractor registered: $item")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Extraction error: ${e.message}")
            }
        }
    }

    // --- Reflection Mode Features ---

    fun startReflectionListening() {
        ttsEngine?.stop()
        _partialSpeechInput.value = ""
        _voiceAmplitude.value = 0f

        val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
            getApplication(),
            android.Manifest.permission.RECORD_AUDIO
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            _companionState.value = SaarthiState.IDLE
            _errorMessage.value = "Microphone permission is required to use voice input. Please ensure browser/device permissions are granted."
            return
        }

        _companionState.value = SaarthiState.LISTENING

        recognizerEngine = SpeechSpeechRecognizer(
            context = getApplication(),
            onResult = { resultText ->
                _partialSpeechInput.value = ""
                _voiceAmplitude.value = 0f
                _companionState.value = SaarthiState.IDLE
                if (resultText.isNotBlank()) {
                    _reflectionSpeechInput.value = (_reflectionSpeechInput.value + " " + resultText).trim()
                }
            },
            onError = { err ->
                _companionState.value = SaarthiState.IDLE
                _voiceAmplitude.value = 0f
                _errorMessage.value = err
            },
            onPartialResult = { partialText ->
                _partialSpeechInput.value = partialText
            },
            onRmsChanged = { rmsdB ->
                val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                _voiceAmplitude.value = normalized
            }
        )
        recognizerEngine?.startListening()
    }

    fun setReflectionSpeechInput(text: String) {
        _reflectionSpeechInput.value = text
    }

    fun clearReflectionSpeechInput() {
        _reflectionSpeechInput.value = ""
    }

    private fun cleanJsonString(raw: String): String {
        var cleaned = raw.trim()
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring("```json".length)
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring("```".length)
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length - 3)
        }
        return cleaned.trim()
    }

    fun analyzeReflection(text: String, onComplete: (Boolean) -> Unit = {}) {
        if (text.isBlank()) return
        viewModelScope.launch {
            _isGeneratingReflection.value = true
            try {
                if (!com.aistudio.saarthi.util.NetworkUtils.isNetworkAvailable(getApplication())) {
                    _errorMessage.value = "Please check your internet connection."
                    onComplete(false)
                    return@launch
                }
                val systemPrompt = """
                    You are Saarthi, an expert empathetic cognitive behavioral companion. 
                    Analyze the user's long-form personal reflection and produce a JSON object with the following fields:
                    {
                      "detectedFeelings": "A summary of the user's emotional state, sentiments, and specific feelings detected",
                      "keyTakeaways": "A multi-line summary of 3-4 bullet points capturing core insights, situations, or challenges",
                      "actionableSteps": "A list of 2-3 structured next steps, breathing exercises, cognitive shifts or physical action items to support them",
                      "mainMoodEmoji": "Single symbolic emoji representing general emotional state (e.g., 🔥, 🤯, 🩹, 🌀, 💤, 🌱)"
                    }
                    Analyze carefully, remaining warm and supportive. 
                    Return ONLY the raw JSON object. Do NOT include markdown code blocks of json. Keep it strictly raw JSON format.
                """.trimIndent()

                val history = listOf(
                    GeminiContent(role = "user", parts = listOf(GeminiPart(text = "Here is my personal reflection: $text")))
                )

                val rawResult = GeminiClient.generateResponse(history, systemPrompt)
                val jsonString = cleanJsonString(rawResult)
                
                val parsed = try {
                    val moshiObj = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
                    moshiObj.adapter(ReflectionJson::class.java).fromJson(jsonString)
                } catch (e: Exception) {
                    Log.e("SaarthiViewModel", "Failed to parse reflection JSON: $jsonString", e)
                    null
                }

                if (parsed != null) {
                    val summary = ReflectionSummary(
                        originalText = text,
                        detectedFeelings = parsed.detectedFeelings,
                        keyTakeaways = parsed.keyTakeaways,
                        actionableSteps = parsed.actionableSteps,
                        mainMoodEmoji = parsed.mainMoodEmoji
                    )
                    repository.insertReflection(summary)
                    onComplete(true)
                } else {
                    // Fallback in case raw text matches or can't be parsed as JSON:
                    val summary = ReflectionSummary(
                        originalText = text,
                        detectedFeelings = "Detected emotional sentiments from reflection data.",
                        keyTakeaways = "• " + rawResult.take(500),
                        actionableSteps = "• Try writing another reflection with bullet points.\n• Rest and take deep breaths.",
                        mainMoodEmoji = "🌱"
                    )
                    repository.insertReflection(summary)
                    onComplete(true)
                }
            } catch (e: Exception) {
                Log.e("SaarthiViewModel", "Error generating reflection: ${e.message}", e)
                _errorMessage.value = "Failed to analyze reflection: ${e.message}"
                onComplete(false)
            } finally {
                _isGeneratingReflection.value = false
            }
        }
    }

    fun removeReflection(summary: ReflectionSummary) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteReflection(summary)
        }
    }

    fun clearAllReflections() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearAllReflections()
        }
    }

    fun generateGrowthReport() {
        val records = moodRecords.value
        if (records.isEmpty()) {
            _errorMessage.value = "No emotional logs available to analyze."
            return
        }
        viewModelScope.launch {
            _isGeneratingReport.value = true
            _growthReport.value = null
            try {
                val totalLogs = records.size
                val avgClarity = records.map { it.clarity }.average()
                val avgConfidence = records.map { it.confidence }.average()
                
                val moodCounts = records.groupBy { it.mood }.mapValues { it.value.size }
                val dominantMood = moodCounts.maxByOrNull { it.value }?.key ?: "Normal"
                
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
                val logData = records.takeLast(30).joinToString("\n") { record ->
                    val dateStr = sdf.format(java.util.Date(record.timestamp))
                    "- $dateStr: Mood=[${record.mood}], Clarity=${record.clarity}/10, Confidence=${record.confidence}/10. Notes: \"${record.notes}\""
                }

                val systemPrompt = """
                    You are Saarthi, an expert empathic cognitive-behavioral companion and growth coach.
                    The user has been tracking their emotional clarity (Spark Sparkle) and confidence (Wave Shield) on a scale of 1-10.
                    Analyze their emotional state logs and short reflection comments to produce a monthly summary report.
                    Identify key patterns, emotional triggers, cognitive behavior details, and provide next actions.
                    Format your response in beautiful, scannable human-friendly Markdown with clean Emojis, Bullet Points, and bold text headers.
                    The structure should be:
                    
                    # 📈 Emotional Growth & Trend Summary
                    
                    ## 📊 Month-to-Date Performance Stats
                    - **Total Logs recorded:** $totalLogs
                    - **Dominant Mental State:** $dominantMood
                    - **Mean Clarity Level:** ${String.format(java.util.Locale.getDefault(), "%.1f", avgClarity)}/10
                    - **Mean Action Confidence:** ${String.format(java.util.Locale.getDefault(), "%.1f", avgConfidence)}/10
                    
                    ## 🔍 Detected Key Cognitive Patterns
                    [Analyze trends, correlations between clarity & confidence, and what their dominant mood means for their mental focus.]
                    
                    ## 💡 Stress Factors & Positivity Triggers
                    [Highlight linkages found in notes: what triggers higher clarity vs lower confidence, stressful patterns noticed, or positivity sparks.]
                    
                    ## 🧘 Empowering Mindful Recommendations (Next Steps)
                    [Provide 3-4 highly tailored cognitive workouts, breathing patterns, or journaling tasks based on these outputs.]
                """.trimIndent()

                if (!com.aistudio.saarthi.util.NetworkUtils.isNetworkAvailable(getApplication())) {
                    _errorMessage.value = "Failed to compile report: Please check your internet connection."
                    _isGeneratingReport.value = false
                    return@launch
                }

                val promptMsg = """
                    Here is my historical emotional state log data:
                    $logData
                    
                    Please construct the emotional growth summary report now.
                """.trimIndent()

                val history = listOf(
                    GeminiContent(role = "user", parts = listOf(GeminiPart(text = promptMsg)))
                )

                val generatedReport = GeminiClient.generateResponse(history, systemPrompt)
                _growthReport.value = generatedReport
            } catch (e: Exception) {
                Log.e("SaarthiViewModel", "Error generating growth report: ${e.message}", e)
                _errorMessage.value = "Failed to compile growth report: ${e.message}"
            } finally {
                _isGeneratingReport.value = false
            }
        }
    }

    fun clearGrowthReport() {
        _growthReport.value = null
    }

    fun generatePersonalizedPrompts() {
        if (_isGeneratingReflectivePrompts.value) return
        viewModelScope.launch {
            _isGeneratingReflectivePrompts.value = true
            try {
                if (!com.aistudio.saarthi.util.NetworkUtils.isNetworkAvailable(getApplication())) {
                    _isGeneratingReflectivePrompts.value = false
                    return@launch
                }
                // Gather active memories (goals/habits etc.)
                val activeMemories = memories.value
                val memoryStr = if (activeMemories.isEmpty()) {
                    "No goals or habits saved yet."
                } else {
                    activeMemories.filter { it.category in listOf("Goal", "Habit", "Milestone", "Interest") }
                        .joinToString("\n") { "- [${it.category}] ${it.key}: ${it.value}" }
                }

                // Gather past conversation snippets
                val pastMsgs = messages.value.takeLast(10)
                val msgStr = if (pastMsgs.isEmpty()) {
                    "No past conversation history yet."
                } else {
                    pastMsgs.joinToString("\n") { "${it.role.uppercase()}: ${it.text}" }
                }

                val systemPrompt = """
                    You are Saarthi, an expert empathic cognitive-behavioral companion and mindful journaling guide.
                    Your goal is to suggest exactly 4 personalized, highly context-aware reflective journaling prompts for the user to answer.
                    These prompts should be directly relevant to the user's saved goals, habits, or milestones, AND refer back to issues, sentiments, or topics from their past conversation history if any.
                    Make the prompts feel deeply understanding, open-ended, and supportive, guiding the user towards mental clarity, journaling, and self-acceptance.
                    
                    Strict guidelines:
                    1. Generate exactly 4 prompts.
                    2. Output each prompt on a new line.
                    3. Do NOT include numbers, bullet points, asterisks, prefix titles, quotes, or any extra text. Each line must contain ONLY the raw text of the prompt.
                    4. Keep each prompt short, powerful, and easy to respond to (maximum 1-2 sentences).
                """.trimIndent()

                val contextQuery = """
                    Saved Goals & Habits:
                    $memoryStr
                    
                    Recent Chat Messages:
                    $msgStr
                """.trimIndent()

                val history = listOf(
                    GeminiContent(role = "user", parts = listOf(GeminiPart(text = contextQuery)))
                )

                val rawResult = GeminiClient.generateResponse(history, systemPrompt)
                val lines = rawResult.split("\n")
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .map { line ->
                        var cleaned = line
                        // Strip leading stars/hyphens
                        if (cleaned.startsWith("- ") || cleaned.startsWith("* ")) {
                            cleaned = cleaned.substring(2)
                        } else if (cleaned.any { it.isDigit() } && (cleaned.startsWith("1.") || cleaned.startsWith("2.") || cleaned.startsWith("3.") || cleaned.startsWith("4.") || cleaned.startsWith("5."))) {
                            cleaned = cleaned.substring(cleaned.indexOf('.') + 1).trim()
                        }
                        cleaned.trim()
                    }
                    .filter { it.isNotEmpty() }
                    .take(4)

                if (lines.size >= 2) {
                    _reflectivePrompts.value = lines
                }
            } catch (e: Exception) {
                Log.e("SaarthiViewModel", "Error generating reflective prompts", e)
            } finally {
                _isGeneratingReflectivePrompts.value = false
            }
        }
    }

    fun detectLanguageOfInput(text: String): String {
        if (text.isBlank()) return "English"
        
        // 1. Check for Devanagari Unicode characters (Hindi script)
        // Range: \u0900 to \u097F
        val hasDevanagari = text.any { it.code in 0x0900..0x097F }
        if (hasDevanagari) {
            return "Hindi"
        }

        // 2. Check for Hinglish signature words (Hindi in Roman script)
        val hinglishKeywords = setOf(
            "aap", "kaise", "ho", "hai", "mera", "mujhe", "tum", "kya", "na", "hi", 
            "bhai", "yaar", "ka", "ki", "ko", "se", "aur", "sab", "theek", "acha", 
            "achha", "karo", "sakte", "haan", "baat", "kar", "rha", "raha", "rhi", 
            "rahi", "samajh", "baje", "kuch", "kuchh", "ab", "kab", "tab", "gaya", 
            "gayi", "chal", "kam", "jyada", "pyaar", "dost", "shuru", "soch", 
            "rahein", "main", "hum", "humein", "chahiye", "bol", "bolo", "batao",
            "karta", "karti", "karne", "unko", "inko", "apna", "apni", "apne"
        )
        
        val words = text.lowercase()
            .split(Regex("[^a-zA-Z]"))
            .filter { it.isNotBlank() }
            
        val matchCount = words.count { it in hinglishKeywords }
        
        // If there are multiple matches of common Hinglish vocabulary, it is classified as Hinglish
        if (matchCount >= 2 || (words.size <= 4 && matchCount >= 1)) {
            return "Hinglish"
        }

        return "English"
    }

    fun triggerGratitudeCheckIn() {
        _isGratitudeLoggingActive.value = true
        viewModelScope.launch(Dispatchers.IO) {
            val checkInMsg = ChatMessage(
                role = "saarthi",
                text = "Let's log your nightly gratitude. 🌌 What is one thing you are grateful for today? It could be anything big or small."
            )
            repository.insertMessage(checkInMsg)
            
            if (_ttsEnabled.value) {
                ttsEngine?.speak(checkInMsg.text)
            }
        }
    }

    fun cancelGratitudeCheckIn() {
        _isGratitudeLoggingActive.value = false
    }

    override fun onCleared() {
        super.onCleared()
        ttsEngine?.shutdown()
        recognizerEngine?.stopListening()
    }
}
